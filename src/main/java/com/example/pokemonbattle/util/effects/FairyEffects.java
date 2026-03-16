// FairyEffects.java
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
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class FairyEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Fairy colour palette
    private static final Color FAIRY_PINK    = Color.web("#F48FB1");
    private static final Color FAIRY_ROSE    = Color.web("#E91E63");
    private static final Color FAIRY_LIGHT   = Color.web("#FCE4EC");
    private static final Color FAIRY_GOLD    = Color.web("#FFD54F");
    private static final Color FAIRY_WHITE   = Color.web("#FFFFFF");
    private static final Color FAIRY_LAVENDER = Color.web("#CE93D8");
    private static final Color FAIRY_MOON    = Color.web("#B0BEC5");
    private static final Color FAIRY_BLUE    = Color.web("#B3E5FC");

    public FairyEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // Public API – single-point overload (melee / contact moves)

    public void createImpactEffect(double x, double y, String moveName,
                                   int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    // Public API – full signature (all fairy moves)

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.8,2.4);

        switch (moveName) {
            // Physical melee moves
            case "play-rough"       -> addPlayRough(startX, startY, endX, endY, intensity, timeline);
            case "spirit-break"     -> addSpiritBreak(endX, endY, intensity, timeline);
            case "draining-kiss"    -> addDrainingKiss(startX, startY, endX, endY, intensity, timeline);

            // Sparkle / wind effects
            case "fairy-wind"       -> addFairyWind(endX, endY, intensity, timeline);
            case "charm"            -> addFairyWind(endX, endY, intensity * 0.7, timeline);

            // Ranged beam / burst
            case "moonblast"        -> addMoonblast(startX, startY, endX, endY, intensity, timeline);
            case "dazzling-gleam"   -> addDazzlingGleam(startX, startY, endX, endY, intensity, timeline);
            case "disarming-voice"  -> addDazzlingGleam(startX, startY, endX, endY, intensity * 0.85, timeline);
            case "moongeist-beam"   -> addMoongeistBeam(startX, startY, endX, endY, intensity, timeline);
            case "misty-explosion"  -> addMistyExplosion(endX, endY, intensity, timeline);
            case "sparkling-aria"   -> addDazzlingGleam(startX, startY, endX, endY, intensity * 0.9, timeline);
            case "strange-steam"    -> addFairyWind(endX, endY, intensity * 1.1, timeline);

            default                 -> addDefaultFairyBurst(endX, endY, intensity, timeline);
        }
    }

    // Public API – ranged lead effect

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8,2.4);
        switch (moveName) {
            case "moonblast"     -> addMoonblast(startX, startY, endX, endY, intensity, timeline);
            case "moongeist-beam" -> addMoongeistBeam(startX, startY, endX, endY, intensity, timeline);
            default              -> addDazzlingGleam(startX, startY, endX, endY, intensity, timeline);
        }
    }

    // Play rough – tumbling sparkle-and-paw swipes

    private void addPlayRough(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        // Sparkle trail along approach
        int trailCount = (int) (16 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 18;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 18;

            Circle sparkle = new Circle(10 + random.nextDouble() * 3,
                    i % 3 == 0 ? FAIRY_PINK : i % 3 == 1 ? FAIRY_GOLD : FAIRY_LIGHT);
            sparkle.setEffect(new GaussianBlur(3));
            sparkle.setCenterX(tx);
            sparkle.setCenterY(ty);
            sparkle.setOpacity(0);
            prepareTransientNode(sparkle);
            battleField.getChildren().add(sparkle);

            int delay = (int) (t * 140);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(sparkle.opacityProperty(), 0.8));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(sparkle.centerYProperty(), ty - 10 - random.nextDouble() * 10),
                    new KeyValue(sparkle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, sparkle);
        }

        // Slash marks at impact
        int slashCount = (int) (10 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle = -45 + i * (90.0 / Math.max(slashCount - 1, 1));
            double rad = Math.toRadians(angle);
            double slashLen = 22 + 14 * intensity;

            Line slash = new Line(
                    ex - Math.cos(rad) * slashLen * 0.5,
                    ey - Math.sin(rad) * slashLen * 0.5,
                    ex + Math.cos(rad) * slashLen * 0.5,
                    ey + Math.sin(rad) * slashLen * 0.5);
            slash.setStroke(i % 2 == 0 ? FAIRY_PINK : FAIRY_ROSE);
            slash.setStrokeWidth(6 + intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(8 + 3 * intensity, FAIRY_PINK));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = 100 + i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(slash.opacityProperty(), 0.95));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 75),
                    new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.6),
                    new KeyValue(slash.opacityProperty(), 0.85));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(slash.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, slash);
        }

        addFairyFlash(ex, ey, 22 + 10 * intensity, FAIRY_GOLD, 90, 200, timeline);
    }

    // Spirit break – shattering pink energy burst

    private void addSpiritBreak(double x, double y, double intensity, Timeline timeline) {
        // Expanding pink shockwave ring
        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(FAIRY_PINK.deriveColor(0, 1, 1, 0.75));
        ring.setStrokeWidth(8 + 1.5 * intensity);
        ring.setCenterX(x);
        ring.setCenterY(y);
        ring.setEffect(new DropShadow(12 + 4 * intensity, FAIRY_ROSE));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        double ringR = 35 + 22 * intensity;
        KeyFrame rAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(ring.opacityProperty(), 0.9));
        KeyFrame rExpand = new KeyFrame(Duration.millis(200),
                new KeyValue(ring.radiusProperty(), ringR),
                new KeyValue(ring.opacityProperty(), 0.4));
        KeyFrame rFade = new KeyFrame(Duration.millis(360),
                new KeyValue(ring.radiusProperty(), ringR * 1.5),
                new KeyValue(ring.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(rAppear, rExpand, rFade);
        registerCleanup(timeline, ring);

        // Burst shards
        addFairyShards(x, y, intensity, 0, timeline);
        addFairyFlash(x, y, 28 + 12 * intensity, FAIRY_GOLD, 0, 180, timeline);
    }

    // Draining kiss – heart-shaped energy pulled toward attacker

    private void addDrainingKiss(double sx, double sy, double ex, double ey,
                                 double intensity, Timeline timeline) {
        int orbCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < orbCount; i++) {
            Circle orb = new Circle(9 + random.nextDouble() * 4,
                    i % 2 == 0 ? FAIRY_PINK : FAIRY_LAVENDER);
            orb.setEffect(new DropShadow(6, FAIRY_ROSE));
            orb.setCenterX(ex + (random.nextDouble() - 0.5) * 20);
            orb.setCenterY(ey + (random.nextDouble() - 0.5) * 20);
            orb.setOpacity(0);
            prepareTransientNode(orb);
            battleField.getChildren().add(orb);

            int delay = i * 40;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(orb.opacityProperty(), 0.85));
            KeyFrame drain = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(orb.centerXProperty(), sx + (random.nextDouble() - 0.5) * 16),
                    new KeyValue(orb.centerYProperty(), sy + (random.nextDouble() - 0.5) * 16),
                    new KeyValue(orb.radiusProperty(), orb.getRadius() * 0.5));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(orb.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drain, fade);
            registerCleanup(timeline, orb);
        }

        addFairyFlash(ex, ey, 20 + 8 * intensity, FAIRY_PINK, 0, 180, timeline);
    }

    // Fairy wind – swirling pastel sparkles

    private void addFairyWind(double x, double y, double intensity, Timeline timeline) {
        int count = (int) (18 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            double startR = 8 + random.nextDouble() * 10;
            double px = x + Math.cos(angle) * startR;
            double py = y + Math.sin(angle) * startR;

            Circle sparkle = new Circle(8 + random.nextDouble() * 3,
                    i % 3 == 0 ? FAIRY_PINK : i % 3 == 1 ? FAIRY_LIGHT : FAIRY_LAVENDER);
            sparkle.setEffect(new GaussianBlur(3));
            sparkle.setCenterX(px);
            sparkle.setCenterY(py);
            sparkle.setOpacity(0);
            prepareTransientNode(sparkle);
            battleField.getChildren().add(sparkle);

            double spiralAngle = angle + 1.0;
            double targetR = startR + 28 + random.nextDouble() * 18 * intensity;
            int delay = i * 28;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(sparkle.opacityProperty(), 0.85));
            KeyFrame spiral = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(sparkle.centerXProperty(), x + Math.cos(spiralAngle) * targetR),
                    new KeyValue(sparkle.centerYProperty(), y + Math.sin(spiralAngle) * targetR),
                    new KeyValue(sparkle.opacityProperty(), 0.4));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 340),
                    new KeyValue(sparkle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, spiral, fade);
            registerCleanup(timeline, sparkle);
        }
    }

    // Moonblast – pink moon-energy sphere hurled at target

    private void addMoonblast(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        double orbRadius = 20 + 7 * intensity;

        // Main orb
        Circle orb = new Circle(orbRadius, FAIRY_LIGHT.deriveColor(0, 1, 1, 0.9));
        orb.setStroke(FAIRY_PINK);
        orb.setStrokeWidth(3);
        orb.setEffect(new DropShadow(20 + 8 * intensity, FAIRY_ROSE));
        orb.setCenterX(sx);
        orb.setCenterY(sy);
        orb.setOpacity(0);
        prepareTransientNode(orb);
        battleField.getChildren().add(orb);

        // Inner glow
        Circle glow = new Circle(orbRadius * 0.5, FAIRY_GOLD.deriveColor(0, 1, 1, 0.7));
        glow.setEffect(new GaussianBlur(6));
        glow.setCenterX(sx);
        glow.setCenterY(sy);
        glow.setOpacity(0);
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);

        double midX = (sx + ex) / 2;
        double midY = Math.min(sy, ey) - 40 - 10 * intensity;

        KeyFrame appear = new KeyFrame(Duration.millis(30),
                new KeyValue(orb.opacityProperty(), 1.0),
                new KeyValue(glow.opacityProperty(), 0.9));
        KeyFrame arc = new KeyFrame(Duration.millis(160),
                new KeyValue(orb.centerXProperty(), midX),
                new KeyValue(orb.centerYProperty(), midY),
                new KeyValue(glow.centerXProperty(), midX),
                new KeyValue(glow.centerYProperty(), midY));
        KeyFrame impact = new KeyFrame(Duration.millis(280),
                new KeyValue(orb.centerXProperty(), ex),
                new KeyValue(orb.centerYProperty(), ey),
                new KeyValue(glow.centerXProperty(), ex),
                new KeyValue(glow.centerYProperty(), ey));
        KeyFrame burst = new KeyFrame(Duration.millis(360),
                new KeyValue(orb.opacityProperty(), 0),
                new KeyValue(glow.opacityProperty(), 0),
                new KeyValue(orb.radiusProperty(), orbRadius * 0.4));

        timeline.getKeyFrames().addAll(appear, arc, impact, burst);
        registerCleanup(timeline, orb);
        registerCleanup(timeline, glow);

        // Trailing sparkles
        addMoonTrail(sx, sy, ex, ey, intensity, timeline);
        addFairyShards(ex, ey, intensity, 280, timeline);
    }

    /** Sparkle trail behind the moonblast orb. */
    private void addMoonTrail(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int count = (int) (18 + 5 * intensity);
        double dx = ex - sx;
        double dy = ey - sy;

        for (int i = 0; i < count; i++) {
            double t = (i + random.nextDouble()) / count * 0.9;
            double wx = sx + dx * t + (random.nextDouble() - 0.5) * 18;
            double wy = sy + dy * t + (random.nextDouble() - 0.5) * 18;

            Circle sparkle = new Circle(8 + random.nextDouble() * 3,
                    i % 2 == 0 ? FAIRY_LIGHT : FAIRY_PINK);
            sparkle.setEffect(new GaussianBlur(3));
            sparkle.setCenterX(wx);
            sparkle.setCenterY(wy);
            sparkle.setOpacity(0);
            prepareTransientNode(sparkle);
            battleField.getChildren().add(sparkle);

            int delay = (int) (t * 240) + 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(sparkle.opacityProperty(), 0.75));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(sparkle.centerYProperty(), wy - 12 - random.nextDouble() * 10),
                    new KeyValue(sparkle.radiusProperty(), sparkle.getRadius() * 1.5),
                    new KeyValue(sparkle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, sparkle);
        }
    }

    // Dazzling gleam – wide horizontal burst of rainbow sparkles

    private void addDazzlingGleam(double sx, double sy, double ex, double ey,
                                  double intensity, Timeline timeline) {
        // Wide horizontal beam
        Line beam = new Line(sx, sy, sx, sy);
        beam.setStroke(FAIRY_WHITE.deriveColor(0, 1, 1, 0.85));
        beam.setStrokeWidth(12 + 2 * intensity);
        beam.setEffect(new DropShadow(28 + 5 * intensity, FAIRY_PINK));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame bExtend = new KeyFrame(Duration.millis(200),
                new KeyValue(beam.endXProperty(), ex),
                new KeyValue(beam.endYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(340),
                new KeyValue(beam.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bExtend, bFade);
        registerCleanup(timeline, beam);

        // Rainbow sparkles along beam
        int count = (int) (18 + 6 * intensity);
        Color[] sparkleColors = { FAIRY_PINK, FAIRY_GOLD, FAIRY_LAVENDER, FAIRY_BLUE, FAIRY_WHITE };
        for (int i = 0; i < count; i++) {
            double t = (i + random.nextDouble()) / count;
            double wx = sx + (ex - sx) * t + (random.nextDouble() - 0.5) * 14;
            double wy = sy + (ey - sy) * t + (random.nextDouble() - 0.5) * 14;

            Circle sparkle = new Circle(8 + random.nextDouble() * 3.5,
                    sparkleColors[i % sparkleColors.length]);
            sparkle.setEffect(new GaussianBlur(3));
            sparkle.setCenterX(wx);
            sparkle.setCenterY(wy);
            sparkle.setOpacity(0);
            prepareTransientNode(sparkle);
            battleField.getChildren().add(sparkle);

            int delay = (int) (t * 180) + 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(sparkle.opacityProperty(), 0.85));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(sparkle.centerYProperty(), wy - 14 - random.nextDouble() * 12),
                    new KeyValue(sparkle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, sparkle);
        }

        addFairyFlash(ex, ey, 26 + 12 * intensity, FAIRY_WHITE, 180, 200, timeline);
    }

    // Moongeist beam – silver moon-beam lance

    private void addMoongeistBeam(double sx, double sy, double ex, double ey,
                                  double intensity, Timeline timeline) {
        // Core silver beam
        Line beam = new Line(sx, sy, sx, sy);
        beam.setStroke(FAIRY_MOON.deriveColor(0, 1, 1, 0.9));
        beam.setStrokeWidth(10 + 3 * intensity);
        beam.setEffect(new DropShadow(22 + 6 * intensity, FAIRY_LIGHT));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        // Inner bright core
        Line core = new Line(sx, sy, sx, sy);
        core.setStroke(FAIRY_WHITE.deriveColor(0, 1, 1, 0.95));
        core.setStrokeWidth(8 + 2 * intensity);
        core.setEffect(new GaussianBlur(2));
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(beam.opacityProperty(), 0.9),
                new KeyValue(core.opacityProperty(), 1.0));
        KeyFrame bExtend = new KeyFrame(Duration.millis(220),
                new KeyValue(beam.endXProperty(), ex),
                new KeyValue(beam.endYProperty(), ey),
                new KeyValue(core.endXProperty(), ex),
                new KeyValue(core.endYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(360),
                new KeyValue(beam.opacityProperty(), 0),
                new KeyValue(core.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bExtend, bFade);
        registerCleanup(timeline, beam);
        registerCleanup(timeline, core);

        // Moon crescent particles along beam
        int count = (int) (15 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;
            double wx = sx + (ex - sx) * t + (random.nextDouble() - 0.5) * 12;
            double wy = sy + (ey - sy) * t + (random.nextDouble() - 0.5) * 12;

            Circle moonParticle = new Circle(10 + random.nextDouble() * 3, FAIRY_MOON);
            moonParticle.setEffect(new GaussianBlur(3));
            moonParticle.setCenterX(wx);
            moonParticle.setCenterY(wy);
            moonParticle.setOpacity(0);
            prepareTransientNode(moonParticle);
            battleField.getChildren().add(moonParticle);

            int delay = (int) (t * 200) + 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(moonParticle.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(moonParticle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, moonParticle);
        }

        addFairyFlash(ex, ey, 30 + 14 * intensity, FAIRY_MOON, 200, 220, timeline);
    }

    // Misty explosion – huge pastel burst expanding in all directions

    private void addMistyExplosion(double x, double y, double intensity, Timeline timeline) {
        // Large misty expansion circle
        Circle mist = new Circle(0, FAIRY_LIGHT.deriveColor(0, 1, 1, 0.6));
        mist.setEffect(new GaussianBlur(20 + 6 * intensity));
        mist.setCenterX(x);
        mist.setCenterY(y);
        mist.setOpacity(0);
        prepareTransientNode(mist);
        battleField.getChildren().add(mist);

        double mistR = 50 + 30 * intensity;
        KeyFrame mAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(mist.opacityProperty(), 0.8));
        KeyFrame mExpand = new KeyFrame(Duration.millis(240),
                new KeyValue(mist.radiusProperty(), mistR),
                new KeyValue(mist.opacityProperty(), 0.4));
        KeyFrame mFade = new KeyFrame(Duration.millis(420),
                new KeyValue(mist.radiusProperty(), mistR * 1.5),
                new KeyValue(mist.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(mAppear, mExpand, mFade);
        registerCleanup(timeline, mist);

        // Ring overlay
        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(FAIRY_PINK.deriveColor(0, 1, 1, 0.7));
        ring.setStrokeWidth(8 + 1.5 * intensity);
        ring.setCenterX(x);
        ring.setCenterY(y);
        ring.setEffect(new GaussianBlur(5));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        KeyFrame rAppear = new KeyFrame(Duration.millis(30),
                new KeyValue(ring.opacityProperty(), 0.85));
        KeyFrame rExpand = new KeyFrame(Duration.millis(260),
                new KeyValue(ring.radiusProperty(), mistR * 0.9),
                new KeyValue(ring.opacityProperty(), 0.3));
        KeyFrame rFade = new KeyFrame(Duration.millis(400),
                new KeyValue(ring.radiusProperty(), mistR * 1.4),
                new KeyValue(ring.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(rAppear, rExpand, rFade);
        registerCleanup(timeline, ring);

        // Sparkle burst in all directions
        addFairyShards(x, y, intensity * 1.8, 0, timeline);
        addFairyFlash(x, y, 26 + 16 * intensity, FAIRY_GOLD, 0, 200, timeline);
    }

    // Default fairy burst – basic sparkle circle

    private void addDefaultFairyBurst(double x, double y, double intensity, Timeline timeline) {
        addSpiritBreak(x, y, intensity, timeline);
    }

    // Shared helpers – fairy shards, flash

    private void addFairyShards(double x, double y, double intensity,
                                int startDelay, Timeline timeline) {
        int count = (int) (18 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            Circle shard = new Circle(8 + random.nextDouble() * 3,
                    i % 3 == 0 ? FAIRY_PINK : i % 3 == 1 ? FAIRY_GOLD : FAIRY_LIGHT);
            shard.setEffect(new GaussianBlur(2));
            shard.setCenterX(x);
            shard.setCenterY(y);
            shard.setOpacity(0);
            prepareTransientNode(shard);
            battleField.getChildren().add(shard);

            double burstR = 30 + 20 * intensity;
            int delay = startDelay + i * 18;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(shard.opacityProperty(), 0.9));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(shard.centerXProperty(), x + Math.cos(angle) * burstR),
                    new KeyValue(shard.centerYProperty(), y + Math.sin(angle) * burstR),
                    new KeyValue(shard.radiusProperty(), shard.getRadius() * 1.5));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(shard.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, shard);
        }
    }

    private void addFairyFlash(double x, double y, double radius, Color color,
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

    // Utilities

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
