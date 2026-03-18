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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class DragonEffects {

    private final Pane battleField;
    private final Random random = new Random();

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

    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        switch (moveName) {
            case "dragon-claw"    -> addDragonClaw(startX, startY, endX, endY, intensity, timeline);
            case "spacial-rend"   -> addDragonClaw(startX, startY, endX, endY, intensity * 1.2, timeline);
            case "dual-chop"      -> addDragonClaw(startX, startY, endX, endY, intensity, timeline);
            case "breaking-swipe" -> addDragonClaw(startX, startY, endX, endY, intensity * 0.85, timeline);
            case "dragon-rush"    -> addDragonRush(startX, startY, endX, endY, intensity, timeline);
            case "outrage"        -> addDragonRush(startX, startY, endX, endY, intensity * 1.15, timeline);
            case "dragon-pulse"   -> addDragonBeam(startX, startY, endX, endY, intensity, timeline);
            case "dragon-breath"  -> addDragonBreath(startX, startY, endX, endY, intensity, timeline);
            case "dragon-rage"    -> addDragonBeam(startX, startY, endX, endY, intensity * 0.8, timeline);
            case "draco-meteor"   -> addDracoMeteor(startX, startY, endX, endY, intensity, timeline);
            case "scale-shot"     -> addScaleShot(startX, startY, endX, endY, intensity, timeline);
            default               -> addDefaultDragonBurst(endX, endY, intensity, timeline);
        }
    }

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        switch (moveName) {
            case "draco-meteor"  -> addDracoMeteor(startX, startY, endX, endY, intensity, timeline);
            case "dragon-breath" -> addDragonBreath(startX, startY, endX, endY, intensity, timeline);
            case "scale-shot"    -> addScaleShot(startX, startY, endX, endY, intensity, timeline);
            default              -> addDragonBeam(startX, startY, endX, endY, intensity, timeline);
        }
    }

    private void addDragonClaw(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        int trailCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 16;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 16;
            Circle energy = new Circle(10 + random.nextDouble() * 3,
                    i % 3 == 0 ? DRAGON_CYAN : i % 3 == 1 ? DRAGON_TEAL : DRAGON_BLUE);
            energy.setEffect(new GaussianBlur(4));
            energy.setCenterX(tx);
            energy.setCenterY(ty);
            energy.setOpacity(0);
            prepareTransientNode(energy);
            battleField.getChildren().add(energy);
            int delay = (int) (t * 140);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(energy.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 180), new KeyValue(energy.opacityProperty(), 0)));
            registerCleanup(timeline, energy);
        }

        int clawCount = (int) (3 + 2 * intensity);
        for (int i = 0; i < clawCount; i++) {
            double offset = (i - (clawCount - 1) / 2.0) * (8 + 4 * intensity);
            Line claw = new Line(ex + offset - 6, ey - 20 - 8 * intensity,
                                 ex + offset + 4, ey + 14 + 8 * intensity);
            claw.setStroke(i % 2 == 0 ? DRAGON_CYAN : DRAGON_GOLD);
            claw.setStrokeWidth(8 + 1.5 * intensity);
            claw.setOpacity(0);
            claw.setEffect(new DropShadow(10 + 4 * intensity, DRAGON_BLUE));
            prepareTransientNode(claw);
            battleField.getChildren().add(claw);
            int delay = 100 + i * 30;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(claw.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(delay + 80),
                            new KeyValue(claw.strokeWidthProperty(), claw.getStrokeWidth() * 1.6),
                            new KeyValue(claw.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 240), new KeyValue(claw.opacityProperty(), 0)));
            registerCleanup(timeline, claw);
        }

        addDragonFlash(ex, ey, 22 + 10 * intensity, DRAGON_CYAN, 90, 200, timeline);
    }

    private void addDragonRush(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        int auraCount = (int) (13 + 2 * intensity);
        for (int i = 0; i < auraCount; i++) {
            double t = (i + 0.5) / auraCount;
            double ax = sx + (ex - sx) * t;
            double ay = sy + (ey - sy) * t;
            Circle aura = new Circle(0, DRAGON_BLUE.deriveColor(0, 1, 1, 0.5));
            aura.setStroke(DRAGON_GOLD.deriveColor(0, 1, 1, 0.65));
            aura.setStrokeWidth(8 + intensity);
            aura.setCenterX(ax);
            aura.setCenterY(ay);
            aura.setEffect(new GaussianBlur(6 + 2 * intensity));
            aura.setOpacity(0);
            prepareTransientNode(aura);
            battleField.getChildren().add(aura);
            int delay = (int) (t * 120);
            double auraR = 20 + 14 * intensity;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(aura.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 180),
                            new KeyValue(aura.radiusProperty(), auraR),
                            new KeyValue(aura.opacityProperty(), 0.3)),
                    new KeyFrame(Duration.millis(delay + 280), new KeyValue(aura.opacityProperty(), 0)));
            registerCleanup(timeline, aura);
        }

        Circle shock = new Circle(0, DRAGON_GOLD.deriveColor(0, 1, 1, 0.6));
        shock.setEffect(new DropShadow(20 + 8 * intensity, DRAGON_RED));
        shock.setCenterX(ex);
        shock.setCenterY(ey);
        shock.setOpacity(0);
        prepareTransientNode(shock);
        battleField.getChildren().add(shock);
        double shockR = 40 + 20 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(110),
                        new KeyValue(shock.opacityProperty(), 0.9),
                        new KeyValue(shock.radiusProperty(), shockR * 0.25)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(shock.radiusProperty(), shockR),
                        new KeyValue(shock.opacityProperty(), 0.45)),
                new KeyFrame(Duration.millis(360),
                        new KeyValue(shock.radiusProperty(), shockR * 1.5),
                        new KeyValue(shock.opacityProperty(), 0)));
        registerCleanup(timeline, shock);
        addDragonFlash(ex, ey, 34 + 14 * intensity, DRAGON_GOLD, 110, 200, timeline);
    }

    // Dragon pulse / dragon rage — thick Rectangle beam
    private void addDragonBeam(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double angle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist  = Math.hypot(ex - sx, ey - sy);

        // Increase beamH to make the beam thicker
        double beamH = 20 + 6 * intensity;
        double glowH = beamH * 2.0;

        Rectangle glow = new Rectangle(0, glowH);
        glow.setFill(DRAGON_BLUE.deriveColor(0, 1, 1, 0.30));
        glow.setArcWidth(glowH); glow.setArcHeight(glowH);
        glow.setX(sx); glow.setY(sy - glowH / 2);
        glow.setRotate(angle);
        glow.setEffect(new GaussianBlur(glowH * 0.4));
        glow.setOpacity(0);
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);

        Rectangle beam = new Rectangle(0, beamH);
        beam.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, DRAGON_LIGHT.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.5, DRAGON_CYAN.deriveColor(0, 1, 1, 0.90)),
                new Stop(1.0, DRAGON_TEAL.deriveColor(0, 1, 1, 0.85))));
        beam.setArcWidth(beamH); beam.setArcHeight(beamH);
        beam.setX(sx); beam.setY(sy - beamH / 2);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(beamH * 0.9, DRAGON_BLUE));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        double coreH = beamH * 0.35;
        Rectangle core = new Rectangle(0, coreH);
        core.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.9));
        core.setArcWidth(coreH); core.setArcHeight(coreH);
        core.setX(sx); core.setY(sy - coreH / 2);
        core.setRotate(angle);
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),
                        new KeyValue(glow.opacityProperty(), 0.7),
                        new KeyValue(beam.opacityProperty(), 0.92),
                        new KeyValue(core.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(210),
                        new KeyValue(glow.widthProperty(), dist),
                        new KeyValue(beam.widthProperty(), dist),
                        new KeyValue(core.widthProperty(), dist)),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(glow.opacityProperty(), 0.7),
                        new KeyValue(beam.opacityProperty(), 0.92),
                        new KeyValue(core.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(440),
                        new KeyValue(glow.opacityProperty(), 0),
                        new KeyValue(beam.opacityProperty(), 0),
                        new KeyValue(core.opacityProperty(), 0)));
        registerCleanup(timeline, glow);
        registerCleanup(timeline, beam);
        registerCleanup(timeline, core);

        int count = (int) (16 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;
            double px = sx + (ex - sx) * t + (random.nextDouble() - 0.5) * 12;
            double py = sy + (ey - sy) * t + (random.nextDouble() - 0.5) * 12;
            Circle particle = new Circle(10 + random.nextDouble() * 3,
                    i % 2 == 0 ? DRAGON_TEAL : DRAGON_CYAN);
            particle.setEffect(new GaussianBlur(3));
            particle.setCenterX(px);
            particle.setCenterY(py);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);
            int delay = (int) (t * 190) + 20;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(particle.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 190), new KeyValue(particle.opacityProperty(), 0)));
            registerCleanup(timeline, particle);
        }

        addDragonFlash(ex, ey, 26 + 12 * intensity, DRAGON_CYAN, 190, 200, timeline);
    }

    // Dragon breath — wide fan using three thick Rectangle beams
    private void addDragonBreath(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double dist      = Math.hypot(ex - sx, ey - sy);
        double baseAngle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double[] offsets = { -10.0, 0.0, 10.0 };

        for (int b = 0; b < offsets.length; b++) {
            double angle  = baseAngle + offsets[b];
            double beamH  = 14 + (3 - Math.abs(offsets[b] / 5.0)) * intensity; // centre beam is widest
            double glowH  = beamH * 1.8;

            Rectangle glow = new Rectangle(0, glowH);
            glow.setFill(DRAGON_TEAL.deriveColor(0, 1, 1, 0.25));
            glow.setArcWidth(glowH); glow.setArcHeight(glowH);
            glow.setX(sx); glow.setY(sy - glowH / 2);
            glow.setRotate(angle);
            glow.setEffect(new GaussianBlur(glowH * 0.4));
            glow.setOpacity(0);
            prepareTransientNode(glow);
            battleField.getChildren().add(glow);

            Rectangle beam = new Rectangle(0, beamH);
            beam.setFill(DRAGON_TEAL.deriveColor(0, 1, 1, 0.80));
            beam.setArcWidth(beamH); beam.setArcHeight(beamH);
            beam.setX(sx); beam.setY(sy - beamH / 2);
            beam.setRotate(angle);
            beam.setEffect(new GaussianBlur(4 + intensity));
            beam.setOpacity(0);
            prepareTransientNode(beam);
            battleField.getChildren().add(beam);

            int beamDelay = b * 18;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(beamDelay + 20),
                            new KeyValue(glow.opacityProperty(), 0.7),
                            new KeyValue(beam.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(beamDelay + 200),
                            new KeyValue(glow.widthProperty(), dist),
                            new KeyValue(beam.widthProperty(), dist)),
                    new KeyFrame(Duration.millis(beamDelay + 340),
                            new KeyValue(glow.opacityProperty(), 0),
                            new KeyValue(beam.opacityProperty(), 0)));
            registerCleanup(timeline, glow);
            registerCleanup(timeline, beam);
        }

        addDragonFlash(ex, ey, 28 + 10 * intensity, DRAGON_TEAL, 180, 200, timeline);
    }

    private void addDracoMeteor(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double meteorStartX = ex + (random.nextDouble() - 0.5) * 30;
        double meteorStartY = ey - 180 - 40 * intensity;

        Circle meteor = new Circle(18 + 6 * intensity, DRAGON_GOLD);
        meteor.setEffect(new DropShadow(22 + 8 * intensity, DRAGON_RED));
        meteor.setCenterX(meteorStartX);
        meteor.setCenterY(meteorStartY);
        meteor.setOpacity(0);
        prepareTransientNode(meteor);
        battleField.getChildren().add(meteor);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),  new KeyValue(meteor.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(meteor.centerXProperty(), ex),
                        new KeyValue(meteor.centerYProperty(), ey)),
                new KeyFrame(Duration.millis(360), new KeyValue(meteor.opacityProperty(), 0)));
        registerCleanup(timeline, meteor);

        int trailCount = (int) (16 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = meteorStartX + (ex - meteorStartX) * t + (random.nextDouble() - 0.5) * 14;
            double ty = meteorStartY + (ey - meteorStartY) * t;
            Circle trail = new Circle(8 + random.nextDouble() * 4,
                    i % 2 == 0 ? DRAGON_RED : DRAGON_GOLD);
            trail.setEffect(new GaussianBlur(4));
            trail.setCenterX(tx);
            trail.setCenterY(ty);
            trail.setOpacity(0);
            prepareTransientNode(trail);
            battleField.getChildren().add(trail);
            int delay = (int) (t * 240);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(trail.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 200), new KeyValue(trail.opacityProperty(), 0)));
            registerCleanup(timeline, trail);
        }

        Circle explosion = new Circle(0, DRAGON_GOLD.deriveColor(0, 1, 1, 0.75));
        explosion.setEffect(new DropShadow(24 + 10 * intensity, DRAGON_RED));
        explosion.setCenterX(ex);
        explosion.setCenterY(ey);
        explosion.setOpacity(0);
        prepareTransientNode(explosion);
        battleField.getChildren().add(explosion);
        double exR = 36 + 22 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(290),
                        new KeyValue(explosion.opacityProperty(), 1.0),
                        new KeyValue(explosion.radiusProperty(), exR * 0.3)),
                new KeyFrame(Duration.millis(380),
                        new KeyValue(explosion.radiusProperty(), exR),
                        new KeyValue(explosion.opacityProperty(), 0.5)),
                new KeyFrame(Duration.millis(500),
                        new KeyValue(explosion.radiusProperty(), exR * 1.5),
                        new KeyValue(explosion.opacityProperty(), 0)));
        registerCleanup(timeline, explosion);
        addDragonFlash(ex, ey, 32 + 16 * intensity, DRAGON_GOLD, 290, 220, timeline);
    }

    private void addScaleShot(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        int count = (int) (14 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon scale = buildScalePolygon(10 + random.nextDouble() * 5 * intensity);
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
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(scale.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(scale.layoutXProperty(), tx),
                            new KeyValue(scale.layoutYProperty(), ty),
                            new KeyValue(scale.rotateProperty(), scale.getRotate() + 120)),
                    new KeyFrame(Duration.millis(delay + 280), new KeyValue(scale.opacityProperty(), 0)));
            registerCleanup(timeline, scale);
        }
        addDragonFlash(ex, ey, 20 + 8 * intensity, DRAGON_CYAN, count * 45, 180, timeline);
    }

    private Polygon buildScalePolygon(double size) {
        return new Polygon(0, -size, size * 0.7, 0, 0, size * 0.8, -size * 0.7, 0);
    }

    private void addDefaultDragonBurst(double x, double y, double intensity, Timeline timeline) {
        addDragonRush(x, y, x, y, intensity, timeline);
    }

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