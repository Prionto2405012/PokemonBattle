package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class IntroController {

    @FXML private StackPane rootPane;
    @FXML private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private static final double TRIGGER_BEFORE_END_MS = 1000.0;
    private volatile boolean transitionTriggered = false;

    @FXML
    public void initialize() {
        // (b/c) Claim the pre-built, autoPlay=false player instead of
        //       constructing one on the FX thread every time.
        mediaPlayer = com.example.pokemonbattle.util.MediaCache.claimMediaPlayer("intro.mp4");

        if (mediaPlayer == null) {
            System.err.println("IntroController: intro.mp4 player unavailable, skipping intro.");
            goToStartScreen();
            return;
        }

        mediaView.setMediaPlayer(mediaPlayer);
        mediaView.fitWidthProperty().bind(rootPane.widthProperty());
        mediaView.fitHeightProperty().bind(rootPane.heightProperty());

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (transitionTriggered) return;

            Duration total = mediaPlayer.getTotalDuration();
            if (total == null || total.isUnknown() || total.isIndefinite()) return;

            double remainingMs = total.subtract(newTime).toMillis();
            if (remainingMs <= TRIGGER_BEFORE_END_MS) {
                transitionTriggered = true;
                startFlashTransition();
            }
        });
        mediaPlayer.setOnEndOfMedia(this::handleEndOfMedia);
        mediaPlayer.setOnError(() -> {
            Throwable error = mediaPlayer.getError();
            System.err.println("IntroController: MediaPlayer error — " + (error != null ? error.getMessage() : "Unknown error"));
            goToStartScreen();
        });

        // (b) Play only once the player signals it is ready to render
        mediaPlayer.setOnReady(mediaPlayer::play);
    }
    private void handleEndOfMedia() {
        if (!transitionTriggered) {
            transitionTriggered = true;
            startFlashTransition();
        }
    }
    private void startFlashTransition() {
        // Black overlay fades in over 700 ms
        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.BLACK);
        overlay.widthProperty().bind(rootPane.widthProperty());
        overlay.heightProperty().bind(rootPane.heightProperty());
        overlay.setOpacity(0);
        overlay.setManaged(false);
        rootPane.getChildren().add(overlay);

        // Fade audio out in parallel with the visual fade
        if (mediaPlayer != null) {
            Timeline volumeFade = new Timeline(
                new KeyFrame(Duration.ZERO,         new KeyValue(mediaPlayer.volumeProperty(), mediaPlayer.getVolume())),
                new KeyFrame(Duration.millis(700),  new KeyValue(mediaPlayer.volumeProperty(), 0.0))
            );
            volumeFade.play();
        }

        FadeTransition fadeToBlack = new FadeTransition(Duration.millis(700), overlay);
        fadeToBlack.setFromValue(0.0);
        fadeToBlack.setToValue(1.0);
        fadeToBlack.setOnFinished(e -> {
            disposeMediaPlayer();
            goToStartScreen(); // switch while screen is fully black
        });
        fadeToBlack.play();
    }
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
    private void goToStartScreen() {
        SceneManager.switchScene("start.fxml", "Pokemon Battle", 1200, 700);
    }
}
