package com.example.pokemonbattle.util;

import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class MusicManager {

    private static final String[] BGM_TRACKS = {
        "/com/example/pokemonbattle/audio/gen1.mp3",
        "/com/example/pokemonbattle/audio/gen2.mp3",
        "/com/example/pokemonbattle/audio/gen3_1.mp3"
    };
    private static final String CLICK   = "/com/example/pokemonbattle/audio/click.mp3";
    private static final String VICTORY = "/com/example/pokemonbattle/audio/victory.mp3";

    /**
     * Single-threaded background executor — all MediaPlayer construction and
     * disposal runs here, never on the JavaFX Application Thread.
     * Mirrors the pattern used by MediaCache.MEDIA_POOL.
     */
    private static final ExecutorService MEDIA_OP = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MusicManager-MediaThread");
        t.setDaemon(true);
        return t;
    });

    private static MusicManager instance;

    private final AtomicReference<MediaPlayer> bgmRef   = new AtomicReference<>();
    private final AtomicReference<MediaPlayer> clickRef = new AtomicReference<>();
    private volatile double  masterVolume    = 0.7;
    private volatile boolean soundEnabled    = true;
    private volatile String  currentTrackPath;

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    public void playRandomBGM() {
        switchBGM(BGM_TRACKS[new Random().nextInt(BGM_TRACKS.length)]);
        if (clickRef.get() == null) {
            MEDIA_OP.execute(this::buildAndStoreClickPlayer);
        }
    }

    public void switchBGM(String resourcePath) {
        currentTrackPath = resourcePath;
        if (soundEnabled) startBGM(resourcePath);
    }

    public void playBGM(String resourcePath) { switchBGM(resourcePath); }

    /** Submits BGM construction + disposal entirely to the media thread. */
    private void startBGM(String path) {
        MEDIA_OP.execute(() -> {
            MediaPlayer old = bgmRef.getAndSet(null);
            if (old != null) { old.stop(); old.dispose(); }

            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("[MusicManager] Not found: " + path);
                return;
            }
            MediaPlayer player = new MediaPlayer(new Media(url.toExternalForm()));
            player.setVolume(masterVolume);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            bgmRef.set(player);

            Runnable tryPlay = () -> {
                if (soundEnabled && bgmRef.get() == player) player.play();
            };
            player.setOnReady(tryPlay);
            if (player.getStatus() == MediaPlayer.Status.READY) tryPlay.run();
        });
    }

    public void stopBGM() {
        MEDIA_OP.execute(() -> {
            MediaPlayer old = bgmRef.getAndSet(null);
            if (old != null) { old.stop(); old.dispose(); }
        });
    }

    public void pauseBGM() {
        MediaPlayer p = bgmRef.get();
        if (p != null) p.pause();
    }

    public void resumeBGM() {
        MediaPlayer p = bgmRef.get();
        if (p != null && soundEnabled) p.play();
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) {
            stopBGM();
        } else {
            if (currentTrackPath != null) startBGM(currentTrackPath);
            else playRandomBGM();
        }
    }

    public void setMasterVolume(double volume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, volume));
        MediaPlayer p = bgmRef.get();
        if (p != null) p.setVolume(this.masterVolume);
    }
    public void setBGMVolume(double v) { setMasterVolume(v); }

    /** Non-click SFX (victory etc.) — constructed on the media thread. */
    public void playSFX(String resourcePath) {
        if (!soundEnabled) return;
        MEDIA_OP.execute(() -> {
            URL url = getClass().getResource(resourcePath);
            if (url == null) return;
            MediaPlayer sfx = new MediaPlayer(new Media(url.toExternalForm()));
            sfx.setVolume(Math.min(masterVolume + 0.2, 1.0));
            sfx.play();
            sfx.setOnEndOfMedia(sfx::dispose);
        });
    }

    /**
     * Click SFX — uses a pre-warmed, reused player for zero latency.
     * seek+play are thread-safe and instant; construction is off-thread.
     */
    public void playClickSFX() {
        if (!soundEnabled) return;
        MediaPlayer p = clickRef.get();
        if (p == null) {
            MEDIA_OP.execute(() -> {
                buildAndStoreClickPlayer();
                MediaPlayer built = clickRef.get();
                if (built != null) { built.seek(Duration.ZERO); built.play(); }
            });
            return;
        }
        p.setVolume(masterVolume * 0.4);
        p.seek(Duration.ZERO);
        p.play();
    }

    private void buildAndStoreClickPlayer() {
        URL url = getClass().getResource(CLICK);
        if (url == null) return;
        MediaPlayer p = new MediaPlayer(new Media(url.toExternalForm()));
        p.setVolume(masterVolume * 0.4);
        p.setCycleCount(1);
        clickRef.set(p);
        System.out.println("[MusicManager] Click player pre-warmed on media thread.");
    }

    public void playVictorySFX() { playSFX(VICTORY); }

    public void attachClickSounds(Parent root) { walkButtons(root); }

    private void walkButtons(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button btn && !"click-wired".equals(btn.getUserData())) {
                var existingAction = btn.getOnAction();
                btn.setOnAction(e -> {
                    playClickSFX();
                    if (existingAction != null) existingAction.handle(e);
                });
                if (btn.getUserData() == null) btn.setUserData("click-wired");
            }
            if (child instanceof Parent p) walkButtons(p);
        }
    }

    public boolean isSoundEnabled()  { return soundEnabled;      }
    public double  getMasterVolume() { return masterVolume;      }
    public double  getBGMVolume()    { return masterVolume;      }
    public String  getCurrentTrack() { return currentTrackPath;  }
}