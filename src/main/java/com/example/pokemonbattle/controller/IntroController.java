package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * IntroController — plays intro.mp4 and transitions to start.fxml.
 *
 * Flow:
 *   1. Video auto-plays on initialize().
 *   2. currentTimeProperty listener detects the 0.4s-before-end moment.
 *   3. Black overlay fades in smoothly over 380ms (soft fade-to-black).
 *   4. After fade completes: MediaPlayer disposed → start.fxml loaded.
 *
 * No FX-thread blocking at any point.
 */
public class IntroController {

    @FXML private StackPane rootPane;
    @FXML private MediaView mediaView;
    @FXML private Rectangle flashOverlay;

    private MediaPlayer mediaPlayer;

    // How many milliseconds before end to start the fade-to-black
    private static final double TRIGGER_BEFORE_END_MS = 300.0;

    // Guard so the transition fires exactly once
    private volatile boolean transitionTriggered = false;

    @FXML
    public void initialize() {
        // Resolve intro.mp4 from assets — must be on the classpath
        var videoUrl = getClass().getResource(
                "/com/example/pokemonbattle/assets/intro.mp4");

        if (videoUrl == null) {
            // Asset missing — skip straight to start screen
            System.err.println("IntroController: intro.mp4 not found, skipping intro.");
            goToStartScreen();
            return;
        }

        Media media = new Media(videoUrl.toExternalForm());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        // Fit media view to scene dimensions
        mediaView.fitWidthProperty().bind(rootPane.widthProperty());
        mediaView.fitHeightProperty().bind(rootPane.heightProperty());

        // Monitor playback time to trigger flash near end
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (transitionTriggered) return;

            Duration total = mediaPlayer.getTotalDuration();
            if (total == null || total.isUnknown() || total.isIndefinite()) return;

            double remainingMs = total.subtract(newTime).toMillis();
            if (remainingMs <= TRIGGER_BEFORE_END_MS) {
                transitionTriggered = true;
                // Must update UI on FX thread — listener fires on FX thread already
                startFlashTransition();
            }
        });

        // Fallback: if video ends before listener fires
        mediaPlayer.setOnEndOfMedia(this::handleEndOfMedia);

        // Fallback for errors — skip to start
        mediaPlayer.setOnError(() -> {
            Throwable error = mediaPlayer.getError();
            System.err.println("IntroController: MediaPlayer error — " + (error != null ? error.getMessage() : "Unknown error"));
            goToStartScreen();
        });

        mediaPlayer.play();
    }

    /**
     * Called by the MediaPlayer end-of-media event as a safety fallback.
     */
    private void handleEndOfMedia() {
        if (!transitionTriggered) {
            transitionTriggered = true;
            startFlashTransition();
        }
    }

    /**
     * Smoothly fades visual to black AND audio to silence over 380ms in parallel,
     * then switches scene. Starts 0.4s before the video ends.
     * All runs on the FX thread.
     */
    private void startFlashTransition() {
        Duration fadeDuration = Duration.millis(200);

        // Visual: fade black overlay from transparent to opaque
        FadeTransition fadeToBlack = new FadeTransition(fadeDuration, flashOverlay);
        fadeToBlack.setFromValue(0.0);
        fadeToBlack.setToValue(0.9);
        fadeToBlack.setOnFinished(e -> {
            disposeMediaPlayer();
            goToStartScreen();
        });

        // Audio: fade volume from current level to silence in parallel
        if (mediaPlayer != null) {
            Timeline volumeFade = new Timeline(
                new KeyFrame(Duration.ZERO,    new KeyValue(mediaPlayer.volumeProperty(), mediaPlayer.getVolume())),
                new KeyFrame(fadeDuration,     new KeyValue(mediaPlayer.volumeProperty(), 0.0))
            );
            volumeFade.play();
        }

        fadeToBlack.play();
    }

    /**
     * Safely stop and dispose the MediaPlayer.
     * Must be called before switching scenes to free native resources.
     */
    private void disposeMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception e) {
                System.err.println("IntroController: error disposing MediaPlayer — " + e.getMessage());
            } finally {
                mediaPlayer = null;
            }
        }
    }

    /**
     * Switch to the start screen.
     */
    private void goToStartScreen() {
        SceneManager.switchScene("start.fxml", "Pokemon Battle", 1200, 700);
    }
}
