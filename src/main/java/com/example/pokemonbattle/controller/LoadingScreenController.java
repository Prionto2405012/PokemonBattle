package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.CurtainTransitionManager;
import com.example.pokemonbattle.util.MediaCache;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

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
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class LoadingScreenController {

    //  FXML fields 
    @FXML private StackPane rootPane;
    @FXML private MediaView  bgVideo;
    @FXML private ImageView  fallbackImage;
    @FXML private VBox       progressContainer;
    @FXML private Rectangle  progressFill;
    @FXML private Label      loadingLabel;

    //  Constants ─
    private static final String  PRIMARY_LOADING_VIDEO    = "Pikachu.mp4";
    private static final String  FALLBACK_LOADING_IMAGE   = "loading.png";
    private static final double  FALLBACK_POKEBALL_SCALE  = 0.48;
    private static final double  FALLBACK_IMAGE_ZOOM      = 1.04;
    private static final double  LOAD_DURATION_MS         = 3000.0;
    private static final double  BAR_WIDTH                = 500.0;

    /**
     * How long (ms) to wait for the MediaPlayer to reach READY before giving up
     * and switching to the image+pokeball fallback. Kept generous so that even on
     * slower machines the video gets a fair chance.
     */
    private static final long VIDEO_READY_TIMEOUT_MS = 2500;

    //  State ─
    private MediaPlayer    mediaPlayer;
    private PokeballOverlay fallbackPokeball;
    private Timeline       progressTimeline;

    /** True once we have confirmed the video is playing (or will play imminently). */
    private boolean videoConfirmed = false;
    /** True once the fallback path has been activated — prevents double-init. */
    private boolean fallbackActivated = false;

    //  Lifecycle ─

    @FXML
    public void initialize() {
        // Always set up the progress UI first so it is visible regardless of path.
        setupProgressBar();

        // Try to get a pre-built MediaPlayer from the cache.
        MediaPlayer claimed = MediaCache.claimMediaPlayer(PRIMARY_LOADING_VIDEO);
        if (claimed != null && claimed.getError() == null) {
            attachMediaPlayer(claimed);
        } else {
            // Cache miss or errored player — ask MediaCache to build one async and
            // hand it back on the FX thread when ready.
            MediaCache.buildVideoPlayer(PRIMARY_LOADING_VIDEO, this::attachMediaPlayer);
            // Start the fallback timeout while the async build is in progress.
            scheduleFallbackTimeout();
        }

        startLoading();
        MusicManager.getInstance().attachClickSounds(rootPane);
    }
    //  Video setup
    /**
     * Called (always on the FX thread) with a freshly-built or pre-cached
     * MediaPlayer.  Wires it up to the MediaView and begins playback.
     * If we have already fallen back, the player is disposed immediately.
     */
    private void attachMediaPlayer(MediaPlayer player) {
        if (player == null || player.getError() != null) {
            activateFallback("Received null/errored MediaPlayer for " + PRIMARY_LOADING_VIDEO);
            return;
        }

        // If the fallback was already activated before this callback fired, we no
        // longer need the player.
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
        player.setVolume(0); // loading screen is silent; music is handled by MusicManager

        // Register all possible ready/error/stalled callbacks so we cover every
        // status the player could be in when we receive it.
        player.setOnReady(this::onPlayerReady);
        player.setOnError(this::onPlayerError);
        player.setOnHalted(this::onPlayerError);

        // The player may already be READY (common when claimed from the pre-built cache).
        switch (player.getStatus()) {
            case READY, PAUSED, STOPPED -> onPlayerReady();
            case PLAYING                -> confirmVideo();   // already going
            case HALTED                 -> onPlayerError();
            default                   -> { /* still loading; wait for callbacks */}
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

    /** Marks the video path as successful; suppresses any pending fallback. */
    private void confirmVideo() {
        videoConfirmed = true;
        // Ensure the progress UI is on top of the video.
        ensureProgressOnTop();
    }
    public void playCurtainReveal(){
        CurtainTransitionManager.riseOn(rootPane);
    }
    /**
     * Schedules a one-shot check: if the video hasn't confirmed within
     * VIDEO_READY_TIMEOUT_MS, activate the fallback.
     */
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

    //  Fallback (image + pokeball) ─

    /**
     * Switches to the loading.png + spinning pokeball fallback.
     * Safe to call from any state; no-ops if the fallback is already active.
     */
    private void activateFallback(String reason) {
        if (fallbackActivated || videoConfirmed) return;
        fallbackActivated = true;

        System.err.println("[LoadingScreen] Falling back to image+pokeball. Reason: " + reason);

        // Detach the video player cleanly.
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) { }
            mediaPlayer = null;
        }
        bgVideo.setMediaPlayer(null);

        // Show the fallback image.
        if (fallbackImage != null) {
            Image img = MediaCache.getImage(FALLBACK_LOADING_IMAGE);
            if (img != null) {
                fallbackImage.setImage(img);
            } else {
                System.err.println("[LoadingScreen] " + FALLBACK_LOADING_IMAGE + " also unavailable.");
            }
            // Bind size so it fills the pane exactly (matching CSS behaviour).
            fallbackImage.fitWidthProperty().bind(rootPane.widthProperty());
            fallbackImage.fitHeightProperty().bind(rootPane.heightProperty());
            fallbackImage.setPreserveRatio(false);
            fallbackImage.setScaleX(FALLBACK_IMAGE_ZOOM);
            fallbackImage.setScaleY(FALLBACK_IMAGE_ZOOM);
            StackPane.setAlignment(fallbackImage, Pos.CENTER);
            fallbackImage.setVisible(true);
            fallbackImage.setManaged(true);
        }

        // Spin the pokeball overlay on top.
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

    //  Progress bar 

    private void setupProgressBar() {
        progressFill.setWidth(0);
        progressFill.setTranslateX(-(BAR_WIDTH / 2.0));
        loadingLabel.setText("Loading... 0%");
        ensureProgressOnTop();
    }

    private void startLoading() {
        progressTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressFill.widthProperty(),     0,             Interpolator.LINEAR),
                        new KeyValue(progressFill.translateXProperty(), -(BAR_WIDTH / 2.0), Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(LOAD_DURATION_MS),
                        new KeyValue(progressFill.widthProperty(),     BAR_WIDTH,     Interpolator.LINEAR),
                        new KeyValue(progressFill.translateXProperty(), 0,             Interpolator.LINEAR)));

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

    /** Ensures progress bar + label always render above the video / image layers. */
    private void ensureProgressOnTop() {
        if (progressContainer != null) progressContainer.toFront();
        if (loadingLabel      != null) loadingLabel.toFront();
    }

    //  Navigation 

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

    //  Public API (used by SceneManager task binding) 

    /**
     * Replaces the timed progress animation with one driven by a background Task.
     * Call this after {@code initialize()} if you want task-driven progress.
     */
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
            if (onSuccess != null) Platform.runLater(onSuccess);
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            loadingLabel.textProperty().unbind();
            loadingLabel.setText(ex != null ? "Failed: " + ex.getMessage() : "Failed to load scene.");
        });
    }
}