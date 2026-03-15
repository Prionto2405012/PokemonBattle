// WaterEffects.java
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
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class WaterEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Water colour palette
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
    // PUBLIC ENTRY POINT

    /**
     * Called from BattleAnimationManager.createTypeSpecificImpact for water moves.
     * startX/Y  = attacker centre,  endX/Y = defender centre.
     */
    public void createImpactEffect(double startX, double startY, double endX, double endY,
            String moveName, int movePower, Timeline timeline) {

        switch (moveName) {
            // Wave moves
            case "surf", "liquidation", "waterfall", "water-pledge",
                 "brine", "scald", "wave-crash" ->
                    addWaveEffect(startX, startY, endX, endY, movePower, false, false, timeline);

            // Dive: wave + attacker dives underground
            case "dive" ->
                    addDiveEffect(startX, startY, endX, endY, movePower, timeline);

            // Aqua Jet: attacker rides the wave
            // (movement handled separately; here we just do the visual wave)
            case "aqua-jet" ->
                    addWaveEffect(startX, startY, endX, endY, movePower, false, true, timeline);

            // Beam moves
            case "water-gun", "hydro-pump", "muddy-water", "hydro-cannon" ->
                    addWaterBeam(startX, startY, endX, endY, movePower, timeline);

            // Bubble moves
            case "bubble", "bubble-beam" ->
                    addBubbleBeam(startX, startY, endX, endY, movePower, timeline);

            // Whirlpool
            case "whirlpool" ->
                    addWhirlpool(startX, startY, endX, endY, movePower, timeline);

            // Water Pulse
            case "water-pulse" ->
                    addWaterPulse(startX, startY, endX, endY, movePower, timeline);

            // Aqua Tail
            case "aqua-tail" ->
                    addAquaTail(startX, startY, endX, endY, movePower, timeline);

            // Chilling Water
            case "chilling-water" ->
                    addChillingWater(startX, startY, endX, endY, movePower, timeline);

            // Razor Shell / Aqua Cutter
            case "razor-shell", "aqua-cutter" ->
                    addRazorShell(startX, startY, endX, endY, movePower, timeline);

            // Sparkling Aria
            case "sparkling-aria" ->
                    addSparklingAria(startX, startY, endX, endY, movePower, timeline);

            // Origin Pulse
            case "origin-pulse" ->
                    addOriginPulse(startX, startY, endX, endY, movePower, timeline);

            // Crabhammer: melee — no extra effect needed here
            case "crabhammer" ->
                    addFallbackBeam(startX, startY, endX, endY, movePower, timeline);

            // Clamp
            case "clamp" ->
                    addClampEffect(startX, startY, endX, endY, movePower, timeline);

            // Flip Turn
            case "flip-turn" ->
                    addFlipTurn(startX, startY, endX, endY, movePower, timeline);

            // Octazooka
            case "octazooka" ->
                    addOctazooka(startX, startY, endX, endY, movePower, timeline);

            // Water Spout
            case "water-spout" ->
                    addWaterSpout(startX, startY, endX, endY, movePower, timeline);

            // Fallback
            default ->
                    addFallbackBeam(startX, startY, endX, endY, movePower, timeline);
        }
    }
    // WAVE (surf, liquidation, waterfall, water-pledge, brine, scald, wave-crash)

    /**
     * A towering wave emerges from the base of the defender and crashes over them.
     *
     * @param aquaJetRide  if true the attacker visually "rides" the wave (aqua jet)
     * @param diveEmerge   if true the wave is the emerge-phase of dive (smaller, offset)
     */
    private void addWaveEffect(double startX, double startY, double endX, double endY,
            int movePower, boolean diveEmerge, boolean aquaJetRide, Timeline timeline) {

        // Wave base sits at defender's feet (bottom of sprite region = endY + ~80)
        double waveBaseX = endX;
        double waveBaseY = endY + 100;

        // Wave body: a rounded rectangle that scales up from the ground
        int layerCount = 15;
        for (int i = 0; i < layerCount; i++) {
            double w  = 90 + i * 28 + movePower / 8.0;
            double h0 = 0;
            double h1 = 140 + i * 20 + movePower / 6.0;

            Rectangle waveLayer = new Rectangle(w, h0);
            waveLayer.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, WATER_FOAM.deriveColor(0, 1, 1, 0.7)),
                    new Stop(0.4, WATER_LIGHT.deriveColor(0, 1, 1, 0.85)),
                    new Stop(1, WATER_DEEP.deriveColor(0, 1, 1, 0.9))));
            waveLayer.setArcWidth(w * 0.8);
            waveLayer.setArcHeight(w * 0.6);
            waveLayer.setEffect(new GaussianBlur(4 + i));
            waveLayer.setOpacity(0);
            waveLayer.setX(waveBaseX - w / 2.0);
            waveLayer.setY(waveBaseY);
            prepareTransientNode(waveLayer);
            battleField.getChildren().add(waveLayer);

            int delay = diveEmerge ? 100 + i * 30 : i * 35;

            KeyFrame rise = new KeyFrame(Duration.millis(delay + 60),
                    new KeyValue(waveLayer.opacityProperty(), 0.85 - i * 0.06));
            KeyFrame peak = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(waveLayer.heightProperty(), h1),
                    new KeyValue(waveLayer.yProperty(), waveBaseY - h1));
            KeyFrame crash = new KeyFrame(Duration.millis(delay + 350),
                    new KeyValue(waveLayer.heightProperty(), h1 * 0.3),
                    new KeyValue(waveLayer.yProperty(), waveBaseY - h1 * 0.3),
                    new KeyValue(waveLayer.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(rise, peak, crash);
            registerCleanup(timeline, waveLayer);
        }

        // Foam droplets scatter around the crest
        int dropCount = 24 + movePower / 10;
        for (int i = 0; i < dropCount; i++) {
            Circle drop = new Circle(8 + random.nextDouble() * 4, WATER_FOAM);
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

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(drop.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(drop.centerXProperty(), drop.getCenterX() + driftX),
                    new KeyValue(drop.centerYProperty(), drop.getCenterY() + driftY),
                    new KeyValue(drop.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, drop);
        }
    }

    // DIVE

    /**
     * Attacker sinks below the battleField floor then re-emerges near the defender
     * to hit with a wave.
     * The "sinking" and "emergence" are visual overlays — the actual sprite
     * movement is still handled by BattleAnimationManager.
     */
    private void addDiveEffect(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        // Ripple at attacker's feet (dive-in)
        for (int r = 0; r < 3; r++) {
            Ellipse ripple = new Ellipse(27 + r * 18, 12 + r * 4);
            ripple.setCenterX(startX);
            ripple.setCenterY(startY + 60);
            ripple.setFill(Color.TRANSPARENT);
            ripple.setStroke(WATER_LIGHT);
            ripple.setStrokeWidth(2.5 - r * 0.5);
            ripple.setOpacity(0);
            prepareTransientNode(ripple);
            battleField.getChildren().add(ripple);

            int delay = r * 55;
            KeyFrame appear  = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ripple.opacityProperty(), 0.9));
            KeyFrame expand  = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(ripple.radiusXProperty(), ripple.getRadiusX() * 2.2),
                    new KeyValue(ripple.radiusYProperty(), ripple.getRadiusY() * 2.0),
                    new KeyValue(ripple.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, expand);
            registerCleanup(timeline, ripple);
        }

        // Underground travel: a dark water trail along the bottom
        int trailCount = 20;
        double dx = endX - startX;
        double dy = endY - startY;
        double dist = Math.max(1, Math.hypot(dx, dy));
        for (int i = 0; i < trailCount; i++) {
            double t = i / (double) trailCount;
            Circle bubble = new Circle(5 + random.nextDouble() * 4, WATER_MID);
            bubble.setEffect(new GaussianBlur(4));
            bubble.setCenterX(startX + dx * t);
            bubble.setCenterY(startY + 70 + dy * t * 0.2);  // stays near ground
            bubble.setOpacity(0);
            prepareTransientNode(bubble);
            battleField.getChildren().add(bubble);

            int delay = 180 + (int)(t * 200);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(bubble.opacityProperty(), 0.7));
            KeyFrame fade   = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(bubble.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, bubble);
        }

        // Emerge ripple near defender
        for (int r = 0; r < 3; r++) {
            Ellipse ripple = new Ellipse(27 + r * 16, 12 + r * 3);
            ripple.setCenterX(endX);
            ripple.setCenterY(endY + 60);
            ripple.setFill(Color.TRANSPARENT);
            ripple.setStroke(WATER_FOAM);
            ripple.setStrokeWidth(2.5 - r * 0.5);
            ripple.setOpacity(0);
            prepareTransientNode(ripple);
            battleField.getChildren().add(ripple);

            int delay = 380 + r * 45;
            KeyFrame appear  = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ripple.opacityProperty(), 0.9));
            KeyFrame expand  = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(ripple.radiusXProperty(), ripple.getRadiusX() * 2.2),
                    new KeyValue(ripple.radiusYProperty(), ripple.getRadiusY() * 2.0),
                    new KeyValue(ripple.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, expand);
            registerCleanup(timeline, ripple);
        }

        // Wave crash at defender
        addWaveEffect(startX, startY, endX, endY, movePower, true, false, timeline);
    }

    // WATER BEAM  (water-gun, hydro-pump, muddy-water, hydro-cannon)

    private void addWaterBeam(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double angle  = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
        double dist   = Math.hypot(endX - startX, endY - startY);
        double beamW  = Math.min(28 + movePower / 8.0, 40);

        // Core beam
        Rectangle beam = new Rectangle(0, beamW);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, WATER_FOAM.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.4, WATER_LIGHT.deriveColor(0, 1, 1, 0.9)),
                new Stop(1, WATER_DEEP.deriveColor(0, 1, 1, 0.8))));
        beam.setArcWidth(beamW);
        beam.setArcHeight(beamW);
        beam.setX(startX);
        beam.setY(startY - beamW / 2.0);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(beamW * 0.8, WATER_LIGHT));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        KeyFrame show  = new KeyFrame(Duration.millis(30),
                new KeyValue(beam.opacityProperty(), 0.92));
        KeyFrame shoot = new KeyFrame(Duration.millis(180),
                new KeyValue(beam.widthProperty(), dist));
        KeyFrame hold  = new KeyFrame(Duration.millis(300),
                new KeyValue(beam.opacityProperty(), 0.92));
        KeyFrame fade  = new KeyFrame(Duration.millis(420),
                new KeyValue(beam.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(show, shoot, hold, fade);
        registerCleanup(timeline, beam);

        // Water droplets along beam
        int dropCount = 22 + movePower / 12;
        for (int i = 0; i < dropCount; i++) {
            double t  = (i + random.nextDouble()) / dropCount;
            double px = startX + (endX - startX) * t;
            double py = startY + (endY - startY) * t;
            Circle drop = new Circle(2.5 + random.nextDouble() * 3, WATER_FOAM);
            drop.setCenterX(px);
            drop.setCenterY(py);
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            int delay = (int)(t * 160);
            KeyFrame appear   = new KeyFrame(Duration.millis(delay + 40),
                    new KeyValue(drop.opacityProperty(), 0.85));
            KeyFrame scatter  = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(drop.centerXProperty(), px + (random.nextDouble() - 0.5) * 30),
                    new KeyValue(drop.centerYProperty(), py + random.nextDouble() * 25),
                    new KeyValue(drop.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, drop);
        }

        // Splash at impact
        addImpactSplash(endX, endY, movePower, 180, timeline);
    }

    // BUBBLE BEAM  (bubble, bubble-beam)

    private void addBubbleBeam(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double dx = endX - startX;
        double dy = endY - startY;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux   = dx / dist;
        double uy   = dy / dist;
        double px   = -uy;
        double py   =  ux;

        int bubbleCount = 35 + movePower / 8;
        for (int i = 0; i < bubbleCount; i++) {
            double t   = (i + random.nextDouble() * 0.5) / bubbleCount;
            double bx  = startX + ux * dist * t;
            double by  = startY + uy * dist * t;
            double off = (random.nextDouble() - 0.5) * 22;
            bx += px * off;
            by += py * off;

            double radius = 10 + random.nextDouble() * 7;
            Circle bubble = new Circle(radius);
            bubble.setFill(BUBBLE_COLOR.deriveColor(0, 1, 1, 0.35));
            bubble.setStroke(WATER_LIGHT);
            bubble.setStrokeWidth(1.8);
            bubble.setEffect(new DropShadow(radius * 0.6, WATER_CYAN));
            bubble.setCenterX(bx);
            bubble.setCenterY(by);
            bubble.setOpacity(0);
            prepareTransientNode(bubble);
            battleField.getChildren().add(bubble);

            int delay = (int)(t * 300);
            KeyFrame appear  = new KeyFrame(Duration.millis(delay),
                    new KeyValue(bubble.opacityProperty(), 0.9));
            KeyFrame travel  = new KeyFrame(Duration.millis(delay + 140),
                    new KeyValue(bubble.centerXProperty(), bx + ux * dist * 0.08),
                    new KeyValue(bubble.centerYProperty(), by + uy * dist * 0.08));
            KeyFrame pop     = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(bubble.radiusProperty(), radius * 1.5),
                    new KeyValue(bubble.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, travel, pop);
            registerCleanup(timeline, bubble);
        }

        // Small pop splash at impact
        addImpactSplash(endX, endY, movePower / 2, 300, timeline);
    }

    // WHIRLPOOL

    private void addWhirlpool(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double dx   = endX - startX;
        double dy   = endY - startY;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux   = dx / dist;
        double uy   = dy / dist;
        double px   = -uy;
        double py   =  ux;

        // Tornado body: stacked ellipses that travel toward opponent
        int rings = 18;
        for (int r = 0; r < rings; r++) {
            double progress = r / (double) rings;
            double ringW    = 60 + r * 8;
            double ringH    = 18 + r * 3;
            Ellipse ring    = new Ellipse(ringW / 2, ringH / 2);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(WATER_MID.deriveColor(0, 1, 1 - progress * 0.3, 1));
            ring.setStrokeWidth(3 - r * 0.2);
            ring.setEffect(new DropShadow(8, WATER_LIGHT));
            double startRingX = startX;
            double startRingY = startY - r * 14;
            ring.setCenterX(startRingX);
            ring.setCenterY(startRingY);
            ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            int delay = r * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ring.opacityProperty(), 0.9 - progress * 0.2));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 350),
                    new KeyValue(ring.centerXProperty(), endX),
                    new KeyValue(ring.centerYProperty(), endY - r * 14),
                    new KeyValue(ring.radiusXProperty(), ringW * 0.5),
                    new KeyValue(ring.opacityProperty(), 0.9 - progress * 0.2));
            KeyFrame fade   = new KeyFrame(Duration.millis(delay + 450),
                    new KeyValue(ring.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, ring);
        }

        // Water spray around the tornado
        int sprayCount = 26 + movePower / 10;
        for (int i = 0; i < sprayCount; i++) {
            double t  = (i + random.nextDouble()) / sprayCount;
            double bx = startX + ux * dist * t + (random.nextDouble() - 0.5) * 40;
            double by = startY + uy * dist * t + (random.nextDouble() - 0.5) * 20;
            Circle drop = new Circle(3 + random.nextDouble() * 3, WATER_LIGHT);
            drop.setCenterX(bx);
            drop.setCenterY(by);
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            int delay = (int)(t * 300);
            KeyFrame appear  = new KeyFrame(Duration.millis(delay),
                    new KeyValue(drop.opacityProperty(), 0.8));
            KeyFrame scatter = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(drop.centerXProperty(), bx + (random.nextDouble() - 0.5) * 40),
                    new KeyValue(drop.centerYProperty(), by - random.nextDouble() * 25),
                    new KeyValue(drop.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, drop);
        }
    }

    // WATER PULSE

    private void addWaterPulse(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double dx   = endX - startX;
        double dy   = endY - startY;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux   = dx / dist;
        double uy   = dy / dist;

        int pulseCount = 10 + movePower / 20;
        for (int p = 0; p < pulseCount; p++) {
            double ringW = 80 + movePower / 10.0;
            double ringH = 60 + movePower / 20.0;
            Ellipse ring = new Ellipse(ringW / 2, ringH / 2);
            ring.setFill(WATER_LIGHT.deriveColor(0, 1, 1, 0.18));
            ring.setStroke(WATER_MID);
            ring.setStrokeWidth(3);
            ring.setEffect(new DropShadow(10, WATER_LIGHT));
            ring.setCenterX(startX);
            ring.setCenterY(startY);
            ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            int delay = p * 80;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ring.opacityProperty(), 0.9));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(ring.centerXProperty(), endX),
                    new KeyValue(ring.centerYProperty(), endY),
                    new KeyValue(ring.opacityProperty(), 0.75));
            KeyFrame fade   = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(ring.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, ring);
        }

        // Small splash at impact
        addImpactSplash(endX, endY, movePower / 2, pulseCount * 80 + 120, timeline);
    }

    // AQUA TAIL

    private void addAquaTail(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        // Semi-elliptical tail arc to the attacker's right (attacker's POV)
        // In screen space, attacker is on the left, so "right of attacker" means
        // the arc bows upward (above the line between attacker and defender).
        double midX = (startX + endX) / 2.0;
        double midY = Math.min(startY, endY) - 60;   // arc peak above midpoint

        int arcPoints = 25;
        for (int i = 0; i < arcPoints - 1; i++) {
            double t0 = i / (double)(arcPoints - 1);
            double t1 = (i + 1) / (double)(arcPoints - 1);

            // Quadratic bezier sampling
            double ax0 = lerp(lerp(startX, midX, t0), lerp(midX, endX, t0), t0);
            double ay0 = lerp(lerp(startY, midY, t0), lerp(midY, endY, t0), t0);
            double ax1 = lerp(lerp(startX, midX, t1), lerp(midX, endX, t1), t1);
            double ay1 = lerp(lerp(startY, midY, t1), lerp(midY, endY, t1), t1);

            Line seg = new Line(ax0, ay0, ax1, ay1);
            seg.setStroke(WATER_MID.deriveColor(0, 1, 1, 0.85 - i * 0.02));
            seg.setStrokeWidth(5 + (Math.sin(t0 * Math.PI) * 6));
            seg.setEffect(new DropShadow(8, WATER_LIGHT));
            seg.setOpacity(0);
            prepareTransientNode(seg);
            battleField.getChildren().add(seg);

            int delay = i * 18;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(seg.opacityProperty(), 0.95));
            KeyFrame fade   = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(seg.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, seg);
        }

        // Short wave crest that hits receiver
        int layerCount = 10;
        for (int i = 0; i < layerCount; i++) {
            double w = 55 + i * 18 + movePower / 12.0;
            Rectangle waveLayer = new Rectangle(w, 0);
            waveLayer.setFill(WATER_LIGHT.deriveColor(0, 1, 1, 0.8 - i * 0.1));
            waveLayer.setArcWidth(w * 0.5);
            waveLayer.setArcHeight(w * 0.4);
            waveLayer.setX(endX - w / 2.0);
            waveLayer.setY(endY);
            waveLayer.setOpacity(0);
            prepareTransientNode(waveLayer);
            battleField.getChildren().add(waveLayer);

            int delay = 160 + i * 25;
            double peakH = 70 + i * 15 + movePower / 8.0;
            KeyFrame rise  = new KeyFrame(Duration.millis(delay),
                    new KeyValue(waveLayer.opacityProperty(), 0.85 - i * 0.1));
            KeyFrame peak  = new KeyFrame(Duration.millis(delay + 140),
                    new KeyValue(waveLayer.heightProperty(), peakH),
                    new KeyValue(waveLayer.yProperty(), endY - peakH));
            KeyFrame crash = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(waveLayer.heightProperty(), 0),
                    new KeyValue(waveLayer.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(rise, peak, crash);
            registerCleanup(timeline, waveLayer);
        }
    }

    // CHILLING WATER

    private void addChillingWater(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double fieldW = safeBattleWidth();
        double fieldH = safeBattleHeight();

        // Raindrops
        int rainCount = 50 + movePower / 5;
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
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(raindrop.opacityProperty(), 0.8));
            KeyFrame fall   = new KeyFrame(Duration.millis(delay + 350),
                    new KeyValue(raindrop.startYProperty(), ry + fallDist),
                    new KeyValue(raindrop.endYProperty(), ry + fallDist + 14),
                    new KeyValue(raindrop.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, fall);
            registerCleanup(timeline, raindrop);
        }

        // Ice cubes scattered within the rain
        int iceCount = 20 + movePower / 15;
        for (int i = 0; i < iceCount; i++) {
            double cx = endX + (random.nextDouble() - 0.5) * 140;
            double cy = -15 - random.nextDouble() * 60;
            double size = 8 + random.nextDouble() * 8;
            Rectangle iceCube = new Rectangle(size, size);
            iceCube.setFill(Color.LIGHTCYAN.deriveColor(0, 1, 1, 0.75));
            iceCube.setStroke(Color.CYAN);
            iceCube.setStrokeWidth(1.5);
            iceCube.setX(cx);
            iceCube.setY(cy);
            iceCube.setRotate(random.nextDouble() * 45);
            iceCube.setOpacity(0);
            prepareTransientNode(iceCube);
            battleField.getChildren().add(iceCube);

            int delay = 60 + random.nextInt(280);
            double fallDist = fieldH * 0.55 + random.nextDouble() * fieldH * 0.3;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(iceCube.opacityProperty(), 0.85));
            KeyFrame fall   = new KeyFrame(Duration.millis(delay + 400),
                    new KeyValue(iceCube.yProperty(), cy + fallDist),
                    new KeyValue(iceCube.rotateProperty(), iceCube.getRotate() + 120),
                    new KeyValue(iceCube.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, fall);
            registerCleanup(timeline, iceCube);
        }
    }

    // RAZOR SHELL / AQUA CUTTER

    private void addRazorShell(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
        double dist  = Math.hypot(endX - startX, endY - startY);
        double beamH = 12;

        // Thin beam
        Rectangle beam = new Rectangle(0, beamH);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, WATER_FOAM.deriveColor(0, 1, 1, 0.9)),
                new Stop(1, WATER_DEEP.deriveColor(0, 1, 1, 0.8))));
        beam.setArcWidth(beamH);
        beam.setArcHeight(beamH);
        beam.setX(startX);
        beam.setY(startY - beamH / 2.0);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(8, WATER_LIGHT));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        KeyFrame show  = new KeyFrame(Duration.millis(20),
                new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame shoot = new KeyFrame(Duration.millis(160),
                new KeyValue(beam.widthProperty(), dist));
        KeyFrame hold  = new KeyFrame(Duration.millis(280),
                new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame fade  = new KeyFrame(Duration.millis(380),
                new KeyValue(beam.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(show, shoot, hold, fade);
        registerCleanup(timeline, beam);

        // Shell/blade triangles riding along the beam
        int shellCount = 12 + movePower / 18;
        double ux = (endX - startX) / dist;
        double uy = (endY - startY) / dist;
        double perpX = -uy;
        double perpY =  ux;

        for (int i = 0; i < shellCount; i++) {
            double t = (i + 0.5) / shellCount;
            double bx = startX + ux * dist * t;
            double by = startY + uy * dist * t;

            // Deformed triangle pointing forward
            double flip = (i % 2 == 0) ? 1 : -1;
            Polygon shell = new Polygon(
                    bx,              by,
                    bx + perpX * 14 * flip - ux * 10, by + perpY * 14 * flip - uy * 10,
                    bx - perpX * 8  * flip + ux * 16, by - perpY * 8  * flip + uy * 16
            );
            shell.setFill(WATER_LIGHT.deriveColor(0, 0.9, 1, 0.8));
            shell.setStroke(WATER_FOAM);
            shell.setStrokeWidth(1.2);
            shell.setEffect(new DropShadow(6, WATER_CYAN));
            shell.setOpacity(0);
            prepareTransientNode(shell);
            battleField.getChildren().add(shell);

            int delay = (int)(t * 160);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(shell.opacityProperty(), 0.9));
            KeyFrame slide  = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(shell.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, slide);
            registerCleanup(timeline, shell);
        }
    }

    // SPARKLING ARIA

    private void addSparklingAria(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double fieldW = safeBattleWidth();
        double fieldH = safeBattleHeight();

        // Bubbles filling entire field, then drifting toward opponent
        int bubbleCount = 50 + movePower / 6;
        for (int i = 0; i < bubbleCount; i++) {
            double bx = random.nextDouble() * fieldW;
            double by = random.nextDouble() * fieldH;
            double radius = 10 + random.nextDouble() * 12;
            Circle bubble = new Circle(radius);
            bubble.setFill(BUBBLE_COLOR.deriveColor(0, 1, 1, 0.3));
            bubble.setStroke(WATER_LIGHT);
            bubble.setStrokeWidth(1.5);
            bubble.setEffect(new DropShadow(radius * 0.5, WATER_CYAN));
            bubble.setCenterX(bx);
            bubble.setCenterY(by);
            bubble.setOpacity(0);
            prepareTransientNode(bubble);
            battleField.getChildren().add(bubble);

            int spawnDelay = random.nextInt(200);
            // Phase 1: appear
            KeyFrame appear = new KeyFrame(Duration.millis(spawnDelay),
                    new KeyValue(bubble.opacityProperty(), 0.85));
            // Phase 2: drift toward opponent
            KeyFrame converge = new KeyFrame(Duration.millis(spawnDelay + 380),
                    new KeyValue(bubble.centerXProperty(), endX + (random.nextDouble() - 0.5) * 50),
                    new KeyValue(bubble.centerYProperty(), endY + (random.nextDouble() - 0.5) * 50),
                    new KeyValue(bubble.opacityProperty(), 0.7));
            // Phase 3: pop
            KeyFrame pop = new KeyFrame(Duration.millis(spawnDelay + 480),
                    new KeyValue(bubble.radiusProperty(), radius * 1.6),
                    new KeyValue(bubble.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, converge, pop);
            registerCleanup(timeline, bubble);
        }

        // Sparkle stars around the opponent
        int starCount = 10 + movePower / 15;
        for (int i = 0; i < starCount; i++) {
            double angle  = Math.PI * 2 * i / starCount;
            double radius = 50 + random.nextDouble() * 30;
            Circle star   = new Circle(10 + random.nextDouble() * 3, WATER_WHITE);
            star.setEffect(new DropShadow(8, WATER_CYAN));
            star.setCenterX(endX + Math.cos(angle) * radius);
            star.setCenterY(endY + Math.sin(angle) * radius);
            star.setOpacity(0);
            prepareTransientNode(star);
            battleField.getChildren().add(star);

            int delay = 280 + i * 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(star.opacityProperty(), 1.0));
            KeyFrame shrink = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(star.radiusProperty(), 0.5),
                    new KeyValue(star.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, shrink);
            registerCleanup(timeline, star);
        }
    }

    // ORIGIN PULSE — countless deep brilliant blue light beams

    private void addOriginPulse(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        int beamCount = 38 + movePower / 5;
        double baseAngle = Math.atan2(endY - startY, endX - startX);

        for (int i = 0; i < beamCount; i++) {
            // Spread beams within a ±35° cone toward opponent, plus a few wild ones
            double spread = (random.nextDouble() - 0.5) * Math.toRadians(70);
            double beamAngle = baseAngle + spread;

            double beamLen  = 80 + random.nextDouble() * 200;
            double beamW    = 10 + random.nextDouble() * 5;
            Color  beamCol  = Color.color(
                    0.05 + random.nextDouble() * 0.1,
                    0.3  + random.nextDouble() * 0.3,
                    0.85 + random.nextDouble() * 0.15, 1.0);

            // Stagger origin points slightly around attacker
            double ox = startX + (random.nextDouble() - 0.5) * 40;
            double oy = startY + (random.nextDouble() - 0.5) * 40;

            Rectangle beam = new Rectangle(0, beamW);
            beam.setFill(beamCol);
            beam.setX(ox);
            beam.setY(oy - beamW / 2.0);
            beam.setRotate(Math.toDegrees(beamAngle));
            beam.setEffect(new DropShadow(beamW * 2, beamCol.brighter()));
            beam.setOpacity(0);
            prepareTransientNode(beam);
            battleField.getChildren().add(beam);

            int delay = random.nextInt(160);
            KeyFrame shoot = new KeyFrame(Duration.millis(delay + 80),
                    new KeyValue(beam.opacityProperty(), 0.95),
                    new KeyValue(beam.widthProperty(), beamLen));
            KeyFrame hold  = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(beam.opacityProperty(), 0.95));
            KeyFrame fade  = new KeyFrame(Duration.millis(delay + 340),
                    new KeyValue(beam.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(shoot, hold, fade);
            registerCleanup(timeline, beam);
        }

        // Deep blue shockwave at impact
        for (int r = 0; r < 4; r++) {
            Ellipse wave = new Ellipse(10, 6);
            wave.setCenterX(endX);
            wave.setCenterY(endY);
            wave.setFill(Color.TRANSPARENT);
            wave.setStroke(WATER_DEEP.brighter());
            wave.setStrokeWidth(3 - r * 0.5);
            wave.setEffect(new DropShadow(12, WATER_LIGHT));
            wave.setOpacity(0);
            prepareTransientNode(wave);
            battleField.getChildren().add(wave);

            int delay = 140 + r * 60;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(wave.opacityProperty(), 0.9));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(wave.radiusXProperty(), 80 + r * 20),
                    new KeyValue(wave.radiusYProperty(), 50 + r * 12),
                    new KeyValue(wave.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, expand);
            registerCleanup(timeline, wave);
        }
    }

    // CLAMP — two shell-like ellipses close around the defender

    private void addClampEffect(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double shellW = 70 + movePower / 4.0;
        double shellH = 45 + movePower / 6.0;

        // Top shell (starts above defender, closes downward)
        Ellipse topShell = new Ellipse(shellW / 2, shellH / 2);
        topShell.setCenterX(endX);
        topShell.setCenterY(endY - 80);
        topShell.setFill(WATER_DEEP.deriveColor(0, 1, 1, 0.75));
        topShell.setStroke(WATER_MID);
        topShell.setStrokeWidth(3);
        topShell.setEffect(new DropShadow(10, WATER_LIGHT));
        topShell.setOpacity(0);
        prepareTransientNode(topShell);
        battleField.getChildren().add(topShell);

        // Bottom shell (starts below defender, closes upward)
        Ellipse bottomShell = new Ellipse(shellW / 2, shellH / 2);
        bottomShell.setCenterX(endX);
        bottomShell.setCenterY(endY + 80);
        bottomShell.setFill(WATER_DEEP.deriveColor(0, 1, 1, 0.75));
        bottomShell.setStroke(WATER_MID);
        bottomShell.setStrokeWidth(3);
        bottomShell.setEffect(new DropShadow(10, WATER_LIGHT));
        bottomShell.setOpacity(0);
        prepareTransientNode(bottomShell);
        battleField.getChildren().add(bottomShell);

        // Shells appear and slam shut
        KeyFrame shellsAppear = new KeyFrame(Duration.millis(40),
                new KeyValue(topShell.opacityProperty(), 0.9),
                new KeyValue(bottomShell.opacityProperty(), 0.9));
        KeyFrame shellsClose = new KeyFrame(Duration.millis(220),
                new KeyValue(topShell.centerYProperty(), endY - 8),
                new KeyValue(bottomShell.centerYProperty(), endY + 8));
        KeyFrame shellsHold = new KeyFrame(Duration.millis(380),
                new KeyValue(topShell.opacityProperty(), 0.85),
                new KeyValue(bottomShell.opacityProperty(), 0.85));
        KeyFrame shellsFade = new KeyFrame(Duration.millis(500),
                new KeyValue(topShell.opacityProperty(), 0),
                new KeyValue(bottomShell.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(shellsAppear, shellsClose, shellsHold, shellsFade);
        registerCleanup(timeline, topShell);
        registerCleanup(timeline, bottomShell);

        // Water droplets spraying out on impact
        int dropCount = 25 + movePower / 8;
        for (int i = 0; i < dropCount; i++) {
            Circle drop = new Circle(5.5 + random.nextDouble() * 3, WATER_FOAM);
            drop.setCenterX(endX + (random.nextDouble() - 0.5) * 30);
            drop.setCenterY(endY + (random.nextDouble() - 0.5) * 10);
            drop.setEffect(new DropShadow(4, WATER_LIGHT));
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            double driftAngle = Math.PI * 2 * i / dropCount + (random.nextDouble() - 0.5) * 0.5;
            double driftDist = 25 + random.nextDouble() * 35;

            KeyFrame appear  = new KeyFrame(Duration.millis(220),
                    new KeyValue(drop.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(420),
                    new KeyValue(drop.centerXProperty(), endX + Math.cos(driftAngle) * driftDist),
                    new KeyValue(drop.centerYProperty(), endY + Math.sin(driftAngle) * driftDist),
                    new KeyValue(drop.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, drop);
        }
    }

    // FLIP TURN — quick arcing water trail, then retreat

    private void addFlipTurn(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        // Arc peak: tight curve for a quick hit-and-retreat
        double midX = (startX + endX) / 2.0;
        double midY = Math.min(startY, endY) - 40;

        // Arcing water trail
        int arcPoints = 25;
        for (int i = 0; i < arcPoints - 1; i++) {
            double t0 = i / (double)(arcPoints - 1);
            double t1 = (i + 1) / (double)(arcPoints - 1);

            double ax0 = lerp(lerp(startX, midX, t0), lerp(midX, endX, t0), t0);
            double ay0 = lerp(lerp(startY, midY, t0), lerp(midY, endY, t0), t0);
            double ax1 = lerp(lerp(startX, midX, t1), lerp(midX, endX, t1), t1);
            double ay1 = lerp(lerp(startY, midY, t1), lerp(midY, endY, t1), t1);

            Line seg = new Line(ax0, ay0, ax1, ay1);
            seg.setStroke(WATER_LIGHT.deriveColor(0, 1, 1, 0.9 - i * 0.03));
            seg.setStrokeWidth(10 + (Math.sin(t0 * Math.PI) * 5));
            seg.setEffect(new DropShadow(6, WATER_CYAN));
            seg.setOpacity(0);
            prepareTransientNode(seg);
            battleField.getChildren().add(seg);

            int delay = i * 12;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(seg.opacityProperty(), 0.95));
            KeyFrame fade   = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(seg.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, seg);
        }

        // Small wave crest at impact
        for (int i = 0; i < 2; i++) {
            double w = 50 + i * 14 + movePower / 10.0;
            Rectangle waveCrest = new Rectangle(w, 0);
            waveCrest.setFill(WATER_MID.deriveColor(0, 1, 1, 0.8 - i * 0.15));
            waveCrest.setArcWidth(w * 0.5);
            waveCrest.setArcHeight(w * 0.4);
            waveCrest.setX(endX - w / 2.0);
            waveCrest.setY(endY);
            waveCrest.setOpacity(0);
            prepareTransientNode(waveCrest);
            battleField.getChildren().add(waveCrest);

            int delay = 120 + i * 20;
            double peakH = 50 + i * 12 + movePower / 10.0;
            KeyFrame rise  = new KeyFrame(Duration.millis(delay),
                    new KeyValue(waveCrest.opacityProperty(), 0.85 - i * 0.1));
            KeyFrame peak  = new KeyFrame(Duration.millis(delay + 100),
                    new KeyValue(waveCrest.heightProperty(), peakH),
                    new KeyValue(waveCrest.yProperty(), endY - peakH));
            KeyFrame crash = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(waveCrest.heightProperty(), 0),
                    new KeyValue(waveCrest.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(rise, peak, crash);
            registerCleanup(timeline, waveCrest);
        }

        // Splash droplets
        addImpactSplash(endX, endY, movePower / 2, 140, timeline);
    }

    // OCTAZOOKA — dark ink blob projectile that splatters on impact

    private void addOctazooka(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        Color inkDark   = Color.web("#1A237E");
        Color inkMid    = Color.web("#283593");

        // Main ink blob projectile
        double blobR = 26 + movePower / 10.0;
        Circle inkBlob = new Circle(blobR);
        inkBlob.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, inkDark),
                new Stop(0.5, inkMid),
                new Stop(1, WATER_DEEP)));
        inkBlob.setEffect(new DropShadow(blobR * 0.8, inkDark));
        inkBlob.setCenterX(startX);
        inkBlob.setCenterY(startY);
        inkBlob.setOpacity(0);
        prepareTransientNode(inkBlob);
        battleField.getChildren().add(inkBlob);

        KeyFrame blobAppear = new KeyFrame(Duration.millis(30),
                new KeyValue(inkBlob.opacityProperty(), 0.95));
        KeyFrame blobTravel = new KeyFrame(Duration.millis(220),
                new KeyValue(inkBlob.centerXProperty(), endX),
                new KeyValue(inkBlob.centerYProperty(), endY));
        KeyFrame blobHit    = new KeyFrame(Duration.millis(260),
                new KeyValue(inkBlob.radiusProperty(), blobR * 1.4),
                new KeyValue(inkBlob.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(blobAppear, blobTravel, blobHit);
        registerCleanup(timeline, inkBlob);

        // Smaller trailing ink drops along the path
        int trailCount = 20 + movePower / 12;
        double dx = endX - startX;
        double dy = endY - startY;
        for (int i = 0; i < trailCount; i++) {
            double t = (i + random.nextDouble() * 0.5) / trailCount;
            double px = startX + dx * t;
            double py = startY + dy * t;
            Circle trail = new Circle(8 + random.nextDouble() * 4,
                    (i % 2 == 0) ? inkDark : inkMid);
            trail.setCenterX(px + (random.nextDouble() - 0.5) * 14);
            trail.setCenterY(py + (random.nextDouble() - 0.5) * 14);
            trail.setEffect(new GaussianBlur(3));
            trail.setOpacity(0);
            prepareTransientNode(trail);
            battleField.getChildren().add(trail);

            int delay = (int)(t * 200);
            KeyFrame appear = new KeyFrame(Duration.millis(delay + 40),
                    new KeyValue(trail.opacityProperty(), 0.8));
            KeyFrame fade   = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(trail.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, trail);
        }

        // Ink splatter at impact
        int splatCount = 20 + movePower / 8;
        for (int i = 0; i < splatCount; i++) {
            double splatR = 10 + random.nextDouble() * 8;
            Color splatCol = (i % 3 == 0) ? inkDark
                           : (i % 3 == 1) ? inkMid
                           : WATER_DEEP;
            Circle splat = new Circle(splatR, splatCol);
            splat.setCenterX(endX);
            splat.setCenterY(endY);
            splat.setEffect(new GaussianBlur(3));
            splat.setOpacity(0);
            prepareTransientNode(splat);
            battleField.getChildren().add(splat);

            double driftAngle = Math.PI * 2 * i / splatCount + (random.nextDouble() - 0.5) * 0.6;
            double driftDist = 20 + random.nextDouble() * 45;

            KeyFrame appear  = new KeyFrame(Duration.millis(220),
                    new KeyValue(splat.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(420),
                    new KeyValue(splat.centerXProperty(), endX + Math.cos(driftAngle) * driftDist),
                    new KeyValue(splat.centerYProperty(), endY + Math.sin(driftAngle) * driftDist),
                    new KeyValue(splat.radiusProperty(), splatR * 1.3),
                    new KeyValue(splat.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, splat);
        }
    }

    // WATER SPOUT — massive geyser erupting from below the defender

    private void addWaterSpout(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double geyserBaseY = endY + 80;
        double geyserPeakY = endY - 180 - movePower / 3.0;

        // Geyser column: layered rectangles erupting upward
        int columnLayers = 17;
        for (int i = 0; i < columnLayers; i++) {
            double w = 50 + i * 12 + movePower / 6.0;
            Rectangle column = new Rectangle(w, 0);
            column.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, WATER_FOAM.deriveColor(0, 1, 1, 0.85 - i * 0.05)),
                    new Stop(0.3, WATER_LIGHT.deriveColor(0, 1, 1, 0.8)),
                    new Stop(0.7, WATER_MID.deriveColor(0, 1, 1, 0.75)),
                    new Stop(1, WATER_DEEP.deriveColor(0, 1, 1, 0.7))));
            column.setArcWidth(w * 0.5);
            column.setArcHeight(20);
            column.setEffect(new DropShadow(12 + i * 2, WATER_LIGHT));
            column.setX(endX - w / 2.0);
            column.setY(geyserBaseY);
            column.setOpacity(0);
            prepareTransientNode(column);
            battleField.getChildren().add(column);

            int delay = i * 25;
            double columnH = geyserBaseY - geyserPeakY + i * 10;

            KeyFrame erupt = new KeyFrame(Duration.millis(delay + 40),
                    new KeyValue(column.opacityProperty(), 0.9 - i * 0.05));
            KeyFrame peak  = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(column.heightProperty(), columnH),
                    new KeyValue(column.yProperty(), geyserBaseY - columnH));
            KeyFrame hold  = new KeyFrame(Duration.millis(delay + 360),
                    new KeyValue(column.opacityProperty(), 0.8 - i * 0.05));
            KeyFrame cascade = new KeyFrame(Duration.millis(delay + 520),
                    new KeyValue(column.heightProperty(), columnH * 0.15),
                    new KeyValue(column.yProperty(), geyserBaseY - columnH * 0.15),
                    new KeyValue(column.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(erupt, peak, hold, cascade);
            registerCleanup(timeline, column);
        }

        // Spray droplets erupting from the top
        int sprayCount = 30 + movePower / 6;
        for (int i = 0; i < sprayCount; i++) {
            Circle drop = new Circle(3 + random.nextDouble() * 5,
                    (i % 3 == 0) ? WATER_FOAM
                  : (i % 3 == 1) ? WATER_LIGHT
                  : WATER_CYAN);
            drop.setCenterX(endX + (random.nextDouble() - 0.5) * 50);
            drop.setCenterY(geyserPeakY + random.nextDouble() * 30);
            drop.setEffect(new DropShadow(5, WATER_LIGHT));
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            int delay = 140 + random.nextInt(120);
            double driftX = (random.nextDouble() - 0.5) * 100;
            double driftY = -40 - random.nextDouble() * 60;

            KeyFrame appear  = new KeyFrame(Duration.millis(delay),
                    new KeyValue(drop.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(drop.centerXProperty(), drop.getCenterX() + driftX),
                    new KeyValue(drop.centerYProperty(), drop.getCenterY() + driftY),
                    new KeyValue(drop.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, drop);
        }

        // Cascading water falling back down
        int cascadeCount = 25 + movePower / 8;
        for (int i = 0; i < cascadeCount; i++) {
            Circle drop = new Circle(3.5 + random.nextDouble() * 4, WATER_MID);
            double ox = (random.nextDouble() - 0.5) * 70;
            drop.setCenterX(endX + ox);
            drop.setCenterY(geyserPeakY + random.nextDouble() * 40);
            drop.setEffect(new GaussianBlur(3));
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            int delay = 320 + random.nextInt(160);
            double fallDist = 120 + random.nextDouble() * 100;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(drop.opacityProperty(), 0.8));
            KeyFrame fall   = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(drop.centerYProperty(), drop.getCenterY() + fallDist),
                    new KeyValue(drop.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, fall);
            registerCleanup(timeline, drop);
        }

        // Expanding ripple rings at the base
        for (int r = 0; r < 4; r++) {
            Ellipse ripple = new Ellipse(25 + r * 8, 15 + r * 3);
            ripple.setCenterX(endX);
            ripple.setCenterY(geyserBaseY);
            ripple.setFill(Color.TRANSPARENT);
            ripple.setStroke(WATER_FOAM);
            ripple.setStrokeWidth(2.5 - r * 0.4);
            ripple.setEffect(new DropShadow(6, WATER_LIGHT));
            ripple.setOpacity(0);
            prepareTransientNode(ripple);
            battleField.getChildren().add(ripple);

            int delay = 60 + r * 70;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ripple.opacityProperty(), 0.9));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(ripple.radiusXProperty(), ripple.getRadiusX() * 3),
                    new KeyValue(ripple.radiusYProperty(), ripple.getRadiusY() * 2.5),
                    new KeyValue(ripple.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, expand);
            registerCleanup(timeline, ripple);
        }
    }

    // FALLBACK BEAM  (generic water, and crabhammer impact)

    private void addFallbackBeam(double startX, double startY, double endX, double endY,
            int movePower, Timeline timeline) {

        double angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
        double dist  = Math.hypot(endX - startX, endY - startY);
        double beamW = 30 + movePower / 9.0;

        Rectangle beam = new Rectangle(0, beamW);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, WATER_FOAM.deriveColor(0, 1, 1, 0.9)),
                new Stop(0.5, WATER_LIGHT.deriveColor(0, 1, 1, 0.85)),
                new Stop(1, WATER_DEEP.deriveColor(0, 1, 1, 0.75))));
        beam.setArcWidth(beamW);
        beam.setArcHeight(beamW);
        beam.setX(startX);
        beam.setY(startY - beamW / 2.0);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(beamW * 0.7, WATER_LIGHT));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        KeyFrame show  = new KeyFrame(Duration.millis(25),
                new KeyValue(beam.opacityProperty(), 0.88));
        KeyFrame shoot = new KeyFrame(Duration.millis(200),
                new KeyValue(beam.widthProperty(), dist));
        KeyFrame hold  = new KeyFrame(Duration.millis(300),
                new KeyValue(beam.opacityProperty(), 0.88));
        KeyFrame fade  = new KeyFrame(Duration.millis(400),
                new KeyValue(beam.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(show, shoot, hold, fade);
        registerCleanup(timeline, beam);

        addImpactSplash(endX, endY, movePower / 2, 200, timeline);
    }

    // SHARED HELPERS

    /** Small splash of droplets at the impact point. */
    private void addImpactSplash(double x, double y, int movePower, int startDelay, Timeline timeline) {
        int dropCount = 20 + movePower / 12;
        for (int i = 0; i < dropCount; i++) {
            Circle drop = new Circle(8 + random.nextDouble() * 4, WATER_FOAM);
            drop.setCenterX(x);
            drop.setCenterY(y);
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            double driftAngle = Math.PI * 2 * i / dropCount + (random.nextDouble() - 0.5) * 0.6;
            double driftDist  = 20 + random.nextDouble() * 35;

            KeyFrame appear  = new KeyFrame(Duration.millis(startDelay),
                    new KeyValue(drop.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(startDelay + 220),
                    new KeyValue(drop.centerXProperty(), x + Math.cos(driftAngle) * driftDist),
                    new KeyValue(drop.centerYProperty(), y + Math.sin(driftAngle) * driftDist - 15),
                    new KeyValue(drop.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, drop);
        }
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
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
            if (previousOnFinished != null) {
                previousOnFinished.handle(e);
            }
        });
    }
}