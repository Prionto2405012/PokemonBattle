// WaterEffects.java
package com.example.pokemonbattle.util.effects;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class WaterEffects {

    private final Pane battleField;
    private final Random random = new Random();

    private static final Color WATER_DEEP   = Color.web("#1565C0");
    private static final Color WATER_MID    = Color.web("#1E88E5");
    private static final Color WATER_LIGHT  = Color.web("#64B5F6");
    private static final Color WATER_FOAM   = Color.web("#B3E5FC");
    private static final Color WATER_CYAN   = Color.CYAN;
    private static final Color WATER_WHITE  = Color.WHITE;
    private static final Color BUBBLE_COLOR = Color.web("#80DEEA");

    public WaterEffects(Pane battleField) {
        this.battleField = battleField;
    }

    public void createImpactEffect(double startX, double startY, double endX, double endY,
            String moveName, int movePower, Timeline timeline) {

        switch (moveName) {
            case "surf", "liquidation", "waterfall", "water-pledge",
                 "brine", "scald", "wave-crash" ->
                    addWaveEffect(startX, startY, endX, endY, movePower, false, false, timeline);
            case "dive" ->
                    addDiveEffect(startX, startY, endX, endY, movePower, timeline);
            case "aqua-jet" ->
                    addWaveEffect(startX, startY, endX, endY, movePower, false, true, timeline);
            case "water-gun", "hydro-pump", "muddy-water", "hydro-cannon" ->
                    addWaterBeam(startX, startY, endX, endY, movePower, timeline);
            case "bubble", "bubble-beam" ->
                    addBubbleBeam(startX, startY, endX, endY, movePower, timeline);
            case "whirlpool" ->
                    addWhirlpool(startX, startY, endX, endY, movePower, timeline);
            case "water-pulse" ->
                    addWaterPulse(startX, startY, endX, endY, movePower, timeline);
            case "aqua-tail" ->
                    addAquaTail(startX, startY, endX, endY, movePower, timeline);
            case "chilling-water" ->
                    addChillingWater(startX, startY, endX, endY, movePower, timeline);
            case "razor-shell", "aqua-cutter" ->
                    addRazorShell(startX, startY, endX, endY, movePower, timeline);
            case "sparkling-aria" ->
                    addSparklingAria(startX, startY, endX, endY, movePower, timeline);
            case "origin-pulse" ->
                    addOriginPulse(startX, startY, endX, endY, movePower, timeline);
            default ->
                    addFallbackBeam(startX, startY, endX, endY, movePower, timeline);
        }
    }

    // ── WAVE ─────────────────────────────────────────────────────────────────

    private void addWaveEffect(double startX, double startY, double endX, double endY,
            int movePower, boolean diveEmerge, boolean aquaJetRide, Timeline timeline) {

        double waveBaseX = endX;
        double waveBaseY = endY + 80;

        int layerCount = 5;
        for (int i = 0; i < layerCount; i++) {
            double w  = 90 + i * 28 + movePower / 8.0;
            double h1 = 140 + i * 20 + movePower / 6.0;

            Rectangle waveLayer = new Rectangle(w, 0);
            waveLayer.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, WATER_FOAM.deriveColor(0, 1, 1, 0.7)),
                    new Stop(0.4, WATER_LIGHT.deriveColor(0, 1, 1, 0.85)),
                    new Stop(1, WATER_DEEP.deriveColor(0, 1, 1, 0.9))));
            waveLayer.setArcWidth(w * 0.6);
            waveLayer.setArcHeight(w * 0.4);
            waveLayer.setEffect(new GaussianBlur(4 + i));
            waveLayer.setOpacity(0);
            waveLayer.setX(waveBaseX - w / 2.0);
            waveLayer.setY(waveBaseY);
            prepareTransientNode(waveLayer);
            battleField.getChildren().add(waveLayer);

            int delay = diveEmerge ? 100 + i * 30 : i * 35;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay + 60),
                            new KeyValue(waveLayer.opacityProperty(), 0.85 - i * 0.06)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(waveLayer.heightProperty(), h1),
                            new KeyValue(waveLayer.yProperty(), waveBaseY - h1)),
                    new KeyFrame(Duration.millis(delay + 350),
                            new KeyValue(waveLayer.heightProperty(), h1 * 0.3),
                            new KeyValue(waveLayer.yProperty(), waveBaseY - h1 * 0.3),
                            new KeyValue(waveLayer.opacityProperty(), 0)));
            registerCleanup(timeline, waveLayer);
        }

        int dropCount = 14 + movePower / 10;
        for (int i = 0; i < dropCount; i++) {
            Circle drop = new Circle(3 + random.nextDouble() * 4, WATER_FOAM);
            drop.setOpacity(0);
            drop.setEffect(new DropShadow(6, WATER_LIGHT));
            double ox = (random.nextDouble() - 0.5) * 80;
            drop.setCenterX(waveBaseX + ox);
            drop.setCenterY(waveBaseY - 80 - random.nextDouble() * 60);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            int delay = 160 + i * 18;
            double driftX = (random.nextDouble() - 0.5) * 60;
            double driftY = -30 - random.nextDouble() * 50;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(drop.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 280),
                            new KeyValue(drop.centerXProperty(), drop.getCenterX() + driftX),
                            new KeyValue(drop.centerYProperty(), drop.getCenterY() + driftY),
                            new KeyValue(drop.opacityProperty(), 0)));
            registerCleanup(timeline, drop);
        }
    }

    // ── DIVE ─────────────────────────────────────────────────────────────────

    private void addDiveEffect(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        for (int r = 0; r < 3; r++) {
            Ellipse ripple = new Ellipse(20 + r * 18, 6 + r * 4);
            ripple.setCenterX(startX); ripple.setCenterY(startY + 60);
            ripple.setFill(Color.TRANSPARENT);
            ripple.setStroke(WATER_LIGHT);
            ripple.setStrokeWidth(2.5 - r * 0.5);
            ripple.setOpacity(0);
            prepareTransientNode(ripple);
            battleField.getChildren().add(ripple);
            int delay = r * 55;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(ripple.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(ripple.radiusXProperty(), ripple.getRadiusX() * 2.2),
                            new KeyValue(ripple.radiusYProperty(), ripple.getRadiusY() * 2.0),
                            new KeyValue(ripple.opacityProperty(), 0)));
            registerCleanup(timeline, ripple);
        }

        double dx = endX - startX, dy = endY - startY;
        int trailCount = 10;
        for (int i = 0; i < trailCount; i++) {
            double t = i / (double) trailCount;
            Circle bubble = new Circle(5 + random.nextDouble() * 4, WATER_MID);
            bubble.setEffect(new GaussianBlur(4));
            bubble.setCenterX(startX + dx * t);
            bubble.setCenterY(startY + 70 + dy * t * 0.2);
            bubble.setOpacity(0);
            prepareTransientNode(bubble);
            battleField.getChildren().add(bubble);
            int delay = 180 + (int)(t * 200);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(bubble.opacityProperty(), 0.7)),
                    new KeyFrame(Duration.millis(delay + 160), new KeyValue(bubble.opacityProperty(), 0)));
            registerCleanup(timeline, bubble);
        }

        for (int r = 0; r < 3; r++) {
            Ellipse ripple = new Ellipse(18 + r * 16, 5 + r * 3);
            ripple.setCenterX(endX); ripple.setCenterY(endY + 60);
            ripple.setFill(Color.TRANSPARENT);
            ripple.setStroke(WATER_FOAM);
            ripple.setStrokeWidth(2.5 - r * 0.5);
            ripple.setOpacity(0);
            prepareTransientNode(ripple);
            battleField.getChildren().add(ripple);
            int delay = 380 + r * 45;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(ripple.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(ripple.radiusXProperty(), ripple.getRadiusX() * 2.2),
                            new KeyValue(ripple.radiusYProperty(), ripple.getRadiusY() * 2.0),
                            new KeyValue(ripple.opacityProperty(), 0)));
            registerCleanup(timeline, ripple);
        }

        addWaveEffect(startX, startY, endX, endY, movePower, true, false, timeline);
    }

    // ── WATER BEAM ───────────────────────────────────────────────────────────

    private void addWaterBeam(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
        double dist  = Math.hypot(endX - startX, endY - startY);
        double beamW = Math.min(18 + movePower / 8.0, 38);

        // Fixed-pivot beam via Group
        Rectangle beam = new Rectangle(0, beamW);
        beam.setX(0); beam.setY(-beamW / 2.0);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0,   WATER_FOAM.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.4, WATER_LIGHT.deriveColor(0, 1, 1, 0.9)),
                new Stop(1,   WATER_DEEP.deriveColor(0, 1, 1, 0.8))));
        beam.setArcWidth(beamW); beam.setArcHeight(beamW);
        beam.setEffect(new DropShadow(beamW * 0.8, WATER_LIGHT));

        Group beamGroup = new Group(beam);
        beamGroup.setLayoutX(startX); beamGroup.setLayoutY(startY);
        beamGroup.setRotate(angle);
        beamGroup.setOpacity(0);
        prepareTransientNode(beamGroup);
        battleField.getChildren().add(beamGroup);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(30),  new KeyValue(beamGroup.opacityProperty(), 0.92)),
                new KeyFrame(Duration.millis(180), new KeyValue(beam.widthProperty(), dist)),
                new KeyFrame(Duration.millis(300), new KeyValue(beamGroup.opacityProperty(), 0.92)),
                new KeyFrame(Duration.millis(420), new KeyValue(beamGroup.opacityProperty(), 0)));
        registerCleanup(timeline, beamGroup);

        int dropCount = 12 + movePower / 12;
        for (int i = 0; i < dropCount; i++) {
            double t  = (i + random.nextDouble()) / dropCount;
            double px = startX + (endX - startX) * t;
            double py = startY + (endY - startY) * t;
            Circle drop = new Circle(2.5 + random.nextDouble() * 3, WATER_FOAM);
            drop.setCenterX(px); drop.setCenterY(py); drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);
            int delay = (int)(t * 160);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay + 40), new KeyValue(drop.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 240),
                            new KeyValue(drop.centerXProperty(), px + (random.nextDouble() - 0.5) * 30),
                            new KeyValue(drop.centerYProperty(), py + random.nextDouble() * 25),
                            new KeyValue(drop.opacityProperty(), 0)));
            registerCleanup(timeline, drop);
        }
        addImpactSplash(endX, endY, movePower, 180, timeline);
    }

    // ── BUBBLE BEAM ──────────────────────────────────────────────────────────

    private void addBubbleBeam(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double dx = endX - startX, dy = endY - startY;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux = dx / dist, uy = dy / dist;
        double px = -uy,       py  = ux;

        int bubbleCount = 22 + movePower / 8;
        for (int i = 0; i < bubbleCount; i++) {
            double t   = (i + random.nextDouble() * 0.5) / bubbleCount;
            double bx  = startX + ux * dist * t + px * (random.nextDouble() - 0.5) * 22;
            double by  = startY + uy * dist * t + py * (random.nextDouble() - 0.5) * 22;
            double radius = 7 + random.nextDouble() * 7;
            Circle bubble = new Circle(radius);
            bubble.setFill(BUBBLE_COLOR.deriveColor(0, 1, 1, 0.35));
            bubble.setStroke(WATER_LIGHT);
            bubble.setStrokeWidth(1.8);
            bubble.setEffect(new DropShadow(radius * 0.6, WATER_CYAN));
            bubble.setCenterX(bx); bubble.setCenterY(by); bubble.setOpacity(0);
            prepareTransientNode(bubble);
            battleField.getChildren().add(bubble);
            int delay = (int)(t * 300);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(bubble.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 140),
                            new KeyValue(bubble.centerXProperty(), bx + ux * dist * 0.08),
                            new KeyValue(bubble.centerYProperty(), by + uy * dist * 0.08)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(bubble.radiusProperty(), radius * 1.5),
                            new KeyValue(bubble.opacityProperty(), 0)));
            registerCleanup(timeline, bubble);
        }
        addImpactSplash(endX, endY, movePower / 2, 300, timeline);
    }

    // ── WHIRLPOOL ────────────────────────────────────────────────────────────

    private void addWhirlpool(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double dx = endX - startX, dy = endY - startY;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux = dx / dist, uy = dy / dist;

        int rings = 8;
        for (int r = 0; r < rings; r++) {
            double progress = r / (double) rings;
            double ringW = 60 + r * 8, ringH = 18 + r * 3;
            Ellipse ring = new Ellipse(ringW / 2, ringH / 2);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(WATER_MID.deriveColor(0, 1, 1 - progress * 0.3, 1));
            ring.setStrokeWidth(3 - r * 0.2);
            ring.setEffect(new DropShadow(8, WATER_LIGHT));
            ring.setCenterX(startX); ring.setCenterY(startY - r * 14); ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);
            int delay = r * 30;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(ring.opacityProperty(), 0.9 - progress * 0.2)),
                    new KeyFrame(Duration.millis(delay + 350),
                            new KeyValue(ring.centerXProperty(), endX),
                            new KeyValue(ring.centerYProperty(), endY - r * 14),
                            new KeyValue(ring.opacityProperty(), 0.9 - progress * 0.2)),
                    new KeyFrame(Duration.millis(delay + 450),
                            new KeyValue(ring.opacityProperty(), 0)));
            registerCleanup(timeline, ring);
        }

        int sprayCount = 16 + movePower / 10;
        for (int i = 0; i < sprayCount; i++) {
            double t  = (i + random.nextDouble()) / sprayCount;
            double bx = startX + ux * dist * t + (random.nextDouble() - 0.5) * 40;
            double by = startY + uy * dist * t + (random.nextDouble() - 0.5) * 20;
            Circle drop = new Circle(3 + random.nextDouble() * 3, WATER_LIGHT);
            drop.setCenterX(bx); drop.setCenterY(by); drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);
            int delay = (int)(t * 300);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(drop.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(drop.centerXProperty(), bx + (random.nextDouble() - 0.5) * 40),
                            new KeyValue(drop.centerYProperty(), by - random.nextDouble() * 25),
                            new KeyValue(drop.opacityProperty(), 0)));
            registerCleanup(timeline, drop);
        }
    }

    // ── WATER PULSE ──────────────────────────────────────────────────────────

    private void addWaterPulse(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        int pulseCount = 4 + movePower / 30;
        for (int p = 0; p < pulseCount; p++) {
            double ringW = 44 + movePower / 10.0, ringH = 22 + movePower / 20.0;
            Ellipse ring = new Ellipse(ringW / 2, ringH / 2);
            ring.setFill(WATER_LIGHT.deriveColor(0, 1, 1, 0.18));
            ring.setStroke(WATER_MID);
            ring.setStrokeWidth(3);
            ring.setEffect(new DropShadow(10, WATER_LIGHT));
            ring.setCenterX(startX); ring.setCenterY(startY); ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);
            int delay = p * 80;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(ring.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 240),
                            new KeyValue(ring.centerXProperty(), endX),
                            new KeyValue(ring.centerYProperty(), endY),
                            new KeyValue(ring.opacityProperty(), 0.75)),
                    new KeyFrame(Duration.millis(delay + 320), new KeyValue(ring.opacityProperty(), 0)));
            registerCleanup(timeline, ring);
        }
        addImpactSplash(endX, endY, movePower / 2, pulseCount * 80 + 120, timeline);
    }

    // ── AQUA TAIL ────────────────────────────────────────────────────────────

    private void addAquaTail(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double rightOffset = 60 + movePower / 10.0;
        double arcHeight   = 80 + movePower / 8.0;
        double p0x = startX + rightOffset, p0y = startY + 20;
        double p1x = startX + rightOffset * 0.25 - 30, p1y = startY - arcHeight;
        double p2x = endX - 15, p2y = endY;

        int segCount = 22;
        for (int i = 0; i < segCount; i++) {
            double t0 = i / (double) segCount, t1 = (i + 1) / (double) segCount;
            double taper = 1.0 - t0 * 0.6;
            Line seg = new Line(bezier(p0x, p1x, p2x, t0), bezier(p0y, p1y, p2y, t0),
                                bezier(p0x, p1x, p2x, t1), bezier(p0y, p1y, p2y, t1));
            seg.setStroke(i % 2 == 0
                    ? WATER_MID.deriveColor(0, 1, 1, 0.90 - i * 0.015)
                    : WATER_LIGHT.deriveColor(0, 1, 1, 0.80 - i * 0.015));
            seg.setStrokeWidth((10 + 5 * (movePower / 100.0)) * taper);
            seg.setEffect(new DropShadow(10, WATER_LIGHT));
            seg.setOpacity(0);
            prepareTransientNode(seg);
            battleField.getChildren().add(seg);
            int delay = i * 13;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(seg.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 110), new KeyValue(seg.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 300), new KeyValue(seg.opacityProperty(), 0)));
            registerCleanup(timeline, seg);
        }

        int ghostCount = 13;
        for (int i = 0; i < ghostCount; i++) {
            double t0 = i / (double) ghostCount, t1 = (i + 1) / (double) ghostCount;
            Line ghost = new Line(
                    bezier(p0x + 8, p1x + 6, p2x + 6, t0), bezier(p0y - 4, p1y - 6, p2y - 4, t0),
                    bezier(p0x + 8, p1x + 6, p2x + 6, t1), bezier(p0y - 4, p1y - 6, p2y - 4, t1));
            ghost.setStroke(WATER_FOAM.deriveColor(0, 1, 1, 0.40));
            ghost.setStrokeWidth((7 + 3 * (movePower / 100.0)) * (1.0 - t0 * 0.5));
            ghost.setEffect(new GaussianBlur(3));
            ghost.setOpacity(0);
            prepareTransientNode(ghost);
            battleField.getChildren().add(ghost);
            int delay = i * 13 + 18;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(ghost.opacityProperty(), 0.55)),
                    new KeyFrame(Duration.millis(delay + 190), new KeyValue(ghost.opacityProperty(), 0)));
            registerCleanup(timeline, ghost);
        }

        Circle shine = new Circle(5 + movePower / 25.0, WATER_FOAM);
        shine.setCenterX(p0x); shine.setCenterY(p0y);
        shine.setEffect(new GaussianBlur(4));
        shine.setOpacity(0);
        prepareTransientNode(shine);
        battleField.getChildren().add(shine);
        int travelMs = segCount * 13;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),            new KeyValue(shine.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(travelMs / 2),
                        new KeyValue(shine.centerXProperty(), p1x),
                        new KeyValue(shine.centerYProperty(), p1y)),
                new KeyFrame(Duration.millis(travelMs),
                        new KeyValue(shine.centerXProperty(), p2x),
                        new KeyValue(shine.centerYProperty(), p2y),
                        new KeyValue(shine.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(travelMs + 80), new KeyValue(shine.opacityProperty(), 0)));
        registerCleanup(timeline, shine);

        int layerCount = 5;
        int impactDelay = travelMs - 20;
        for (int i = 0; i < layerCount; i++) {
            double w = 55 + i * 18 + movePower / 12.0;
            Rectangle waveLayer = new Rectangle(w, 0);
            waveLayer.setFill(WATER_LIGHT.deriveColor(0, 1, 1, 0.8 - i * 0.1));
            waveLayer.setArcWidth(w * 0.5); waveLayer.setArcHeight(w * 0.4);
            waveLayer.setX(endX - w / 2.0); waveLayer.setY(endY);
            waveLayer.setOpacity(0);
            prepareTransientNode(waveLayer);
            battleField.getChildren().add(waveLayer);
            int delay = impactDelay + i * 22;
            double peakH = 70 + i * 15 + movePower / 8.0;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(waveLayer.opacityProperty(), 0.85 - i * 0.1)),
                    new KeyFrame(Duration.millis(delay + 140),
                            new KeyValue(waveLayer.heightProperty(), peakH),
                            new KeyValue(waveLayer.yProperty(), endY - peakH)),
                    new KeyFrame(Duration.millis(delay + 260),
                            new KeyValue(waveLayer.heightProperty(), 0),
                            new KeyValue(waveLayer.opacityProperty(), 0)));
            registerCleanup(timeline, waveLayer);
        }
    }

    private double bezier(double p0, double p1, double p2, double t) {
        return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2;
    }

    // ── CHILLING WATER ────────────────────────────────────────────────────────

    private void addChillingWater(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double fieldH = safeBattleHeight();
        int rainCount = 40 + movePower / 5;
        for (int i = 0; i < rainCount; i++) {
            double rx = endX + (random.nextDouble() - 0.5) * 160;
            double ry = -20 - random.nextDouble() * 80;
            Line raindrop = new Line(rx, ry, rx + 3, ry + 14);
            raindrop.setStroke(WATER_LIGHT.deriveColor(0, 1, 1, 0.7));
            raindrop.setStrokeWidth(1.5);
            raindrop.setOpacity(0);
            prepareTransientNode(raindrop);
            battleField.getChildren().add(raindrop);
            int delay = random.nextInt(300);
            double fallDist = fieldH * 0.6 + random.nextDouble() * fieldH * 0.4;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(raindrop.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 350),
                            new KeyValue(raindrop.startYProperty(), ry + fallDist),
                            new KeyValue(raindrop.endYProperty(), ry + fallDist + 14),
                            new KeyValue(raindrop.opacityProperty(), 0)));
            registerCleanup(timeline, raindrop);
        }

        int iceCount = 8 + movePower / 15;
        for (int i = 0; i < iceCount; i++) {
            double cx = endX + (random.nextDouble() - 0.5) * 140;
            double cy = -15 - random.nextDouble() * 60;
            double size = 8 + random.nextDouble() * 8;
            Rectangle iceCube = new Rectangle(size, size);
            iceCube.setFill(Color.LIGHTCYAN.deriveColor(0, 1, 1, 0.75));
            iceCube.setStroke(Color.CYAN); iceCube.setStrokeWidth(1.5);
            iceCube.setX(cx); iceCube.setY(cy);
            iceCube.setRotate(random.nextDouble() * 45);
            iceCube.setOpacity(0);
            prepareTransientNode(iceCube);
            battleField.getChildren().add(iceCube);
            int delay = 60 + random.nextInt(280);
            double fallDist = fieldH * 0.55 + random.nextDouble() * fieldH * 0.3;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(iceCube.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 400),
                            new KeyValue(iceCube.yProperty(), cy + fallDist),
                            new KeyValue(iceCube.rotateProperty(), iceCube.getRotate() + 120),
                            new KeyValue(iceCube.opacityProperty(), 0)));
            registerCleanup(timeline, iceCube);
        }
    }

    // ── RAZOR SHELL / AQUA CUTTER ─────────────────────────────────────────────

    private void addRazorShell(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
        double dist  = Math.hypot(endX - startX, endY - startY);
        double beamH = 12;

        // Fixed-pivot beam via Group
        Rectangle beam = new Rectangle(0, beamH);
        beam.setX(0); beam.setY(-beamH / 2.0);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, WATER_FOAM.deriveColor(0, 1, 1, 0.9)),
                new Stop(1, WATER_DEEP.deriveColor(0, 1, 1, 0.8))));
        beam.setArcWidth(beamH); beam.setArcHeight(beamH);
        beam.setEffect(new DropShadow(8, WATER_LIGHT));

        Group beamGroup = new Group(beam);
        beamGroup.setLayoutX(startX); beamGroup.setLayoutY(startY);
        beamGroup.setRotate(angle);
        beamGroup.setOpacity(0);
        prepareTransientNode(beamGroup);
        battleField.getChildren().add(beamGroup);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),  new KeyValue(beamGroup.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(160), new KeyValue(beam.widthProperty(), dist)),
                new KeyFrame(Duration.millis(280), new KeyValue(beamGroup.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(380), new KeyValue(beamGroup.opacityProperty(), 0)));
        registerCleanup(timeline, beamGroup);

        int shellCount = 6 + movePower / 18;
        double ux = (endX - startX) / dist, uy = (endY - startY) / dist;
        double perpX = -uy, perpY = ux;
        for (int i = 0; i < shellCount; i++) {
            double t = (i + 0.5) / shellCount;
            double bx = startX + ux * dist * t, by = startY + uy * dist * t;
            double flip = (i % 2 == 0) ? 1 : -1;
            Polygon shell = new Polygon(
                    bx,              by,
                    bx + perpX * 14 * flip - ux * 10, by + perpY * 14 * flip - uy * 10,
                    bx - perpX * 8  * flip + ux * 16, by - perpY * 8  * flip + uy * 16);
            shell.setFill(WATER_LIGHT.deriveColor(0, 0.9, 1, 0.8));
            shell.setStroke(WATER_FOAM); shell.setStrokeWidth(1.2);
            shell.setEffect(new DropShadow(6, WATER_CYAN));
            shell.setOpacity(0);
            prepareTransientNode(shell);
            battleField.getChildren().add(shell);
            int delay = (int)(t * 160);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(shell.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 180), new KeyValue(shell.opacityProperty(), 0)));
            registerCleanup(timeline, shell);
        }
    }

    // ── SPARKLING ARIA ───────────────────────────────────────────────────────

    private void addSparklingAria(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double fieldW = safeBattleWidth(), fieldH = safeBattleHeight();
        int bubbleCount = 30 + movePower / 6;
        for (int i = 0; i < bubbleCount; i++) {
            double bx = random.nextDouble() * fieldW, by = random.nextDouble() * fieldH;
            double radius = 8 + random.nextDouble() * 12;
            Circle bubble = new Circle(radius);
            bubble.setFill(BUBBLE_COLOR.deriveColor(0, 1, 1, 0.3));
            bubble.setStroke(WATER_LIGHT); bubble.setStrokeWidth(1.5);
            bubble.setEffect(new DropShadow(radius * 0.5, WATER_CYAN));
            bubble.setCenterX(bx); bubble.setCenterY(by); bubble.setOpacity(0);
            prepareTransientNode(bubble);
            battleField.getChildren().add(bubble);
            int spawnDelay = random.nextInt(200);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(spawnDelay), new KeyValue(bubble.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(spawnDelay + 380),
                            new KeyValue(bubble.centerXProperty(), endX + (random.nextDouble() - 0.5) * 50),
                            new KeyValue(bubble.centerYProperty(), endY + (random.nextDouble() - 0.5) * 50),
                            new KeyValue(bubble.opacityProperty(), 0.7)),
                    new KeyFrame(Duration.millis(spawnDelay + 480),
                            new KeyValue(bubble.radiusProperty(), radius * 1.6),
                            new KeyValue(bubble.opacityProperty(), 0)));
            registerCleanup(timeline, bubble);
        }

        int starCount = 10 + movePower / 15;
        for (int i = 0; i < starCount; i++) {
            double angle  = Math.PI * 2 * i / starCount;
            double radius = 50 + random.nextDouble() * 30;
            Circle star   = new Circle(3 + random.nextDouble() * 3, WATER_WHITE);
            star.setEffect(new DropShadow(8, WATER_CYAN));
            star.setCenterX(endX + Math.cos(angle) * radius);
            star.setCenterY(endY + Math.sin(angle) * radius);
            star.setOpacity(0);
            prepareTransientNode(star);
            battleField.getChildren().add(star);
            int delay = 280 + i * 20;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(star.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(delay + 180),
                            new KeyValue(star.radiusProperty(), 0.5),
                            new KeyValue(star.opacityProperty(), 0)));
            registerCleanup(timeline, star);
        }
    }

    // ── ORIGIN PULSE ─────────────────────────────────────────────────────────

    private void addOriginPulse(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        int beamCount = 28 + movePower / 5;
        double baseAngle = Math.atan2(endY - startY, endX - startX);

        for (int i = 0; i < beamCount; i++) {
            double spread   = (random.nextDouble() - 0.5) * Math.toRadians(70);
            double beamAngle = baseAngle + spread;
            double beamLen   = 80 + random.nextDouble() * 200;
            double beamW     = 2 + random.nextDouble() * 5;
            Color  beamCol   = Color.color(
                    0.05 + random.nextDouble() * 0.1,
                    0.3  + random.nextDouble() * 0.3,
                    0.85 + random.nextDouble() * 0.15, 1.0);
            double ox = startX + (random.nextDouble() - 0.5) * 40;
            double oy = startY + (random.nextDouble() - 0.5) * 40;

            // Fixed-pivot beam via Group
            Rectangle beam = new Rectangle(0, beamW);
            beam.setX(0); beam.setY(-beamW / 2.0);
            beam.setFill(beamCol);
            beam.setEffect(new DropShadow(beamW * 2, beamCol.brighter()));

            Group beamGroup = new Group(beam);
            beamGroup.setLayoutX(ox); beamGroup.setLayoutY(oy);
            beamGroup.setRotate(Math.toDegrees(beamAngle));
            beamGroup.setOpacity(0);
            prepareTransientNode(beamGroup);
            battleField.getChildren().add(beamGroup);

            int delay = random.nextInt(160);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay + 80),
                            new KeyValue(beamGroup.opacityProperty(), 0.95),
                            new KeyValue(beam.widthProperty(), beamLen)),
                    new KeyFrame(Duration.millis(delay + 200), new KeyValue(beamGroup.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 340), new KeyValue(beamGroup.opacityProperty(), 0)));
            registerCleanup(timeline, beamGroup);
        }

        for (int r = 0; r < 4; r++) {
            Ellipse wave = new Ellipse(10, 6);
            wave.setCenterX(endX); wave.setCenterY(endY);
            wave.setFill(Color.TRANSPARENT);
            wave.setStroke(WATER_DEEP.brighter());
            wave.setStrokeWidth(3 - r * 0.5);
            wave.setEffect(new DropShadow(12, WATER_LIGHT));
            wave.setOpacity(0);
            prepareTransientNode(wave);
            battleField.getChildren().add(wave);
            int delay = 140 + r * 60;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(wave.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 280),
                            new KeyValue(wave.radiusXProperty(), 80 + r * 20),
                            new KeyValue(wave.radiusYProperty(), 50 + r * 12),
                            new KeyValue(wave.opacityProperty(), 0)));
            registerCleanup(timeline, wave);
        }
    }

    // ── FALLBACK BEAM ─────────────────────────────────────────────────────────

    private void addFallbackBeam(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
        double dist  = Math.hypot(endX - startX, endY - startY);
        double beamW = 20 + movePower / 9.0;

        // Fixed-pivot beam via Group
        Rectangle beam = new Rectangle(0, beamW);
        beam.setX(0); beam.setY(-beamW / 2.0);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0,   WATER_FOAM.deriveColor(0, 1, 1, 0.9)),
                new Stop(0.5, WATER_LIGHT.deriveColor(0, 1, 1, 0.85)),
                new Stop(1,   WATER_DEEP.deriveColor(0, 1, 1, 0.75))));
        beam.setArcWidth(beamW); beam.setArcHeight(beamW);
        beam.setEffect(new DropShadow(beamW * 0.7, WATER_LIGHT));

        Group beamGroup = new Group(beam);
        beamGroup.setLayoutX(startX); beamGroup.setLayoutY(startY);
        beamGroup.setRotate(angle);
        beamGroup.setOpacity(0);
        prepareTransientNode(beamGroup);
        battleField.getChildren().add(beamGroup);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(25),  new KeyValue(beamGroup.opacityProperty(), 0.88)),
                new KeyFrame(Duration.millis(200), new KeyValue(beam.widthProperty(), dist)),
                new KeyFrame(Duration.millis(300), new KeyValue(beamGroup.opacityProperty(), 0.88)),
                new KeyFrame(Duration.millis(400), new KeyValue(beamGroup.opacityProperty(), 0)));
        registerCleanup(timeline, beamGroup);

        addImpactSplash(endX, endY, movePower / 2, 200, timeline);
    }

    // ── SHARED HELPERS ────────────────────────────────────────────────────────

    private void addImpactSplash(double x, double y, int movePower, int startDelay, Timeline timeline) {
        int dropCount = 8 + movePower / 12;
        for (int i = 0; i < dropCount; i++) {
            Circle drop = new Circle(3 + random.nextDouble() * 3.5, WATER_FOAM);
            drop.setCenterX(x); drop.setCenterY(y); drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);
            double driftAngle = Math.PI * 2 * i / dropCount + (random.nextDouble() - 0.5) * 0.6;
            double driftDist  = 20 + random.nextDouble() * 35;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(startDelay), new KeyValue(drop.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(startDelay + 220),
                            new KeyValue(drop.centerXProperty(), x + Math.cos(driftAngle) * driftDist),
                            new KeyValue(drop.centerYProperty(), y + Math.sin(driftAngle) * driftDist - 15),
                            new KeyValue(drop.opacityProperty(), 0)));
            registerCleanup(timeline, drop);
        }
    }

    private double safeBattleWidth() {
        double w = battleField.getWidth();
        return w > 0 ? w : 1200.0;
    }

    private double safeBattleHeight() {
        double h = battleField.getHeight();
        return h > 0 ? h : 700.0;
    }

    private void prepareTransientNode(Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private void registerCleanup(Timeline timeline, Node node) {
        EventHandler<ActionEvent> previousOnFinished = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (previousOnFinished != null) previousOnFinished.handle(e);
        });
    }
}