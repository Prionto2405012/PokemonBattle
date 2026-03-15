// SteelEffects.java
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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class SteelEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Steel colour palette
    private static final Color STEEL_SILVER  = Color.web("#9E9E9E");
    private static final Color STEEL_LIGHT   = Color.web("#E0E0E0");
    private static final Color STEEL_DARK    = Color.web("#424242");
    private static final Color STEEL_BLUE    = Color.web("#607D8B");
    private static final Color STEEL_SHINE   = Color.web("#F5F5F5");
    private static final Color STEEL_CHROME  = Color.web("#B0BEC5");
    private static final Color STEEL_DEEP    = Color.web("#263238");
    private static final Color STEEL_SPARK   = Color.web("#FFEB3B");

    public SteelEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // -----------------------------------------------------------------
    // Public API – single-point overload (melee / contact moves)
    // -----------------------------------------------------------------

    public void createImpactEffect(double x, double y, String moveName,
                                   int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    // -----------------------------------------------------------------
    // Public API – full signature (all steel moves)
    // -----------------------------------------------------------------

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.4, 1.8);

        switch (moveName) {
            // Heavy metal melee impacts
            case "iron-head"      -> addMetalImpact(startX, startY, endX, endY, intensity, timeline);
            case "iron-tail"      -> addMetalImpact(startX, startY, endX, endY, intensity, timeline);
            case "bullet-punch"   -> addMetalImpact(startX, startY, endX, endY, intensity * 0.9, timeline);
            case "meteor-mash"    -> addMetalImpact(startX, startY, endX, endY, intensity * 1.2, timeline);
            case "smart-strike"   -> addMetalImpact(startX, startY, endX, endY, intensity, timeline);

            // Wing / blade melee
            case "steel-wing"     -> addSteelSlash(endX, endY, intensity, timeline);

            // Gear grind – spinning metallic discs
            case "gear-grind"     -> addGearGrind(startX, startY, endX, endY, intensity, timeline);

            // Heavy slam – weight drop
            case "heavy-slam"     -> addHeavySlam(endX, endY, intensity, timeline);

            // Ranged beam / projectile
            case "flash-cannon"   -> addFlashCannon(startX, startY, endX, endY, intensity, timeline);
            case "magnet-bomb"    -> addMagnetBomb(startX, startY, endX, endY, intensity, timeline);
            case "anchor-shot"    -> addMagnetBomb(startX, startY, endX, endY, intensity * 0.85, timeline);
            case "gyro-ball"      -> addGyroBall(startX, startY, endX, endY, intensity, timeline);

            default               -> addDefaultSteelClash(endX, endY, intensity, timeline);
        }
    }

    // -----------------------------------------------------------------
    // Public API – ranged lead effect
    // -----------------------------------------------------------------

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.4, 1.8);
        switch (moveName) {
            case "flash-cannon"  -> addFlashCannon(startX, startY, endX, endY, intensity, timeline);
            case "gyro-ball"     -> addGyroBall(startX, startY, endX, endY, intensity, timeline);
            default              -> addMagnetBomb(startX, startY, endX, endY, intensity, timeline);
        }
    }

    // =================================================================
    // Metal impact – sparks and ring on heavy metallic strike
    // =================================================================

    private void addMetalImpact(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        // Chrome shards along approach
        int shardCount = (int) (5 + 4 * intensity);
        for (int i = 0; i < shardCount; i++) {
            double t = (i + 0.5) / shardCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 14;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 14;

            Rectangle shard = new Rectangle(
                    4 + random.nextDouble() * 6 * intensity,
                    3 + random.nextDouble() * 3 * intensity);
            shard.setFill(i % 2 == 0 ? STEEL_CHROME : STEEL_LIGHT);
            shard.setStroke(STEEL_DARK.deriveColor(0, 1, 1, 0.4));
            shard.setStrokeWidth(0.8);
            shard.setEffect(new DropShadow(4, STEEL_DARK));
            shard.setX(tx - shard.getWidth() / 2);
            shard.setY(ty - shard.getHeight() / 2);
            shard.setRotate(random.nextDouble() * 360);
            shard.setOpacity(0);
            prepareTransientNode(shard);
            battleField.getChildren().add(shard);

            int delay = (int) (t * 130);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(shard.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(shard.xProperty(), shard.getX() + (random.nextDouble() - 0.5) * 20),
                    new KeyValue(shard.yProperty(), shard.getY() - 8 - random.nextDouble() * 14),
                    new KeyValue(shard.rotateProperty(), shard.getRotate() + 180),
                    new KeyValue(shard.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, shard);
        }

        // Yellow impact sparks at target
        addMetalSparks(ex, ey, intensity, 90, timeline);

        // Metal clang ring
        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(STEEL_SILVER.deriveColor(0, 1, 1, 0.7));
        ring.setStrokeWidth(3 + intensity);
        ring.setCenterX(ex);
        ring.setCenterY(ey);
        ring.setEffect(new GaussianBlur(3));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        double ringR = 28 + 18 * intensity;
        KeyFrame rAppear = new KeyFrame(Duration.millis(90),
                new KeyValue(ring.opacityProperty(), 0.8));
        KeyFrame rExpand = new KeyFrame(Duration.millis(220),
                new KeyValue(ring.radiusProperty(), ringR),
                new KeyValue(ring.opacityProperty(), 0.3));
        KeyFrame rFade = new KeyFrame(Duration.millis(320),
                new KeyValue(ring.radiusProperty(), ringR * 1.3),
                new KeyValue(ring.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(rAppear, rExpand, rFade);
        registerCleanup(timeline, ring);

        addSteelFlash(ex, ey, 16 + 10 * intensity, STEEL_SHINE, 90, 180, timeline);
    }

    // =================================================================
    // Steel slash – wing/blade sharp cuts
    // =================================================================

    private void addSteelSlash(double x, double y, double intensity, Timeline timeline) {
        int slashCount = (int) (3 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle = -50 + i * (100.0 / Math.max(slashCount - 1, 1));
            double rad = Math.toRadians(angle);
            double slashLen = 26 + 16 * intensity;

            Line slash = new Line(
                    x - Math.cos(rad) * slashLen * 0.5,
                    y - Math.sin(rad) * slashLen * 0.5,
                    x + Math.cos(rad) * slashLen * 0.5,
                    y + Math.sin(rad) * slashLen * 0.5);
            slash.setStroke(i % 2 == 0 ? STEEL_CHROME : STEEL_LIGHT);
            slash.setStrokeWidth(3.5 + 1.5 * intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(10 + 4 * intensity, STEEL_BLUE));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = 60 + i * 32;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(slash.opacityProperty(), 1.0));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 80),
                    new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.7),
                    new KeyValue(slash.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(slash.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, slash);
        }

        addMetalSparks(x, y, intensity, 60, timeline);
        addSteelFlash(x, y, 14 + 10 * intensity, STEEL_SHINE, 60, 180, timeline);
    }

    // =================================================================
    // Gear grind – spinning metallic discs hurled at target
    // =================================================================

    private void addGearGrind(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int gearCount = (int) (2 + intensity);
        for (int i = 0; i < gearCount; i++) {
            Circle gear = new Circle(10 + 5 * intensity, STEEL_SILVER);
            gear.setStroke(STEEL_DARK);
            gear.setStrokeWidth(2.5);
            gear.setEffect(new DropShadow(8, STEEL_DEEP));
            gear.setCenterX(sx + (random.nextDouble() - 0.5) * 16);
            gear.setCenterY(sy + (random.nextDouble() - 0.5) * 16);
            gear.setOpacity(0);
            prepareTransientNode(gear);
            battleField.getChildren().add(gear);

            int delay = i * 80;
            double tx = ex + (random.nextDouble() - 0.5) * 18;
            double ty = ey + (random.nextDouble() - 0.5) * 18;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(gear.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(gear.centerXProperty(), tx),
                    new KeyValue(gear.centerYProperty(), ty));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(gear.opacityProperty(), 0),
                    new KeyValue(gear.radiusProperty(), gear.getRadius() * 1.4));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, gear);
        }

        addMetalSparks(ex, ey, intensity, gearCount * 80, timeline);
        addSteelFlash(ex, ey, 16 + 10 * intensity, STEEL_SHINE, gearCount * 80, 180, timeline);
    }

    // =================================================================
    // Heavy slam – seismic impact with shockwave
    // =================================================================

    private void addHeavySlam(double x, double y, double intensity, Timeline timeline) {
        // Ground-level shockwave ellipse
        Ellipse shockwave = new Ellipse(0, 0);
        shockwave.setFill(Color.TRANSPARENT);
        shockwave.setStroke(STEEL_SILVER.deriveColor(0, 1, 1, 0.7));
        shockwave.setStrokeWidth(5 + 2 * intensity);
        shockwave.setEffect(new GaussianBlur(5));
        shockwave.setCenterX(x);
        shockwave.setCenterY(y + 10);
        shockwave.setOpacity(0);
        prepareTransientNode(shockwave);
        battleField.getChildren().add(shockwave);

        double maxRX = 55 + 30 * intensity;
        double maxRY = 18 + 10 * intensity;

        KeyFrame swAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(shockwave.opacityProperty(), 0.85));
        KeyFrame swExpand = new KeyFrame(Duration.millis(220),
                new KeyValue(shockwave.radiusXProperty(), maxRX),
                new KeyValue(shockwave.radiusYProperty(), maxRY),
                new KeyValue(shockwave.opacityProperty(), 0.35));
        KeyFrame swFade = new KeyFrame(Duration.millis(360),
                new KeyValue(shockwave.radiusXProperty(), maxRX * 1.4),
                new KeyValue(shockwave.radiusYProperty(), maxRY * 1.4),
                new KeyValue(shockwave.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(swAppear, swExpand, swFade);
        registerCleanup(timeline, shockwave);

        // Metal debris scatter
        int debrisCount = (int) (6 + 4 * intensity);
        for (int i = 0; i < debrisCount; i++) {
            Rectangle debris = new Rectangle(3 + random.nextDouble() * 5 * intensity,
                    3 + random.nextDouble() * 4 * intensity);
            debris.setFill(i % 2 == 0 ? STEEL_SILVER : STEEL_CHROME);
            debris.setX(x - debris.getWidth() / 2);
            debris.setY(y - debris.getHeight() / 2);
            debris.setRotate(random.nextDouble() * 360);
            debris.setOpacity(0);
            prepareTransientNode(debris);
            battleField.getChildren().add(debris);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 14 + random.nextDouble() * 22 * intensity;
            int delay = i * 22;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(debris.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(debris.xProperty(), x + Math.cos(angle) * dist - debris.getWidth() / 2),
                    new KeyValue(debris.yProperty(), y + Math.sin(angle) * dist - debris.getHeight() / 2),
                    new KeyValue(debris.rotateProperty(), debris.getRotate() + 200));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(debris.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, scatter, fade);
            registerCleanup(timeline, debris);
        }

        addSteelFlash(x, y, 24 + 14 * intensity, STEEL_SHINE, 0, 200, timeline);
    }

    // =================================================================
    // Flash cannon – bright silver energy beam
    // =================================================================

    private void addFlashCannon(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {
        // Main beam
        Line beam = new Line(sx, sy, sx, sy);
        beam.setStroke(STEEL_SHINE.deriveColor(0, 1, 1, 0.92));
        beam.setStrokeWidth(7 + 3 * intensity);
        beam.setEffect(new DropShadow(16 + 6 * intensity, STEEL_CHROME));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        // Bright core line
        Line core = new Line(sx, sy, sx, sy);
        core.setStroke(Color.WHITE);
        core.setStrokeWidth(3 + intensity);
        core.setEffect(new GaussianBlur(2));
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(beam.opacityProperty(), 0.9),
                new KeyValue(core.opacityProperty(), 1.0));
        KeyFrame bExtend = new KeyFrame(Duration.millis(200),
                new KeyValue(beam.endXProperty(), ex),
                new KeyValue(beam.endYProperty(), ey),
                new KeyValue(core.endXProperty(), ex),
                new KeyValue(core.endYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(340),
                new KeyValue(beam.opacityProperty(), 0),
                new KeyValue(core.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bExtend, bFade);
        registerCleanup(timeline, beam);
        registerCleanup(timeline, core);

        // Glint particles along beam
        int count = (int) (6 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;
            double wx = sx + (ex - sx) * t + (random.nextDouble() - 0.5) * 10;
            double wy = sy + (ey - sy) * t + (random.nextDouble() - 0.5) * 10;

            Circle glint = new Circle(3 + random.nextDouble() * 3, STEEL_SHINE);
            glint.setEffect(new GaussianBlur(2));
            glint.setCenterX(wx);
            glint.setCenterY(wy);
            glint.setOpacity(0);
            prepareTransientNode(glint);
            battleField.getChildren().add(glint);

            int delay = (int) (t * 180) + 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(glint.opacityProperty(), 0.9));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(glint.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, glint);
        }

        addSteelFlash(ex, ey, 22 + 12 * intensity, STEEL_SHINE, 180, 200, timeline);
    }

    // =================================================================
    // Magnet bomb – magnetic projectile with metallic clang
    // =================================================================

    private void addMagnetBomb(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        // Metallic sphere projectile
        Circle bomb = new Circle(8 + 4 * intensity, STEEL_SILVER);
        bomb.setStroke(STEEL_DARK);
        bomb.setStrokeWidth(2);
        bomb.setEffect(new DropShadow(10, STEEL_DEEP));
        bomb.setCenterX(sx);
        bomb.setCenterY(sy);
        bomb.setOpacity(0);
        prepareTransientNode(bomb);
        battleField.getChildren().add(bomb);

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(bomb.opacityProperty(), 0.95));
        KeyFrame bTravel = new KeyFrame(Duration.millis(220),
                new KeyValue(bomb.centerXProperty(), ex),
                new KeyValue(bomb.centerYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(300),
                new KeyValue(bomb.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bTravel, bFade);
        registerCleanup(timeline, bomb);

        // Magnetic field rings around bomb during travel
        int fieldCount = (int) (2 + intensity);
        for (int i = 0; i < fieldCount; i++) {
            Circle field = new Circle(bomb.getRadius() * (1.4 + i * 0.4), Color.TRANSPARENT);
            field.setStroke(STEEL_BLUE.deriveColor(0, 1, 1, 0.45 - i * 0.1));
            field.setStrokeWidth(1.5);
            field.setEffect(new GaussianBlur(3));
            field.setCenterX(sx);
            field.setCenterY(sy);
            field.setOpacity(0);
            prepareTransientNode(field);
            battleField.getChildren().add(field);

            int delay = i * 30;
            KeyFrame fAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(field.opacityProperty(), 0.6));
            KeyFrame fTravel = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(field.centerXProperty(), ex),
                    new KeyValue(field.centerYProperty(), ey));
            KeyFrame fFade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(field.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(fAppear, fTravel, fFade);
            registerCleanup(timeline, field);
        }

        // Impact explosion
        addMetalSparks(ex, ey, intensity, 220, timeline);
        addSteelFlash(ex, ey, 18 + 10 * intensity, STEEL_SHINE, 220, 200, timeline);
    }

    // =================================================================
    // Gyro ball – spinning metallic sphere growing on approach
    // =================================================================

    private void addGyroBall(double sx, double sy, double ex, double ey,
                             double intensity, Timeline timeline) {
        Circle ball = new Circle(6 + 3 * intensity, STEEL_CHROME);
        ball.setStroke(STEEL_SILVER);
        ball.setStrokeWidth(2.5);
        ball.setEffect(new DropShadow(10, STEEL_DEEP));
        ball.setCenterX(sx);
        ball.setCenterY(sy);
        ball.setOpacity(0);
        prepareTransientNode(ball);
        battleField.getChildren().add(ball);

        double maxRadius = 14 + 8 * intensity;
        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(ball.opacityProperty(), 0.95));
        KeyFrame bGrow = new KeyFrame(Duration.millis(130),
                new KeyValue(ball.radiusProperty(), maxRadius));
        KeyFrame bTravel = new KeyFrame(Duration.millis(240),
                new KeyValue(ball.centerXProperty(), ex),
                new KeyValue(ball.centerYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(320),
                new KeyValue(ball.opacityProperty(), 0),
                new KeyValue(ball.radiusProperty(), maxRadius * 1.5));

        timeline.getKeyFrames().addAll(bAppear, bGrow, bTravel, bFade);
        registerCleanup(timeline, ball);

        addMetalSparks(ex, ey, intensity, 240, timeline);
        addSteelFlash(ex, ey, 20 + 12 * intensity, STEEL_SHINE, 240, 200, timeline);
    }

    // =================================================================
    // Default steel clash – sparks and ring
    // =================================================================

    private void addDefaultSteelClash(double x, double y, double intensity, Timeline timeline) {
        addMetalImpact(x, y, x, y, intensity, timeline);
    }

    // =================================================================
    // Shared helpers – metal sparks
    // =================================================================

    private void addMetalSparks(double x, double y, double intensity,
                                int startDelay, Timeline timeline) {
        int count = (int) (6 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double len = 8 + random.nextDouble() * 12 * intensity;
            Line spark = new Line(x, y, x + Math.cos(angle) * len, y + Math.sin(angle) * len);
            spark.setStroke(i % 3 == 0 ? STEEL_SPARK : i % 3 == 1 ? STEEL_SHINE : STEEL_LIGHT);
            spark.setStrokeWidth(1.5 + random.nextDouble() * intensity);
            spark.setOpacity(0);
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);

            int delay = startDelay + i * 16;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(spark.opacityProperty(), 0.95));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 140),
                    new KeyValue(spark.endXProperty(), spark.getEndX() + (random.nextDouble() - 0.5) * 14),
                    new KeyValue(spark.endYProperty(), spark.getEndY() + random.nextDouble() * 16));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(spark.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift, fade);
            registerCleanup(timeline, spark);
        }
    }

    // =================================================================
    // Flash circle helper
    // =================================================================

    private void addSteelFlash(double x, double y, double radius, Color color,
                               int startDelay, int fadeDuration, Timeline timeline) {
        Circle flash = new Circle(0, color.deriveColor(0, 1, 1, 0.7));
        flash.setCenterX(x);
        flash.setCenterY(y);
        flash.setEffect(new GaussianBlur(radius * 0.4));
        flash.setOpacity(0);
        prepareTransientNode(flash);
        battleField.getChildren().add(flash);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(startDelay),
                        new KeyValue(flash.opacityProperty(), 0.85),
                        new KeyValue(flash.radiusProperty(), radius)),
                new KeyFrame(Duration.millis(startDelay + fadeDuration),
                        new KeyValue(flash.opacityProperty(), 0)));
        registerCleanup(timeline, flash);
    }

    // =================================================================
    // Utilities
    // =================================================================

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

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
