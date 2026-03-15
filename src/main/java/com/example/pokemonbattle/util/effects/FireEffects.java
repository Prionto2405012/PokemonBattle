// FireEffects.java
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
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class FireEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Fire colour palette
    private static final Color FIRE_WHITE   = Color.web("#FFFDE7");
    private static final Color FIRE_YELLOW  = Color.web("#FFD600");
    private static final Color FIRE_ORANGE  = Color.ORANGE;
    private static final Color FIRE_RED     = Color.ORANGERED;
    private static final Color FIRE_DEEP    = Color.web("#BF360C");
    private static final Color FIRE_DARK    = Color.web("#4E342E");
    private static final Color FIRE_MAGENTA = Color.web("#CE93D8");
    private static final Color FIRE_PURPLE  = Color.web("#7B1FA2");
    private static final Color LAVA_RED     = Color.web("#D32F2F");
    private static final Color LAVA_ORANGE  = Color.web("#FF6F00");
    private static final Color ASH_GRAY     = Color.web("#616161");

    public FireEffects(Pane battleField) {
        this.battleField = battleField;
    }
    // PUBLIC ENTRY POINT

    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    /**
     * Full signature used when start coords are available (beam/charge moves).
     */
    public void createImpactEffect(double startX, double startY, double endX, double endY,
            String moveName, int movePower, Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.4, 1.8);

        switch (moveName) {
            case "fire-punch"        -> { addFirePunchEmbers(endX, endY, intensity, timeline); }
            case "ember"             -> addEmberBurst(startX, startY, endX, endY, intensity, timeline);
            case "flamethrower"      -> addFlamethrowerStream(startX, startY, endX, endY, intensity, timeline);
            case "fire-spin"         -> addFireVortex(endX, endY, intensity, timeline);
            case "fire-blast"        -> addExplosionCore(endX, endY, intensity, true, timeline);
            case "heat-wave"         -> addHeatWaveDistortion(startX, startY, endX, endY, intensity, timeline);
            case "overheat"          -> addOverheatOverdrive(startX, startY, endX, endY, intensity, timeline);
            case "flare-blitz"       -> { addExplosionCore(endX, endY, intensity, false, timeline);
                                          addReboundSpark(startX, startY, timeline); }
            case "fire-fang"         -> { addFangVisual(endX, endY, timeline);
                                          addEmberBurst(endX, endY, endX, endY, 0.6, timeline); }
            case "flame-burst"       -> addBurstSplash(endX, endY, intensity, timeline);
            case "flame-charge"      -> addChargeFlareTrail(startX, startY, endX, endY, intensity, false, timeline);
            case "incinerate"        -> addBerryIncinerateAsh(endX, endY, intensity, timeline);
            case "inferno"           -> addInfernoPillar(endX, endY, intensity, timeline);
            case "fire-pledge"       -> addPledgeColumn(endX, endY, intensity, timeline);
            case "temper-flare"      -> addTemperFlareBacklash(endX, endY, intensity, timeline);
            case "blaze-kick"        -> addKickArcFlame(endX, endY, intensity, timeline);
            case "blast-burn"        -> addBlastBurnDetonation(startX, startY, endX, endY, intensity, timeline);
            case "mystical-fire"     -> addMysticFlameSpiral(startX, startY, endX, endY, intensity, timeline);
            case "flame-wheel"       -> addWheelSpinRing(startX, startY, endX, endY, intensity, timeline);
            case "burning-jealousy"  -> addJealousyDarkFlare(endX, endY, intensity, timeline);
            case "burn-up"           -> addBurnUpCollapse(startX, startY, endX, endY, intensity, timeline);
            case "raging-fury"       -> addRagingFuryMultiBursts(endX, endY, intensity, timeline);
            case "lava-plume"        -> addLavaPlumeGroundPlume(endX, endY, intensity, timeline);
            case "eruption"          -> addEruptionRadialColumns(endX, endY, intensity, timeline);
            case "sacred-fire"       -> addSacredFireWhiteCore(endX, endY, intensity, timeline);
            case "magma-storm"       -> addMagmaStormTrapRing(endX, endY, intensity, timeline);
            default                  -> addDefaultFlames(endX, endY, intensity, timeline);
        }
    }

    // MOVEMENT EFFECT — called from BattleAnimationManager during attacker rush

    /**
     * Charge trail shown while attacker rushes forward (flare-blitz, flame-wheel).
     * Called by BattleAnimationManager.createMovementEffect.
     */
    public void addChargeTrailForMove(String moveName, double ax, double ay,
            boolean attackingRight, Timeline timeline) {
        double intensity = moveName.equals("flare-blitz") ? 3.4 : 2.7;
        addChargeFlareTrail(ax, ay, ax + (attackingRight ? 120 : -120), ay, intensity,
                moveName.equals("flame-wheel"), timeline);
    }

    // 1) FIRE PUNCH — embers + small flame pop

    private void addFirePunchEmbers(double x, double y, double intensity, Timeline timeline) {
        int count = (int)(18 * intensity);
        for (int i = 0; i < count; i++) {
            Circle ember = new Circle(4 + random.nextDouble() * 4, i % 2 == 0 ? FIRE_ORANGE : FIRE_RED);
            ember.setEffect(new DropShadow(8, FIRE_YELLOW));
            double angle = Math.PI * 2 * i / count + (random.nextDouble() - 0.5) * 0.5;
            double dist  = 28 + random.nextDouble() * 24;
            ember.setCenterX(x);
            ember.setCenterY(y);
            ember.setOpacity(0);
            prepareTransientNode(ember);
            battleField.getChildren().add(ember);

            KeyFrame appear  = new KeyFrame(Duration.millis(35),  new KeyValue(ember.opacityProperty(), 1.0));
            KeyFrame scatter = new KeyFrame(Duration.millis(220),
                    new KeyValue(ember.centerXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(ember.centerYProperty(), y + Math.sin(angle) * dist - 10),
                    new KeyValue(ember.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, scatter);
            registerCleanup(timeline, ember);
        }
    }

    // 2) EMBER — narrow cone of small particles

    private void addEmberBurst(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        double dx   = ex - sx;
        double dy   = ey - sy;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux   = dx / dist;
        double uy   = dy / dist;

        int count = (int)(20 * intensity);
        for (int i = 0; i < count; i++) {
            double spread  = (random.nextDouble() - 0.5) * 0.65;
            double ax      = ux * Math.cos(spread) - uy * Math.sin(spread);
            double ay      = ux * Math.sin(spread) + uy * Math.cos(spread);
            double travelDist = dist * (0.6 + random.nextDouble() * 0.4);

            Circle ember = new Circle(7 + random.nextDouble() * 3,
                    i % 3 == 0 ? FIRE_YELLOW : i % 3 == 1 ? FIRE_ORANGE : FIRE_RED);
            ember.setCenterX(sx);
            ember.setCenterY(sy);
            ember.setOpacity(0);
            prepareTransientNode(ember);
            battleField.getChildren().add(ember);

            int delay = i * 15;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ember.opacityProperty(), 0.9));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(ember.centerXProperty(), sx + ax * travelDist),
                    new KeyValue(ember.centerYProperty(), sy + ay * travelDist),
                    new KeyValue(ember.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(appear, travel);
            registerCleanup(timeline, ember);
        }

        // small flash at impact
        addFlashCircle(ex, ey, 24 * intensity, FIRE_ORANGE, 185, 80, timeline);
    }

    // 3) FLAMETHROWER — continuous ribbon beam + side sparks

    private void addFlamethrowerStream(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        double angle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist  = Math.hypot(ex - sx, ey - sy);
        double w = 25 + 12 * intensity;

        // Core beam
        Rectangle beam = new Rectangle(0, w);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, FIRE_YELLOW.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.4, FIRE_ORANGE.deriveColor(0, 1, 1, 0.9)),
                new Stop(1.0, FIRE_RED.deriveColor(0, 1, 1, 0.7))));
        beam.setArcWidth(w); 
        beam.setArcHeight(w);
        beam.setX(sx); beam.setY(sy - w / 2);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(w * 0.9, FIRE_ORANGE));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(20),  new KeyValue(beam.opacityProperty(), 0.92)),
                new KeyFrame(Duration.millis(160), new KeyValue(beam.widthProperty(), dist)),
                new KeyFrame(Duration.millis(240), new KeyValue(beam.opacityProperty(), 0.92)),
                new KeyFrame(Duration.millis(380), new KeyValue(beam.opacityProperty(), 0)));
        registerCleanup(timeline, beam);

        // Side sparks
        int sparkCount = (int)(28 * intensity);
        double ux = (ex - sx) / dist, uy = (ey - sy) / dist;
        double px = -uy, py = ux;
        for (int i = 0; i < sparkCount; i++) {
            double t  = (i + random.nextDouble()) / sparkCount;
            double bx = sx + ux * dist * t + px * (random.nextDouble() - 0.5) * 28;
            double by = sy + uy * dist * t + py * (random.nextDouble() - 0.5) * 28;
            Circle spark = new Circle(4.5 + random.nextDouble() * 2, FIRE_YELLOW);
            spark.setCenterX(bx); spark.setCenterY(by); spark.setOpacity(0);
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);
            int d = (int)(t * 200);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(d + 20), new KeyValue(spark.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(d + 160),
                            new KeyValue(spark.centerXProperty(), bx + px * (random.nextDouble() - 0.5) * 20),
                            new KeyValue(spark.centerYProperty(), by - 20 - random.nextDouble() * 20),
                            new KeyValue(spark.opacityProperty(), 0)));
            registerCleanup(timeline, spark);
        }

        // Impact bloom
        addFlashCircle(ex, ey, 75 * intensity, FIRE_ORANGE, 240, 180, timeline);
    }

    // 4) FIRE SPIN — two counter-rotating flame rings around defender

    private void addFireVortex(double x, double y, double intensity, Timeline timeline) {
        for (int ring = 0; ring < 2; ring++) {
            double r = 45 + ring * 22;
            int segCount = 22;
            for (int i = 0; i < segCount; i++) {
                double startAngle = (i / (double) segCount) * 360;
                Arc arc = new Arc(x, y, r, r * 0.55, startAngle, 18);
                arc.setType(ArcType.OPEN);
                arc.setFill(Color.TRANSPARENT);
                arc.setStroke(ring == 0 ? FIRE_ORANGE : FIRE_RED);
                arc.setStrokeWidth(10 + ring * 2.0);
                arc.setEffect(new DropShadow(10, FIRE_YELLOW));
                arc.setOpacity(0);
                prepareTransientNode(arc);
                battleField.getChildren().add(arc);

                int dir = ring == 0 ? 1 : -1;
                int appearAt = i * 18;
                timeline.getKeyFrames().addAll(
                        new KeyFrame(Duration.millis(appearAt),
                                new KeyValue(arc.opacityProperty(), 0.85)),
                        new KeyFrame(Duration.millis(280),
                                new KeyValue(arc.startAngleProperty(), startAngle + dir * 110)),
                        new KeyFrame(Duration.millis(900),
                                new KeyValue(arc.startAngleProperty(), startAngle + dir * 360),
                                new KeyValue(arc.opacityProperty(), 0)));
                registerCleanup(timeline, arc);
            }
        }
        addFlashCircle(x, y, 30 * intensity, FIRE_RED, 0, 120, timeline);
    }

    // 5) FIRE BLAST — large 5-spoke blast + fragments

    private void addExplosionCore(double x, double y, double intensity,
            boolean fiveSpoke, Timeline timeline) {
        // Central flash
        addFlashCircle(x, y, 65 * intensity, FIRE_WHITE, 0, 90, timeline);
        addFlashCircle(x, y, 50 * intensity, FIRE_YELLOW, 20, 120, timeline);

        int spokeCount = fiveSpoke ? 5 : 8;
        for (int s = 0; s < spokeCount; s++) {
            double angle = Math.PI * 2 * s / spokeCount;
            double len   = (100 + 40 * intensity) * (0.8 + random.nextDouble() * 0.4);
            Rectangle spoke = new Rectangle(0, 16 + 4 * intensity);
            spoke.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, FIRE_WHITE.deriveColor(0, 1, 1, 0.9)),
                    new Stop(1, FIRE_RED.deriveColor(0, 1, 1, 0.0))));
            spoke.setX(x); spoke.setY(y - spoke.getHeight() / 2);
            spoke.setRotate(Math.toDegrees(angle));
            spoke.setEffect(new DropShadow(12, FIRE_ORANGE));
            spoke.setOpacity(0);
            prepareTransientNode(spoke);
            battleField.getChildren().add(spoke);

            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(30),  new KeyValue(spoke.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(120), new KeyValue(spoke.widthProperty(), len)),
                    new KeyFrame(Duration.millis(280), new KeyValue(spoke.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(450), new KeyValue(spoke.opacityProperty(), 0)));
            registerCleanup(timeline, spoke);
        }

        // Radial fragments
        int fragCount = (int)(20 * intensity);
        for (int i = 0; i < fragCount; i++) {
            double angle  = Math.PI * 2 * i / fragCount + (random.nextDouble() - 0.5) * 0.3;
            double radius = 55 + random.nextDouble() * 45 * intensity;
            Circle frag   = new Circle(8 + random.nextDouble() * 4,
                    i % 2 == 0 ? FIRE_ORANGE : FIRE_RED);
            frag.setCenterX(x); frag.setCenterY(y); frag.setOpacity(0);
            prepareTransientNode(frag);
            battleField.getChildren().add(frag);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(50),  new KeyValue(frag.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(280),
                            new KeyValue(frag.centerXProperty(), x + Math.cos(angle) * radius),
                            new KeyValue(frag.centerYProperty(), y + Math.sin(angle) * radius),
                            new KeyValue(frag.opacityProperty(), 0)));
            registerCleanup(timeline, frag);
        }
    }

    // 6) HEAT WAVE — broad translucent wave bands with blur shimmer

    private void addHeatWaveDistortion(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        double angle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist  = Math.hypot(ex - sx, ey - sy);

        int bandCount = (int)(10 + 3 * intensity);
        for (int b = 0; b < bandCount; b++) {
            double h = 40 + b * 18 * intensity;
            Rectangle band = new Rectangle(0, h);
            band.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, FIRE_RED.deriveColor(0, 1, 1, 0.45)),
                    new Stop(0.5, FIRE_ORANGE.deriveColor(0, 1, 1, 0.30)),
                    new Stop(1.0, FIRE_YELLOW.deriveColor(0, 1, 1, 0.0))));
            band.setArcWidth(h * 0.6); band.setArcHeight(h * 0.8);
            band.setX(sx); band.setY(sy - h / 2 + (b - bandCount / 2.0) * 14);
            band.setRotate(angle);
            band.setEffect(new GaussianBlur(8 + b * 2));
            band.setOpacity(0);
            prepareTransientNode(band);
            battleField.getChildren().add(band);

            int delay = b * 30;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay + 20),  new KeyValue(band.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 200), new KeyValue(band.widthProperty(), dist)),
                    new KeyFrame(Duration.millis(delay + 260), new KeyValue(band.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 420), new KeyValue(band.opacityProperty(), 0)));
            registerCleanup(timeline, band);
        }

        // Impact haze
        addFlashCircle(ex, ey, 50 * intensity, FIRE_RED.deriveColor(0, 1, 1, 0.5), 260, 160, timeline);
    }

    // 7) OVERHEAT — charge sphere + huge discharge beam + violent bloom

    private void addOverheatOverdrive(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        // Charge sphere on attacker
        Circle charge = new Circle(0, FIRE_WHITE);
        charge.setCenterX(sx); charge.setCenterY(sy);
        charge.setEffect(new DropShadow(30, FIRE_ORANGE));
        charge.setOpacity(0);
        prepareTransientNode(charge);
        battleField.getChildren().add(charge);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),   new KeyValue(charge.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(60),  new KeyValue(charge.opacityProperty(), 0.9),
                        new KeyValue(charge.radiusProperty(), 30 * intensity)),
                new KeyFrame(Duration.millis(140), new KeyValue(charge.radiusProperty(), 50 * intensity)),
                new KeyFrame(Duration.millis(160), new KeyValue(charge.opacityProperty(), 0)));
        registerCleanup(timeline, charge);

        // Discharge beam
        double angle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist  = Math.hypot(ex - sx, ey - sy);
        double w     = 35 + 16 * intensity;
        Rectangle beam = new Rectangle(0, w);
        beam.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, FIRE_WHITE.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.5, FIRE_YELLOW.deriveColor(0, 1, 1, 0.9)),
                new Stop(1, FIRE_RED.deriveColor(0, 1, 1, 0.6))));
        beam.setArcWidth(w); beam.setArcHeight(w);
        beam.setX(sx); beam.setY(sy - w / 2);
        beam.setRotate(angle);
        beam.setEffect(new DropShadow(w, FIRE_ORANGE));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(140), new KeyValue(beam.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(260), new KeyValue(beam.widthProperty(), dist)),
                new KeyFrame(Duration.millis(310), new KeyValue(beam.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(520), new KeyValue(beam.opacityProperty(), 0)));
        registerCleanup(timeline, beam);

        // Violent impact bloom
        addFlashCircle(ex, ey, 80 * intensity, FIRE_WHITE, 260, 60, timeline);
        addExplosionCore(ex, ey, intensity * 0.85, false, timeline);
    }
    // 8) FLARE BLITZ — handled in createImpactEffect (explosion + rebound)

    private void addReboundSpark(double ax, double ay, Timeline timeline) {
        int count = 16;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI + (random.nextDouble() - 0.5) * Math.PI * 0.8;
            Circle spark = new Circle(8 + random.nextDouble() * 3, FIRE_YELLOW);
            spark.setCenterX(ax); spark.setCenterY(ay); spark.setOpacity(0);
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);
            double dist = 20 + random.nextDouble() * 25;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(320), new KeyValue(spark.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(480),
                            new KeyValue(spark.centerXProperty(), ax + Math.cos(angle) * dist),
                            new KeyValue(spark.centerYProperty(), ay + Math.sin(angle) * dist),
                            new KeyValue(spark.opacityProperty(), 0)));
            registerCleanup(timeline, spark);
        }
    }

    // 9) FIRE FANG — fang visual (shared) + embers at bite

    public void addFangVisual(double x, double y, Timeline timeline) {
        for (int i = 0; i < 2; i++) {
            Polygon fang = new Polygon(
                    0.0, 0.0, -18.0, -25.0, 0.0, -55.0, 18.0, -25.0);
            fang.setFill(FIRE_RED);
            fang.setStroke(FIRE_ORANGE);
            fang.setStrokeWidth(10);
            fang.setEffect(new DropShadow(20, Color.DARKORANGE));
            double xOff = i == 0 ? -15 : 15;
            fang.setLayoutX(x + xOff); fang.setLayoutY(y);
            fang.setOpacity(0);
            fang.setRotate(i == 0 ? -20 : 20);
            prepareTransientNode(fang);
            battleField.getChildren().add(fang);

            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(50),  new KeyValue(fang.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(100),
                            new KeyValue(fang.scaleXProperty(), 1.4),
                            new KeyValue(fang.scaleYProperty(), 1.4)),
                    new KeyFrame(Duration.millis(200), new KeyValue(fang.opacityProperty(), 0)));
            registerCleanup(timeline, fang);
        }
    }

    // 10) FLAME BURST — core bubble then radial mini-bursts

    private void addBurstSplash(double x, double y, double intensity, Timeline timeline) {
        // Core
        Circle core = new Circle(0, FIRE_ORANGE);
        core.setCenterX(x); core.setCenterY(y);
        core.setEffect(new DropShadow(20, FIRE_YELLOW));
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),   new KeyValue(core.opacityProperty(), 0.9),
                        new KeyValue(core.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(130), new KeyValue(core.radiusProperty(), 35 * intensity),
                        new KeyValue(core.opacityProperty(), 0.6)),
                new KeyFrame(Duration.millis(200), new KeyValue(core.opacityProperty(), 0)));
        registerCleanup(timeline, core);

        // Mini bursts
        int dir = 18;
        for (int i = 0; i < dir; i++) {
            double angle = Math.PI * 2 * i / dir;
            double dist  = 45 + random.nextDouble() * 30 * intensity;
            Polygon mini = buildFlameTriangle(FIRE_RED, FIRE_ORANGE, 10 + 5 * intensity);
            mini.setLayoutX(x); mini.setLayoutY(y);
            mini.setRotate(Math.toDegrees(angle));
            mini.setOpacity(0);
            prepareTransientNode(mini);
            battleField.getChildren().add(mini);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(130), new KeyValue(mini.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(320),
                            new KeyValue(mini.translateXProperty(), Math.cos(angle) * dist),
                            new KeyValue(mini.translateYProperty(), Math.sin(angle) * dist),
                            new KeyValue(mini.opacityProperty(), 0)));
            registerCleanup(timeline, mini);
        }
    }

    // 11/8b) CHARGE FLARE TRAIL — flame afterimages during movement

    private void addChargeFlareTrail(double sx, double sy, double ex, double ey,
            double intensity, boolean isWheel, Timeline timeline) {
        double dx   = ex - sx;
        double dy   = ey - sy;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux   = dx / dist;
        double uy   = dy / dist;

        int count = (int)(18 * intensity);
        for (int i = 0; i < count; i++) {
            double t  = i / (double) count;
            double bx = sx + ux * dist * t;
            double by = sy + uy * dist * t;
            Color c   = isWheel ? FIRE_YELLOW : (i % 2 == 0 ? FIRE_ORANGE : FIRE_RED);
            Circle ghost = new Circle(isWheel ? 20 + 8 * intensity : 12 + 5 * intensity, c);
            ghost.setCenterX(bx); ghost.setCenterY(by);
            ghost.setEffect(new GaussianBlur(6));
            ghost.setOpacity(0);
            prepareTransientNode(ghost);
            battleField.getChildren().add(ghost);

            int delay = i * 22;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(ghost.opacityProperty(), 0.75)),
                    new KeyFrame(Duration.millis(delay + 180), new KeyValue(ghost.opacityProperty(), 0)));
            registerCleanup(timeline, ghost);
        }

        // Impact pop
        addFlashCircle(ex, ey, isWheel ? 40 * intensity : 22 * intensity, FIRE_ORANGE, count * 22, 100, timeline);
    }

    // 12) INCINERATE — hit flash + ash motes drifting up

    private void addBerryIncinerateAsh(double x, double y, double intensity, Timeline timeline) {
        addFlashCircle(x, y, 30 * intensity, FIRE_RED, 0, 100, timeline);

        int ashCount = (int)(28 * intensity);
        for (int i = 0; i < ashCount; i++) {
            Circle ash = new Circle(7 + random.nextDouble() * 3, ASH_GRAY);
            double ox = (random.nextDouble() - 0.5) * 50;
            ash.setCenterX(x + ox);
            ash.setCenterY(y + 10);
            ash.setOpacity(0);
            prepareTransientNode(ash);
            battleField.getChildren().add(ash);

            int delay = 140 + random.nextInt(120);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(ash.opacityProperty(), 0.55)),
                    new KeyFrame(Duration.millis(delay + 360),
                            new KeyValue(ash.centerXProperty(), x + ox + (random.nextDouble() - 0.5) * 30),
                            new KeyValue(ash.centerYProperty(), y - 60 - random.nextDouble() * 60),
                            new KeyValue(ash.opacityProperty(), 0)));
            registerCleanup(timeline, ash);
        }
    }

    // 13) INFERNO — tall flame column

    private void addInfernoPillar(double x, double y, double intensity, Timeline timeline) {
        int layerCount = (int)(10 + 3 * intensity);
        for (int i = 0; i < layerCount; i++) {
            double w   = 50 + i * 12 * intensity;
            double maxH = 200 + i * 30 * intensity;
            Rectangle col = new Rectangle(w, 0);
            col.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, FIRE_DEEP.deriveColor(0, 1, 1, 0.9)),
                    new Stop(0.4, FIRE_RED.deriveColor(0, 1, 1, 0.85)),
                    new Stop(0.8, FIRE_ORANGE.deriveColor(0, 1, 1, 0.7)),
                    new Stop(1.0, FIRE_YELLOW.deriveColor(0, 1, 1, 0.0))));
            col.setArcWidth(w * 0.7); col.setArcHeight(w * 0.5);
            col.setX(x - w / 2);
            col.setY(y);
            col.setEffect(new DropShadow(18, FIRE_ORANGE));
            col.setOpacity(0);
            prepareTransientNode(col);
            battleField.getChildren().add(col);

            int delay = i * 25;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(col.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 170),
                            new KeyValue(col.heightProperty(), maxH),
                            new KeyValue(col.yProperty(), y - maxH)),
                    new KeyFrame(Duration.millis(delay + 340),
                            new KeyValue(col.heightProperty(), maxH)),
                    new KeyFrame(Duration.millis(delay + 560),
                            new KeyValue(col.heightProperty(), 0),
                            new KeyValue(col.opacityProperty(), 0)));
            registerCleanup(timeline, col);
        }
        addFlashCircle(x, y, 40 * intensity, FIRE_ORANGE, 0, 120, timeline);
    }

    // 14) FIRE PLEDGE — clean vertical red-gold pillar

    private void addPledgeColumn(double x, double y, double intensity, Timeline timeline) {
        double w   = 38 + 10 * intensity;
        double maxH = 160 + 40 * intensity;
        Rectangle col = new Rectangle(w, 0);
        col.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, FIRE_DEEP.deriveColor(0, 1, 1, 0.95)),
                new Stop(0.5, FIRE_ORANGE.deriveColor(0, 1, 1, 0.85)),
                new Stop(1.0, FIRE_YELLOW.deriveColor(0, 1, 1, 0.0))));
        col.setArcWidth(w * 0.7); col.setArcHeight(w * 0.5);
        col.setX(x - w / 2); col.setY(y);
        col.setEffect(new DropShadow(14, FIRE_ORANGE));
        col.setOpacity(0);
        prepareTransientNode(col);
        battleField.getChildren().add(col);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),   new KeyValue(col.opacityProperty(), 0.92)),
                new KeyFrame(Duration.millis(150), new KeyValue(col.heightProperty(), maxH),
                        new KeyValue(col.yProperty(), y - maxH)),
                new KeyFrame(Duration.millis(280), new KeyValue(col.opacityProperty(), 0.92)),
                new KeyFrame(Duration.millis(430), new KeyValue(col.heightProperty(), 0),
                        new KeyValue(col.opacityProperty(), 0)));
        registerCleanup(timeline, col);

        addFlashCircle(x, y, 28 * intensity, FIRE_YELLOW, 0, 100, timeline);
    }

    // 15) TEMPER FLARE — base burst + dark-red ring second detonation

    private void addTemperFlareBacklash(double x, double y, double intensity, Timeline timeline) {
        addBurstSplash(x, y, intensity * 0.9, timeline);

        // Dark-red ring
        for (int r = 0; r < 2; r++) {
            Circle ring = new Circle(0, Color.TRANSPARENT);
            ring.setStroke(LAVA_RED.deriveColor(0, 1, 0.7, 1));
            ring.setStrokeWidth(10 - r);
            ring.setCenterX(x); ring.setCenterY(y); ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);
            int delay = 90 + r * 30;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(ring.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(ring.radiusProperty(), 55 + r * 20),
                            new KeyValue(ring.opacityProperty(), 0)));
            registerCleanup(timeline, ring);
        }
        addFlashCircle(x, y, 35 * intensity, LAVA_RED, 90, 120, timeline);
    }

    // 16) BLAZE KICK — diagonal crescent arc + foot sparks

    private void addKickArcFlame(double x, double y, double intensity, Timeline timeline) {
        // Crescent made of arc segments along a diagonal sweep
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            double t = i / (double)(segments - 1);
            double startA = -60 + t * 120;
            Arc arc = new Arc(x, y, 50 + 8 * intensity, 50 + 8 * intensity, startA, 18);
            arc.setType(ArcType.OPEN);
            arc.setFill(Color.TRANSPARENT);
            arc.setStroke(t < 0.5 ? FIRE_ORANGE : FIRE_RED);
            arc.setStrokeWidth(10 + 3 * intensity);
            arc.setEffect(new DropShadow(12, FIRE_YELLOW));
            arc.setOpacity(0);
            prepareTransientNode(arc);
            battleField.getChildren().add(arc);

            int delay = i * 8;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay + 70),  new KeyValue(arc.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 160), new KeyValue(arc.startAngleProperty(), startA + 35)),
                    new KeyFrame(Duration.millis(delay + 260), new KeyValue(arc.opacityProperty(), 0)));
            registerCleanup(timeline, arc);
        }

        // Foot sparks
        int sparkCount = (int)(18 * intensity);
        for (int i = 0; i < sparkCount; i++) {
            double angle = (random.nextDouble() - 0.5) * Math.PI * 0.9 - Math.PI / 2;
            double dist  = 20 + random.nextDouble() * 30;
            Circle spark = new Circle(5.5 + random.nextDouble() * 2, FIRE_YELLOW);
            spark.setCenterX(x); spark.setCenterY(y); spark.setOpacity(0);
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(70),  new KeyValue(spark.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(260),
                            new KeyValue(spark.centerXProperty(), x + Math.cos(angle) * dist),
                            new KeyValue(spark.centerYProperty(), y + Math.sin(angle) * dist),
                            new KeyValue(spark.opacityProperty(), 0)));
            registerCleanup(timeline, spark);
        }
    }

    // 17) BLAST BURN — long windup then massive detonation

    private void addBlastBurnDetonation(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        // Windup glow on attacker
        Circle windup = new Circle(0, FIRE_WHITE);
        windup.setCenterX(sx); windup.setCenterY(sy);
        windup.setEffect(new DropShadow(35, FIRE_ORANGE));
        windup.setOpacity(0);
        prepareTransientNode(windup);
        battleField.getChildren().add(windup);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),   new KeyValue(windup.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(80),  new KeyValue(windup.opacityProperty(), 0.9),
                        new KeyValue(windup.radiusProperty(), 40 * intensity)),
                new KeyFrame(Duration.millis(220), new KeyValue(windup.radiusProperty(), 70 * intensity)),
                new KeyFrame(Duration.millis(230), new KeyValue(windup.opacityProperty(), 0)));
        registerCleanup(timeline, windup);

        // Launch streak
        double angle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
        double dist  = Math.hypot(ex - sx, ey - sy);
        double w = 30 + 12 * intensity;
        Rectangle streak = new Rectangle(0, w);
        streak.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, FIRE_WHITE.deriveColor(0, 1, 1, 0.9)),
                new Stop(1, FIRE_RED.deriveColor(0, 1, 1, 0.0))));
        streak.setArcWidth(w); streak.setArcHeight(w);
        streak.setX(sx); streak.setY(sy - w / 2);
        streak.setRotate(angle);
        streak.setEffect(new DropShadow(w, FIRE_ORANGE));
        streak.setOpacity(0);
        prepareTransientNode(streak);
        battleField.getChildren().add(streak);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(220), new KeyValue(streak.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(310), new KeyValue(streak.widthProperty(), dist)),
                new KeyFrame(Duration.millis(320), new KeyValue(streak.opacityProperty(), 0)));
        registerCleanup(timeline, streak);

        // Detonation
        addFlashCircle(ex, ey, 100 * intensity, FIRE_WHITE, 310, 60, timeline);
        addFlashCircle(ex, ey,  75 * intensity, FIRE_YELLOW, 340, 90, timeline);
        addExplosionCore(ex, ey, intensity, true, timeline);

        // Lingering ember rain
        int rainCount = (int)(30 * intensity);
        for (int i = 0; i < rainCount; i++) {
            Circle ember = new Circle(5.5 + random.nextDouble() * 3,
                    i % 2 == 0 ? FIRE_ORANGE : FIRE_RED);
            double ox = (random.nextDouble() - 0.5) * 120;
            ember.setCenterX(ex + ox); ember.setCenterY(ey - 20);
            ember.setOpacity(0);
            prepareTransientNode(ember);
            battleField.getChildren().add(ember);
            int delay = 370 + random.nextInt(200);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(ember.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 300),
                            new KeyValue(ember.centerYProperty(), ey + 80 + random.nextDouble() * 60),
                            new KeyValue(ember.opacityProperty(), 0)));
            registerCleanup(timeline, ember);
        }
    }

    // 18) MYSTICAL FIRE — purple-magenta wisps spiraling into target

    private void addMysticFlameSpiral(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        double dx   = ex - sx;
        double dy   = ey - sy;
        double dist = Math.max(1, Math.hypot(dx, dy));
        double ux   = dx / dist;
        double uy   = dy / dist;
        double px   = -uy;
        double py   =  ux;

        int wispCount = (int)(24 * intensity);
        for (int i = 0; i < wispCount; i++) {
            double t = (i + random.nextDouble()) / wispCount;
            double spiral = Math.sin(t * Math.PI * 3) * 35 * intensity;
            double bx = sx + ux * dist * t + px * spiral;
            double by = sy + uy * dist * t + py * spiral;

            Circle wisp = new Circle(10 + random.nextDouble() * 5, i % 2 == 0 ? FIRE_MAGENTA : FIRE_PURPLE);
            wisp.setCenterX(bx); wisp.setCenterY(by);
            wisp.setEffect(new GaussianBlur(5));
            wisp.setOpacity(0);
            prepareTransientNode(wisp);
            battleField.getChildren().add(wisp);

            int delay = (int)(t * 200);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(wisp.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(wisp.centerXProperty(), ex + (random.nextDouble() - 0.5) * 20),
                            new KeyValue(wisp.centerYProperty(), ey + (random.nextDouble() - 0.5) * 20),
                            new KeyValue(wisp.opacityProperty(), 0.7)),
                    new KeyFrame(Duration.millis(delay + 250), new KeyValue(wisp.opacityProperty(), 0)));
            registerCleanup(timeline, wisp);
        }

        // Soft magical bloom
        Circle bloom = new Circle(0, FIRE_MAGENTA.deriveColor(0, 0.7, 1.2, 0.6));
        bloom.setCenterX(ex); bloom.setCenterY(ey);
        bloom.setEffect(new GaussianBlur(18));
        bloom.setOpacity(0);
        prepareTransientNode(bloom);
        battleField.getChildren().add(bloom);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(250), new KeyValue(bloom.opacityProperty(), 0.8),
                        new KeyValue(bloom.radiusProperty(), 50 * intensity)),
                new KeyFrame(Duration.millis(430), new KeyValue(bloom.opacityProperty(), 0)));
        registerCleanup(timeline, bloom);
    }

    // 19) FLAME WHEEL — rolling circular ring + burst on hit

    private void addWheelSpinRing(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        // Rolling ring that travels with attacker
        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(FIRE_ORANGE);
        ring.setStrokeWidth(10 + 3 * intensity);
        ring.setCenterX(sx); ring.setCenterY(sy);
        ring.setEffect(new DropShadow(14, FIRE_YELLOW));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(ring.opacityProperty(), 0.9),
                        new KeyValue(ring.radiusProperty(), 20 + 10 * intensity)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(ring.centerXProperty(), ex),
                        new KeyValue(ring.centerYProperty(), ey),
                        new KeyValue(ring.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(240),
                        new KeyValue(ring.radiusProperty(), 50 + 20 * intensity),
                        new KeyValue(ring.opacityProperty(), 0)));
        registerCleanup(timeline, ring);

        // Burst on hit
        addBurstSplash(ex, ey, intensity * 0.9, timeline);
    }

    // 20) BURNING JEALOUSY — dark crimson flames + black smoke streaks

    private void addJealousyDarkFlare(double x, double y, double intensity, Timeline timeline) {
        // Two-stage pulse
        for (int pulse = 0; pulse < 2; pulse++) {
            int baseDelay = pulse * 160;

            addFlashCircle(x, y, 32 * intensity, LAVA_RED.deriveColor(0, 1, 0.6, 1),
                    baseDelay, 100, timeline);

            int flameCount = (int)(18 * intensity);
            for (int i = 0; i < flameCount; i++) {
                double angle = Math.PI * 2 * i / flameCount;
                Polygon flame = buildFlameTriangle(
                        FIRE_DEEP.deriveColor(0, 1, 0.7, 1), LAVA_RED, 12 + 5 * intensity);
                flame.setLayoutX(x + Math.cos(angle) * 20);
                flame.setLayoutY(y + Math.sin(angle) * 20);
                flame.setRotate(Math.toDegrees(angle));
                flame.setOpacity(0);
                prepareTransientNode(flame);
                battleField.getChildren().add(flame);

                timeline.getKeyFrames().addAll(
                        new KeyFrame(Duration.millis(baseDelay + 10),
                                new KeyValue(flame.opacityProperty(), 0.9)),
                        new KeyFrame(Duration.millis(baseDelay + 140),
                                new KeyValue(flame.translateXProperty(), Math.cos(angle) * 30),
                                new KeyValue(flame.translateYProperty(), Math.sin(angle) * 30),
                                new KeyValue(flame.opacityProperty(), 0)));
                registerCleanup(timeline, flame);
            }

            // Smoke streaks
            int smokeCount = (int)(16 * intensity);
            for (int i = 0; i < smokeCount; i++) {
                double angle = (random.nextDouble() - 0.5) * Math.PI;
                Circle smoke = new Circle(9 + random.nextDouble() * 4,
                        FIRE_DARK.deriveColor(0, 1, 1, 0.6));
                smoke.setCenterX(x); smoke.setCenterY(y);
                smoke.setEffect(new GaussianBlur(8));
                smoke.setOpacity(0);
                prepareTransientNode(smoke);
                battleField.getChildren().add(smoke);
                timeline.getKeyFrames().addAll(
                        new KeyFrame(Duration.millis(baseDelay + 30),
                                new KeyValue(smoke.opacityProperty(), 0.6)),
                        new KeyFrame(Duration.millis(baseDelay + 300),
                                new KeyValue(smoke.centerXProperty(), x + Math.cos(angle) * 40),
                                new KeyValue(smoke.centerYProperty(), y - 50 - random.nextDouble() * 40),
                                new KeyValue(smoke.opacityProperty(), 0)));
                registerCleanup(timeline, smoke);
            }
        }
    }

    // 21) BURN UP — attacker ignites, emits blast, aura collapses

    private void addBurnUpCollapse(double sx, double sy, double ex, double ey,
            double intensity, Timeline timeline) {
        // Attacker ignition aura
        Circle aura = new Circle(45 * intensity, FIRE_ORANGE.deriveColor(0, 1, 1, 0.6));
        aura.setCenterX(sx); aura.setCenterY(sy);
        aura.setEffect(new GaussianBlur(14));
        aura.setOpacity(0);
        prepareTransientNode(aura);
        battleField.getChildren().add(aura);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),   new KeyValue(aura.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(60),  new KeyValue(aura.opacityProperty(), 0.85)),
                new KeyFrame(Duration.millis(200), new KeyValue(aura.opacityProperty(), 0.85)),
                new KeyFrame(Duration.millis(300), new KeyValue(aura.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(520), new KeyValue(aura.radiusProperty(), 5)));
        registerCleanup(timeline, aura);

        // Blast beam
        addOverheatOverdrive(sx, sy, ex, ey, intensity * 0.85, timeline);

        // Attacker side fadeout embers (loss of fire type)
        int emberCount = (int)(20 * intensity);
        for (int i = 0; i < emberCount; i++) {
            double angle = Math.PI + (random.nextDouble() - 0.5) * Math.PI;
            Circle ember = new Circle(8 + random.nextDouble() * 3, FIRE_ORANGE);
            ember.setCenterX(sx); ember.setCenterY(sy); ember.setOpacity(0);
            prepareTransientNode(ember);
            battleField.getChildren().add(ember);
            int delay = 300 + random.nextInt(120);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(ember.opacityProperty(), 0.7)),
                    new KeyFrame(Duration.millis(delay + 220),
                            new KeyValue(ember.centerXProperty(), sx + Math.cos(angle) * (20 + random.nextDouble() * 30)),
                            new KeyValue(ember.centerYProperty(), sy + Math.sin(angle) * (20 + random.nextDouble() * 30)),
                            new KeyValue(ember.opacityProperty(), 0)));
            registerCleanup(timeline, ember);
        }
    }

    // 22) RAGING FURY — 3 rapid consecutive impact bursts

    private void addRagingFuryMultiBursts(double x, double y, double intensity, Timeline timeline) {
        for (int burst = 0; burst < 3; burst++) {
            int baseDelay = burst * 100;
            double ox = (random.nextDouble() - 0.5) * 28;
            double oy = (random.nextDouble() - 0.5) * 18;

            addFlashCircle(x + ox, y + oy, 38 * intensity, FIRE_ORANGE, baseDelay, 90, timeline);

            int fragCount = (int)(18 * intensity);
            for (int i = 0; i < fragCount; i++) {
                double angle = Math.PI * 2 * i / fragCount + random.nextDouble() * 0.4;
                double dist  = 30 + random.nextDouble() * 25 * intensity;
                Circle frag  = new Circle(8 + random.nextDouble() * 3, FIRE_RED);
                frag.setCenterX(x + ox); frag.setCenterY(y + oy); frag.setOpacity(0);
                prepareTransientNode(frag);
                battleField.getChildren().add(frag);
                timeline.getKeyFrames().addAll(
                        new KeyFrame(Duration.millis(baseDelay + 10),
                                new KeyValue(frag.opacityProperty(), 0.9)),
                        new KeyFrame(Duration.millis(baseDelay + 200),
                                new KeyValue(frag.centerXProperty(), x + ox + Math.cos(angle) * dist),
                                new KeyValue(frag.centerYProperty(), y + oy + Math.sin(angle) * dist),
                                new KeyValue(frag.opacityProperty(), 0)));
                registerCleanup(timeline, frag);
            }
        }
    }

    // 23) LAVA PLUME — ground crack glow → upward plume → ember rain

    private void addLavaPlumeGroundPlume(double x, double y, double intensity, Timeline timeline) {
        // Ground crack glow
        Ellipse crack = new Ellipse(40 * intensity, 8);
        crack.setCenterX(x); crack.setCenterY(y + 40);
        crack.setFill(LAVA_ORANGE.deriveColor(0, 1, 1, 0.8));
        crack.setEffect(new GaussianBlur(10));
        crack.setOpacity(0);
        prepareTransientNode(crack);
        battleField.getChildren().add(crack);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0),   new KeyValue(crack.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(110), new KeyValue(crack.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(180), new KeyValue(crack.opacityProperty(), 0)));
        registerCleanup(timeline, crack);

        // Upward lava plume
        int layerCount = (int)(10 + 2 * intensity);
        for (int i = 0; i < layerCount; i++) {
            double w   = 35 + i * 15 * intensity;
            double maxH = 150 + i * 25 * intensity;
            Rectangle plume = new Rectangle(w, 0);
            plume.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, LAVA_RED.deriveColor(0, 1, 1, 0.95)),
                    new Stop(0.5, LAVA_ORANGE.deriveColor(0, 1, 1, 0.85)),
                    new Stop(1.0, FIRE_YELLOW.deriveColor(0, 1, 1, 0.0))));
            plume.setArcWidth(w * 0.6); plume.setArcHeight(w * 0.3);
            plume.setX(x - w / 2 + (random.nextDouble() - 0.5) * 20);
            plume.setY(y + 40);
            plume.setEffect(new DropShadow(14, LAVA_ORANGE));
            plume.setOpacity(0);
            prepareTransientNode(plume);
            battleField.getChildren().add(plume);

            int delay = 110 + i * 28;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(plume.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 250),
                            new KeyValue(plume.heightProperty(), maxH),
                            new KeyValue(plume.yProperty(), y + 40 - maxH)),
                    new KeyFrame(Duration.millis(delay + 360),
                            new KeyValue(plume.opacityProperty(), 0)));
            registerCleanup(timeline, plume);
        }

        // Ember rain
        int rainCount = (int)(26 * intensity);
        for (int i = 0; i < rainCount; i++) {
            double ox = (random.nextDouble() - 0.5) * 90;
            Circle ember = new Circle(5.5 + random.nextDouble() * 3, i % 2 == 0 ? FIRE_ORANGE : LAVA_RED);
            ember.setCenterX(x + ox); ember.setCenterY(y - 60);
            ember.setOpacity(0);
            prepareTransientNode(ember);
            battleField.getChildren().add(ember);
            int delay = 360 + random.nextInt(120);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(ember.opacityProperty(), 0.8)),
                    new KeyFrame(Duration.millis(delay + 160),
                            new KeyValue(ember.centerYProperty(), y + 50 + random.nextDouble() * 60),
                            new KeyValue(ember.opacityProperty(), 0)));
            registerCleanup(timeline, ember);
        }
    }

    // 24) ERUPTION — multiple outward fire columns, count scales by intensity

    private void addEruptionRadialColumns(double x, double y, double intensity, Timeline timeline) {
        int colCount = (int) clamp(10 + intensity * 6, 3, 10);
        for (int c = 0; c < colCount; c++) {
            double angle   = Math.PI * 2 * c / colCount;
            double spread  = 40 + 45 * intensity;
            double colX    = x + Math.cos(angle) * spread;
            double colY    = y + Math.sin(angle) * (spread * 0.4);
            double w       = 26 + 8 * intensity;
            double maxH    = 120 + 60 * intensity;
            Rectangle col  = new Rectangle(w, 0);
            col.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, LAVA_RED.deriveColor(0, 1, 1, 0.9)),
                    new Stop(0.6, FIRE_ORANGE.deriveColor(0, 1, 1, 0.8)),
                    new Stop(1.0, FIRE_YELLOW.deriveColor(0, 1, 1, 0.0))));
            col.setArcWidth(w * 0.8); col.setArcHeight(w * 0.5);
            col.setX(colX - w / 2); col.setY(colY);
            col.setEffect(new DropShadow(12, LAVA_ORANGE));
            col.setOpacity(0);
            prepareTransientNode(col);
            battleField.getChildren().add(col);

            int delay = c * 35;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(col.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(delay + 200),
                            new KeyValue(col.heightProperty(), maxH),
                            new KeyValue(col.yProperty(), colY - maxH)),
                    new KeyFrame(Duration.millis(delay + 380),
                            new KeyValue(col.opacityProperty(), 0)));
            registerCleanup(timeline, col);
        }

        addFlashCircle(x, y, 55 * intensity, LAVA_ORANGE, 0, 140, timeline);
    }

    // 25) SACRED FIRE — white-hot center, orange halo, feather-like arcs

    private void addSacredFireWhiteCore(double x, double y, double intensity, Timeline timeline) {
        // Holy flash
        addFlashCircle(x, y, 70 * intensity, FIRE_WHITE, 90, 60, timeline);
        addFlashCircle(x, y, 50 * intensity, FIRE_YELLOW, 110, 80, timeline);

        // Orange halo
        Circle halo = new Circle(0, Color.TRANSPARENT);
        halo.setStroke(FIRE_ORANGE.deriveColor(0, 1, 1, 0.8));
        halo.setStrokeWidth(9);
        halo.setCenterX(x); halo.setCenterY(y); halo.setOpacity(0);
        prepareTransientNode(halo);
        battleField.getChildren().add(halo);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(90),  new KeyValue(halo.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(halo.radiusProperty(), 60 * intensity),
                        new KeyValue(halo.opacityProperty(), 0)));
        registerCleanup(timeline, halo);

        // Elegant feather arcs
        int arcCount = 16;
        for (int a = 0; a < arcCount; a++) {
            double baseAngle = 360.0 * a / arcCount;
            Arc arc = new Arc(x, y, 38 + 10 * intensity, 38 + 10 * intensity, baseAngle, 28);
            arc.setType(ArcType.OPEN);
            arc.setFill(Color.TRANSPARENT);
            arc.setStroke(a % 2 == 0 ? FIRE_WHITE : FIRE_YELLOW);
            arc.setStrokeWidth(7 + 2 * intensity);
            arc.setEffect(new DropShadow(10, FIRE_ORANGE));
            arc.setOpacity(0);
            prepareTransientNode(arc);
            battleField.getChildren().add(arc);

            int delay = 90 + a * 20;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),       new KeyValue(arc.opacityProperty(), 0.95)),
                    new KeyFrame(Duration.millis(delay + 180), new KeyValue(arc.startAngleProperty(), baseAngle + 45)),
                    new KeyFrame(Duration.millis(delay + 420), new KeyValue(arc.opacityProperty(), 0)));
            registerCleanup(timeline, arc);
        }
    }

    // 26) MAGMA STORM — thick rotating molten ring + upward sparks, persists

    private void addMagmaStormTrapRing(double x, double y, double intensity, Timeline timeline) {
        // Initial strike
        addExplosionCore(x, y, intensity * 0.8, false, timeline);

        // Outer rotating ring segments
        int segCount = 20;
        for (int i = 0; i < segCount; i++) {
            double startAngle = (i / (double) segCount) * 360;
            Arc seg = new Arc(x, y, 55 + 15 * intensity, (55 + 15 * intensity) * 0.55,
                    startAngle, 22);
            seg.setType(ArcType.OPEN);
            seg.setFill(Color.TRANSPARENT);
            seg.setStroke(i % 2 == 0 ? LAVA_RED : LAVA_ORANGE);
            seg.setStrokeWidth(15 + 3 * intensity);
            seg.setEffect(new DropShadow(14, FIRE_ORANGE));
            seg.setOpacity(0);
            prepareTransientNode(seg);
            battleField.getChildren().add(seg);

            int appearAt = 220 + i * 20;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(appearAt),
                            new KeyValue(seg.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(1000),
                            new KeyValue(seg.startAngleProperty(), startAngle + 340),
                            new KeyValue(seg.opacityProperty(), 0)));
            registerCleanup(timeline, seg);
        }

        // Upward sparks
        int sparkCount = (int)(28 * intensity);
        for (int i = 0; i < sparkCount; i++) {
            double ox = (random.nextDouble() - 0.5) * 80;
            Circle spark = new Circle(5.5 + random.nextDouble() * 3,
                    i % 2 == 0 ? LAVA_ORANGE : FIRE_RED);
            spark.setCenterX(x + ox); spark.setCenterY(y + 20);
            spark.setOpacity(0);
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);
            int delay = 220 + random.nextInt(400);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay), new KeyValue(spark.opacityProperty(), 0.85)),
                    new KeyFrame(Duration.millis(delay + 250),
                            new KeyValue(spark.centerXProperty(), x + ox + (random.nextDouble() - 0.5) * 30),
                            new KeyValue(spark.centerYProperty(), y - 50 - random.nextDouble() * 80),
                            new KeyValue(spark.opacityProperty(), 0)));
            registerCleanup(timeline, spark);
        }
    }

    // FALLBACK — default flames for any unmatched fire move

    private void addDefaultFlames(double x, double y, double intensity, Timeline timeline) {
        int flameCount = (int)(30 + 10 * intensity);
        for (int i = 0; i < flameCount; i++) {
            Polygon flame = buildFlameTriangle(
                    i % 2 == 0 ? FIRE_RED : FIRE_ORANGE, FIRE_YELLOW, 20 + 6 * intensity);
            double angle = (i / (double) flameCount) * 2 * Math.PI;
            double radius = 35 + 8 * intensity;
            flame.setLayoutX(x + Math.cos(angle) * radius);
            flame.setLayoutY(y + Math.sin(angle) * radius);
            flame.setRotate(Math.toDegrees(angle));
            flame.setOpacity(0);
            flame.setEffect(new GaussianBlur(4));
            prepareTransientNode(flame);
            battleField.getChildren().add(flame);

            int delay = i * 28;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),        new KeyValue(flame.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(delay + 60),
                            new KeyValue(flame.scaleXProperty(), 1.6),
                            new KeyValue(flame.scaleYProperty(), 1.6)),
                    new KeyFrame(Duration.millis(delay + 180),
                            new KeyValue(flame.opacityProperty(), 0),
                            new KeyValue(flame.scaleYProperty(), 2.2)));
            registerCleanup(timeline, flame);
        }
    }

    // SHARED HELPERS

    /** Expanding flash circle at a point, fading out over fadeDuration ms. */
    private void addFlashCircle(double x, double y, double radius, Color color,
            int startDelay, int fadeDuration, Timeline timeline) {
        Circle flash = new Circle(0, color.deriveColor(0, 1, 1,
                Math.min(1.0, color.getOpacity() > 0 ? color.getOpacity() : 0.85)));
        flash.setCenterX(x); flash.setCenterY(y);
        flash.setEffect(new GaussianBlur(radius * 0.3));
        flash.setOpacity(0);
        prepareTransientNode(flash);
        battleField.getChildren().add(flash);

        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(startDelay),
                        new KeyValue(flash.opacityProperty(), 0.9),
                        new KeyValue(flash.radiusProperty(), radius)),
                new KeyFrame(Duration.millis(startDelay + fadeDuration),
                        new KeyValue(flash.opacityProperty(), 0)));
        registerCleanup(timeline, flash);
    }

    /** Simple diamond/flame triangle polygon. */
    private Polygon buildFlameTriangle(Color fill, Color stroke, double size) {
        Polygon p = new Polygon(
                0.0, 0.0,
                -size * 0.65, -size * 0.75,
                0.0,          -size * 1.4,
                size * 0.65,  -size * 0.75);
        p.setFill(fill);
        p.setStroke(stroke);
        p.setStrokeWidth(1);
        return p;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private void prepareTransientNode(Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private void registerCleanup(Timeline timeline, Node node) {
        EventHandler<ActionEvent> previous = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (previous != null) previous.handle(e);
        });
    }
}