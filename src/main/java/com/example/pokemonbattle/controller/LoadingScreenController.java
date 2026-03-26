package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.MediaCache;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class LoadingScreenController {

    // FXML fields
    @FXML private StackPane rootPane;
    @FXML private MediaView  bgVideo;
    @FXML private ImageView  fallbackImage;
    @FXML private VBox       progressContainer;
    @FXML private Rectangle  progressFill;
    @FXML private Label      loadingLabel;

    // Constants
    private static final String  PRIMARY_LOADING_VIDEO    = "Pikachu.mp4";
    private static final String  FALLBACK_LOADING_IMAGE   = "loading.png";
    private static final double  FALLBACK_POKEBALL_SCALE  = 0.48;
    private static final double  FALLBACK_IMAGE_ZOOM      = 1.04;
    private static final double  LOAD_DURATION_MS         = 3000.0;
    private static final double  BAR_WIDTH                = 500.0;
    private static final long    VIDEO_READY_TIMEOUT_MS   = 2500;

    /** How long the black overlay takes to fade OUT when the loading screen reveals itself. */
    private static final int FADE_IN_MS  = 400;
    /** How long the black overlay takes to fade IN when the loading screen exits. */
    private static final int FADE_OUT_MS = 300;

    // State
    private MediaPlayer     mediaPlayer;
    private PokeballOverlay fallbackPokeball;
    private Timeline        progressTimeline;
    private boolean         videoConfirmed    = false;
    private boolean         fallbackActivated = false;

    /** The black overlay Rectangle that covers the loading screen on entry and exit. */
    private Rectangle blackOverlay;

    // Lifecycle

    @FXML
    public void initialize() {
        // Add a fully-opaque black overlay immediately — the loading screen starts
        // hidden behind black and reveals itself via playCurtainReveal().
        // Mirrors the pattern used in StartController.
        blackOverlay = new Rectangle();
        blackOverlay.setFill(Color.BLACK);
        blackOverlay.widthProperty().bind(rootPane.widthProperty());
        blackOverlay.heightProperty().bind(rootPane.heightProperty());
        blackOverlay.setOpacity(1.0);
        blackOverlay.setManaged(false);
        rootPane.getChildren().add(blackOverlay);
        ensureProgressOnTop();

        setupProgressBar();

        MediaPlayer claimed = MediaCache.claimMediaPlayer(PRIMARY_LOADING_VIDEO);
        if (claimed != null && claimed.getError() == null) {
            attachMediaPlayer(claimed);
        } else {
            MediaCache.buildVideoPlayer(PRIMARY_LOADING_VIDEO, this::attachMediaPlayer);
            scheduleFallbackTimeout();
        }

        startLoading();
        MusicManager.getInstance().attachClickSounds(rootPane);
    }

    // Video setup

    private void attachMediaPlayer(MediaPlayer player) {
        if (player == null || player.getError() != null) {
            activateFallback("Received null/errored MediaPlayer for " + PRIMARY_LOADING_VIDEO);
            return;
        }
        if (fallbackActivated || videoConfirmed) {
            player.dispose();
            return;
        }

        mediaPlayer = player;

        bgVideo.setMediaPlayer(player);
        bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
        bgVideo.fitHeightProperty().bind(rootPane.heightProperty());
        bgVideo.setPreserveRatio(false);

        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.setVolume(0);

        player.setOnReady(this::onPlayerReady);
        player.setOnError(this::onPlayerError);
        player.setOnHalted(this::onPlayerError);

        switch (player.getStatus()) {
            case READY, PAUSED, STOPPED -> onPlayerReady();
            case PLAYING                -> confirmVideo();
            case HALTED                 -> onPlayerError();
            default                     -> { /* wait for callbacks */ }
        }
    }

    private void onPlayerReady() {
        if (fallbackActivated) return;
        if (mediaPlayer == null || mediaPlayer.getError() != null) {
            activateFallback("Player errored before onReady");
            return;
        }
        mediaPlayer.play();
        confirmVideo();
    }

    private void onPlayerError() {
        Throwable error = (mediaPlayer != null) ? mediaPlayer.getError() : null;
        String detail;
        if (error == null) {
            detail = "unknown error";
        } else if (error.getMessage() != null && !error.getMessage().isBlank()) {
            detail = error.getClass().getSimpleName() + ": " + error.getMessage();
        } else {
            detail = error.toString();
        }
        activateFallback(PRIMARY_LOADING_VIDEO + " player error: " + detail);
    }

    private void confirmVideo() {
        videoConfirmed = true;
        ensureProgressOnTop();
    }

    /**
     * Fades the black overlay OUT, revealing the loading screen beneath.
     * Mirrors exactly what StartController does with its blackOverlay.
     * Called by SceneManager right after the loading scene is shown on stage.
     */
    public void playCurtainReveal() {
        if (blackOverlay == null) return;
        ensureProgressOnTop();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(FADE_IN_MS), blackOverlay);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_OUT);
        fadeOut.setOnFinished(e -> {
            if (blackOverlay != null) {
                blackOverlay.widthProperty().unbind();
                blackOverlay.heightProperty().unbind();
                rootPane.getChildren().remove(blackOverlay);
                blackOverlay = null;
            }
        });
        fadeOut.play();
    }

    private void scheduleFallbackTimeout() {
        Timeline timeout = new Timeline(
                new KeyFrame(Duration.millis(VIDEO_READY_TIMEOUT_MS), e -> {
                    if (!videoConfirmed && !fallbackActivated) {
                        activateFallback(PRIMARY_LOADING_VIDEO + " did not become ready within "
                                + VIDEO_READY_TIMEOUT_MS + " ms");
                    }
                }));
        timeout.setCycleCount(1);
        timeout.play();
    }

    // Fallback (image + pokeball)

    private void activateFallback(String reason) {
        if (fallbackActivated || videoConfirmed) return;
        fallbackActivated = true;

        System.err.println("[LoadingScreen] Falling back to image+pokeball. Reason: " + reason);

        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) { }
            mediaPlayer = null;
        }
        bgVideo.setMediaPlayer(null);

        if (fallbackImage != null) {
            Image img = MediaCache.getImage(FALLBACK_LOADING_IMAGE);
            if (img != null) {
                fallbackImage.setImage(img);
            } else {
                System.err.println("[LoadingScreen] " + FALLBACK_LOADING_IMAGE + " also unavailable.");
            }
            fallbackImage.fitWidthProperty().bind(rootPane.widthProperty());
            fallbackImage.fitHeightProperty().bind(rootPane.heightProperty());
            fallbackImage.setPreserveRatio(false);
            fallbackImage.setScaleX(FALLBACK_IMAGE_ZOOM);
            fallbackImage.setScaleY(FALLBACK_IMAGE_ZOOM);
            StackPane.setAlignment(fallbackImage, Pos.CENTER);
            fallbackImage.setVisible(true);
            fallbackImage.setManaged(true);
        }

        fallbackPokeball = PokeballOverlay.showOn(rootPane);
        fallbackPokeball.setScaleX(FALLBACK_POKEBALL_SCALE);
        fallbackPokeball.setScaleY(FALLBACK_POKEBALL_SCALE);
        fallbackPokeball.toFront();

        ensureProgressOnTop();
    }

    private void removeFallback() {
        if (fallbackPokeball != null && rootPane != null) {
            PokeballOverlay.hideFrom(rootPane, fallbackPokeball, null);
            fallbackPokeball = null;
        }
        if (fallbackImage != null) {
            fallbackImage.setVisible(false);
            fallbackImage.setManaged(false);
        }
    }

    // Progress bar

    private void setupProgressBar() {
        progressFill.setWidth(0);
        progressFill.setTranslateX(-(BAR_WIDTH / 2.0));
        loadingLabel.setText("Loading... 0%");
        ensureProgressOnTop();
    }

    private void startLoading() {
        progressTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressFill.widthProperty(),      0,                  Interpolator.LINEAR),
                        new KeyValue(progressFill.translateXProperty(), -(BAR_WIDTH / 2.0), Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(LOAD_DURATION_MS),
                        new KeyValue(progressFill.widthProperty(),      BAR_WIDTH,          Interpolator.LINEAR),
                        new KeyValue(progressFill.translateXProperty(), 0,                  Interpolator.LINEAR)));

        progressTimeline.currentTimeProperty().addListener((obs, oldT, newT) -> {
            double pct = Math.min(newT.toMillis() / LOAD_DURATION_MS, 1.0) * 100;
            loadingLabel.setText(String.format("Loading... %d%%", (int) pct));
        });

        progressTimeline.setOnFinished(e -> {
            loadingLabel.setText("Loading... 100%");
            fadeOutThenRun(this::disposeAndNavigate);
        });

        progressTimeline.play();
    }

    private void ensureProgressOnTop() {
        if (progressContainer != null) progressContainer.toFront();
        if (loadingLabel      != null) loadingLabel.toFront();
        // Keep the black overlay on top until reveal is triggered
        if (blackOverlay != null) blackOverlay.toFront();
        if (progressContainer != null) progressContainer.toFront();
        if (loadingLabel      != null) loadingLabel.toFront();
    }

    // Navigation

    /**
     * Fades a black overlay IN over the loading screen, then calls {@code action}.
     * Mirrors the pattern in IntroController's startFlashTransition().
     */
    private void fadeOutThenRun(Runnable action) {
        Rectangle exitOverlay = new Rectangle();
        exitOverlay.setFill(Color.BLACK);
        exitOverlay.widthProperty().bind(rootPane.widthProperty());
        exitOverlay.heightProperty().bind(rootPane.heightProperty());
        exitOverlay.setOpacity(0);
        exitOverlay.setManaged(false);
        rootPane.getChildren().add(exitOverlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_OUT_MS), exitOverlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_IN);
        fadeIn.setOnFinished(e -> {
            exitOverlay.widthProperty().unbind();
            exitOverlay.heightProperty().unbind();
            rootPane.getChildren().remove(exitOverlay);
            if (action != null) action.run();
        });
        fadeIn.play();
    }

    private void disposeAndNavigate() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) { }
            mediaPlayer = null;
        }
        removeFallback();
        SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
    }

    // Public API

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
            progressFill.widthProperty().unbind();
            loadingLabel.textProperty().unbind();
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                } catch (Exception ignored) { }
                mediaPlayer = null;
            }
            removeFallback();
            // Fade to black, then hand off to the next scene
            fadeOutThenRun(() -> {
                if (onSuccess != null) Platform.runLater(onSuccess);
            });
        });

        task.setOnFailed(event -> {
            progressFill.widthProperty().unbind();
            loadingLabel.textProperty().unbind();
            Throwable ex = task.getException();
            loadingLabel.setText(ex != null ? "Failed: " + ex.getMessage() : "Failed to load scene.");
        });
    }
}