package com.example.pokemonbattle.util;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NodeList;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public final class MediaCache {
    private static final String BASE = "/com/example/pokemonbattle/assets/";
    private static final String[] IMAGE_ASSETS = { "wc_bg.png", "menu1.png", "new_game.png", "fight.png", "battle2.jpg"};
    private static final String[] MEDIA_ASSETS = { "intro.mp4", "start.mp4", "Pikachu.mp4", "Pokeball loading animation.mp4" };

    public record GifFrameData(WritableImage[] frames, long[] delaysMs) {
        public boolean isEmpty() {
            return frames == null || frames.length == 0;
        }
    }

    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> MEDIA_URLS = new ConcurrentHashMap<>();
    private static final Map<String, GifFrameData> GIF_FRAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, MediaPlayer> PLAYER_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, CompletableFuture<Image>> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<GifFrameData>> GIF_IN_FLIGHT = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<MediaPlayer>> PLAYER_IN_FLIGHT = new ConcurrentHashMap<>();
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "MediaCache-ImageLoader");
        t.setDaemon(true);
        return t;
    });
    private static final ExecutorService MEDIA_POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MediaCache-MediaBuilder");
        t.setDaemon(true);
        return t;
    });

    private MediaCache() {}
    public static void preload() {
        resolveMediaUrls();
        for (String name : IMAGE_ASSETS) {
            scheduleImageLoad(name);
            if (name.endsWith(".gif")) {
                scheduleGifDecode(name); 
            }
        }
        for (String name : MEDIA_ASSETS) {
            scheduleMediaPlayerBuild(name);
        }
        System.out.println("[MediaCache] Parallel pre-load dispatched for "+ IMAGE_ASSETS.length + " image(s) and "+ MEDIA_ASSETS.length + " media player(s).");
    }
    public static Image getImage(String assetName) {
        Image cached = IMAGE_CACHE.get(assetName);
        if (cached != null)
            return cached;
        CompletableFuture<Image> future = IN_FLIGHT.get(assetName);
        if (future != null) {
            System.out.println("[MediaCache] Waiting for in-flight load: " + assetName);
            return future.join(); 
        }
        System.err.println("[MediaCache] Cache miss — loading synchronously: " + assetName);
        return loadImageBlocking(assetName);
    }
    public static Media getMedia(String assetName) {
        String url = MEDIA_URLS.get(assetName);
        if (url != null)
            return new Media(url);
        System.err.println("[MediaCache] Media URL cache miss — resolving: " + assetName);
        URL resource = MediaCache.class.getResource(BASE + assetName);
        if (resource == null) {
            System.err.println("[MediaCache] Media not found on classpath: " + assetName);
            return null;
        }
        String externalUrl = resource.toExternalForm();
        MEDIA_URLS.put(assetName, externalUrl);
        return new Media(externalUrl);
    }
    private static void scheduleImageLoad(String name) {
        CompletableFuture<Image> future = CompletableFuture
                .supplyAsync(() -> loadImageBlocking(name), POOL)
                .whenComplete((img, ex) -> {
                    IN_FLIGHT.remove(name);
                    if (ex != null) {
                        System.err.println("[MediaCache] Load failed: " + name
                                + " — " + ex.getMessage());
                    }
                });
        IN_FLIGHT.put(name, future);
    }

    @SuppressWarnings("CallToThreadRun")
    private static Image loadImageBlocking(String name) {
        URL url = MediaCache.class.getResource(BASE + name);
        if (url == null) {
            System.err.println("[MediaCache] Not found on classpath: " + name);
            return null;
        }
        Image img = new Image(url.toExternalForm());
        if (img.isError()) {
            System.err.println("[MediaCache] Decode error: " + name);
            return null;
        }
        IMAGE_CACHE.put(name, img);
        System.out.printf("[MediaCache] Loaded: %-25s  %.0f×%.0f%n",
                name, img.getWidth(), img.getHeight());
        return img;
    }
    public static GifFrameData getGifFrames(String assetName) {
        GifFrameData cached = GIF_FRAME_CACHE.get(assetName);
        if (cached != null)
            return cached;

        CompletableFuture<GifFrameData> future = GIF_IN_FLIGHT.get(assetName);
        if (future != null) {
            System.out.println("[MediaCache] Waiting for in-flight GIF decode: " + assetName);
            return future.join();
        }

        System.err.println("[MediaCache] GIF frame cache miss — decoding synchronously: " + assetName);
        return decodeGifFrames(assetName);
    }

    private static void scheduleGifDecode(String name) {
        CompletableFuture<GifFrameData> future = CompletableFuture
                .supplyAsync(() -> decodeGifFrames(name), POOL)
                .whenComplete((data, ex) -> {
                    GIF_IN_FLIGHT.remove(name);
                    if (ex != null)
                        System.err.println("[MediaCache] GIF decode failed: " + name + " — " + ex.getMessage());
                });
        GIF_IN_FLIGHT.put(name, future);
    }

    private static GifFrameData decodeGifFrames(String name) {
        URL url = MediaCache.class.getResource(BASE + name);
        if (url == null)
            return new GifFrameData(new WritableImage[0], new long[0]);
        try (ImageInputStream stream = ImageIO.createImageInputStream(url.openStream())) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(stream, false);
            int count = reader.getNumImages(true);
            WritableImage[] frames = new WritableImage[count];
            long[] delays = new long[count];
            for (int i = 0; i < count; i++) {
                BufferedImage buf = reader.read(i);
                frames[i] = SwingFXUtils.toFXImage(buf, null);
                long delayMs = 100; 
                try {
                    IIOMetadata meta = reader.getImageMetadata(i);
                    IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(meta.getNativeMetadataFormatName());
                    NodeList gces = root.getElementsByTagName("GraphicControlExtension");
                    if (gces.getLength() > 0) {
                        String dt = ((IIOMetadataNode) gces.item(0)).getAttribute("delayTime");
                        long raw = Long.parseLong(dt) * 10L; // centiseconds → ms
                        delayMs = raw > 0 ? raw : 100;
                    }
                } catch (IllegalArgumentException ignored) {
                }
                delays[i] = delayMs;
            }
            reader.dispose();
            GifFrameData data = new GifFrameData(frames, delays);
            GIF_FRAME_CACHE.put(name, data);
            System.out.printf("[MediaCache] GIF decoded: %-25s  %d frames%n", name, count);
            return data;
        } catch (Exception e) {
            System.err.println("[MediaCache] GIF decode error: " + name + " — " + e.getMessage());
            return new GifFrameData(new WritableImage[0], new long[0]);
        }
    }
    public static MediaPlayer claimMediaPlayer(String assetName) {
        MediaPlayer player = PLAYER_CACHE.remove(assetName);
        if (player != null) {
            if (player.getError() == null) {
                return player;
            }
            System.err.println("[MediaCache] Discarding errored cached player for " + assetName + ", rebuilding.");
            try {
                player.dispose();
            } catch (Exception ignored) {
            }
            return buildMediaPlayer(assetName);
        }
        CompletableFuture<MediaPlayer> future = PLAYER_IN_FLIGHT.remove(assetName);
        if (future != null) {
            System.out.println("[MediaCache] Waiting for in-flight MediaPlayer: " + assetName);
            MediaPlayer inFlightPlayer = future.join();
            if (inFlightPlayer == null) {
                return null;
            }
            if (inFlightPlayer.getError() == null) {
                return inFlightPlayer;
            }
            System.err.println("[MediaCache] In-flight player errored for " + assetName + ", rebuilding.");
            try {
                inFlightPlayer.dispose();
            } catch (Exception ignored) {
            }
            return buildMediaPlayer(assetName);
        }
        System.err.println("[MediaCache] MediaPlayer cache miss — building now: " + assetName);
        return buildMediaPlayer(assetName);
    }
    public static void buildVideoPlayer(String assetName, Consumer<MediaPlayer> onReady) {
    String url = MEDIA_URLS.get(assetName); 
    if (url != null) {
        constructAndWait(url, onReady);
    } else {
        CompletableFuture.supplyAsync(() -> {
            URL resource = MediaCache.class.getResource(BASE + assetName);
            if (resource == null) {
                System.err.println("[MediaCache] Video not found: " + assetName);
                return null;
            }
            String ext = resource.toExternalForm();
            MEDIA_URLS.put(assetName, ext);
            return ext;
        }, POOL).thenAcceptAsync(resolvedUrl -> {
            if (resolvedUrl != null) constructAndWait(resolvedUrl, onReady);
        }, Platform::runLater);
    }
}
private static void constructAndWait(String url, Consumer<MediaPlayer> onReady) {
    MediaPlayer player = new MediaPlayer(new Media(url));
    player.setAutoPlay(false);
    player.setOnReady(() -> {
        System.out.println("[MediaCache] Video READY: " + url);
        onReady.accept(player);
    });
    player.setOnError(() -> {
        Throwable error = player.getError();
        System.err.println("[MediaCache] Video error: "
            + (error != null ? error.getMessage() : "unknown"));
    });
}
    private static void scheduleMediaPlayerBuild(String name) {
        CompletableFuture<MediaPlayer> future = CompletableFuture
                .supplyAsync(() -> buildMediaPlayer(name), MEDIA_POOL)
                .whenComplete((mp, ex) -> {
                    PLAYER_IN_FLIGHT.remove(name);
                    if (mp != null)
                        PLAYER_CACHE.put(name, mp);
                    if (ex != null)
                        System.err.println("[MediaCache] Player build failed: " + name + " — " + ex.getMessage());
                });
        PLAYER_IN_FLIGHT.put(name, future);
    }
    private static MediaPlayer buildMediaPlayer(String name) {
        String url = MEDIA_URLS.get(name);
        if (url == null) {
            URL resource = MediaCache.class.getResource(BASE + name);
            if (resource == null) {
                System.err.println("[MediaCache] Media not found for player: " + name);
                return null;
            }
            url = resource.toExternalForm();
        }
        MediaPlayer player = new MediaPlayer(new Media(url));
        player.setAutoPlay(false);
        player.setOnError(() -> {
            Throwable error = player.getError();
            System.err.println("[MediaCache] Pre-built player error for " + name + ": "
                + (error != null ? error.getMessage() : "unknown"));
        });
        System.out.println("[MediaCache] Pre-built MediaPlayer: " + name);
        return player;
    }
    private static void resolveMediaUrls() {
        for (String name : MEDIA_ASSETS) {
            URL url = MediaCache.class.getResource(BASE + name);
            if (url == null) {
                System.err.println("[MediaCache] Media not found: " + name);
            } else {
                MEDIA_URLS.put(name, url.toExternalForm());
                System.out.println("[MediaCache] Media URL cached: " + name);
            }
        }
    }
}
