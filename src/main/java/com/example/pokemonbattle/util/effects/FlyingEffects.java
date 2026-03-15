// FlyingEffects.java
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

public class FlyingEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Flying colour palette
    private static final Color FLY_SKY      = Color.web("#64B5F6");
    private static final Color FLY_LIGHT    = Color.web("#B3E5FC");
    private static final Color FLY_WHITE    = Color.web("#FFFFFF");
    private static final Color FLY_CYAN     = Color.web("#80DEEA");
    private static final Color FLY_WIND     = Color.web("#E3F2FD");
    private static final Color FLY_FEATHER  = Color.web("#F5F5F5");
    private static final Color FLY_SLASH    = Color.web("#90CAF9");
    private static final Color FLY_DARK     = Color.web("#1565C0");

    public FlyingEffects(Pane battleField) {
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
    // Public API – full signature (all flying moves)
    // -----------------------------------------------------------------

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.4, 1.8);

        switch (moveName) {
            // Wing strikes – sweeping feather burst on contact
            case "wing-attack"  -> addWingStrike(startX, startY, endX, endY, intensity, timeline);
            case "brave-bird"   -> addWingStrike(startX, startY, endX, endY, intensity * 1.3, timeline);
            case "drill-peck"   -> addDrillPeck(startX, startY, endX, endY, intensity, timeline);
            case "peck"         -> addDrillPeck(startX, startY, endX, endY, intensity * 0.7, timeline);

            // Aerial slashes – sharp air-blade lines
            case "aerial-ace"   -> addAerialSlash(endX, endY, intensity, timeline);
            case "air-slash"    -> addAerialSlash(endX, endY, intensity, timeline);
            case "air-cutter"   -> addAerialSlash(endX, endY, intensity * 0.85, timeline);

            // Diving strike – swooping arc followed by wind burst
            case "fly"          -> addDivingStrike(startX, startY, endX, endY, intensity, timeline);
            case "bounce"       -> addDivingStrike(startX, startY, endX, endY, intensity, timeline);
            case "sky-attack"   -> addSkyAttack(startX, startY, endX, endY, intensity, timeline);

            // Wind gusts – spiralling air rings
            case "gust"         -> addWindGust(endX, endY, intensity, timeline);
            case "feather-dance" -> addFeatherDance(endX, endY, intensity, timeline);
            case "tailwind"     -> addWindGust(endX, endY, intensity * 0.7, timeline);

            // Ranged wind / beam effects
            case "hurricane"    -> addHurricane(startX, startY, endX, endY, intensity, timeline);
            case "oblivion-wing" -> addOblivionWing(startX, startY, endX, endY, intensity, timeline);

            default             -> addDefaultWindBurst(endX, endY, intensity, timeline);
        }
    }

    // -----------------------------------------------------------------
    // Public API – ranged lead effect (beam / wind to target)
    // -----------------------------------------------------------------

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.4, 1.8);
        switch (moveName) {
            case "oblivion-wing" -> addOblivionWing(startX, startY, endX, endY, intensity, timeline);
            default              -> addHurricane(startX, startY, endX, endY, intensity, timeline);
        }
    }

    // =================================================================
    // Wing strike – sweeping feathers burst on contact
    // =================================================================

    private void addWingStrike(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        // Feather trail along approach path
        int trailCount = (int) (5 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 16;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 16;

            Ellipse feather = new Ellipse(3 + random.nextDouble() * 3, 7 + random.nextDouble() * 5);
            feather.setFill((i % 2 == 0 ? FLY_FEATHER : FLY_LIGHT).deriveColor(0, 1, 1, 0.7));
            feather.setEffect(new GaussianBlur(3));
            feather.setCenterX(tx);
            feather.setCenterY(ty);
            feather.setRotate(random.nextDouble() * 60 - 30);
            feather.setOpacity(0);
            prepareTransientNode(feather);
            battleField.getChildren().add(feather);

            int delay = (int) (t * 130);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(feather.opacityProperty(), 0.8));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(feather.centerYProperty(), ty - 10 - random.nextDouble() * 12),
                    new KeyValue(feather.rotateProperty(), feather.getRotate() + 40),
                    new KeyValue(feather.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, feather);
        }

        // Sweeping arc slashes at impact
        int slashCount = (int) (2 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle = -40 + i * (80.0 / Math.max(slashCount - 1, 1));
            double rad = Math.toRadians(angle);
            double slashLen = 24 + 14 * intensity;
            Line slash = new Line(
                    ex - Math.cos(rad) * slashLen * 0.5,
                    ey - Math.sin(rad) * slashLen * 0.5,
                    ex + Math.cos(rad) * slashLen * 0.5,
                    ey + Math.sin(rad) * slashLen * 0.5);
            slash.setStroke(i % 2 == 0 ? FLY_SKY : FLY_WHITE);
            slash.setStrokeWidth(3 + intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(8 + 4 * intensity, FLY_SKY));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = 90 + i * 28;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(slash.opacityProperty(), 0.95));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 70),
                    new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.5),
                    new KeyValue(slash.opacityProperty(), 0.9));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(slash.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, slash);
        }

        addFlashCircle(ex, ey, 18 + 12 * intensity, FLY_CYAN, 80, 200, timeline);
    }

    // =================================================================
    // Drill peck – spiralling narrow cone of wind at impact
    // =================================================================

    private void addDrillPeck(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        // Spiralling wind lines converging to impact
        int lineCount = (int) (6 + 4 * intensity);
        for (int i = 0; i < lineCount; i++) {
            double angle = (i / (double) lineCount) * 2 * Math.PI;
            double startRadius = 20 + 10 * intensity;
            double lx = ex + Math.cos(angle) * startRadius;
            double ly = ey + Math.sin(angle) * startRadius;

            Line line = new Line(lx, ly, ex, ey);
            line.setStroke(i % 2 == 0 ? FLY_SKY : FLY_CYAN);
            line.setStrokeWidth(2 + intensity);
            line.setOpacity(0);
            line.setEffect(new GaussianBlur(2));
            prepareTransientNode(line);
            battleField.getChildren().add(line);

            int delay = i * 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(line.opacityProperty(), 0.85));
            KeyFrame spin = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(line.startXProperty(), ex + Math.cos(angle + 0.8) * startRadius * 0.3),
                    new KeyValue(line.startYProperty(), ey + Math.sin(angle + 0.8) * startRadius * 0.3));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(line.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, spin, fade);
            registerCleanup(timeline, line);
        }

        // Central impact circle
        addFlashCircle(ex, ey, 12 + 8 * intensity, FLY_WHITE, 100, 160, timeline);
    }

    // =================================================================
    // Aerial slash – sharp air-blade marks
    // =================================================================

    private void addAerialSlash(double x, double y, double intensity, Timeline timeline) {
        // Cross-slash pattern
        int slashCount = (int) (3 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle = -60 + i * (120.0 / Math.max(slashCount - 1, 1));
            double rad = Math.toRadians(angle);
            double slashLen = 28 + 16 * intensity;

            Line slash = new Line(
                    x - Math.cos(rad) * slashLen * 0.6,
                    y - Math.sin(rad) * slashLen * 0.6,
                    x + Math.cos(rad) * slashLen * 0.6,
                    y + Math.sin(rad) * slashLen * 0.6);
            slash.setStroke(FLY_SLASH);
            slash.setStrokeWidth(3 + 1.5 * intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(10 + 4 * intensity, FLY_CYAN));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = 60 + i * 35;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(slash.opacityProperty(), 1.0));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 90),
                    new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.8),
                    new KeyValue(slash.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 230),
                    new KeyValue(slash.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, slash);
        }

        // Air pressure ring expanding outward
        Circle ring = new Circle(10, Color.TRANSPARENT);
        ring.setStroke(FLY_SKY.deriveColor(0, 1, 1, 0.6));
        ring.setStrokeWidth(3);
        ring.setCenterX(x);
        ring.setCenterY(y);
        ring.setEffect(new GaussianBlur(4));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        double ringRadius = 38 + 22 * intensity;
        KeyFrame rAppear = new KeyFrame(Duration.millis(50),
                new KeyValue(ring.opacityProperty(), 0.75));
        KeyFrame rExpand = new KeyFrame(Duration.millis(220),
                new KeyValue(ring.radiusProperty(), ringRadius),
                new KeyValue(ring.opacityProperty(), 0.3));
        KeyFrame rFade = new KeyFrame(Duration.millis(340),
                new KeyValue(ring.radiusProperty(), ringRadius * 1.35),
                new KeyValue(ring.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(rAppear, rExpand, rFade);
        registerCleanup(timeline, ring);

        addFlashCircle(x, y, 14 + 10 * intensity, FLY_WHITE, 60, 180, timeline);
    }

    // =================================================================
    // Diving strike – swooping entry with impact shockwave
    // =================================================================

    private void addDivingStrike(double sx, double sy, double ex, double ey,
                                 double intensity, Timeline timeline) {
        // Speed-streak lines showing the swooping arc
        int streakCount = (int) (6 + 4 * intensity);
        for (int i = 0; i < streakCount; i++) {
            double t = (i + 0.5) / streakCount;
            double mx = sx + (ex - sx) * t;
            double my = sy + (ey - sy) * t - 20 * Math.sin(t * Math.PI);

            Line streak = new Line(mx - 8, my - 6, mx + 8, my + 6);
            streak.setStroke(FLY_LIGHT.deriveColor(0, 1, 1, 0.7));
            streak.setStrokeWidth(2.5);
            streak.setEffect(new GaussianBlur(3));
            streak.setOpacity(0);
            prepareTransientNode(streak);
            battleField.getChildren().add(streak);

            int delay = (int) (t * 150);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(streak.opacityProperty(), 0.8));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(streak.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, streak);
        }

        // Impact shockwave ring at landing
        Circle shock = new Circle(0, Color.TRANSPARENT);
        shock.setStroke(FLY_SKY.deriveColor(0, 1, 1, 0.8));
        shock.setStrokeWidth(4 + intensity);
        shock.setCenterX(ex);
        shock.setCenterY(ey);
        shock.setEffect(new DropShadow(10, FLY_CYAN));
        shock.setOpacity(0);
        prepareTransientNode(shock);
        battleField.getChildren().add(shock);

        double shockRadius = 40 + 24 * intensity;
        KeyFrame sAppear = new KeyFrame(Duration.millis(130),
                new KeyValue(shock.opacityProperty(), 0.9),
                new KeyValue(shock.radiusProperty(), 8.0));
        KeyFrame sExpand = new KeyFrame(Duration.millis(280),
                new KeyValue(shock.radiusProperty(), shockRadius),
                new KeyValue(shock.opacityProperty(), 0.4));
        KeyFrame sFade = new KeyFrame(Duration.millis(380),
                new KeyValue(shock.radiusProperty(), shockRadius * 1.3),
                new KeyValue(shock.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(sAppear, sExpand, sFade);
        registerCleanup(timeline, shock);

        // Wind particles scattering on impact
        addWindParticles(ex, ey, intensity, 120, timeline);
        addFlashCircle(ex, ey, 20 + 12 * intensity, FLY_WHITE, 130, 200, timeline);
    }

    // =================================================================
    // Sky Attack – charged glowing strike
    // =================================================================

    private void addSkyAttack(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        // Charging glow around attacker
        Circle charge = new Circle(0, FLY_CYAN.deriveColor(0, 1, 1, 0.5));
        charge.setEffect(new GaussianBlur(10 + 4 * intensity));
        charge.setCenterX(sx);
        charge.setCenterY(sy);
        charge.setOpacity(0);
        prepareTransientNode(charge);
        battleField.getChildren().add(charge);

        double chargeR = 28 + 16 * intensity;
        KeyFrame cAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(charge.opacityProperty(), 0.8),
                new KeyValue(charge.radiusProperty(), chargeR));
        KeyFrame cHold = new KeyFrame(Duration.millis(100),
                new KeyValue(charge.opacityProperty(), 0.6));
        KeyFrame cFade = new KeyFrame(Duration.millis(180),
                new KeyValue(charge.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(cAppear, cHold, cFade);
        registerCleanup(timeline, charge);

        // Bright impact explosion
        Circle explosion = new Circle(0, FLY_WHITE.deriveColor(0, 1, 1, 0.85));
        explosion.setEffect(new DropShadow(20 + 8 * intensity, FLY_SKY));
        explosion.setCenterX(ex);
        explosion.setCenterY(ey);
        explosion.setOpacity(0);
        prepareTransientNode(explosion);
        battleField.getChildren().add(explosion);

        double exR = 32 + 20 * intensity;
        KeyFrame eAppear = new KeyFrame(Duration.millis(150),
                new KeyValue(explosion.opacityProperty(), 1.0),
                new KeyValue(explosion.radiusProperty(), exR * 0.3));
        KeyFrame ePeak = new KeyFrame(Duration.millis(240),
                new KeyValue(explosion.radiusProperty(), exR),
                new KeyValue(explosion.opacityProperty(), 0.5));
        KeyFrame eFade = new KeyFrame(Duration.millis(360),
                new KeyValue(explosion.radiusProperty(), exR * 1.5),
                new KeyValue(explosion.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(eAppear, ePeak, eFade);
        registerCleanup(timeline, explosion);

        addWingStrike(sx, sy, ex, ey, intensity, timeline);
    }

    // =================================================================
    // Wind gust – concentric air pressure rings
    // =================================================================

    private void addWindGust(double x, double y, double intensity, Timeline timeline) {
        int ringCount = (int) (3 + 2 * intensity);
        for (int i = 0; i < ringCount; i++) {
            Circle ring = new Circle(8 + i * 4, Color.TRANSPARENT);
            ring.setStroke(FLY_WIND.deriveColor(0, 1, 1, 0.55 - i * 0.08));
            ring.setStrokeWidth(3 - i * 0.4);
            ring.setCenterX(x + (random.nextDouble() - 0.5) * 10);
            ring.setCenterY(y + (random.nextDouble() - 0.5) * 10);
            ring.setEffect(new GaussianBlur(4 + i));
            ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            int delay = i * 50;
            double targetR = 40 + 22 * intensity + i * 10;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ring.opacityProperty(), 0.7));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(ring.radiusProperty(), targetR),
                    new KeyValue(ring.opacityProperty(), 0.25));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 360),
                    new KeyValue(ring.radiusProperty(), targetR * 1.25),
                    new KeyValue(ring.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand, fade);
            registerCleanup(timeline, ring);
        }

        addWindParticles(x, y, intensity, 0, timeline);
    }

    // =================================================================
    // Feather dance – fluttering feathers swirling at target
    // =================================================================

    private void addFeatherDance(double x, double y, double intensity, Timeline timeline) {
        int count = (int) (10 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 14 + random.nextDouble() * 18;
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;

            Ellipse feather = new Ellipse(3 + random.nextDouble() * 2.5, 8 + random.nextDouble() * 5);
            feather.setFill((i % 3 == 0 ? FLY_FEATHER : i % 3 == 1 ? FLY_LIGHT : FLY_WHITE)
                    .deriveColor(0, 1, 1, 0.75));
            feather.setEffect(new GaussianBlur(2));
            feather.setCenterX(px);
            feather.setCenterY(py);
            feather.setRotate(Math.toDegrees(angle));
            feather.setOpacity(0);
            prepareTransientNode(feather);
            battleField.getChildren().add(feather);

            int delay = i * 25;
            double spiralAngle = angle + 1.2;
            double targetRadius = radius + 20 + random.nextDouble() * 16;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(feather.opacityProperty(), 0.85));
            KeyFrame spiral = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(feather.centerXProperty(), x + Math.cos(spiralAngle) * targetRadius),
                    new KeyValue(feather.centerYProperty(), y + Math.sin(spiralAngle) * targetRadius),
                    new KeyValue(feather.rotateProperty(), feather.getRotate() + 90),
                    new KeyValue(feather.opacityProperty(), 0.6));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 360),
                    new KeyValue(feather.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, spiral, fade);
            registerCleanup(timeline, feather);
        }
    }

    // =================================================================
    // Hurricane – spiralling wind beam travelling to target
    // =================================================================

    private void addHurricane(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        // Spiralling wind-column particles along path
        int count = (int) (10 + 8 * intensity);
        for (int i = 0; i < count; i++) {
            double t = (i + random.nextDouble()) / count;
            double angle = t * 4 * Math.PI;
            double spiralR = 12 + 8 * intensity * (1 - t);

            double px = sx + dx * t + Math.cos(angle) * spiralR;
            double py = sy + dy * t + Math.sin(angle) * spiralR;

            Circle particle = new Circle(3 + random.nextDouble() * 3,
                    i % 3 == 0 ? FLY_SKY : i % 3 == 1 ? FLY_WIND : FLY_CYAN);
            particle.setEffect(new GaussianBlur(3));
            particle.setCenterX(px);
            particle.setCenterY(py);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);

            int delay = (int) (t * 220);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(particle.opacityProperty(), 0.75));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fade);
            registerCleanup(timeline, particle);
        }

        // Impact burst at target
        addWindGust(ex, ey, intensity, timeline);
    }

    // =================================================================
    // Oblivion Wing – dark wing-shaped energy beam
    // =================================================================

    private void addOblivionWing(double sx, double sy, double ex, double ey,
                                 double intensity, Timeline timeline) {
        // Core dark-beam line
        Line beam = new Line(sx, sy, sx, sy);
        beam.setStroke(FLY_DARK.deriveColor(0, 1, 1, 0.85));
        beam.setStrokeWidth(5 + 2.5 * intensity);
        beam.setEffect(new DropShadow(14 + 6 * intensity, FLY_SKY));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        KeyFrame bAppear = new KeyFrame(Duration.millis(30),
                new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame bExtend = new KeyFrame(Duration.millis(220),
                new KeyValue(beam.endXProperty(), ex),
                new KeyValue(beam.endYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(340),
                new KeyValue(beam.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bExtend, bFade);
        registerCleanup(timeline, beam);

        // Wing-shaped side flanges (ellipses rotated perpendicular to beam)
        double angle = Math.atan2(ey - sy, ex - sx);
        double perpAngle = angle + Math.PI / 2;
        int wingCount = (int) (4 + 3 * intensity);
        for (int i = 0; i < wingCount; i++) {
            double t = (i + 0.5) / wingCount;
            double wx = sx + (ex - sx) * t;
            double wy = sy + (ey - sy) * t;

            Ellipse wing = new Ellipse(4 + 2 * intensity, 10 + 6 * intensity);
            wing.setFill(FLY_DARK.deriveColor(0, 1, 1, 0.45));
            wing.setEffect(new GaussianBlur(4));
            wing.setCenterX(wx + Math.cos(perpAngle) * (8 + random.nextDouble() * 8));
            wing.setCenterY(wy + Math.sin(perpAngle) * (8 + random.nextDouble() * 8));
            wing.setRotate(Math.toDegrees(angle));
            wing.setOpacity(0);
            prepareTransientNode(wing);
            battleField.getChildren().add(wing);

            int delay = (int) (t * 200);
            KeyFrame wAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(wing.opacityProperty(), 0.7));
            KeyFrame wFade = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(wing.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(wAppear, wFade);
            registerCleanup(timeline, wing);
        }

        addFlashCircle(ex, ey, 20 + 12 * intensity, FLY_SKY, 200, 200, timeline);
    }

    // =================================================================
    // Default wind burst – simple expanding wind rings
    // =================================================================

    private void addDefaultWindBurst(double x, double y, double intensity, Timeline timeline) {
        addWindGust(x, y, intensity, timeline);
    }

    // =================================================================
    // Shared wind particle scatter
    // =================================================================

    private void addWindParticles(double x, double y, double intensity,
                                  int startDelay, Timeline timeline) {
        int count = (int) (6 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Circle p = new Circle(3 + random.nextDouble() * 3,
                    i % 2 == 0 ? FLY_LIGHT : FLY_CYAN);
            p.setEffect(new GaussianBlur(3));
            double angle = random.nextDouble() * 2 * Math.PI;
            p.setCenterX(x);
            p.setCenterY(y);
            p.setOpacity(0);
            prepareTransientNode(p);
            battleField.getChildren().add(p);

            double dist = 18 + random.nextDouble() * 24 * intensity;
            int delay = startDelay + i * 22;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(p.opacityProperty(), 0.75));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(p.centerXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(p.centerYProperty(), y + Math.sin(angle) * dist),
                    new KeyValue(p.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst);
            registerCleanup(timeline, p);
        }
    }

    // =================================================================
    // Flash circle helper
    // =================================================================

    private void addFlashCircle(double x, double y, double radius, Color color,
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
