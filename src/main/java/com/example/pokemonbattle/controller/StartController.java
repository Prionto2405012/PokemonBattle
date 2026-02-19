package com.example.pokemonbattle.controller;

import java.util.concurrent.atomic.AtomicBoolean;

import com.example.pokemonbattle.util.MediaCache;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Controller for the Start Screen.
 * Background is start.gif (animated GIF, loops automatically). Start button transitions to the wc screen.
 */
@SuppressWarnings("unused")
public class StartController {

    @FXML private StackPane rootPane;
    @FXML private ImageView bgImage;
    @FXML private Region vignetteOverlay;

    @FXML
    public void initialize() {
        // Retrieve pre-loaded GIF from MediaCache (decoded in background during intro)
        Image gif = MediaCache.getImage("start.gif");
        if (gif == null) {
            System.err.println("[StartController] start.gif NOT FOUND in cache – showing black background.");
        } else if (gif.isError()) {
            System.err.println("[StartController] start.gif failed to decode.");
        } else {
            System.out.println("[StartController] start.gif ready from cache.");
        }

        if (gif != null && !gif.isError() && bgImage != null && rootPane != null) {
            bgImage.setImage(gif);
            // Fill the full width; subtract ~8px from height (4px each side ≈ 1 mm at 96 dpi)
            // so the StackPane's black background shows as a hairline top/bottom letterbox.
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty().subtract(8));
            bgImage.setPreserveRatio(false);
        }

        // Bind vignette overlay to cover the full pane
        if (vignetteOverlay != null && rootPane != null) {
            vignetteOverlay.prefWidthProperty().bind(rootPane.widthProperty());
            vignetteOverlay.prefHeightProperty().bind(rootPane.heightProperty());
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

            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), overlay);
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
        phase1.setToValue(0.7);
        phase1.setInterpolator(Interpolator.EASE_IN);

        // Phase 2: screen goes fully black
        FadeTransition phase2 = new FadeTransition(Duration.millis(40), overlay);
        phase2.setFromValue(0.7);
        phase2.setToValue(0.9);
        phase2.setInterpolator(Interpolator.EASE_OUT);
        phase2.setOnFinished(e -> {
            if (!switched.compareAndSet(false, true)) return; // guard against re-entry
            // Unbind before scene switch to prevent resize from re-firing this handler
            overlay.widthProperty().unbind();
            overlay.heightProperty().unbind();
            SceneManager.switchScene("wc.fxml", "Pokemon Battle", 1200, 700);
        });

        new SequentialTransition(phase1, phase2).play();
    }
}
