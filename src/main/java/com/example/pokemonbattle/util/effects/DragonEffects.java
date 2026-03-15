// DragonEffects.java
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

public class DragonEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Dragon colour palette
    private static final Color DRAGON_BLUE   = Color.web("#1A237E");
    private static final Color DRAGON_TEAL   = Color.web("#006064");
    private static final Color DRAGON_CYAN   = Color.web("#00BCD4");
    private static final Color DRAGON_GOLD   = Color.web("#F57F17");
    private static final Color DRAGON_RED    = Color.web("#B71C1C");
    private static final Color DRAGON_PURPLE = Color.web("#4A148C");
    private static final Color DRAGON_LIGHT  = Color.web("#B3E5FC");
    private static final Color DRAGON_DARK   = Color.web("#0D1B2A");

    public DragonEffects(Pane battleField) {
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
    // Public API – full signature (all dragon moves)
    // -----------------------------------------------------------------

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.4, 1.8);

        switch (moveName) {
            // Claw slash melee
            case "dragon-claw"    -> addDragonClaw(startX, startY, endX, endY, intensity, timeline);
            case "spacial-rend"   -> addDragonClaw(startX, startY, endX, endY, intensity * 1.2, timeline);
            case "dual-chop"      -> addDragonClaw(startX, startY, endX, endY, intensity, timeline);
            case "breaking-swipe" -> addDragonClaw(startX, startY, endX, endY, intensity * 0.85, timeline);

            // Charging rush
            case "dragon-rush"    -> addDragonRush(startX, startY, endX, endY, intensity, timeline);
            case "outrage"        -> addDragonRush(startX, startY, endX, endY, intensity * 1.15, timeline);

            // Ranged energy beam
            case "dragon-pulse"   -> addDragonBeam(startX, startY, endX, endY, intensity, timeline);
            case "dragon-breath"  -> addDragonBreath(startX, startY, endX, endY, intensity, timeline);
            case "dragon-rage"    -> addDragonBeam(startX, startY, endX, endY, intensity * 0.8, timeline);

            // Meteor impact from above
            case "draco-meteor"   -> addDracoMeteor(startX, startY, endX, endY, intensity, timeline);

            // Multi-scale projectiles
            case "scale-shot"     -> addScaleShot(startX, startY, endX, endY, intensity, timeline);

            default               -> addDefaultDragonBurst(endX, endY, intensity, timeline);
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
            case "draco-meteor" -> addDracoMeteor(startX, startY, endX, endY, intensity, timeline);
            case "dragon-breath" -> addDragonBreath(startX, startY, endX, endY, intensity, timeline);
            case "scale-shot"   -> addScaleShot(startX, startY, endX, endY, intensity, timeline);
            default             -> addDragonBeam(startX, startY, endX, endY, intensity, timeline);
        }
    }

    // =================================================================
    // Dragon claw – deep tear marks with draconic energy
    // =================================================================

    private void addDragonClaw(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        // Energy trail along approach
        int trailCount = (int) (5 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 16;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 16;

            Circle energy = new Circle(3 + random.nextDouble() * 3,
                    i % 3 == 0 ? DRAGON_CYAN : i % 3 == 1 ? DRAGON_TEAL : DRAGON_BLUE);
            energy.setEffect(new GaussianBlur(4));
            energy.setCenterX(tx);
            energy.setCenterY(ty);
            energy.setOpacity(0);
            prepareTransientNode(energy);
            battleField.getChildren().add(energy);

            int delay = (int) (t * 140);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(energy.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(energy.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, energy);
        }

        // Deep claw tear marks at impact
        int clawCount = (int) (3 + 2 * intensity);
        for (int i = 0; i < clawCount; i++) {
            double offset = (i - (clawCount - 1) / 2.0) * (8 + 4 * intensity);
            Line claw = new Line(ex + offset - 6, ey - 20 - 8 * intensity,
                                 ex + offset + 4, ey + 14 + 8 * intensity);
            claw.setStroke(i % 2 == 0 ? DRAGON_CYAN : DRAGON_GOLD);
            claw.setStrokeWidth(3 + 1.5 * intensity);
            claw.setOpacity(0);
            claw.setEffect(new DropShadow(10 + 4 * intensity, DRAGON_BLUE));
            prepareTransientNode(claw);
            battleField.getChildren().add(claw);

            int delay = 100 + i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(claw.opacityProperty(), 1.0));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 80),
                    new KeyValue(claw.strokeWidthProperty(), claw.getStrokeWidth() * 1.6),
                    new KeyValue(claw.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(claw.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, claw);
        }

        addDragonFlash(ex, ey, 16 + 10 * intensity, DRAGON_CYAN, 90, 200, timeline);
    }

    // =================================================================
    // Dragon rush – full-speed charge with fiery draconic aura
    // =================================================================

    private void addDragonRush(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        // Aura rings expanding from attacker during charge
        int auraCount = (int) (3 + 2 * intensity);
        for (int i = 0; i < auraCount; i++) {
            double t = (i + 0.5) / auraCount;
            double ax = sx + (ex - sx) * t;
            double ay = sy + (ey - sy) * t;

            Circle aura = new Circle(0, DRAGON_BLUE.deriveColor(0, 1, 1, 0.5));
            aura.setStroke(DRAGON_GOLD.deriveColor(0, 1, 1, 0.65));
            aura.setStrokeWidth(3 + intensity);
            aura.setCenterX(ax);
            aura.setCenterY(ay);
            aura.setEffect(new GaussianBlur(6 + 2 * intensity));
            aura.setOpacity(0);
            prepareTransientNode(aura);
            battleField.getChildren().add(aura);

            int delay = (int) (t * 120);
            double auraR = 20 + 14 * intensity;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(aura.opacityProperty(), 0.8));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(aura.radiusProperty(), auraR),
                    new KeyValue(aura.opacityProperty(), 0.3));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(aura.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand, fade);
            registerCleanup(timeline, aura);
        }

        // Impact shockwave at defender
        Circle shock = new Circle(0, DRAGON_GOLD.deriveColor(0, 1, 1, 0.6));
        shock.setEffect(new DropShadow(20 + 8 * intensity, DRAGON_RED));
        shock.setCenterX(ex);
        shock.setCenterY(ey);
        shock.setOpacity(0);
        prepareTransientNode(shock);
        battleField.getChildren().add(shock);

        double shockR = 34 + 20 * intensity;
        KeyFrame sAppear = new KeyFrame(Duration.millis(110),
                new KeyValue(shock.opacityProperty(), 0.9),
                new KeyValue(shock.radiusProperty(), shockR * 0.25));
        KeyFrame sPeak = new KeyFrame(Duration.millis(220),
                new KeyValue(shock.radiusProperty(), shockR),
                new KeyValue(shock.opacityProperty(), 0.45));
        KeyFrame sFade = new KeyFrame(Duration.millis(360),
                new KeyValue(shock.radiusProperty(), shockR * 1.5),
                new KeyValue(shock.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(sAppear, sPeak, sFade);
        registerCleanup(timeline, shock);

        addDragonFlash(ex, ey, 24 + 14 * intensity, DRAGON_GOLD, 110, 200, timeline);
    }

    // =================================================================
    // Dragon pulse / dragon rage – draconic energy beam
    // =================================================================

    private void addDragonBeam(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        // Core beam
        Line beam = new Line(sx, sy, sx, sy);
        beam.setStroke(DRAGON_CYAN.deriveColor(0, 1, 1, 0.88));
        beam.setStrokeWidth(7 + 3 * intensity);
        beam.setEffect(new DropShadow(16 + 6 * intensity, DRAGON_BLUE));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        // Inner bright core
        Line core = new Line(sx, sy, sx, sy);
        core.setStroke(DRAGON_LIGHT.deriveColor(0, 1, 1, 0.95));
        core.setStrokeWidth(3 + intensity);
        core.setEffect(new GaussianBlur(2));
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(beam.opacityProperty(), 0.9),
                new KeyValue(core.opacityProperty(), 1.0));
        KeyFrame bExtend = new KeyFrame(Duration.millis(210),
                new KeyValue(beam.endXProperty(), ex),
                new KeyValue(beam.endYProperty(), ey),
                new KeyValue(core.endXProperty(), ex),
                new KeyValue(core.endYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(350),
                new KeyValue(beam.opacityProperty(), 0),
                new KeyValue(core.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bExtend, bFade);
        registerCleanup(timeline, beam);
        registerCleanup(timeline, core);

        // Energy particles along beam
        int count = (int) (6 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;
            double px = sx + (ex - sx) * t + (random.nextDouble() - 0.5) * 12;
            double py = sy + (ey - sy) * t + (random.nextDouble() - 0.5) * 12;

            Circle particle = new Circle(3 + random.nextDouble() * 3,
                    i % 2 == 0 ? DRAGON_TEAL : DRAGON_CYAN);
            particle.setEffect(new GaussianBlur(3));
            particle.setCenterX(px);
            particle.setCenterY(py);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);

            int delay = (int) (t * 190) + 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(particle.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 190),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, particle);
        }

        addDragonFlash(ex, ey, 22 + 12 * intensity, DRAGON_CYAN, 190, 200, timeline);
    }

    // =================================================================
    // Dragon breath – wide fan of draconic energy
    // =================================================================

    private void addDragonBreath(double sx, double sy, double ex, double ey,
                                 double intensity, Timeline timeline) {
        // Fan of 3 beams spreading from attacker
        double baseAngle = Math.atan2(ey - sy, ex - sx);
        double[] fanAngles = { baseAngle - 0.15, baseAngle, baseAngle + 0.15 };

        for (double angle : fanAngles) {
            Line breath = new Line(sx, sy, sx, sy);
            breath.setStroke(DRAGON_TEAL.deriveColor(0, 1, 1, 0.8));
            breath.setStrokeWidth(4 + 2 * intensity);
            breath.setEffect(new GaussianBlur(4 + intensity));
            breath.setOpacity(0);
            prepareTransientNode(breath);
            battleField.getChildren().add(breath);

            double dist = Math.sqrt((ex - sx) * (ex - sx) + (ey - sy) * (ey - sy));
            double tx = sx + Math.cos(angle) * dist;
            double ty = sy + Math.sin(angle) * dist;

            KeyFrame appear = new KeyFrame(Duration.millis(20),
                    new KeyValue(breath.opacityProperty(), 0.85));
            KeyFrame extend = new KeyFrame(Duration.millis(200),
                    new KeyValue(breath.endXProperty(), tx),
                    new KeyValue(breath.endYProperty(), ty));
            KeyFrame fade = new KeyFrame(Duration.millis(340),
                    new KeyValue(breath.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, extend, fade);
            registerCleanup(timeline, breath);
        }

        addDragonFlash(ex, ey, 20 + 10 * intensity, DRAGON_TEAL, 180, 200, timeline);
    }

    // =================================================================
    // Draco meteor – meteor plunging from above with explosion
    // =================================================================

    private void addDracoMeteor(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {
        // Meteor falling from top
        double meteorStartX = ex + (random.nextDouble() - 0.5) * 30;
        double meteorStartY = ey - 180 - 40 * intensity;

        Circle meteor = new Circle(14 + 6 * intensity, DRAGON_GOLD);
        meteor.setEffect(new DropShadow(22 + 8 * intensity, DRAGON_RED));
        meteor.setCenterX(meteorStartX);
        meteor.setCenterY(meteorStartY);
        meteor.setOpacity(0);
        prepareTransientNode(meteor);
        battleField.getChildren().add(meteor);

        KeyFrame mAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(meteor.opacityProperty(), 1.0));
        KeyFrame mFall = new KeyFrame(Duration.millis(280),
                new KeyValue(meteor.centerXProperty(), ex),
                new KeyValue(meteor.centerYProperty(), ey));
        KeyFrame mFade = new KeyFrame(Duration.millis(360),
                new KeyValue(meteor.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(mAppear, mFall, mFade);
        registerCleanup(timeline, meteor);

        // Meteor trail
        int trailCount = (int) (6 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = meteorStartX + (ex - meteorStartX) * t + (random.nextDouble() - 0.5) * 14;
            double ty = meteorStartY + (ey - meteorStartY) * t;

            Circle trail = new Circle(4 + random.nextDouble() * 4,
                    i % 2 == 0 ? DRAGON_RED : DRAGON_GOLD);
            trail.setEffect(new GaussianBlur(4));
            trail.setCenterX(tx);
            trail.setCenterY(ty);
            trail.setOpacity(0);
            prepareTransientNode(trail);
            battleField.getChildren().add(trail);

            int delay = (int) (t * 240);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(trail.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(trail.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, trail);
        }

        // Explosion on impact
        Circle explosion = new Circle(0, DRAGON_GOLD.deriveColor(0, 1, 1, 0.75));
        explosion.setEffect(new DropShadow(24 + 10 * intensity, DRAGON_RED));
        explosion.setCenterX(ex);
        explosion.setCenterY(ey);
        explosion.setOpacity(0);
        prepareTransientNode(explosion);
        battleField.getChildren().add(explosion);

        double exR = 36 + 22 * intensity;
        KeyFrame eAppear = new KeyFrame(Duration.millis(290),
                new KeyValue(explosion.opacityProperty(), 1.0),
                new KeyValue(explosion.radiusProperty(), exR * 0.3));
        KeyFrame ePeak = new KeyFrame(Duration.millis(380),
                new KeyValue(explosion.radiusProperty(), exR),
                new KeyValue(explosion.opacityProperty(), 0.5));
        KeyFrame eFade = new KeyFrame(Duration.millis(500),
                new KeyValue(explosion.radiusProperty(), exR * 1.5),
                new KeyValue(explosion.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(eAppear, ePeak, eFade);
        registerCleanup(timeline, explosion);

        addDragonFlash(ex, ey, 28 + 16 * intensity, DRAGON_GOLD, 290, 220, timeline);
    }

    // =================================================================
    // Scale shot – rapid scales fired as projectiles
    // =================================================================

    private void addScaleShot(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int count = (int) (4 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon scale = buildScalePolygon(6 + random.nextDouble() * 5 * intensity);
            scale.setFill(i % 3 == 0 ? DRAGON_TEAL : i % 3 == 1 ? DRAGON_BLUE : DRAGON_CYAN);
            scale.setEffect(new DropShadow(5, DRAGON_DARK));
            scale.setLayoutX(sx + (random.nextDouble() - 0.5) * 16);
            scale.setLayoutY(sy + (random.nextDouble() - 0.5) * 16);
            scale.setOpacity(0);

            double targetAngle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
            scale.setRotate(targetAngle + (random.nextDouble() - 0.5) * 20);
            prepareTransientNode(scale);
            battleField.getChildren().add(scale);

            int delay = i * 45;
            double tx = ex + (random.nextDouble() - 0.5) * 20;
            double ty = ey + (random.nextDouble() - 0.5) * 20;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(scale.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(scale.layoutXProperty(), tx),
                    new KeyValue(scale.layoutYProperty(), ty),
                    new KeyValue(scale.rotateProperty(), scale.getRotate() + 120));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(scale.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, scale);
        }

        addDragonFlash(ex, ey, 14 + 8 * intensity, DRAGON_CYAN, count * 45, 180, timeline);
    }

    /** Diamond-shaped scale polygon. */
    private Polygon buildScalePolygon(double size) {
        return new Polygon(0, -size, size * 0.55, 0, 0, size * 0.7, -size * 0.55, 0);
    }

    // =================================================================
    // Default dragon burst
    // =================================================================

    private void addDefaultDragonBurst(double x, double y, double intensity, Timeline timeline) {
        addDragonRush(x, y, x, y, intensity, timeline);
    }

    // =================================================================
    // Flash circle helper
    // =================================================================

    private void addDragonFlash(double x, double y, double radius, Color color,
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
