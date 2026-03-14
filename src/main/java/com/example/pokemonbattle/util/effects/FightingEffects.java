// FightingEffects.java
package com.example.pokemonbattle.util.effects;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

public class FightingEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Cached so the PNG is only decoded once per JVM run
    private static Image punchImageCache = null;

    public FightingEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Full fighting-type impact: punch image + white impact starburst.
     * Called for pure Fighting-type moves (Mach Punch, Close Combat, …).
     */
    public void createImpactEffect(double x, double y, String moveName,
                                   int movePower, Timeline timeline) {
        addPunchImage(x, y, timeline);
        addImpactStarburst(x, y, movePower, timeline);
    }

    /**
     * Called for non-fighting type punch moves (fire-punch, thunder-punch,
     * ice-punch, mega-punch …) to overlay the punch image + matching elemental
     * sparks on top of the existing type-specific effect.
     */
    public void addPunchImageAndOverlay(double x, double y, String moveName,
                                        String moveType, Timeline timeline) {
        addPunchImage(x, y, timeline);

        if (moveType.equals("fire") && moveName.contains("punch")) {
            addFirePunchEmbers(x, y, timeline);
        } else if (moveType.equals("electric") && moveName.contains("punch")) {
            addThunderPunchSparks(x, y, timeline);
        } else if (moveType.equals("ice") && moveName.contains("punch")) {
            addIcePunchShards(x, y, timeline);
        }
        // Other punch types (psychic-punch variants, etc.) just get the image
    }

    // -----------------------------------------------------------------------
    // Punch image
    // -----------------------------------------------------------------------

    /**
     * Loads punch.png once (cached), places it centred on (x, y), pops in fast
     * then fades out — the visual "landing" moment of the punch.
     */
    private void addPunchImage(double x, double y, Timeline timeline) {
        try {
            Image img = loadPunchImage();
            if (img == null) return;

            ImageView iv = new ImageView(img);
            iv.setFitWidth(82);
            iv.setFitHeight(82);
            iv.setPreserveRatio(true);
            // Centre the image on the impact coordinate; offset slightly upward
            iv.setLayoutX(x - 41);
            iv.setLayoutY(y - 52);
            iv.setOpacity(0);
            iv.setScaleX(0.35);
            iv.setScaleY(0.35);
            prepareTransientNode(iv);
            battleField.getChildren().add(iv);

            // 0 ms  → invisible, tiny
            // 35 ms → fully visible, slight over-scale (pop)
            // 115 ms → settled at 1×
            // 330 ms → faded to 0
            KeyFrame appear = new KeyFrame(Duration.millis(35),
                new KeyValue(iv.opacityProperty(), 1.0),
                new KeyValue(iv.scaleXProperty(), 1.25),
                new KeyValue(iv.scaleYProperty(), 1.25));
            KeyFrame settle = new KeyFrame(Duration.millis(115),
                new KeyValue(iv.scaleXProperty(), 1.0),
                new KeyValue(iv.scaleYProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(330),
                new KeyValue(iv.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(appear, settle, fade);
            registerCleanup(timeline, iv);
        } catch (Exception ignored) {
            // Animation is non-critical; silently skip if asset is missing
        }
    }

    private static Image loadPunchImage() {
        if (punchImageCache == null) {
            var url = FightingEffects.class.getResource(
                    "/com/example/pokemonbattle/assets/punch.png");
            if (url != null) {
                punchImageCache = new Image(url.toExternalForm());
            }
        }
        return punchImageCache;
    }

    // -----------------------------------------------------------------------
    // Fighting-type: impact starburst
    // -----------------------------------------------------------------------

    /** White starburst that pops at the impact location — comic-book "POW". */
    private void addImpactStarburst(double x, double y, int movePower,
                                    Timeline timeline) {
        int points = 8;
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
        star.setStrokeWidth(2);
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

    // -----------------------------------------------------------------------
    // Elemental overlays — appear around the punch impact moment
    // -----------------------------------------------------------------------

    /** 7 small ember polygons that burst outward from the punch point. */
    private void addFirePunchEmbers(double x, double y, Timeline timeline) {
        int count = 7;
        for (int i = 0; i < count; i++) {
            Polygon ember = new Polygon(0.0, 0.0, -5.0, -9.0, 0.0, -18.0, 5.0, -9.0);
            Color col = (i % 2 == 0) ? Color.ORANGERED : Color.color(1.0, 0.55, 0.0);
            ember.setFill(col);
            ember.setEffect(new GaussianBlur(3));

            double angle = (i / (double) count) * 2 * Math.PI;
            ember.setLayoutX(x + Math.cos(angle) * 10);
            ember.setLayoutY(y + Math.sin(angle) * 10);
            ember.setOpacity(0);
            ember.setRotate(Math.toDegrees(angle));
            prepareTransientNode(ember);
            battleField.getChildren().add(ember);

            // Start staggered slightly after punch image appears
            int delay = i * 20 + 35;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(ember.opacityProperty(), 1.0));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 185),
                new KeyValue(ember.layoutXProperty(), x + Math.cos(angle) * 55),
                new KeyValue(ember.layoutYProperty(), y + Math.sin(angle) * 55),
                new KeyValue(ember.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(appear, burst);
            registerCleanup(timeline, ember);
        }
    }

    /** 9 yellow spark lines that flash outward from the punch point. */
    private void addThunderPunchSparks(double x, double y, Timeline timeline) {
        int count = 9;
        for (int i = 0; i < count; i++) {
            Line spark = new Line();
            spark.setStroke(Color.YELLOW);
            spark.setStrokeWidth(3 + random.nextDouble() * 2);
            spark.setEffect(new DropShadow(8, Color.GOLD));

            double angle = (i / (double) count) * 2 * Math.PI + random.nextDouble() * 0.35;
            double len = 18 + random.nextDouble() * 18;
            double ox = x + Math.cos(angle) * 6;
            double oy = y + Math.sin(angle) * 6;
            spark.setStartX(ox);
            spark.setStartY(oy);
            spark.setEndX(ox + Math.cos(angle) * len);
            spark.setEndY(oy + Math.sin(angle) * len);
            spark.setOpacity(0);
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);

            int delay = i * 14 + 20;
            KeyFrame on   = new KeyFrame(Duration.millis(delay),
                new KeyValue(spark.opacityProperty(), 1.0));
            KeyFrame off  = new KeyFrame(Duration.millis(delay + 55),
                new KeyValue(spark.opacityProperty(), 0.0));
            KeyFrame on2  = new KeyFrame(Duration.millis(delay + 85),
                new KeyValue(spark.opacityProperty(), 0.75));
            KeyFrame off2 = new KeyFrame(Duration.millis(delay + 130),
                new KeyValue(spark.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(on, off, on2, off2);
            registerCleanup(timeline, spark);
        }
    }

    /**
     * 5 small snowflakes (3 crossing lines each) + 4 ice shards that fly
     * outward from the punch point.
     */
    private void addIcePunchShards(double x, double y, Timeline timeline) {
        // Snowflakes
        for (int i = 0; i < 5; i++) {
            double cx = x + (random.nextDouble() - 0.5) * 44;
            double cy = y + (random.nextDouble() - 0.5) * 44;
            double len = 9 + random.nextDouble() * 6;
            for (int j = 0; j < 3; j++) {
                Line arm = new Line();
                arm.setStroke(Color.LIGHTCYAN);
                arm.setStrokeWidth(2.5);
                arm.setEffect(new DropShadow(5, Color.DEEPSKYBLUE));
                double a = j * Math.PI / 3;
                arm.setStartX(cx - Math.cos(a) * len);
                arm.setStartY(cy - Math.sin(a) * len);
                arm.setEndX(cx + Math.cos(a) * len);
                arm.setEndY(cy + Math.sin(a) * len);
                arm.setOpacity(0);
                prepareTransientNode(arm);
                battleField.getChildren().add(arm);

                int delay = i * 28 + 20;
                KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(arm.opacityProperty(), 0.9));
                KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(arm.opacityProperty(), 0.0));
                timeline.getKeyFrames().addAll(appear, fade);
                registerCleanup(timeline, arm);
            }
        }

        // Ice shards flying outward
        for (int i = 0; i < 4; i++) {
            Polygon shard = new Polygon(0.0, 0.0, -5.0, -11.0, 0.0, -18.0, 5.0, -11.0);
            shard.setFill(Color.color(0.66, 0.92, 1.0));
            shard.setEffect(new DropShadow(5, Color.CYAN));

            double angle = (i / 4.0) * 2 * Math.PI;
            shard.setLayoutX(x + Math.cos(angle) * 12);
            shard.setLayoutY(y + Math.sin(angle) * 12);
            shard.setRotate(Math.toDegrees(angle));
            shard.setOpacity(0);
            prepareTransientNode(shard);
            battleField.getChildren().add(shard);

            int delay = i * 25 + 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(shard.opacityProperty(), 1.0));
            KeyFrame shoot = new KeyFrame(Duration.millis(delay + 215),
                new KeyValue(shard.layoutXProperty(), x + Math.cos(angle) * 50),
                new KeyValue(shard.layoutYProperty(), y + Math.sin(angle) * 50),
                new KeyValue(shard.opacityProperty(), 0.0));
            timeline.getKeyFrames().addAll(appear, shoot);
            registerCleanup(timeline, shard);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers (same pattern as sibling effect classes)
    // -----------------------------------------------------------------------

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
