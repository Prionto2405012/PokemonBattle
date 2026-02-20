package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.CurtainTransitionManager;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class IntroController {

    @FXML private StackPane rootPane;
    @FXML private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private static final double TRIGGER_BEFORE_END_MS = 800.0;
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
        if (mediaPlayer != null) {
            Duration audioFadeDuration = Duration.millis(80);
            Timeline volumeFade = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(mediaPlayer.volumeProperty(), mediaPlayer.getVolume())),
                new KeyFrame(audioFadeDuration,    new KeyValue(mediaPlayer.volumeProperty(), 0.0))
            );
            volumeFade.setOnFinished(e -> {
                disposeMediaPlayer();
                launchTransition();
            });
            volumeFade.play();
        } else {
            launchTransition();
        }
    }
    private void launchTransition() {
        CurtainTransitionManager.executeCurtainTransition(rootPane, () -> {
            // Switch scene while screen is fully black
            goToStartScreen();
            // Platform.runLater ensures the new scene has completed its first layout
            // pass before we read rootPane.getHeight() for the rise animation
            javafx.application.Platform.runLater(() -> {
                javafx.scene.layout.Pane newRoot =
                    (javafx.scene.layout.Pane) SceneManager.getPrimaryStage().getScene().getRoot();
                CurtainTransitionManager.riseOn(newRoot);
            });
        });
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
