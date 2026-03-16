package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

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

        MusicManager.getInstance().attachClickSounds(rootPane);
    }

    private void setupVideo() {
        mediaPlayer = com.example.pokemonbattle.util.MediaCache.claimMediaPlayer("Pikachu.mp4");

        if (mediaPlayer == null) {
            System.err.println("LoadingScreenController: Pikachu.mp4 player unavailable.");
            return;
        }
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        bgVideo.setMediaPlayer(mediaPlayer);
        bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
        bgVideo.fitHeightProperty().bind(rootPane.heightProperty());
        bgVideo.setPreserveRatio(false);
        mediaPlayer.setOnReady(() -> {
            if (mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                mediaPlayer.play();
            }
        });
        if (mediaPlayer.getStatus() == MediaPlayer.Status.READY
                && mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            mediaPlayer.play();
        }
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
        if (progressTimeline != null) {
            progressTimeline.stop();
            progressTimeline = null;
        }
        progressFill.setWidth(0);
        progressFill.setTranslateX(-(BAR_WIDTH / 2.0));
        progressFill.widthProperty().bind(task.progressProperty().multiply(BAR_WIDTH));
        task.progressProperty().addListener((obs, oldP, newP) -> {
            double w = Math.max(0, newP.doubleValue()) * BAR_WIDTH;
            progressFill.setTranslateX(-(BAR_WIDTH - w) / 2.0);
        });
        loadingLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(event -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            loadingLabel.textProperty().unbind();
            loadingLabel.setText(
                ex != null ? "Failed: " + ex.getMessage() : "Failed to load scene."
            );
        });
    }
}
