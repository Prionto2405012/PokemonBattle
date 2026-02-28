package com.example.pokemonbattle.util;

import java.net.URL;
import java.util.Random;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MusicManager {

    /** Available BGM tracks (classpath-relative). */
    private static final String[] BGM_TRACKS = {
        "/com/example/pokemonbattle/audio/gen1.mp3",
        "/com/example/pokemonbattle/audio/gen2.mp3",
        "/com/example/pokemonbattle/audio/gen3.mp3"
    };

    private static MusicManager instance;
    private MediaPlayer bgmPlayer;       // current BGM player
    private double masterVolume = 0.7;   // master volume 0.0–1.0
    private boolean soundEnabled = true;
    private String currentTrackPath;     // last selected track path

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    // ── BGM ──────────────────────────────────────────────────────────────────

    /**
     * Pick a random track from BGM_TRACKS, store it, and play it
     * (only if sound is enabled).
     */
    public void playRandomBGM() {
        String track = BGM_TRACKS[new Random().nextInt(BGM_TRACKS.length)];
        switchBGM(track);
    }

    /**
     * Store {@code resourcePath} as the current track. If sound is currently
     * enabled, stop any existing BGM and start the new one.
     */
    public void switchBGM(String resourcePath) {
        currentTrackPath = resourcePath;
        if (!soundEnabled) return;
        startBGM(resourcePath);
    }

    /** Backward-compatible alias for {@link #switchBGM(String)}. */
    public void playBGM(String resourcePath) {
        switchBGM(resourcePath);
    }

    /** Internal: dispose old player, create & start new one. */
    private void startBGM(String resourcePath) {
        stopBGM(); // dispose previous player first to avoid overlap / leak
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            System.err.println("[MusicManager] Audio not found: " + resourcePath);
            return;
        }
        bgmPlayer = new MediaPlayer(new Media(url.toExternalForm()));
        bgmPlayer.setVolume(masterVolume);
        bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        bgmPlayer.play();
    }

    // ── SFX ──────────────────────────────────────────────────────────────────

    /** Play a one-shot sound effect. Creates a short-lived MediaPlayer, auto-disposes. */
    public void playSFX(String resourcePath) {
        if (!soundEnabled) return;
        URL url = getClass().getResource(resourcePath);
        if (url == null) return;
        MediaPlayer sfx = new MediaPlayer(new Media(url.toExternalForm()));
        sfx.setVolume(0.9);
        sfx.play();
        sfx.setOnEndOfMedia(sfx::dispose);
    }

    // ── Playback control ─────────────────────────────────────────────────────

    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
    }

    public void pauseBGM() {
        if (bgmPlayer != null) bgmPlayer.pause();
    }

    public void resumeBGM() {
        if (bgmPlayer != null && soundEnabled) bgmPlayer.play();
    }

    // ── Volume ───────────────────────────────────────────────────────────────

    /**
     * Set the master volume (clamped to 0.0–1.0). Updates the live player
     * immediately if one is running.
     */
    public void setMasterVolume(double volume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, volume));
        if (bgmPlayer != null) bgmPlayer.setVolume(this.masterVolume);
    }

    /** Backward-compatible alias for {@link #setMasterVolume(double)}. */
    public void setBGMVolume(double volume) {
        setMasterVolume(volume);
    }

    // ── Sound on/off ─────────────────────────────────────────────────────────

    /**
     * Enable or disable all sound.
     * <ul>
     *   <li>OFF → stops and disposes the BGM player immediately.</li>
     *   <li>ON  → resumes the previously stored track; if none, picks randomly.</li>
     * </ul>
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) {
            stopBGM();
        } else {
            if (currentTrackPath != null) {
                startBGM(currentTrackPath);
            } else {
                playRandomBGM();
            }
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public boolean isSoundEnabled()  { return soundEnabled;    }
    public double  getMasterVolume() { return masterVolume;    }
    /** Backward-compatible alias. */
    public double  getBGMVolume()    { return masterVolume;    }
    public String  getCurrentTrack() { return currentTrackPath; }
}