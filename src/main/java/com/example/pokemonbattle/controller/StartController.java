package com.example.pokemonbattle.controller;

import java.util.concurrent.atomic.AtomicBoolean;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * Controller for the Start Screen.
 * Background is start.mp4 (looping video). Start button transitions to the Loading screen.
 */
@SuppressWarnings("unused")
public class StartController {

    @FXML private StackPane rootPane;
    @FXML private MediaView bgVideo;

    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        java.net.URL videoUrl = getClass().getResource(
                "/com/example/pokemonbattle/assets/start.mp4");

        if (videoUrl != null && bgVideo != null && rootPane != null) {
            Media media = new Media(videoUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop forever
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setMute(true);
            bgVideo.setMediaPlayer(mediaPlayer);

            bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
            bgVideo.fitHeightProperty().bind(rootPane.heightProperty().subtract(8));
            bgVideo.setPreserveRatio(false);
        }

        // Phase 3: start scene fades in from black (completes the 3-phase transition)
        if (rootPane != null) {
            javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle();
            overlay.setFill(javafx.scene.paint.Color.BLACK);
            overlay.widthProperty().bind(rootPane.widthProperty());
            overlay.heightProperty().bind(rootPane.heightProperty());
            overlay.setManaged(false);
            overlay.setOpacity(1.0);
            rootPane.getChildren().add(overlay);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
            fadeIn.setFromValue(1.0);
            fadeIn.setToValue(0.0);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            fadeIn.setOnFinished(e -> {
                overlay.widthProperty().unbind();
                overlay.heightProperty().unbind();
                rootPane.getChildren().remove(overlay);
            });
            fadeIn.play();
        }
    }

    /**
     * Start button click — 3-phase transition:
     *   Phase 1 (200ms): Start scene slowly darkens (overlay 0 → 0.6)
     *   Phase 2 (150ms): Goes fully black     (overlay 0.6 → 1.0)
     *   Phase 3 runs in WcController.initialize() after the scene switch
     */
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

        // Phase 1: start scene slowly darkens
        FadeTransition phase1 = new FadeTransition(Duration.millis(60), overlay);
        phase1.setFromValue(0.0);
        phase1.setToValue(0.8);
        phase1.setInterpolator(Interpolator.EASE_IN);

        // Phase 2: screen goes fully black
        FadeTransition phase2 = new FadeTransition(Duration.millis(40), overlay);
        phase2.setFromValue(0.8);
        phase2.setToValue(0.9);
        phase2.setInterpolator(Interpolator.EASE_OUT);
        phase2.setOnFinished(e -> {
            if (!switched.compareAndSet(false, true)) return; // guard against re-entry
            // Unbind before scene switch to prevent resize from re-firing this handler
            overlay.widthProperty().unbind();
            overlay.heightProperty().unbind();
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
