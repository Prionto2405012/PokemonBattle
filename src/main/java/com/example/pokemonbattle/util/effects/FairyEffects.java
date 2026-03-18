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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class FairyEffects {

    private final Pane battleField;
    private final Random random = new Random();

    private static final Color FAIRY_PINK     = Color.web("#F48FB1");
    private static final Color FAIRY_ROSE     = Color.web("#E91E63");
    private static final Color FAIRY_LIGHT    = Color.web("#FCE4EC");
    private static final Color FAIRY_GOLD     = Color.web("#FFD54F");
    private static final Color FAIRY_WHITE    = Color.web("#FFFFFF");
    private static final Color FAIRY_LAVENDER = Color.web("#CE93D8");
    private static final Color FAIRY_MOON     = Color.web("#B0BEC5");
    private static final Color FAIRY_BLUE     = Color.web("#B3E5FC");

    public FairyEffects(Pane battleField) {
        this.battleField = battleField;
    }

    public void createImpactEffect(double x, double y, String moveName,
                                   int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        switch (moveName) {
            case "play-rough"      -> addPlayRough(startX, startY, endX, endY, intensity, timeline);
            case "spirit-break"    -> addSpiritBreak(endX, endY, intensity, timeline);
            case "draining-kiss"   -> addDrainingKiss(startX, startY, endX, endY, intensity, timeline);
            case "fairy-wind",
                 "charm"           -> addFairyWind(endX, endY, intensity * 0.85, timeline);
            case "moonblast"       -> addMoonblast(startX, startY, endX, endY, intensity, timeline);
            case "dazzling-gleam",
                 "disarming-voice",
                 "sparkling-aria"  -> addDazzlingGleam(startX, startY, endX, endY, intensity, timeline);
            case "moongeist-beam"  -> addMoongeistBeam(startX, startY, endX, endY, intensity, timeline);
            case "misty-explosion" -> addMistyExplosion(endX, endY, intensity, timeline);
            case "strange-steam"   -> addFairyWind(endX, endY, intensity * 1.1, timeline);
            default                -> addDefaultFairyBurst(endX, endY, intensity, timeline);
        }
    }

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        switch (moveName) {
            case "moonblast"      -> addMoonblast(startX, startY, endX, endY, intensity, timeline);
            case "moongeist-beam" -> addMoongeistBeam(startX, startY, endX, endY, intensity, timeline);
            default               -> addDazzlingGleam(startX, startY, endX, endY, intensity, timeline);
        }
    }

    private void addPlayRough(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;
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
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(sparkle.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 180),
                            new KeyValue(sparkle.centerYProperty(), ty - 10 - random.nextDouble() * 10),
                            new KeyValue(sparkle.opacityProperty(), 0)));
            registerCleanup(timeline, sparkle);
        }

        int slashCount = (int) (10 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle = -45 + i * (90.0 / Math.max(slashCount - 1, 1));
            double rad = Math.toRadians(angle);
            double slashLen = 22 + 14 * intensity;
            Line slash = new Line(
                    ex - Math.cos(rad) * slashLen * 0.5, ey - Math.sin(rad) * slashLen * 0.5,
                    ex + Math.cos(rad) * slashLen * 0.5, ey + Math.sin(rad) * slashLen * 0.5);
            slash.setStroke(i % 2 == 0 ? FAIRY_PINK : FAIRY_ROSE);
            slash.setStrokeWidth(6 + intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(8 + 3 * intensity, FAIRY_PINK));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);
            int delay = 100 + i * 30;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(slash.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 75),
                            new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.6),
                            new KeyValue(slash.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 220), new KeyValue(slash.opacityProperty(), 0)));
            registerCleanup(timeline, slash);
        }

        addFairyFlash(ex, ey, 22 + 10 * intensity, FAIRY_GOLD, 90, 200, timeline);
    }

    private void addSpiritBreak(double x, double y, double intensity, Timeline timeline) {
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
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0), new KeyValue(ring.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(ring.radiusProperty(), ringR),
                        new KeyValue(ring.opacityProperty(), 0.4)),
                new KeyFrame(Duration.millis(360),
                        new KeyValue(ring.radiusProperty(), ringR * 1.5),
                        new KeyValue(ring.opacityProperty(), 0)));
        registerCleanup(timeline, ring);
        addFairyShards(x, y, intensity, 0, timeline);
        addFairyFlash(x, y, 28 + 12 * intensity, FAIRY_GOLD, 0, 180, timeline);
    }

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
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(orb.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(orb.centerXProperty(), sx + (random.nextDouble() - 0.5) * 16),
                            new KeyValue(orb.centerYProperty(), sy + (random.nextDouble() - 0.5) * 16),
                            new KeyValue(orb.radiusProperty(), orb.getRadius() * 0.5)),
                    new KeyFrame(Duration.millis(delay + 280), new KeyValue(orb.opacityProperty(), 0)));
            registerCleanup(timeline, orb);
        }
        addFairyFlash(ex, ey, 20 + 8 * intensity, FAIRY_PINK, 0, 180, timeline);
    }

    private void addFairyWind(double x, double y, double intensity, Timeline timeline) {
        int count = (int) (18 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            double startR = 8 + random.nextDouble() * 10;
            Circle sparkle = new Circle(8 + random.nextDouble() * 3,
                    i % 3 == 0 ? FAIRY_PINK : i % 3 == 1 ? FAIRY_LIGHT : FAIRY_LAVENDER);
            sparkle.setEffect(new GaussianBlur(3));
            sparkle.setCenterX(x + Math.cos(angle) * startR);
            sparkle.setCenterY(y + Math.sin(angle) * startR);
            sparkle.setOpacity(0);
            prepareTransientNode(sparkle);
            battleField.getChildren().add(sparkle);
            double spiralAngle = angle + 1.0;
            double targetR = startR + 28 + random.nextDouble() * 18 * intensity;
            int delay = i * 28;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(sparkle.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(sparkle.centerXProperty(), x + Math.cos(spiralAngle) * targetR),
                            new KeyValue(sparkle.centerYProperty(), y + Math.sin(spiralAngle) * targetR),
                            new KeyValue(sparkle.opacityProperty(), 0.4)),
                    new KeyFrame(Duration.millis(delay + 340), new KeyValue(sparkle.opacityProperty(), 0)));
            registerCleanup(timeline, sparkle);
        }
    }

    private void addMoonblast(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        double orbRadius = 20 + 7 * intensity;
        Circle orb = new Circle(orbRadius, FAIRY_LIGHT.deriveColor(0, 1, 1, 0.9));
        orb.setStroke(FAIRY_PINK);
        orb.setStrokeWidth(3);
        orb.setEffect(new DropShadow(20 + 8 * intensity, FAIRY_ROSE));
        orb.setCenterX(sx);
        orb.setCenterY(sy);
        orb.setOpacity(0);
        prepareTransientNode(orb);
        battleField.getChildren().add(orb);

        Circle glow = new Circle(orbRadius * 0.5, FAIRY_GOLD.deriveColor(0, 1, 1, 0.7));
        glow.setEffect(new GaussianBlur(6));
        glow.setCenterX(sx);
        glow.setCenterY(sy);
        glow.setOpacity(0);
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);

        double midX = (sx + ex) / 2;
        double midY = Math.min(sy, ey) - 40 - 10 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(30),
                        new KeyValue(orb.opacityProperty(), 1.0),
                        new KeyValue(glow.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(160),
                        new KeyValue(orb.centerXProperty(), midX),
                        new KeyValue(orb.centerYProperty(), midY),
                        new KeyValue(glow.centerXProperty(), midX),
                        new KeyValue(glow.centerYProperty(), midY)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(orb.centerXProperty(), ex),
                        new KeyValue(orb.centerYProperty(), ey),
                        new KeyValue(glow.centerXProperty(), ex),
                        new KeyValue(glow.centerYProperty(), ey)),
                new KeyFrame(Duration.millis(360),
                        new KeyValue(orb.opacityProperty(), 0),
                        new KeyValue(glow.opacityProperty(), 0),
                        new KeyValue(orb.radiusProperty(), orbRadius * 0.4)));
        registerCleanup(timeline, orb);
        registerCleanup(timeline, glow);

        addMoonTrail(sx, sy, ex, ey, intensity, timeline);
        addFairyShards(ex, ey, intensity, 280, timeline);
    }

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
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(sparkle.opacityProperty(), 0.75)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(sparkle.centerYProperty(), wy - 12 - random.nextDouble() * 10),
                            new KeyValue(sparkle.radiusProperty(), sparkle.getRadius() * 1.5),
                            new KeyValue(sparkle.opacityProperty(), 0)));
            registerCleanup(timeline, sparkle);
        }
    }

    // Dazzling gleam — wide rainbow beam using layered Rectangles
    private void addDazzlingGleam(double sx, double sy, double ex, double ey,
                                  double intensity, Timeline timeline) {
        double angle  = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist   = Math.hypot(ex - sx, ey - sy);

        // Increase beamH to make the beam thicker
        double beamH  = 24 + 8 * intensity;
        double glowH  = beamH * 2.2;

        Rectangle glowRect = new Rectangle(0, glowH);
        glowRect.setFill(FAIRY_PINK.deriveColor(0, 1, 1, 0.30));
        glowRect.setArcWidth(glowH); glowRect.setArcHeight(glowH);
        glowRect.setX(sx); glowRect.setY(sy - glowH / 2);
        glowRect.setRotate(angle);
        glowRect.setEffect(new GaussianBlur(glowH * 0.4));
        glowRect.setOpacity(0);
        prepareTransientNode(glowRect);
        battleField.getChildren().add(glowRect);

        Rectangle beam = new Rectangle(0, beamH);
        beam.setFill(FAIRY_WHITE.deriveColor(0, 1, 1, 0.88));
        beam.setArcWidth(beamH); beam.setArcHeight(beamH);
        beam.setX(sx); beam.setY(sy - beamH / 2);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(beamH * 1.1, FAIRY_PINK));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        double coreH = beamH * 0.3;
        Rectangle core = new Rectangle(0, coreH);
        core.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.95));
        core.setArcWidth(coreH); core.setArcHeight(coreH);
        core.setX(sx); core.setY(sy - coreH / 2);
        core.setRotate(angle);
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),
                        new KeyValue(glowRect.opacityProperty(), 0.7),
                        new KeyValue(beam.opacityProperty(), 0.9),
                        new KeyValue(core.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(glowRect.widthProperty(), dist),
                        new KeyValue(beam.widthProperty(), dist),
                        new KeyValue(core.widthProperty(), dist)),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(glowRect.opacityProperty(), 0.7),
                        new KeyValue(beam.opacityProperty(), 0.9),
                        new KeyValue(core.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(440),
                        new KeyValue(glowRect.opacityProperty(), 0),
                        new KeyValue(beam.opacityProperty(), 0),
                        new KeyValue(core.opacityProperty(), 0)));
        registerCleanup(timeline, glowRect);
        registerCleanup(timeline, beam);
        registerCleanup(timeline, core);

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
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(sparkle.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(sparkle.centerYProperty(), wy - 14 - random.nextDouble() * 12),
                            new KeyValue(sparkle.opacityProperty(), 0)));
            registerCleanup(timeline, sparkle);
        }

        addFairyFlash(ex, ey, 26 + 12 * intensity, FAIRY_WHITE, 180, 200, timeline);
    }

    // Moongeist beam — silver moon lance using layered Rectangles
    private void addMoongeistBeam(double sx, double sy, double ex, double ey,
                                  double intensity, Timeline timeline) {
        double angle  = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist   = Math.hypot(ex - sx, ey - sy);

        // Increase beamH to make the beam thicker
        double beamH  = 20 + 6 * intensity;
        double glowH  = beamH * 2.0;

        Rectangle glowRect = new Rectangle(0, glowH);
        glowRect.setFill(FAIRY_MOON.deriveColor(0, 1, 1, 0.28));
        glowRect.setArcWidth(glowH); glowRect.setArcHeight(glowH);
        glowRect.setX(sx); glowRect.setY(sy - glowH / 2);
        glowRect.setRotate(angle);
        glowRect.setEffect(new GaussianBlur(glowH * 0.4));
        glowRect.setOpacity(0);
        prepareTransientNode(glowRect);
        battleField.getChildren().add(glowRect);

        Rectangle beam = new Rectangle(0, beamH);
        beam.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, FAIRY_LIGHT.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.5, FAIRY_MOON.deriveColor(0, 1, 1, 0.90)),
                new Stop(1.0, FAIRY_BLUE.deriveColor(0, 1, 1, 0.85))));
        beam.setArcWidth(beamH); beam.setArcHeight(beamH);
        beam.setX(sx); beam.setY(sy - beamH / 2);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(beamH * 0.9, FAIRY_LIGHT));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        double coreH = beamH * 0.35;
        Rectangle core = new Rectangle(0, coreH);
        core.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.95));
        core.setArcWidth(coreH); core.setArcHeight(coreH);
        core.setX(sx); core.setY(sy - coreH / 2);
        core.setRotate(angle);
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),
                        new KeyValue(glowRect.opacityProperty(), 0.7),
                        new KeyValue(beam.opacityProperty(), 0.9),
                        new KeyValue(core.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(glowRect.widthProperty(), dist),
                        new KeyValue(beam.widthProperty(), dist),
                        new KeyValue(core.widthProperty(), dist)),
                new KeyFrame(Duration.millis(310),
                        new KeyValue(glowRect.opacityProperty(), 0.7),
                        new KeyValue(beam.opacityProperty(), 0.9),
                        new KeyValue(core.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(460),
                        new KeyValue(glowRect.opacityProperty(), 0),
                        new KeyValue(beam.opacityProperty(), 0),
                        new KeyValue(core.opacityProperty(), 0)));
        registerCleanup(timeline, glowRect);
        registerCleanup(timeline, beam);
        registerCleanup(timeline, core);

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
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(moonParticle.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 200), new KeyValue(moonParticle.opacityProperty(), 0)));
            registerCleanup(timeline, moonParticle);
        }

        addFairyFlash(ex, ey, 30 + 14 * intensity, FAIRY_MOON, 200, 220, timeline);
    }

    private void addMistyExplosion(double x, double y, double intensity, Timeline timeline) {
        Circle mist = new Circle(0, FAIRY_LIGHT.deriveColor(0, 1, 1, 0.6));
        mist.setEffect(new GaussianBlur(20 + 6 * intensity));
        mist.setCenterX(x);
        mist.setCenterY(y);
        mist.setOpacity(0);
        prepareTransientNode(mist);
        battleField.getChildren().add(mist);
        double mistR = 50 + 30 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0), new KeyValue(mist.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(240),
                        new KeyValue(mist.radiusProperty(), mistR),
                        new KeyValue(mist.opacityProperty(), 0.4)),
                new KeyFrame(Duration.millis(420),
                        new KeyValue(mist.radiusProperty(), mistR * 1.5),
                        new KeyValue(mist.opacityProperty(), 0)));
        registerCleanup(timeline, mist);

        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(FAIRY_PINK.deriveColor(0, 1, 1, 0.7));
        ring.setStrokeWidth(8 + 1.5 * intensity);
        ring.setCenterX(x);
        ring.setCenterY(y);
        ring.setEffect(new GaussianBlur(5));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(30), new KeyValue(ring.opacityProperty(), 0.85)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(ring.radiusProperty(), mistR * 0.9),
                        new KeyValue(ring.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(ring.radiusProperty(), mistR * 1.4),
                        new KeyValue(ring.opacityProperty(), 0)));
        registerCleanup(timeline, ring);

        addFairyShards(x, y, intensity * 1.8, 0, timeline);
        addFairyFlash(x, y, 26 + 16 * intensity, FAIRY_GOLD, 0, 200, timeline);
    }

    private void addDefaultFairyBurst(double x, double y, double intensity, Timeline timeline) {
        addSpiritBreak(x, y, intensity, timeline);
    }

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
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(shard.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(shard.centerXProperty(), x + Math.cos(angle) * burstR),
                            new KeyValue(shard.centerYProperty(), y + Math.sin(angle) * burstR),
                            new KeyValue(shard.radiusProperty(), shard.getRadius() * 1.5)),
                    new KeyFrame(Duration.millis(delay + 320), new KeyValue(shard.opacityProperty(), 0)));
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