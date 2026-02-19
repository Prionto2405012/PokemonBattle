package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Loading Screen Controller.
 *
 * Plays Pikachu.mp4 on loop as background.
 * Animates a progress bar from 0 → 100% over ~3 seconds, then navigates to menu.
 */
public class LoadingScreenController {

    @FXML private StackPane rootPane;
    @FXML private MediaView bgVideo;
    @FXML private Rectangle progressFill;
    @FXML private Label loadingLabel;

    private MediaPlayer mediaPlayer;
    private Timeline progressTimeline;

    // Total loading duration in milliseconds
    private static final double LOAD_DURATION_MS = 3000.0;
    // Full width of the progress bar track
    private static final double BAR_WIDTH = 500.0;

    @FXML
    public void initialize() {
        setupVideo();
        setupProgressBar();
        startLoading();
    }

    private void setupVideo() {
        var url = getClass().getResource("/com/example/pokemonbattle/assets/Pikachu.mp4");
        if (url == null) {
            System.err.println("LoadingScreenController: Pikachu.mp4 not found.");
            return;
        }
        mediaPlayer = new MediaPlayer(new Media(url.toExternalForm()));
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setAutoPlay(true);
        bgVideo.setMediaPlayer(mediaPlayer);
        bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
        bgVideo.fitHeightProperty().bind(rootPane.heightProperty());
        bgVideo.setPreserveRatio(false);
    }

    private void setupProgressBar() {
        progressFill.setWidth(0);
        progressFill.setTranslateX(-(BAR_WIDTH / 2.0));
        loadingLabel.setText("Loading... 0%");
    }

    private void startLoading() {
        progressTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(progressFill.widthProperty(), 0, Interpolator.LINEAR),
                new KeyValue(progressFill.translateXProperty(), -(BAR_WIDTH / 2.0), Interpolator.LINEAR)
            ),
            new KeyFrame(Duration.millis(LOAD_DURATION_MS),
                new KeyValue(progressFill.widthProperty(), BAR_WIDTH, Interpolator.LINEAR),
                new KeyValue(progressFill.translateXProperty(), 0, Interpolator.LINEAR)
            )
        );

        progressTimeline.currentTimeProperty().addListener((obs, oldT, newT) -> {
            double pct = Math.min(newT.toMillis() / LOAD_DURATION_MS, 1.0) * 100;
            loadingLabel.setText(String.format("Loading... %d%%", (int) pct));
        });

        progressTimeline.setOnFinished(e -> {
            loadingLabel.setText("Loading... 100%");
            disposeAndNavigate();
        });

        progressTimeline.play();
    }

    private void disposeAndNavigate() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
    }

    public void bindToTask(Task<?> task, Runnable onSuccess) {
        // Stop the auto-play timeline first so it no longer animates the
        // same properties we are about to bind — otherwise JavaFX throws
        // "A bound value cannot be set" on every animation pulse.
        if (progressTimeline != null) {
            progressTimeline.stop();
            progressTimeline = null;
        }

        // Reset bar visuals
        progressFill.setWidth(0);
        progressFill.setTranslateX(-(BAR_WIDTH / 2.0));

        // Bind progress bar width to task progress
        progressFill.widthProperty().bind(task.progressProperty().multiply(BAR_WIDTH));

        // Keep translateX in sync so bar grows from left
        task.progressProperty().addListener((obs, oldP, newP) -> {
            double w = Math.max(0, newP.doubleValue()) * BAR_WIDTH;
            progressFill.setTranslateX(-(BAR_WIDTH - w) / 2.0);
        });

        // Bind label text to task message
        loadingLabel.textProperty().bind(task.messageProperty());

        // Success callback (runs on JavaFX Application Thread)
        task.setOnSucceeded(event -> {
            // Dispose video BEFORE switching scene, or audio leaks into the next scene
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        // Optional: show failure in UI
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            loadingLabel.textProperty().unbind();
            loadingLabel.setText(
                ex != null ? "Failed: " + ex.getMessage() : "Failed to load scene."
            );
        });
    }
}
