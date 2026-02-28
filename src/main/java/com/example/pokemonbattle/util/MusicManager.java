package com.example.pokemonbattle.util;

import java.net.URL;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MusicManager {

    private static MusicManager instance;
    private MediaPlayer bgmPlayer;   // background music
    private MediaPlayer sfxPlayer;   // one-shot sound effects
    private double bgmVolume = 0.7;
    private boolean soundEnabled = true;

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }
    public void playBGM(String resourcePath) {
        if (!soundEnabled) return;

        stopBGM(); 

        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            System.err.println("Audio not found: " + resourcePath);
            return;
        }

        bgmPlayer = new MediaPlayer(new Media(url.toExternalForm()));
        bgmPlayer.setVolume(bgmVolume);
        bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop forever
        bgmPlayer.play();
    }

    /** Play a one-shot sound effect (attack, click, etc.) */
    public void playSFX(String resourcePath) {
        if (!soundEnabled) return;

        URL url = getClass().getResource(resourcePath);
        if (url == null) return;

        MediaPlayer sfx = new MediaPlayer(new Media(url.toExternalForm()));
        sfx.setVolume(0.9);
        sfx.play();
        // Auto-dispose when done
        sfx.setOnEndOfMedia(sfx::dispose);
    }

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
    public void setBGMVolume(double volume) {
        this.bgmVolume = volume;
        if (bgmPlayer != null) bgmPlayer.setVolume(volume);
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) pauseBGM();
        else resumeBGM();
    }

    public boolean isSoundEnabled() { return soundEnabled; }
    public double getBGMVolume()    { return bgmVolume; }
}