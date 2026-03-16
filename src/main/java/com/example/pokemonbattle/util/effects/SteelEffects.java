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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class SteelEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Steel colour palette
    private static final Color STEEL_SILVER = Color.web("#9E9E9E");
    private static final Color STEEL_LIGHT  = Color.web("#E0E0E0");
    private static final Color STEEL_DARK   = Color.web("#424242");
    private static final Color STEEL_BLUE   = Color.web("#607D8B");
    private static final Color STEEL_SHINE  = Color.web("#F5F5F5");
    private static final Color STEEL_CHROME = Color.web("#B0BEC5");
    private static final Color STEEL_DEEP   = Color.web("#263238");
    private static final Color STEEL_SPARK  = Color.web("#FFEB3B");

    public SteelEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // PUBLIC API – single-point overload (melee / contact moves)

    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    // PUBLIC API – full signature (all steel moves)

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.8, 2.4);

        switch (moveName) {
            case "iron-head"    -> addMetalImpact(startX, startY, endX, endY, intensity, timeline);
            case "iron-tail"    -> addIronTail(startX, startY, endX, endY, intensity, timeline);
            case "bullet-punch" -> addMetalImpact(startX, startY, endX, endY, intensity * 0.9, timeline);
            case "meteor-mash"  -> addMetalImpact(startX, startY, endX, endY, intensity * 1.2, timeline);
            case "smart-strike" -> addMetalImpact(startX, startY, endX, endY, intensity, timeline);
            case "steel-wing"   -> addSteelSlash(endX, endY, intensity, timeline);
            case "gear-grind"   -> addGearGrind(startX, startY, endX, endY, intensity, timeline);
            case "heavy-slam"   -> addHeavySlam(endX, endY, intensity, timeline);
            case "flash-cannon" -> addFlashCannon(startX, startY, endX, endY, intensity, timeline);
            case "magnet-bomb"  -> addMagnetBomb(startX, startY, endX, endY, intensity, timeline);
            case "anchor-shot"  -> addMagnetBomb(startX, startY, endX, endY, intensity * 0.85, timeline);
            case "gyro-ball"    -> addGyroBall(startX, startY, endX, endY, intensity, timeline);
            default             -> addDefaultSteelClash(endX, endY, intensity, timeline);
        }
    }

    // PUBLIC API – ranged lead effect

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        switch (moveName) {
            case "flash-cannon" -> addFlashCannon(startX, startY, endX, endY, intensity, timeline);
            case "gyro-ball"    -> addGyroBall(startX, startY, endX, endY, intensity, timeline);
            default             -> addMagnetBomb(startX, startY, endX, endY, intensity, timeline);
        }
    }

    // IRON TAIL — sweeping tail arc from attacker's right, bends left to hit

    /**
     * The tail originates from the right side of the attacker (from the player's
     * perspective that means the right edge of the screen, i.e. the side closest
     * to the opponent).  It sweeps in a wide arc that curves LEFT from the
     * defender's perspective — like a heavy metallic tail whipping around.
     *
     * Visually:
     *   1. A thick chromium arc grows from the attacker's lower-right,
     *      curving upward then bending toward the defender.
     *   2. A secondary thinner arc trails behind (motion-blur feel).
     *   3. The tip travels across to strike the defender with metal sparks.
     *   4. A heavy shockwave ring at the defender on impact.
     */
    private void addIronTail(double sx, double sy, double ex, double ey,
                             double intensity, Timeline timeline) {

        // 1. Tail arc segments
        // We draw the arc as a fan of Line segments along a bezier-like curve
        // so we can animate each segment independently for a "sweeping" feel.
        //
        // The arc goes:
        //   origin  → near attacker's right (sx + rightOffset, sy + 20)
        //   peak    → above the midpoint, offset to the attacker's right side
        //   land    → defender position (ex, ey), approached from the left

        double rightOffset = 70 + 20 * intensity;  // how far right the tail originates
        double arcHeight   = 90 + 30 * intensity;  // how high the arc peaks

        // Control points for a quadratic bezier sweep
        double p0x = sx + rightOffset;  // tail root — far right of attacker
        double p0y = sy + 20;
        double p1x = sx + rightOffset * 0.3 - 40;  // peak — bends left toward defender
        double p1y = sy - arcHeight;
        double p2x = ex - 20;           // strike — slightly left of defender centre
        double p2y = ey;

        int segCount = 20;
        // Draw thick arc segments, each appearing with a small delay for sweep feel
        for (int i = 0; i < segCount; i++) {
            double t0 = i / (double) segCount;
            double t1 = (i + 1) / (double) segCount;

            // Quadratic bezier
            double ax0 = bezier(p0x, p1x, p2x, t0);
            double ay0 = bezier(p0y, p1y, p2y, t0);
            double ax1 = bezier(p0x, p1x, p2x, t1);
            double ay1 = bezier(p0y, p1y, p2y, t1);

            // Thickness tapers: thick at root, sharper near the tip
            double taper = 1.0 - t0 * 0.55;
            double strokeW = (10 + 6 * intensity) * taper;

            Line seg = new Line(ax0, ay0, ax1, ay1);
            seg.setStroke(i % 2 == 0 ? STEEL_CHROME : STEEL_LIGHT);
            seg.setStrokeWidth(strokeW);
            seg.setEffect(new DropShadow(12 + 4 * intensity, STEEL_BLUE));
            seg.setOpacity(0);
            prepareTransientNode(seg);
            battleField.getChildren().add(seg);

            int delay = i * 14;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(seg.opacityProperty(), 0.95));
            KeyFrame hold   = new KeyFrame(Duration.millis(delay + 120),
                    new KeyValue(seg.opacityProperty(), 0.85));
            KeyFrame fade   = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(seg.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, hold, fade);
            registerCleanup(timeline, seg);
        }

        // 2. Motion-blur ghost arc (thinner, slightly offset, faster fade)
        int ghostCount = 12;
        for (int i = 0; i < ghostCount; i++) {
            double t0 = i / (double) ghostCount;
            double t1 = (i + 1) / (double) ghostCount;

            double ax0 = bezier(p0x + 10, p1x + 8, p2x + 8, t0);
            double ay0 = bezier(p0y - 5,  p1y - 8, p2y - 5,  t0);
            double ax1 = bezier(p0x + 10, p1x + 8, p2x + 8, t1);
            double ay1 = bezier(p0y - 5,  p1y - 8, p2y - 5,  t1);

            Line ghost = new Line(ax0, ay0, ax1, ay1);
            ghost.setStroke(STEEL_SILVER.deriveColor(0, 1, 1, 0.45));
            ghost.setStrokeWidth((6 + 3 * intensity) * (1.0 - t0 * 0.5));
            ghost.setEffect(new GaussianBlur(3));
            ghost.setOpacity(0);
            prepareTransientNode(ghost);
            battleField.getChildren().add(ghost);

            int delay = i * 14 + 20;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(ghost.opacityProperty(), 0.6)),
                    new KeyFrame(Duration.millis(delay + 180), new KeyValue(ghost.opacityProperty(), 0)));
            registerCleanup(timeline, ghost);
        }

        // 3. Metallic shine streak along the arc peak─
        Circle shine = new Circle(6 + 3 * intensity, STEEL_SHINE);
        shine.setCenterX(p0x);
        shine.setCenterY(p0y);
        shine.setEffect(new GaussianBlur(4));
        shine.setOpacity(0);
        prepareTransientNode(shine);
        battleField.getChildren().add(shine);

        int travelMs = segCount * 14;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),        new KeyValue(shine.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(travelMs / 2),
                        new KeyValue(shine.centerXProperty(), p1x),
                        new KeyValue(shine.centerYProperty(), p1y)),
                new KeyFrame(Duration.millis(travelMs),
                        new KeyValue(shine.centerXProperty(), p2x),
                        new KeyValue(shine.centerYProperty(), p2y),
                        new KeyValue(shine.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(travelMs + 80),
                        new KeyValue(shine.opacityProperty(), 0)));
        registerCleanup(timeline, shine);

        // 4. Impact: shockwave ring + sparks at defender
        int impactDelay = travelMs - 20;

        // Heavy shockwave ring
        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(STEEL_SILVER.deriveColor(0, 1, 1, 0.8));
        ring.setStrokeWidth(6 + 2 * intensity);
        ring.setCenterX(ex);
        ring.setCenterY(ey);
        ring.setEffect(new GaussianBlur(3));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        double ringMax = 34 + 22 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(impactDelay),
                        new KeyValue(ring.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(impactDelay + 180),
                        new KeyValue(ring.radiusProperty(), ringMax),
                        new KeyValue(ring.opacityProperty(), 0.4)),
                new KeyFrame(Duration.millis(impactDelay + 300),
                        new KeyValue(ring.radiusProperty(), ringMax * 1.35),
                        new KeyValue(ring.opacityProperty(), 0)));
        registerCleanup(timeline, ring);

        // Sparks
        addMetalSparks(ex, ey, intensity, impactDelay, timeline);
        addSteelFlash(ex, ey, 22 + 12 * intensity, STEEL_SHINE, impactDelay, 180, timeline);
    }

    // Bezier helper (quadratic)
    private double bezier(double p0, double p1, double p2, double t) {
        return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2;
    }

    // METAL IMPACT — sparks and ring on heavy metallic strike

    private void addMetalImpact(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        // Chrome shards along approach
        int shardCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < shardCount; i++) {
            double t  = (i + 0.5) / shardCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 14;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 14;

            Rectangle shard = new Rectangle(
                    9 + random.nextDouble() * 6 * intensity,
                    8 + random.nextDouble() * 3 * intensity);
            shard.setFill(i % 2 == 0 ? STEEL_CHROME : STEEL_LIGHT);
            shard.setStroke(STEEL_DARK.deriveColor(0, 1, 1, 0.4));
            shard.setStrokeWidth(1);
            shard.setEffect(new DropShadow(4, STEEL_DARK));
            shard.setX(tx - shard.getWidth() / 2);
            shard.setY(ty - shard.getHeight() / 2);
            shard.setRotate(random.nextDouble() * 360);
            shard.setOpacity(0);
            prepareTransientNode(shard);
            battleField.getChildren().add(shard);

            int delay = (int) (t * 130);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(shard.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 160),
                            new KeyValue(shard.xProperty(), shard.getX() + (random.nextDouble() - 0.5) * 20),
                            new KeyValue(shard.yProperty(), shard.getY() - 8 - random.nextDouble() * 14),
                            new KeyValue(shard.rotateProperty(), shard.getRotate() + 180),
                            new KeyValue(shard.opacityProperty(), 0)));
            registerCleanup(timeline, shard);
        }

        addMetalSparks(ex, ey, intensity, 90, timeline);

        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(STEEL_SILVER.deriveColor(0, 1, 1, 0.7));
        ring.setStrokeWidth(5 + intensity);
        ring.setCenterX(ex);
        ring.setCenterY(ey);
        ring.setEffect(new GaussianBlur(3));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        double ringR = 28 + 18 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(90),  new KeyValue(ring.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(ring.radiusProperty(), ringR),
                        new KeyValue(ring.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(ring.radiusProperty(), ringR * 1.3),
                        new KeyValue(ring.opacityProperty(), 0)));
        registerCleanup(timeline, ring);

        addSteelFlash(ex, ey, 20 + 10 * intensity, STEEL_SHINE, 90, 180, timeline);
    }

    // STEEL SLASH — wing/blade sharp cuts

    private void addSteelSlash(double x, double y, double intensity, Timeline timeline) {
        int slashCount = (int) (10 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle   = -50 + i * (100.0 / Math.max(slashCount - 1, 1));
            double rad     = Math.toRadians(angle);
            double slashLen = 26 + 16 * intensity;

            Line slash = new Line(
                    x - Math.cos(rad) * slashLen * 0.5,
                    y - Math.sin(rad) * slashLen * 0.5,
                    x + Math.cos(rad) * slashLen * 0.5,
                    y + Math.sin(rad) * slashLen * 0.5);
            slash.setStroke(i % 2 == 0 ? STEEL_CHROME : STEEL_LIGHT);
            slash.setStrokeWidth(4.5 + 1.5 * intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(10 + 4 * intensity, STEEL_BLUE));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = 60 + i * 32;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(slash.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(delay + 80),
                            new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.7),
                            new KeyValue(slash.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(slash.opacityProperty(), 0)));
            registerCleanup(timeline, slash);
        }

        addMetalSparks(x, y, intensity, 60, timeline);
        addSteelFlash(x, y, 20 + 10 * intensity, STEEL_SHINE, 60, 180, timeline);
    }

    // GEAR GRIND — spinning metallic discs hurled at target

    private void addGearGrind(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int gearCount = (int) (5 + intensity);
        for (int i = 0; i < gearCount; i++) {
            Circle gear = new Circle(13 + 5 * intensity, STEEL_SILVER);
            gear.setStroke(STEEL_DARK);
            gear.setStrokeWidth(3.5);
            gear.setEffect(new DropShadow(8, STEEL_DEEP));
            gear.setCenterX(sx + (random.nextDouble() - 0.5) * 16);
            gear.setCenterY(sy + (random.nextDouble() - 0.5) * 16);
            gear.setOpacity(0);
            prepareTransientNode(gear);
            battleField.getChildren().add(gear);

            int delay = i * 80;
            double tx = ex + (random.nextDouble() - 0.5) * 18;
            double ty = ey + (random.nextDouble() - 0.5) * 18;

            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(gear.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(gear.centerXProperty(), tx),
                            new KeyValue(gear.centerYProperty(), ty)),
                    new KeyFrame(Duration.millis(delay + 320),
                            new KeyValue(gear.opacityProperty(), 0),
                            new KeyValue(gear.radiusProperty(), gear.getRadius() * 1.4)));
            registerCleanup(timeline, gear);
        }

        addMetalSparks(ex, ey, intensity, gearCount * 80, timeline);
        addSteelFlash(ex, ey, 20 + 10 * intensity, STEEL_SHINE, gearCount * 80, 180, timeline);
    }

    // HEAVY SLAM — seismic impact with shockwave

    private void addHeavySlam(double x, double y, double intensity, Timeline timeline) {
        Ellipse shockwave = new Ellipse(0, 0);
        shockwave.setFill(Color.TRANSPARENT);
        shockwave.setStroke(STEEL_SILVER.deriveColor(0, 1, 1, 0.7));
        shockwave.setStrokeWidth(7 + 2 * intensity);
        shockwave.setEffect(new GaussianBlur(5));
        shockwave.setCenterX(x);
        shockwave.setCenterY(y + 10);
        shockwave.setOpacity(0);
        prepareTransientNode(shockwave);
        battleField.getChildren().add(shockwave);

        double maxRX = 55 + 30 * intensity;
        double maxRY = 18 + 10 * intensity;

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(shockwave.opacityProperty(), 0.85)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(shockwave.radiusXProperty(), maxRX),
                        new KeyValue(shockwave.radiusYProperty(), maxRY),
                        new KeyValue(shockwave.opacityProperty(), 0.35)),
                new KeyFrame(Duration.millis(360),
                        new KeyValue(shockwave.radiusXProperty(), maxRX * 1.4),
                        new KeyValue(shockwave.radiusYProperty(), maxRY * 1.4),
                        new KeyValue(shockwave.opacityProperty(), 0)));
        registerCleanup(timeline, shockwave);

        int debrisCount = (int) (16 + 4 * intensity);
        for (int i = 0; i < debrisCount; i++) {
            Rectangle debris = new Rectangle(
                    8 + random.nextDouble() * 5 * intensity,
                    8 + random.nextDouble() * 4 * intensity);
            debris.setFill(i % 2 == 0 ? STEEL_SILVER : STEEL_CHROME);
            debris.setX(x - debris.getWidth() / 2);
            debris.setY(y - debris.getHeight() / 2);
            debris.setRotate(random.nextDouble() * 360);
            debris.setOpacity(0);
            prepareTransientNode(debris);
            battleField.getChildren().add(debris);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist  = 14 + random.nextDouble() * 22 * intensity;
            int delay    = i * 22;

            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(debris.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(debris.xProperty(), x + Math.cos(angle) * dist - debris.getWidth() / 2),
                            new KeyValue(debris.yProperty(), y + Math.sin(angle) * dist - debris.getHeight() / 2),
                            new KeyValue(debris.rotateProperty(), debris.getRotate() + 200)),
                    new KeyFrame(Duration.millis(delay + 320),
                            new KeyValue(debris.opacityProperty(), 0)));
            registerCleanup(timeline, debris);
        }

        addSteelFlash(x, y, 28 + 14 * intensity, STEEL_SHINE, 0, 200, timeline);
    }

    // FLASH CANNON — thick Rectangle-based silver energy beam
    //
    // Uses a Rectangle (not a Line) so the beam can be as wide as needed.
    // Beam width = beamH below. Increase that value for a thicker beam.

    private void addFlashCannon(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {

        double angle  = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist   = Math.hypot(ex - sx, ey - sy);

        // Beam thickness: change these values to make it thicker/thinner─
        double beamH  = 22 + 10 * intensity;   // ← increase this for a thicker beam

        // Outer glow beam (wider, semi-transparent)
        double glowH  = beamH * 2.2;
        Rectangle glow = new Rectangle(0, glowH);
        glow.setFill(STEEL_CHROME.deriveColor(0, 1, 1, 0.35));
        glow.setArcWidth(glowH); glow.setArcHeight(glowH);
        glow.setX(sx); glow.setY(sy - glowH / 2);
        glow.setRotate(angle);
        glow.setEffect(new GaussianBlur(glowH * 0.35));
        glow.setOpacity(0);
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);

        // Core beam
        Rectangle beam = new Rectangle(0, beamH);
        beam.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, STEEL_SHINE.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.5, STEEL_LIGHT.deriveColor(0, 1, 1, 0.90)),
                new Stop(1.0, STEEL_CHROME.deriveColor(0, 1, 1, 0.85))));
        beam.setArcWidth(beamH); beam.setArcHeight(beamH);
        beam.setX(sx); beam.setY(sy - beamH / 2);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(beamH * 0.8, STEEL_CHROME));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        // Bright white core stripe (thinner)
        double coreH = beamH * 0.35;
        Rectangle core = new Rectangle(0, coreH);
        core.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.9));
        core.setArcWidth(coreH); core.setArcHeight(coreH);
        core.setX(sx); core.setY(sy - coreH / 2);
        core.setRotate(angle);
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        // All three extend together
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),
                        new KeyValue(glow.opacityProperty(), 0.7),
                        new KeyValue(beam.opacityProperty(), 0.92),
                        new KeyValue(core.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(200),
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

        // Glint particles along beam path
        int count = (int) (16 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            double t  = (i + 0.5) / count;
            double wx = sx + (ex - sx) * t + (random.nextDouble() - 0.5) * 10;
            double wy = sy + (ey - sy) * t + (random.nextDouble() - 0.5) * 10;

            Circle glint = new Circle(5 + random.nextDouble() * 4, STEEL_SHINE);
            glint.setEffect(new GaussianBlur(2));
            glint.setCenterX(wx);
            glint.setCenterY(wy);
            glint.setOpacity(0);
            prepareTransientNode(glint);
            battleField.getChildren().add(glint);

            int delay = (int) (t * 180) + 20;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(glint.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 180), new KeyValue(glint.opacityProperty(), 0)));
            registerCleanup(timeline, glint);
        }

        addSteelFlash(ex, ey, 25 + 12 * intensity, STEEL_SHINE, 200, 220, timeline);
    }

    // MAGNET BOMB — magnetic projectile with metallic clang

    private void addMagnetBomb(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        Circle bomb = new Circle(12 + 4 * intensity, STEEL_SILVER);
        bomb.setStroke(STEEL_DARK);
        bomb.setStrokeWidth(4);
        bomb.setEffect(new DropShadow(10, STEEL_DEEP));
        bomb.setCenterX(sx);
        bomb.setCenterY(sy);
        bomb.setOpacity(0);
        prepareTransientNode(bomb);
        battleField.getChildren().add(bomb);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),  new KeyValue(bomb.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(bomb.centerXProperty(), ex),
                        new KeyValue(bomb.centerYProperty(), ey)),
                new KeyFrame(Duration.millis(300), new KeyValue(bomb.opacityProperty(), 0)));
        registerCleanup(timeline, bomb);

        int fieldCount = (int) (9 + intensity);
        for (int i = 0; i < fieldCount; i++) {
            Circle field = new Circle(bomb.getRadius() * (1.8 + i * 0.6), Color.TRANSPARENT);
            field.setStroke(STEEL_BLUE.deriveColor(0, 1, 1, 0.45 - i * 0.1));
            field.setStrokeWidth(3);
            field.setEffect(new GaussianBlur(3));
            field.setCenterX(sx);
            field.setCenterY(sy);
            field.setOpacity(0);
            prepareTransientNode(field);
            battleField.getChildren().add(field);

            int delay = i * 30;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(field.opacityProperty(), 0.6)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(field.centerXProperty(), ex),
                            new KeyValue(field.centerYProperty(), ey)),
                    new KeyFrame(Duration.millis(delay + 320),
                            new KeyValue(field.opacityProperty(), 0)));
            registerCleanup(timeline, field);
        }

        addMetalSparks(ex, ey, intensity, 220, timeline);
        addSteelFlash(ex, ey, 23 + 10 * intensity, STEEL_SHINE, 220, 200, timeline);
    }

    // GYRO BALL — spinning metallic sphere growing on approach

    private void addGyroBall(double sx, double sy, double ex, double ey,
                             double intensity, Timeline timeline) {
        Circle ball = new Circle(10 + 3 * intensity, STEEL_CHROME);
        ball.setStroke(STEEL_SILVER);
        ball.setStrokeWidth(3.5);
        ball.setEffect(new DropShadow(10, STEEL_DEEP));
        ball.setCenterX(sx);
        ball.setCenterY(sy);
        ball.setOpacity(0);
        prepareTransientNode(ball);
        battleField.getChildren().add(ball);

        double maxRadius = 14 + 8 * intensity;
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),  new KeyValue(ball.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(130), new KeyValue(ball.radiusProperty(), maxRadius)),
                new KeyFrame(Duration.millis(240),
                        new KeyValue(ball.centerXProperty(), ex),
                        new KeyValue(ball.centerYProperty(), ey)),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(ball.opacityProperty(), 0),
                        new KeyValue(ball.radiusProperty(), maxRadius * 1.5)));
        registerCleanup(timeline, ball);

        addMetalSparks(ex, ey, intensity, 240, timeline);
        addSteelFlash(ex, ey, 25 + 12 * intensity, STEEL_SHINE, 240, 200, timeline);
    }

    // DEFAULT STEEL CLASH

    private void addDefaultSteelClash(double x, double y, double intensity, Timeline timeline) {
        addMetalImpact(x, y, x, y, intensity, timeline);
    }

    // SHARED HELPERS

    private void addMetalSparks(double x, double y, double intensity,
                                int startDelay, Timeline timeline) {
        int count = (int) (16 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double len   = 8 + random.nextDouble() * 12 * intensity;
            Line spark = new Line(x, y,
                    x + Math.cos(angle) * len,
                    y + Math.sin(angle) * len);
            spark.setStroke(i % 3 == 0 ? STEEL_SPARK
                    : i % 3 == 1 ? STEEL_SHINE : STEEL_LIGHT);
            spark.setStrokeWidth(1.5 + random.nextDouble() * intensity);
            spark.setOpacity(0);
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);

            int delay = startDelay + i * 16;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(spark.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 140),
                            new KeyValue(spark.endXProperty(), spark.getEndX() + (random.nextDouble() - 0.5) * 14),
                            new KeyValue(spark.endYProperty(), spark.getEndY() + random.nextDouble() * 16)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(spark.opacityProperty(), 0)));
            registerCleanup(timeline, spark);
        }
    }

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

    // UTILITIES

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