package com.example.pokemonbattle.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

/**
 * LoadingScreenController — Pikachu.mp4 looping background + progress bar.
 *
 * Progress flow:
 *   - bindToTask(task, callback) wires a background Task to the UI.
 *   - Progress bar fills left-to-right.
 *   - Small label to the right of bar shows "XX%".
 *   - On Task success: MediaPlayer stopped + disposed → callback (or default to menu.fxml).
 *
 * No FX-thread blocking.
 */
public class LoadingScreenController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane rootPane;
    @FXML private MediaView mediaView;
    @FXML private Label loadingLabel;   // percentage label (right of bar)
    @FXML private Label statusLabel;
    @FXML private Rectangle progressFill;
    @FXML private Pane sparkleLayer;
    @FXML private Pane backgroundParticles;

    // ── Media ─────────────────────────────────────────────────────────────────
    private MediaPlayer mediaPlayer;

    // ── Misc ──────────────────────────────────────────────────────────────────
    private final List<Animation> activeAnimations = new ArrayList<>();
    private final Random random = new Random();
    private Runnable onCompletionCallback;
    private boolean loadingComplete = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        startVideoLoop();
        createBackgroundParticles();
    }

    /**
     * Start Pikachu.mp4 on an indefinite loop.
     */
    private void startVideoLoop() {
        var videoUrl = getClass().getResource(
                "/com/example/pokemonbattle/assets/Pikachu.mp4");

        if (videoUrl == null) {
            System.err.println("LoadingScreenController: Pikachu.mp4 not found.");
            return;
        }

        Media media = new Media(videoUrl.toExternalForm());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setAutoPlay(true);

        mediaView.setMediaPlayer(mediaPlayer);

        // Scale video responsively
        mediaView.fitWidthProperty().bind(rootPane.widthProperty());
        mediaView.fitHeightProperty().bind(rootPane.heightProperty());

        mediaPlayer.setOnError(() -> {
            javafx.scene.media.MediaException ex = mediaPlayer.getError();
            System.err.println("LoadingScreenController: MediaPlayer error — "
                    + (ex != null ? ex.getMessage() : "unknown"));
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wire a background Task to the loading UI.
     * Call this after switchScene() has shown the loading screen.
     *
     * @param task         background Task (not yet started)
     * @param onCompletion called on FX thread when loading completes; pass null to go to menu.fxml
     */
    public void bindToTask(Task<?> task, Runnable onCompletion) {
        this.onCompletionCallback = onCompletion;

        // Progress → bar + label
        task.progressProperty().addListener((obs, oldVal, newVal) ->
            Platform.runLater(() -> applyProgress(newVal.doubleValue())));

        // Status message
        task.messageProperty().addListener((obs, oldMsg, newMsg) ->
            Platform.runLater(() -> {
                if (newMsg != null && !newMsg.isEmpty()) {
                    statusLabel.setText(newMsg);
                }
            }));

        task.setOnSucceeded(e -> Platform.runLater(this::onLoadingComplete));
        task.setOnFailed(e -> Platform.runLater(() -> {
            statusLabel.setText("Loading failed!");
            statusLabel.setStyle("-fx-text-fill: #ff4444;");
        }));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Progress
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update progress bar and percentage label.
     * @param progress 0.0 – 1.0
     */
    private void applyProgress(double progress) {
        // Clamp to valid range (Task reports -1 before any update)
        if (progress < 0) return;

        int pct = (int) (progress * 100);
        loadingLabel.setText(pct + "%");

        double fillWidth = progress * 400;   // track is 400px wide
        progressFill.setWidth(fillWidth);
        progressFill.setTranslateX(-200 + fillWidth / 2.0);

        if (pct >= 100 && !loadingComplete) {
            onLoadingComplete();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Completion
    // ─────────────────────────────────────────────────────────────────────────

    private void onLoadingComplete() {
        if (loadingComplete) return;
        loadingComplete = true;

        createSparkleBurst();

        // Small delay for burst to render, then clean up and switch
        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(Duration.millis(900));
        pause.setOnFinished(e -> {
            cleanup();
            if (onCompletionCallback != null) {
                onCompletionCallback.run();
            } else {
                SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
            }
        });
        pause.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Particles
    // ─────────────────────────────────────────────────────────────────────────

    private void createBackgroundParticles() {
        if (backgroundParticles == null) return;
        for (int i = 0; i < 15; i++) {
            Circle p = new Circle(2 + random.nextDouble() * 3,
                    Color.rgb(255, 215, 0, 0.15 + random.nextDouble() * 0.25));
            p.setTranslateX(random.nextDouble() * 1200 - 600);
            p.setTranslateY(random.nextDouble() * 700 - 350);
            backgroundParticles.getChildren().add(p);

            TranslateTransition anim = new TranslateTransition(
                    Duration.seconds(5 + random.nextDouble() * 5), p);
            anim.setByY(-50 - random.nextDouble() * 80);
            anim.setCycleCount(Animation.INDEFINITE);
            anim.setAutoReverse(true);
            anim.setInterpolator(Interpolator.EASE_BOTH);
            anim.play();
            activeAnimations.add(anim);
        }
    }

    private void createSparkleBurst() {
        if (sparkleLayer == null) return;
        double cx = 400, cy = 400;   // centre of 800×800 sparkleLayer
        for (int i = 0; i < 40; i++) {
            Shape sp = random.nextBoolean() ? mkCircle() : mkStar();
            sp.getStyleClass().add("sparkle");
            sp.setTranslateX(cx + random.nextDouble() * 40 - 20);
            sp.setTranslateY(cy + random.nextDouble() * 40 - 20);
            sparkleLayer.getChildren().add(sp);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist  = 100 + random.nextDouble() * 150;

            TranslateTransition move = new TranslateTransition(
                    Duration.millis(800 + random.nextInt(400)), sp);
            move.setByX(Math.cos(angle) * dist);
            move.setByY(Math.sin(angle) * dist);
            move.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fade = new FadeTransition(
                    Duration.millis(1000 + random.nextInt(200)), sp);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);

            ScaleTransition scale = new ScaleTransition(
                    Duration.millis(800 + random.nextInt(400)), sp);
            scale.setFromX(0.5); scale.setFromY(0.5);
            scale.setToX(1.5 + random.nextDouble());
            scale.setToY(1.5 + random.nextDouble());

            RotateTransition rot = new RotateTransition(Duration.millis(1000), sp);
            rot.setByAngle(180 + random.nextInt(360));

            ParallelTransition pt = new ParallelTransition(move, fade, scale, rot);
            pt.setOnFinished(e -> sparkleLayer.getChildren().remove(sp));
            pt.play();
            activeAnimations.add(pt);
        }
    }

    private Circle mkCircle() {
        Color[] cols = {Color.GOLD, Color.WHITE, Color.rgb(255, 215, 0), Color.rgb(255, 107, 107)};
        return new Circle(3 + random.nextDouble() * 5, cols[random.nextInt(cols.length)]);
    }

    private Polygon mkStar() {
        Polygon s = new Polygon(0,-8, 2,-2, 8,0, 2,2, 0,8, -2,2, -8,0, -2,-2);
        s.setFill(Color.rgb(255, 215, 0, 0.9));
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stop and dispose the MediaPlayer, then stop all animations.
     * Must be called before leaving the scene.
     */
    public void cleanup() {
        // Stop video
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ex) {
                System.err.println("LoadingScreenController: cleanup error — " + ex.getMessage());
            } finally {
                mediaPlayer = null;
            }
        }

        // Stop animations
        for (Animation a : activeAnimations) {
            if (a != null) a.stop();
        }
        activeAnimations.clear();

        if (sparkleLayer != null) sparkleLayer.getChildren().clear();
        if (backgroundParticles != null) backgroundParticles.getChildren().clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simulate a loading Task for development/testing.
     * In production always use bindToTask() with a real Task.
     */
    @SuppressWarnings("BusyWait")
    public void simulateLoading() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String[] stages = {
                    "Initializing game engine...",
                    "Loading Pokemon data...",
                    "Loading battle mechanics...",
                    "Loading sprites and assets...",
                    "Preparing battle arena...",
                    "Almost ready..."
                };
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(30);
                    updateProgress(i, 100);
                    int s = (i * stages.length) / 100;
                    if (s < stages.length) updateMessage(stages[s]);
                }
                return null;
            }
        };
        bindToTask(task, null);
        new Thread(task).start();
    }
}
