package com.example.pokemonbattle.util;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.scene.image.Image;
import javafx.scene.media.Media;

/**
 * Application-wide media pre-loader and cache.
 *
 * <p>Call {@link #preload()} once from {@code HelloApplication.start()} before
 * the first scene switch.  It immediately spawns a thread-pool that loads
 * every listed asset in parallel in the background.  By the time the intro
 * video finishes and the start scene appears, all images are already decoded
 * in memory — removing every visible first-frame stutter or blank-screen
 * flash caused on-demand I/O.</p>
 *
 * <h3>Images &amp; GIFs</h3>
 * Each asset is loaded on its own pool thread using the single-arg
 * {@code new Image(url)} constructor — the ONLY form that reliably decodes
 * all GIF animation frames in every JavaFX version.  The loads run in
 * parallel so the total wait time is bounded by the largest individual asset,
 * not the sum of all of them.
 *
 * <h3>Media (MP4) assets</h3>
 * {@link Media} objects are lightweight URL + metadata holders.  The actual
 * video data is streamed by the native media pipeline when a
 * {@link javafx.scene.media.MediaPlayer} is created.  We cache the resolved
 * URL string; each controller creates its own fresh {@code MediaPlayer} from
 * it as normal.
 */
public final class MediaCache {

    /* ------------------------------------------------------------------ */
    /*  Constants                                                           */
    /* ------------------------------------------------------------------ */

    private static final String BASE = "/com/example/pokemonbattle/assets/";

    /**
     * Static image / GIF assets to pre-load in parallel.
     * Add any new asset file names here to include them in the warm-up pass.
     */
    private static final String[] IMAGE_ASSETS = {
        "start.gif",
        "wc_bg.png",
        "menu1.png",
        "menu2.png",
        "menu3.png",
        "new_game.png",
        "Arkhai.png",
        "pokeball.gif",
        "pokemon_txt.jpg",
    };

    /** MP4 assets whose URL should be resolved and cached at startup. */
    private static final String[] MEDIA_ASSETS = {
        "intro.mp4",
        "Pikachu.mp4",
    };

    /* ------------------------------------------------------------------ */
    /*  Internal caches                                                     */
    /* ------------------------------------------------------------------ */

    private static final Map<String, Image>             IMAGE_CACHE   = new ConcurrentHashMap<>();
    private static final Map<String, String>            MEDIA_URLS    = new ConcurrentHashMap<>();
    /**
     * In-flight futures kept so getImage() can .join() on a cache miss
     * rather than starting a second redundant load.
     */
    private static final Map<String, CompletableFuture<Image>> IN_FLIGHT = new ConcurrentHashMap<>();

    /** Thread pool: one thread per image so all loads run in parallel. */
    private static final ExecutorService POOL =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "MediaCache-ImageLoader");
                t.setDaemon(true);
                return t;
            });

    private MediaCache() { /* utility class — no instances */ }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Starts the background pre-load pass.  Returns immediately.
     * Safe to call from the JavaFX Application Thread.
     */
    public static void preload() {
        // Media URL resolution runs fast — do it on the calling thread
        resolveMediaUrls();
        // Image loads are parallel, each on its own pool thread
        for (String name : IMAGE_ASSETS) {
            scheduleImageLoad(name);
        }
        System.out.println("[MediaCache] Parallel pre-load dispatched for "
                + IMAGE_ASSETS.length + " image(s).");
    }

    /**
     * Returns the pre-loaded {@link Image} for the given asset file name.
     * <p>If the load is still in progress this call blocks the calling thread
     * (briefly — never on the FX thread for startup assets since the intro
     * video gives ample warm-up time).  If the asset was never queued it is
     * loaded synchronously as a fallback.</p>
     *
     * @param assetName file name relative to {@code assets/}, e.g. {@code "start.gif"}
     * @return the {@link Image}, or {@code null} if the asset doesn't exist
     */
    public static Image getImage(String assetName) {
        // Happy path: already fully loaded
        Image cached = IMAGE_CACHE.get(assetName);
        if (cached != null) return cached;

        // In-flight: wait for the background load to finish
        CompletableFuture<Image> future = IN_FLIGHT.get(assetName);
        if (future != null) {
            System.out.println("[MediaCache] Waiting for in-flight load: " + assetName);
            return future.join(); // blocks until done; returns null on error
        }

        // Total cache miss (asset was not listed in IMAGE_ASSETS)
        System.err.println("[MediaCache] Cache miss — loading synchronously: " + assetName);
        return loadImageBlocking(assetName);
    }

    /**
     * Returns a {@link Media} object for the given asset file name.
     * A new {@code Media} instance is always returned so each caller can
     * create its own {@link javafx.scene.media.MediaPlayer} independently.
     *
     * @param assetName file name relative to {@code assets/}, e.g. {@code "intro.mp4"}
     * @return a fresh {@link Media}, or {@code null} if the asset doesn't exist
     */
    public static Media getMedia(String assetName) {
        String url = MEDIA_URLS.get(assetName);
        if (url != null) return new Media(url);

        // Fallback: resolve on demand
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

    /* ------------------------------------------------------------------ */
    /*  Internal helpers                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Submits a single image load to the thread pool and registers the future
     * in IN_FLIGHT.  When the load completes the result moves to IMAGE_CACHE.
     */
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

    /**
     * Loads a single image synchronously on the <em>calling</em> thread.
     * Uses the single-arg {@code new Image(url)} constructor — the only form
     * that reliably decodes all GIF animation frames in every JavaFX version.
     * Safe to call from any non-FX thread.
     */
    @SuppressWarnings("CallToThreadRun")
    private static Image loadImageBlocking(String name) {
        URL url = MediaCache.class.getResource(BASE + name);
        if (url == null) {
            System.err.println("[MediaCache] Not found on classpath: " + name);
            return null;
        }
        // new Image(String) — blocking, full GIF frame decode
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

    /** Resolves all MP4 URL strings synchronously (fast — no I/O). */
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
