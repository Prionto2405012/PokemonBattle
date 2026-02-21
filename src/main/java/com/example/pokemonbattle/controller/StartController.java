package com.example.pokemonbattle.controller;

import java.util.concurrent.atomic.AtomicBoolean;

import com.example.pokemonbattle.util.MediaCache;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class StartController {

    @FXML private StackPane rootPane;
    @FXML private MediaView  bgVideo;
    @FXML private Region     vignetteOverlay;

    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        // Claim the pre-built, autoPlay=false player
        mediaPlayer = MediaCache.claimMediaPlayer("start.mp4");

        if (mediaPlayer != null) {
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgVideo.setMediaPlayer(mediaPlayer);
            bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
            bgVideo.fitHeightProperty().bind(rootPane.heightProperty());
            bgVideo.setPreserveRatio(false);

            // Play only once the player signals it is ready to render.
            // Also check immediately — pre-warmed players are already READY
            // and setOnReady() won't fire again after the transition has passed.
            mediaPlayer.setOnReady(() -> {
                if (mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                    mediaPlayer.play();
                }
            });
            if (mediaPlayer.getStatus() == MediaPlayer.Status.READY
                    || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
                mediaPlayer.play();
            }
        } else {
            System.err.println("StartController: start.mp4 player unavailable.");
        }

        if (vignetteOverlay != null && rootPane != null) {
            vignetteOverlay.prefWidthProperty().bind(rootPane.widthProperty());
            vignetteOverlay.prefHeightProperty().bind(rootPane.heightProperty());
        }
        // Reveal handled by CurtainTransitionManager.riseOn() in IntroController
    }
    @FXML
    protected void onStartButtonClick() {
        javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle();
        overlay.setFill(javafx.scene.paint.Color.BLACK);
        overlay.widthProperty().bind(rootPane.widthProperty());
        overlay.heightProperty().bind(rootPane.heightProperty());
        overlay.setManaged(false);
        overlay.setOpacity(0);
        rootPane.getChildren().add(overlay);
        AtomicBoolean switched = new AtomicBoolean(false);
        FadeTransition phase1 = new FadeTransition(Duration.millis(60), overlay);
        phase1.setFromValue(0.0);
        phase1.setToValue(0.7);
        phase1.setInterpolator(Interpolator.EASE_IN);
        FadeTransition phase2 = new FadeTransition(Duration.millis(40), overlay);
        phase2.setFromValue(0.7);
        phase2.setToValue(0.9);
        phase2.setInterpolator(Interpolator.EASE_OUT);
        phase2.setOnFinished(e -> {
            if (!switched.compareAndSet(false, true)) return;
            overlay.widthProperty().unbind();
            overlay.heightProperty().unbind();
            // Stop and dispose the looping video before switching scene
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            SceneManager.switchScene("wc.fxml", "Pokemon Battle", 1200, 700);
        });

        new SequentialTransition(phase1, phase2).play();
    }
}
