package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class LoadingScreenController {

    @FXML private StackPane rootPane;
    @FXML private MediaView bgVideo;
    @FXML private ImageView fallbackImage;
    @FXML private VBox progressContainer;
    @FXML private Rectangle progressFill;
    @FXML private Label loadingLabel;

    private MediaPlayer mediaPlayer;
    private PokeballOverlay fallbackPokeball;
    private Timeline progressTimeline;
    private static final String PRIMARY_LOADING_VIDEO = "Pikachu.mp4";
    private static final String FALLBACK_LOADING_IMAGE = "loading.png";
    private static final double FALLBACK_POKEBALL_SCALE = 0.48;
    private static final boolean USE_VIDEO_BACKGROUND = true;
    private static final double FALLBACK_IMAGE_ZOOM = 1.04;

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
        if (fallbackImage != null) {
            fallbackImage.fitWidthProperty().bind(rootPane.widthProperty());
            fallbackImage.fitHeightProperty().bind(rootPane.heightProperty());
            fallbackImage.setPreserveRatio(true);
            fallbackImage.setScaleX(FALLBACK_IMAGE_ZOOM);
            fallbackImage.setScaleY(FALLBACK_IMAGE_ZOOM);
            StackPane.setAlignment(fallbackImage, Pos.CENTER);
            fallbackImage.setVisible(false);
            fallbackImage.setManaged(false);
        }

        if (!USE_VIDEO_BACKGROUND) {
            enablePokeballFallback();
            return;
        }

        mediaPlayer = claimPrimaryLoadingVideoPlayer();
        if (mediaPlayer == null) {
            System.err.println("LoadingScreenController: Pikachu loading video unavailable. Falling back to pokeball overlay.");
            enablePokeballFallback();
            return;
        }
        final MediaPlayer activePlayer = mediaPlayer;
        activePlayer.setCycleCount(MediaPlayer.INDEFINITE);
        bgVideo.setMediaPlayer(activePlayer);
        bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
        bgVideo.fitHeightProperty().bind(rootPane.heightProperty());
        bgVideo.setPreserveRatio(false);
        activePlayer.setOnReady(() -> {
            if (activePlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                activePlayer.play();
            }
        });
        activePlayer.setOnError(() -> {
            Throwable error = activePlayer.getError();
            System.err.println("LoadingScreenController: " + PRIMARY_LOADING_VIDEO + " failed: "
                    + (error != null ? error.getMessage() : "unknown"));
            if (mediaPlayer == activePlayer) {
                try {
                    activePlayer.stop();
                    activePlayer.dispose();
                } catch (Exception ignored) {
                }
                mediaPlayer = null;
            }
            enablePokeballFallback();
        });
        if (activePlayer.getStatus() == MediaPlayer.Status.READY
                && activePlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            activePlayer.play();
        }
    }

    private MediaPlayer claimPrimaryLoadingVideoPlayer() {
        MediaPlayer primary = com.example.pokemonbattle.util.MediaCache.claimMediaPlayer(PRIMARY_LOADING_VIDEO);
        if (primary == null || primary.getError() != null) return null;
        return primary;
    }

    private void enablePokeballFallback() {
        if (fallbackPokeball != null || rootPane == null) {
            return;
        }
        bgVideo.setMediaPlayer(null);

        if (fallbackImage != null) {
            if (fallbackImage.getImage() == null) {
                Image image = com.example.pokemonbattle.util.MediaCache.getImage(FALLBACK_LOADING_IMAGE);
                if (image != null) {
                    fallbackImage.setImage(image);
                } else {
                    System.err.println("LoadingScreenController: " + FALLBACK_LOADING_IMAGE + " fallback unavailable.");
                }
            }
            fallbackImage.setVisible(true);
            fallbackImage.toFront();
        }

        fallbackPokeball = PokeballOverlay.showOn(rootPane);
        fallbackPokeball.setScaleX(FALLBACK_POKEBALL_SCALE);
        fallbackPokeball.setScaleY(FALLBACK_POKEBALL_SCALE);
        fallbackPokeball.toFront();
        ensureLoadingUiOnTop();
    }

    private void ensureLoadingUiOnTop() {
        if (progressContainer != null) {
            progressContainer.toFront();
        }
        if (loadingLabel != null) {
            loadingLabel.toFront();
        }
    }

    private void clearFallbackPokeball() {
        if (fallbackPokeball == null || rootPane == null) {
            if (fallbackImage != null) {
                fallbackImage.setVisible(false);
            }
            return;
        }
        PokeballOverlay.hideFrom(rootPane, fallbackPokeball, null);
        fallbackPokeball = null;
        if (fallbackImage != null) {
            fallbackImage.setVisible(false);
        }
    }

    private void setupProgressBar() {
        progressFill.setWidth(0);
        progressFill.setTranslateX(0);
        loadingLabel.setText("Loading... 0%");
        ensureLoadingUiOnTop();
    }

    private void startLoading() {
        progressTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(progressFill.widthProperty(), 0, Interpolator.LINEAR)
            ),
            new KeyFrame(Duration.millis(LOAD_DURATION_MS),
                new KeyValue(progressFill.widthProperty(), BAR_WIDTH, Interpolator.LINEAR)
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
        clearFallbackPokeball();
        SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
    }

    public void bindToTask(Task<?> task, Runnable onSuccess) {
        if (progressTimeline != null) {
            progressTimeline.stop();
            progressTimeline = null;
        }
        progressFill.setWidth(0);
        progressFill.setTranslateX(0);
        progressFill.widthProperty().bind(task.progressProperty().multiply(BAR_WIDTH));
        loadingLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(event -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            clearFallbackPokeball();
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
