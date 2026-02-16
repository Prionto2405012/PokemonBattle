package com.example.pokemonbattle.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

/**
 * Production-Quality Loading Screen Controller
 * 
 * Features:
 * - Hardware-accelerated Pokeball rotation
 * - Real Task progress binding
 * - Opening animation at 100%
 * - GPU-optimized sparkle particles
 * - Cinematic glow pulse
 * - Motion blur effects
 * - Proper resource cleanup
 * - Memory leak prevention
 * 
 * @author Pokemon Battle Team
 * @version 2.0
 */
public class LoadingScreenController {

    // FXML Injected Components
    @FXML private StackPane rootPane;
    @FXML private StackPane ballContainer;
    @FXML private Arc topHalf;
    @FXML private Arc bottomHalf;
    @FXML private Circle centerButton;
    @FXML private Circle glowCircle;
    @FXML private Ellipse shadow;
    @FXML private Label loadingLabel;
    @FXML private Label statusLabel;
    @FXML private Rectangle progressFill;
    @FXML private Pane sparkleLayer;
    @FXML private Pane backgroundParticles;

    // Animation Controllers
    private RotateTransition rotationAnimation;
    private Timeline glowPulseTimeline;
    private Timeline shadowPulseTimeline;
    private final List<Animation> activeAnimations = new ArrayList<>();
    
    // Performance Optimization
    private final GaussianBlur motionBlur = new GaussianBlur(0);
    private final Random random = new Random();
    
    // State Management
    private boolean loadingComplete = false;
    private Runnable onCompletionCallback;

    /**
     * Initialize the loading screen.
     * Called automatically by FXMLLoader.
     */
    @FXML
    public void initialize() {
        setupPerformanceOptimizations();
        startRotationAnimation();
        startGlowPulseAnimation();
        startShadowPulseAnimation();
        createBackgroundParticles();
    }

    /**
     * Configure hardware acceleration and caching.
     */
    private void setupPerformanceOptimizations() {
        // Enable hardware acceleration cache hints
        ballContainer.setCache(true);
        ballContainer.setCacheHint(javafx.scene.CacheHint.SPEED);
        
        topHalf.setCache(true);
        bottomHalf.setCache(true);
        centerButton.setCache(true);
        
        // Apply GaussianBlur effects directly (moved from CSS for better JavaFX compatibility)
        shadow.setEffect(new GaussianBlur(15));
        glowCircle.setEffect(new GaussianBlur(25));
        
        // Smooth edges
        ballContainer.setStyle("-fx-smooth: true;");
    }

    /**
     * Start continuous Y-axis rotation.
     * Uses LINEAR interpolator for constant speed.
     */
    private void startRotationAnimation() {
        rotationAnimation = new RotateTransition(Duration.seconds(2.5), ballContainer);
        rotationAnimation.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0)); // Y-axis
        rotationAnimation.setByAngle(360);
        rotationAnimation.setCycleCount(Animation.INDEFINITE);
        rotationAnimation.setInterpolator(Interpolator.LINEAR);
        
        // Apply subtle motion blur during rotation
        rotationAnimation.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!loadingComplete) {
                double blurAmount = 3 + Math.sin(newTime.toMillis() / 200.0) * 2;
                motionBlur.setRadius(blurAmount);
                ballContainer.setEffect(motionBlur);
            }
        });
        
        rotationAnimation.play();
        activeAnimations.add(rotationAnimation);
    }

    /**
     * Create cinematic glow pulse effect.
     */
    private void startGlowPulseAnimation() {
        glowPulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(glowCircle.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(glowCircle.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(glowCircle.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.seconds(1.5),
                new KeyValue(glowCircle.opacityProperty(), 0.6, Interpolator.EASE_BOTH),
                new KeyValue(glowCircle.scaleXProperty(), 1.15, Interpolator.EASE_BOTH),
                new KeyValue(glowCircle.scaleYProperty(), 1.15, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.seconds(3.0),
                new KeyValue(glowCircle.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(glowCircle.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(glowCircle.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
            )
        );
        
        glowPulseTimeline.setCycleCount(Animation.INDEFINITE);
        glowPulseTimeline.play();
        activeAnimations.add(glowPulseTimeline);
    }

    /**
     * Animate shadow to sync with glow.
     */
    private void startShadowPulseAnimation() {
        shadowPulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(shadow.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(shadow.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.seconds(1.5),
                new KeyValue(shadow.scaleXProperty(), 1.1, Interpolator.EASE_BOTH),
                new KeyValue(shadow.scaleYProperty(), 0.9, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.seconds(3.0),
                new KeyValue(shadow.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(shadow.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
            )
        );
        
        shadowPulseTimeline.setCycleCount(Animation.INDEFINITE);
        shadowPulseTimeline.play();
        activeAnimations.add(shadowPulseTimeline);
    }

    /**
     * Create subtle floating background particles.
     */
    private void createBackgroundParticles() {
        for (int i = 0; i < 15; i++) {
            Circle particle = new Circle(2 + random.nextDouble() * 3, 
                Color.rgb(138, 43, 226, 0.2 + random.nextDouble() * 0.3));
            
            particle.setTranslateX(random.nextDouble() * 1200 - 600);
            particle.setTranslateY(random.nextDouble() * 700 - 350);
            
            backgroundParticles.getChildren().add(particle);
            
            // Floating animation
            TranslateTransition floatAnim = new TranslateTransition(
                Duration.seconds(5 + random.nextDouble() * 5), particle
            );
            floatAnim.setByY(-50 - random.nextDouble() * 100);
            floatAnim.setCycleCount(Animation.INDEFINITE);
            floatAnim.setAutoReverse(true);
            floatAnim.setInterpolator(Interpolator.EASE_BOTH);
            floatAnim.play();
            
            activeAnimations.add(floatAnim);
        }
    }

    /**
     * Bind loading screen to a background Task.
     * This is the primary integration method.
     * 
     * @param task The JavaFX Task to monitor
     * @param onCompletion Callback when loading completes (optional)
     */
    public void bindToTask(Task<?> task, Runnable onCompletion) {
        this.onCompletionCallback = onCompletion;
        
        // Bind progress to UI
        task.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            Platform.runLater(() -> {
                double progress = newProgress.doubleValue();
                updateProgress(progress);
            });
        });
        
        // Bind status message
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> {
                if (newMsg != null && !newMsg.isEmpty()) {
                    statusLabel.setText(newMsg);
                }
            });
        });
        
        // Handle completion
        task.setOnSucceeded(e -> Platform.runLater(this::onLoadingComplete));
        task.setOnFailed(e -> Platform.runLater(() -> {
            statusLabel.setText("Loading failed!");
            statusLabel.setStyle("-fx-text-fill: #ff4444;");
        }));
    }

    /**
     * Update progress percentage (0.0 to 1.0).
     */
    private void updateProgress(double progress) {
        int percentage = (int) (progress * 100);
        loadingLabel.setText("Loading " + percentage + "%");
        
        // Update progress bar fill
        double fillWidth = progress * 400; // Max width 400px
        progressFill.setWidth(fillWidth);
        progressFill.setTranslateX(-200 + fillWidth / 2);
        
        // Trigger completion at 100%
        if (percentage >= 100 && !loadingComplete) {
            onLoadingComplete();
        }
    }

    /**
     * Handle loading completion - stop rotation and play opening animation.
     */
    private void onLoadingComplete() {
        if (loadingComplete) return;
        loadingComplete = true;
        
        // Stop rotation smoothly
        stopRotationSmoothly();
        
        // Wait for rotation to stop, then open ball
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> {
            openPokeballAnimation();
            createSparkleBurst();
        });
        pause.play();
    }

    /**
     * Stop rotation with deceleration effect.
     */
    private void stopRotationSmoothly() {
        rotationAnimation.stop();
        
        // Clear motion blur
        Timeline blurFade = new Timeline(
            new KeyFrame(Duration.millis(300),
                new KeyValue(motionBlur.radiusProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        blurFade.play();
        activeAnimations.add(blurFade);
    }

    /**
     * Open the Pokeball with cinematic animation.
     */
    private void openPokeballAnimation() {
        // Stop glow pulse
        glowPulseTimeline.stop();
        
        // Intense glow burst
        Timeline glowBurst = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(glowCircle.opacityProperty(), 0.0)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(glowCircle.opacityProperty(), 1.0, Interpolator.EASE_OUT)
            ),
            new KeyFrame(Duration.millis(800),
                new KeyValue(glowCircle.opacityProperty(), 0.0, Interpolator.EASE_IN)
            )
        );
        glowBurst.play();
        
        // Open top half with eased motion
        TranslateTransition openTop = new TranslateTransition(Duration.millis(800), topHalf);
        openTop.setByY(-120);
        openTop.setInterpolator(Interpolator.SPLINE(0.68, 0.0, 0.265, 1.0)); // Smooth ease-out
        
        // Add glow effect to opening animation
        topHalf.setEffect(new Glow(0.8));
        
        // Rotate top half slightly for dramatic effect
        RotateTransition tiltTop = new RotateTransition(Duration.millis(800), topHalf);
        tiltTop.setByAngle(-25);
        tiltTop.setInterpolator(Interpolator.EASE_OUT);
        
        ParallelTransition openAnimation = new ParallelTransition(openTop, tiltTop);
        openAnimation.setOnFinished(e -> transitionToNextScene());
        openAnimation.play();
        
        activeAnimations.add(openAnimation);
    }

    /**
     * Create GPU-optimized sparkle particle burst.
     */
    private void createSparkleBurst() {
        int sparkleCount = 40; // AAA quality particle count
        
        // Calculate ball center position relative to sparkleLayer
        // sparkleLayer is centered in StackPane, so center is at prefWidth/2, prefHeight/2
        double centerX = 400; // Half of sparkleLayer prefWidth (800/2)
        double centerY = 400; // Half of sparkleLayer prefHeight (800/2)
        
        for (int i = 0; i < sparkleCount; i++) {
            // Random sparkle shape (circle or star)
            Shape sparkle = (random.nextBoolean()) ? createSparkleCircle() : createSparkleStar();
            
            sparkle.getStyleClass().add("sparkle");
            
            // Position at ball center with slight randomness
            sparkle.setTranslateX(centerX + random.nextDouble() * 40 - 20);
            sparkle.setTranslateY(centerY + random.nextDouble() * 40 - 20);
            
            sparkleLayer.getChildren().add(sparkle);
            
            // Random direction and distance
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 100 + random.nextDouble() * 150;
            double targetX = Math.cos(angle) * distance;
            double targetY = Math.sin(angle) * distance;
            
            // Movement animation
            TranslateTransition move = new TranslateTransition(
                Duration.millis(800 + random.nextInt(400)), sparkle
            );
            move.setByX(targetX);
            move.setByY(targetY);
            move.setInterpolator(Interpolator.EASE_OUT);
            
            // Fade out animation
            FadeTransition fade = new FadeTransition(
                Duration.millis(1000 + random.nextInt(200)), sparkle
            );
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setInterpolator(Interpolator.EASE_IN);
            
            // Scale animation
            ScaleTransition scale = new ScaleTransition(
                Duration.millis(800 + random.nextInt(400)), sparkle
            );
            scale.setFromX(0.5);
            scale.setFromY(0.5);
            scale.setToX(1.5 + random.nextDouble());
            scale.setToY(1.5 + random.nextDouble());
            scale.setInterpolator(Interpolator.EASE_OUT);
            
            // Rotation for drama
            RotateTransition rotate = new RotateTransition(
                Duration.millis(1000), sparkle
            );
            rotate.setByAngle(180 + random.nextInt(360));
            
            ParallelTransition sparkleAnimation = new ParallelTransition(
                move, fade, scale, rotate
            );
            
            sparkleAnimation.setOnFinished(e -> sparkleLayer.getChildren().remove(sparkle));
            sparkleAnimation.play();
            
            activeAnimations.add(sparkleAnimation);
        }
    }

    /**
     * Create circular sparkle particle.
     */
    private Circle createSparkleCircle() {
        double size = 3 + random.nextDouble() * 5;
        Color[] colors = {
            Color.GOLD, 
            Color.WHITE, 
            Color.rgb(255, 215, 0), 
            Color.rgb(255, 107, 107)
        };
        return new Circle(size, colors[random.nextInt(colors.length)]);
    }

    /**
     * Create star-shaped sparkle particle.
     */
    private Polygon createSparkleStar() {
        Polygon star = new Polygon(
            0, -8,
            2, -2,
            8, 0,
            2, 2,
            0, 8,
            -2, 2,
            -8, 0,
            -2, -2
        );
        star.setFill(Color.rgb(255, 215, 0, 0.9));
        return star;
    }

    /**
     * Transition to the next scene after animation completes.
     */
    private void transitionToNextScene() {
        PauseTransition delay = new PauseTransition(Duration.millis(500));
        delay.setOnFinished(e -> {
            cleanup();
            
            if (onCompletionCallback != null) {
                onCompletionCallback.run();
            } else {
                // Default: transition to menu
                SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
            }
        });
        delay.play();
    }

    /**
     * Clean up resources to prevent memory leaks.
     * CRITICAL for production applications.
     */
    public void cleanup() {
        // Stop all animations
        for (Animation animation : activeAnimations) {
            if (animation != null) {
                animation.stop();
            }
        }
        activeAnimations.clear();
        
        // Clear particle layers
        if (sparkleLayer != null) {
            sparkleLayer.getChildren().clear();
        }
        if (backgroundParticles != null) {
            backgroundParticles.getChildren().clear();
        }
        
        // Clear effects
        if (ballContainer != null) {
            ballContainer.setEffect(null);
        }
        
        System.out.println("LoadingScreen resources cleaned up");
    }

    /**
     * Simulate loading for testing purposes.
     * In production, always use bindToTask() instead.
     */
    public void simulateLoading() {
        Task<Void> simulatedTask = new Task<>() {
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
                    Thread.sleep(30); // Simulate work
                    updateProgress(i, 100);
                    
                    // Update message at milestones
                    int stage = (i * stages.length) / 100;
                    if (stage < stages.length) {
                        updateMessage(stages[stage]);
                    }
                }
                
                return null;
            }
        };
        
        bindToTask(simulatedTask, null);
        new Thread(simulatedTask).start();
    }
}
