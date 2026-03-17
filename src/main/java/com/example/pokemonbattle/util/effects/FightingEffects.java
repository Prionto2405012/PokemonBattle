// FightingEffects.java
package com.example.pokemonbattle.util.effects;

import com.example.pokemonbattle.util.MediaCache;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

public class FightingEffects {

    private final Pane battleField;
    private static final String PUNCH_ASSET = "punch.png";
    private static final String FEET_ASSET = "feet.png";

    public FightingEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // -
    // Public API
    // -

    /**
     * Full fighting-type impact: punch image + white impact starburst.
     * Called for pure Fighting-type moves (Mach Punch, Close Combat, …).
     */
    public void createImpactEffect(double x, double y, String moveName,
                                   int movePower, Timeline timeline) {
        if (moveName.contains("-kick") || moveName.contains("-feet")) {
            addFeetImage(x, y, timeline);
        } else {
            addPunchImage(x, y, timeline);
        }
        addImpactStarburst(x, y, movePower, timeline);
    }

    private void addPunchImage(double x, double y, Timeline timeline) {
        addStaticImpactImage(PUNCH_ASSET, x, y, 160, 160, timeline);
    }

    private void addFeetImage(double x, double y, Timeline timeline) {
        addStaticImpactImage(FEET_ASSET, x, y, 170, 170, timeline);
    }

    private void addStaticImpactImage(String assetName, double x, double y,
                                      double width, double height,
                                      Timeline timeline) {
        try {
            Image image = MediaCache.getImage(assetName);
            if (image == null) {
                return;
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setLayoutX(x - width / 2.0);
            imageView.setLayoutY(y - height / 2.0);
            imageView.setOpacity(0);
            imageView.setScaleX(0.55);
            imageView.setScaleY(0.55);
            prepareTransientNode(imageView);
            battleField.getChildren().add(imageView);

            KeyFrame appear = new KeyFrame(Duration.millis(35),
                new KeyValue(imageView.opacityProperty(), 1.0),
                new KeyValue(imageView.scaleXProperty(), 1.25),
                new KeyValue(imageView.scaleYProperty(), 1.25));
            KeyFrame settle = new KeyFrame(Duration.millis(115),
                new KeyValue(imageView.scaleXProperty(), 1.0),
                new KeyValue(imageView.scaleYProperty(), 1.0));
            long fadeMs = FEET_ASSET.equals(assetName) ? 560L : 330L;
            KeyFrame fade = new KeyFrame(Duration.millis(fadeMs),
                new KeyValue(imageView.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(appear, settle, fade);
            registerCleanup(timeline, imageView);
        } catch (Exception ignored) {
            // Overlay is optional; the core move effect should still play.
        }
    }
    // Punch image

    /**
     * Loads punch.png once (cached), places it centred on (x, y), pops in fast
     * then fades out — the visual "landing" moment of the punch.
     */
    // Fighting-type: impact starburst
    /** White starburst that pops at the impact location — comic-book "POW". */
    private void addImpactStarburst(double x, double y, int movePower,
                                    Timeline timeline) {
        int points = 20;
        Polygon star = new Polygon();
        double outer = 26 + movePower / 6.0;
        double inner = outer * 0.42;
        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double a = (i / (double) (points * 2)) * 2 * Math.PI - Math.PI / 2;
            star.getPoints().addAll(Math.cos(a) * r, Math.sin(a) * r);
        }
        star.setFill(Color.WHITE);
        star.setStroke(Color.color(1.0, 0.95, 0.5));
        star.setStrokeWidth(5);
        star.setEffect(new GaussianBlur(3));
        star.setLayoutX(x);
        star.setLayoutY(y);
        star.setOpacity(0);
        star.setScaleX(0.12);
        star.setScaleY(0.12);
        prepareTransientNode(star);
        battleField.getChildren().add(star);

        KeyFrame pop = new KeyFrame(Duration.millis(38),
            new KeyValue(star.opacityProperty(), 0.95),
            new KeyValue(star.scaleXProperty(), 1.4),
            new KeyValue(star.scaleYProperty(), 1.4));
        KeyFrame settle = new KeyFrame(Duration.millis(140),
            new KeyValue(star.scaleXProperty(), 1.0),
            new KeyValue(star.scaleYProperty(), 1.0));
        KeyFrame fade = new KeyFrame(Duration.millis(310),
            new KeyValue(star.opacityProperty(), 0.0));

        timeline.getKeyFrames().addAll(pop, settle, fade);
        registerCleanup(timeline, star);
    }

    // -
    // Helpers (same pattern as sibling effect classes)
    // -

    private void prepareTransientNode(Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private void registerCleanup(Timeline timeline, Node node) {
        EventHandler<ActionEvent> prev = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (prev != null) prev.handle(e);
        });
    }
}
