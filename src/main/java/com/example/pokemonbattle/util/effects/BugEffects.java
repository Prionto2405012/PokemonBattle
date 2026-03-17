// BugEffects.java
package com.example.pokemonbattle.util.effects;

import com.example.pokemonbattle.util.MediaCache;
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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class BugEffects {

    private final Pane battleField;
    private final Random random = new Random();

    private static final Color BUG_GREEN  = Color.web("#558B2F");
    private static final Color BUG_LIME   = Color.web("#9CCC65");
    private static final Color BUG_AMBER  = Color.web("#FF8F00");
    private static final Color BUG_DARK   = Color.web("#1B5E20");
    private static final Color BUG_YELLOW = Color.web("#F9A825");
    private static final Color BUG_BROWN  = Color.web("#5D4037");
    private static final Color BUG_LIGHT  = Color.web("#DCEDC8");
    private static final Color BUG_TEAL   = Color.web("#00838F");
    private static final String FANG_ASSET = "fang.gif";

    public BugEffects(Pane battleField) {
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
            case "x-scissor",
                 "fury-cutter",
                 "lunge"       -> addScissorCut(startX, startY, endX, endY, intensity, timeline);
            case "twineedle"   -> addTwinNeedle(startX, startY, endX, endY, intensity, timeline);
            case "bug-bite"    -> { addFangImage(endX, endY, timeline);
                                    addBugBite(startX, startY, endX, endY, intensity, timeline); }
            case "leech-life"  -> { addFangImage(endX, endY, timeline);
                                    addLeechLife(startX, startY, endX, endY, intensity, timeline); }
            case "attack-order",
                 "infestation" -> addSwarmStrike(endX, endY, intensity, timeline);
            case "bug-buzz"    -> addBugBuzz(startX, startY, endX, endY, intensity, timeline);
            case "signal-beam" -> addSignalBeam(startX, startY, endX, endY, intensity, timeline);
            case "silver-wind" -> addSilverWind(startX, startY, endX, endY, intensity, timeline);
            case "pollen-puff" -> addPollenPuff(startX, startY, endX, endY, intensity, timeline);
            default            -> addDefaultBugBurst(endX, endY, intensity, timeline);
        }
    }

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        switch (moveName) {
            case "pollen-puff" -> addPollenPuff(startX, startY, endX, endY, intensity, timeline);
            case "signal-beam" -> addSignalBeam(startX, startY, endX, endY, intensity, timeline);
            case "silver-wind" -> addSilverWind(startX, startY, endX, endY, intensity, timeline);
            default            -> addBugBuzz(startX, startY, endX, endY, intensity, timeline);
        }
    }

    private void addScissorCut(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;
        int trailCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 14;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 14;
            Circle trail = new Circle(10 + random.nextDouble() * 3,
                    i % 2 == 0 ? BUG_GREEN : BUG_LIME);
            trail.setEffect(new GaussianBlur(3));
            trail.setCenterX(tx);
            trail.setCenterY(ty);
            trail.setOpacity(0);
            prepareTransientNode(trail);
            battleField.getChildren().add(trail);
            int delay = (int) (t * 130);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(trail.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 170), new KeyValue(trail.opacityProperty(), 0)));
            registerCleanup(timeline, trail);
        }

        double len = 36 + 16 * intensity;
        double[][] slashPairs = { { -55, 145 }, { 55, 235 } };
        int lineIdx = 0;
        for (double[] pair : slashPairs) {
            for (double deg : pair) {
                double rad = Math.toRadians(deg);
                Line slash = new Line(
                        ex + Math.cos(rad) * len * 0.5, ey + Math.sin(rad) * len * 0.5,
                        ex - Math.cos(rad) * len * 0.5, ey - Math.sin(rad) * len * 0.5);
                slash.setStroke(lineIdx % 2 == 0 ? BUG_LIME : BUG_AMBER);
                slash.setStrokeWidth(6 + 1.5 * intensity);
                slash.setOpacity(0);
                slash.setEffect(new DropShadow(8 + 3 * intensity, BUG_GREEN));
                prepareTransientNode(slash);
                battleField.getChildren().add(slash);
                int delay = 90 + lineIdx * 30;
                timeline.getKeyFrames().addAll(
                        new KeyFrame(Duration.millis(delay), new KeyValue(slash.opacityProperty(), 1.0)),
                        new KeyFrame(Duration.millis(delay + 80),
                                new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.6),
                                new KeyValue(slash.opacityProperty(), 0.85)),
                        new KeyFrame(Duration.millis(delay + 230), new KeyValue(slash.opacityProperty(), 0)));
                registerCleanup(timeline, slash);
                lineIdx++;
            }
        }

        addBugFlash(ex, ey, 26 + 10 * intensity, BUG_LIME, 90, 200, timeline);
    }

    private void addTwinNeedle(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;
        double len = Math.max(Math.sqrt(dx * dx + dy * dy), 1);
        double perpX = -dy / len * 8;
        double perpY =  dx / len * 8;
        for (int n = -1; n <= 1; n += 2) {
            Line needle = new Line(sx + perpX * n, sy + perpY * n, ex + perpX * n, ey + perpY * n);
            needle.setStroke(n < 0 ? BUG_GREEN : BUG_AMBER);
            needle.setStrokeWidth(6 + 1.5 * intensity);
            needle.setOpacity(0);
            needle.setEffect(new DropShadow(6 + 2 * intensity, BUG_DARK));
            prepareTransientNode(needle);
            battleField.getChildren().add(needle);
            int delay = (n < 0) ? 30 : 90;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(needle.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 200), new KeyValue(needle.opacityProperty(), 0)));
            registerCleanup(timeline, needle);
        }
        addBugFlash(ex, ey, 20 + 8 * intensity, BUG_YELLOW, 90, 160, timeline);
    }

    private void addBugBite(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;
        int streakCount = (int) (14 + 3 * intensity);
        for (int i = 0; i < streakCount; i++) {
            double t = (i + 0.5) / streakCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 12;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 12;
            Circle dot = new Circle(10 + random.nextDouble() * 2.5,
                    i % 2 == 0 ? BUG_BROWN : BUG_AMBER);
            dot.setEffect(new GaussianBlur(2));
            dot.setCenterX(tx);
            dot.setCenterY(ty);
            dot.setOpacity(0);
            prepareTransientNode(dot);
            battleField.getChildren().add(dot);
            int delay = (int) (t * 120);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(dot.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 160), new KeyValue(dot.opacityProperty(), 0)));
            registerCleanup(timeline, dot);
        }
        addBugFlash(ex, ey, 20 + 8 * intensity, BUG_AMBER, 90, 160, timeline);
    }

    private void addLeechLife(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        int orbCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < orbCount; i++) {
            Circle orb = new Circle(10 + random.nextDouble() * 3.5,
                    i % 2 == 0 ? BUG_GREEN : BUG_LIME);
            orb.setEffect(new DropShadow(5, BUG_DARK));
            orb.setCenterX(ex + (random.nextDouble() - 0.5) * 20);
            orb.setCenterY(ey + (random.nextDouble() - 0.5) * 20);
            orb.setOpacity(0);
            prepareTransientNode(orb);
            battleField.getChildren().add(orb);
            int delay = i * 40;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(orb.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(orb.centerXProperty(), sx + (random.nextDouble() - 0.5) * 14),
                            new KeyValue(orb.centerYProperty(), sy + (random.nextDouble() - 0.5) * 14),
                            new KeyValue(orb.radiusProperty(), orb.getRadius() * 0.4)),
                    new KeyFrame(Duration.millis(delay + 300), new KeyValue(orb.opacityProperty(), 0)));
            registerCleanup(timeline, orb);
        }
        addBugFlash(ex, ey, 20 + 8 * intensity, BUG_LIME, 0, 180, timeline);
    }

    private void addSwarmStrike(double x, double y, double intensity, Timeline timeline) {
        int swarmCount = (int) (18 + 6 * intensity);
        for (int i = 0; i < swarmCount; i++) {
            double angle = (i / (double) swarmCount) * 2 * Math.PI;
            double startR = 50 + 20 * intensity;
            Circle bug = new Circle(10 + random.nextDouble() * 3,
                    i % 3 == 0 ? BUG_GREEN : i % 3 == 1 ? BUG_AMBER : BUG_LIME);
            bug.setCenterX(x + Math.cos(angle) * startR);
            bug.setCenterY(y + Math.sin(angle) * startR);
            bug.setOpacity(0);
            prepareTransientNode(bug);
            battleField.getChildren().add(bug);
            int delay = i * 22;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(bug.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(bug.centerXProperty(), x + (random.nextDouble() - 0.5) * 16),
                            new KeyValue(bug.centerYProperty(), y + (random.nextDouble() - 0.5) * 16)),
                    new KeyFrame(Duration.millis(delay + 340), new KeyValue(bug.opacityProperty(), 0)));
            registerCleanup(timeline, bug);
        }
        addBugFlash(x, y, 28 + 12 * intensity, BUG_LIME, swarmCount * 22, 200, timeline);
    }

    private void addBugBuzz(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        int ringCount = (int) (13 + 2 * intensity);
        for (int i = 0; i < ringCount; i++) {
            Circle ring = new Circle(12 + i * 4, Color.TRANSPARENT);
            ring.setStroke((i % 2 == 0 ? BUG_AMBER : BUG_YELLOW).deriveColor(0, 1, 1, 0.6));
            ring.setStrokeWidth(2.5 - i * 0.3);
            ring.setCenterX(sx);
            ring.setCenterY(sy);
            ring.setEffect(new GaussianBlur(3 + i));
            ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);
            int delay = i * 55;
            double targetRadius = 40 + 22 * intensity + i * 8;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(ring.opacityProperty(), 0.75)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(ring.centerXProperty(), ex),
                            new KeyValue(ring.centerYProperty(), ey)),
                    new KeyFrame(Duration.millis(delay + 320),
                            new KeyValue(ring.radiusProperty(), targetRadius),
                            new KeyValue(ring.opacityProperty(), 0.3)),
                    new KeyFrame(Duration.millis(delay + 420), new KeyValue(ring.opacityProperty(), 0)));
            registerCleanup(timeline, ring);
        }
        addBugFlash(ex, ey, 25 + 10 * intensity, BUG_AMBER, ringCount * 55, 200, timeline);
    }

    // Signal beam — multi-colour thick Rectangle beams
    private void addSignalBeam(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double angle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist  = Math.hypot(ex - sx, ey - sy);

        Color[] beamColors = { BUG_GREEN, BUG_AMBER, BUG_TEAL };
        double[] offsets   = { 6.0, 0.0, -6.0 }; // perpendicular offsets

        double ux = (ex - sx) / dist;
        double uy = (ey - sy) / dist;
        double px = -uy;
        double py =  ux;

        for (int b = 0; b < beamColors.length; b++) {
            // Increase stripH to make each colour band thicker
            double stripH = 10 + 3 * intensity - b;
            double off = offsets[b] * (1 + intensity * 0.3);

            Rectangle strip = new Rectangle(0, stripH);
            strip.setFill(beamColors[b].deriveColor(0, 1, 1, 0.80 - b * 0.05));
            strip.setArcWidth(stripH); strip.setArcHeight(stripH);
            strip.setX(sx + px * off);
            strip.setY(sy + py * off - stripH / 2);
            strip.setRotate(angle);
            strip.setEffect(new GaussianBlur(3 + b));
            strip.setOpacity(0);
            prepareTransientNode(strip);
            battleField.getChildren().add(strip);

            int beamDelay = b * 18;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(beamDelay),     new KeyValue(strip.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(beamDelay + 200), new KeyValue(strip.widthProperty(), dist)),
                    new KeyFrame(Duration.millis(beamDelay + 340), new KeyValue(strip.opacityProperty(), 0)));
            registerCleanup(timeline, strip);
        }

        addBugFlash(ex, ey, 28 + 12 * intensity, BUG_TEAL, 220, 200, timeline);
    }

    private void addSilverWind(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        int count = (int) (18 + 6 * intensity);
        double dx = ex - sx;
        double dy = ey - sy;
        for (int i = 0; i < count; i++) {
            double t = (i + random.nextDouble()) / count;
            double px = sx + dx * t + (random.nextDouble() - 0.5) * 22;
            double py = sy + dy * t + (random.nextDouble() - 0.5) * 22;
            Circle particle = new Circle(10 + random.nextDouble() * 3.5,
                    i % 3 == 0 ? BUG_LIGHT : i % 3 == 1 ? BUG_LIME : BUG_YELLOW);
            particle.setEffect(new GaussianBlur(3));
            particle.setCenterX(px);
            particle.setCenterY(py);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);
            int delay = (int) (t * 200);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(particle.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(particle.centerYProperty(), py - 12 - random.nextDouble() * 12),
                            new KeyValue(particle.radiusProperty(), particle.getRadius() * 1.5),
                            new KeyValue(particle.opacityProperty(), 0)));
            registerCleanup(timeline, particle);
        }
        addBugFlash(ex, ey, 28 + 10 * intensity, BUG_LIGHT, count / 2 * 25, 200, timeline);
    }

    private void addPollenPuff(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        double orbRadius = 18 + 5 * intensity;
        Circle puff = new Circle(orbRadius, BUG_YELLOW.deriveColor(0, 1, 1, 0.85));
        puff.setStroke(BUG_AMBER);
        puff.setStrokeWidth(5.5);
        puff.setEffect(new DropShadow(14 + 5 * intensity, BUG_DARK));
        puff.setCenterX(sx);
        puff.setCenterY(sy);
        puff.setOpacity(0);
        prepareTransientNode(puff);
        battleField.getChildren().add(puff);

        double midX = (sx + ex) / 2;
        double midY = Math.min(sy, ey) - 38 - 10 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),  new KeyValue(puff.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(160),
                        new KeyValue(puff.centerXProperty(), midX),
                        new KeyValue(puff.centerYProperty(), midY)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(puff.centerXProperty(), ex),
                        new KeyValue(puff.centerYProperty(), ey)),
                new KeyFrame(Duration.millis(360),
                        new KeyValue(puff.opacityProperty(), 0),
                        new KeyValue(puff.radiusProperty(), orbRadius * 2)));
        registerCleanup(timeline, puff);

        int pollenCount = (int) (16 + 4 * intensity);
        for (int i = 0; i < pollenCount; i++) {
            Circle pollen = new Circle(8 + random.nextDouble() * 3, BUG_YELLOW);
            pollen.setEffect(new GaussianBlur(2));
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 10 + random.nextDouble() * 16 * intensity;
            pollen.setCenterX(ex);
            pollen.setCenterY(ey);
            pollen.setOpacity(0);
            prepareTransientNode(pollen);
            battleField.getChildren().add(pollen);
            int delay = 280 + i * 18;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(pollen.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 180),
                            new KeyValue(pollen.centerXProperty(), ex + Math.cos(angle) * dist),
                            new KeyValue(pollen.centerYProperty(), ey + Math.sin(angle) * dist)),
                    new KeyFrame(Duration.millis(delay + 280), new KeyValue(pollen.opacityProperty(), 0)));
            registerCleanup(timeline, pollen);
        }
    }

    private void addDefaultBugBurst(double x, double y, double intensity, Timeline timeline) {
        addSwarmStrike(x, y, intensity, timeline);
    }

    private void addBugFlash(double x, double y, double radius, Color color,
                             int startDelay, int fadeDuration, Timeline timeline) {
        Circle flash = new Circle(0, color.deriveColor(0, 1, 1, 0.65));
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

    private void addFangImage(double x, double y, Timeline timeline) {
        try {
            Image image = MediaCache.getImage(FANG_ASSET);
            if (image == null) return;
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(190);
            imageView.setFitHeight(190);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setLayoutX(x - 95);
            imageView.setLayoutY(y - 108);
            imageView.setOpacity(0);
            imageView.setScaleX(0.55);
            imageView.setScaleY(0.55);
            prepareTransientNode(imageView);
            battleField.getChildren().add(imageView);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(35),
                            new KeyValue(imageView.opacityProperty(), 1.0),
                            new KeyValue(imageView.scaleXProperty(), 1.25),
                            new KeyValue(imageView.scaleYProperty(), 1.25)),
                    new KeyFrame(Duration.millis(115),
                            new KeyValue(imageView.scaleXProperty(), 1.0),
                            new KeyValue(imageView.scaleYProperty(), 1.0)),
                        new KeyFrame(Duration.millis(560),
                            new KeyValue(imageView.opacityProperty(), 0.0)));
            registerCleanup(timeline, imageView);
        } catch (Exception ignored) {}
    }

    private void registerCleanup(Timeline timeline, Node node) {
        EventHandler<ActionEvent> prev = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (prev != null) prev.handle(e);
        });
    }
}