// ElectricEffects.java
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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public class ElectricEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Electric colour palette
    private static final Color ELECTRIC_YELLOW = Color.web("#FFE55C");
    private static final Color ELECTRIC_GOLD   = Color.web("#FFB300");
    private static final Color ELECTRIC_WHITE  = Color.WHITE;
    private static final Color ELECTRIC_BLUE   = Color.web("#42A5F5");

    public ElectricEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // PUBLIC API – movement sparks (melee charge trail)

    /**
     * Add sparks during movement for electric moves.
     */
    public void addMovementSparks(double startX, double startY, boolean movingRight, Timeline timeline) {
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double length = 90 + random.nextDouble() * 40;

            double offsetX = movingRight ? i * 15 : -i * 15;
            double offsetY = (random.nextDouble() - 0.5) * 28;

            Polyline spark = createBolt(
                startX + offsetX, startY + offsetY,
                startX + offsetX + Math.cos(angle) * length,
                startY + offsetY + Math.sin(angle) * length,
                4, 14);

            battleField.getChildren().add(spark);

            int delay = i * 25;
            timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),
                    new KeyValue(spark.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 30),
                    new KeyValue(spark.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 60),
                    new KeyValue(spark.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 100),
                    new KeyValue(spark.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(delay + 130),
                    new KeyValue(spark.opacityProperty(), 0)));

            registerCleanup(timeline, spark);
        }
    }

    // PUBLIC API – ranged effect (special moves)

    /**
     * Create ranged effect for electric special moves.
     */
    public Timeline createRangedEffect(double startX, double startY, double endX, double endY,
            String moveName, int movePower) {
        Timeline timeline = new Timeline();

        switch (moveName) {
            case "thunder"         -> addThunderStrike(endX, endY, movePower, timeline);
            case "thunderbolt"     -> addThunderbolt(startX, startY, endX, endY, movePower, timeline);
            case "zap-cannon"      -> addZapCannon(startX, startY, endX, endY, movePower, timeline);
            case "discharge"       -> addDischarge(startX, startY, endX, endY, movePower, timeline);
            case "electroweb"      -> addElectroweb(startX, startY, endX, endY, movePower, timeline);
            case "charge-beam"     -> addChargeBeam(startX, startY, endX, endY, movePower, timeline);
            case "rising-voltage"  -> addRisingVoltage(endX, endY, movePower, timeline);
            case "thunder-shock"   -> addThunderShock(startX, startY, endX, endY, movePower, timeline);
            case "shock-wave"      -> addShockWave(startX, startY, endX, endY, movePower, timeline);
            case "volt-switch"     -> addVoltSwitch(startX, startY, endX, endY, movePower, timeline);
            default                -> addDefaultRangedBolts(startX, startY, endX, endY, movePower, timeline);
        }

        return timeline;
    }

    // PUBLIC API – impact effect (melee / contact moves)

    /**
     * Create impact effect for electric moves.
     */
    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        switch (moveName) {
            case "thunder-fang"    -> { addFangVisual(x, y, timeline);
                                        addDefaultZaps(x, y, movePower, timeline); }
            case "thunder-punch"   -> addPunchZaps(x, y, movePower, timeline);
            case "supercell-slam"  -> addGroundSlamBurst(x, y, movePower, timeline);
            case "volt-tackle",
                 "wild-charge"     -> addExplosionBurst(x, y, movePower, timeline);
            case "nuzzle"          -> addGentleSparks(x, y, timeline);
            case "spark"           -> addRadialSparks(x, y, movePower, timeline);
            default                -> addDefaultZaps(x, y, movePower, timeline);
        }
    }

    // RANGED – thunder: massive bolt from sky

    private void addThunderStrike(double ex, double ey, int power, Timeline tl) {
        double skyY = 0;

        // Bright white screen flash
        Circle flash = new Circle(200, Color.rgb(255, 255, 255, 0.55));
        flash.setCenterX(ex);
        flash.setCenterY(ey);
        flash.setOpacity(0);
        flash.setEffect(new GaussianBlur(60));
        prepareTransientNode(flash);
        battleField.getChildren().add(flash);
        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0),   new KeyValue(flash.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(30),  new KeyValue(flash.opacityProperty(), 0.9)),
            new KeyFrame(Duration.millis(180), new KeyValue(flash.opacityProperty(), 0)));
        registerCleanup(tl, flash);

        // Main thick bolt from sky to target
        for (int i = 0; i < 8; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 18;
            Polyline bolt = createBolt(ex + offsetX, skyY, ex + offsetX * 0.3, ey, 20,
                    28 + power / 10.0);
            bolt.setStrokeWidth(18 - i * 2);
            if (i == 0) bolt.setStroke(ELECTRIC_WHITE);
            battleField.getChildren().add(bolt);

            int delay = i * 25;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),      new KeyValue(bolt.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 20), new KeyValue(bolt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 160),new KeyValue(bolt.opacityProperty(), 0)));
            registerCleanup(tl, bolt);
        }

        addSourceBurst(ex, ey, power, tl);
    }

    // RANGED – thunderbolt: classic bolt attacker → defender

    private void addThunderbolt(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        for (int i = 0; i < 8; i++) {
            double laneOffset = (random.nextDouble() - 0.5) * 40;
            Polyline bolt = createBolt(sx, sy, ex + laneOffset * 0.2, ey + laneOffset, 17, 22 + power / 12.0);
            if (i == 0) {
                bolt.setStrokeWidth(9);
                bolt.setStroke(ELECTRIC_WHITE);
            }
            battleField.getChildren().add(bolt);

            int delay = i * 40;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(bolt.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 30),  new KeyValue(bolt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 130), new KeyValue(bolt.opacityProperty(), 0)));
            registerCleanup(tl, bolt);
        }
    }

    // RANGED – zap-cannon: large electric orb with trailing sparks

    private void addZapCannon(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        // Core orb
        Circle orb = new Circle(22 + power / 15.0, ELECTRIC_BLUE);
        orb.setCenterX(sx);
        orb.setCenterY(sy);
        orb.setOpacity(0);
        orb.setEffect(new DropShadow(28, ELECTRIC_GOLD));
        prepareTransientNode(orb);
        battleField.getChildren().add(orb);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0),
                new KeyValue(orb.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(260),
                new KeyValue(orb.centerXProperty(), ex),
                new KeyValue(orb.centerYProperty(), ey)),
            new KeyFrame(Duration.millis(320),
                new KeyValue(orb.opacityProperty(), 0),
                new KeyValue(orb.radiusProperty(), orb.getRadius() * 2.2)));
        registerCleanup(tl, orb);

        // Trailing sparks along the path
        int trailCount = 16;
        for (int i = 0; i < trailCount; i++) {
            double frac = (i + 1.0) / (trailCount + 1);
            double tx = sx + (ex - sx) * frac + (random.nextDouble() - 0.5) * 20;
            double ty = sy + (ey - sy) * frac + (random.nextDouble() - 0.5) * 20;

            Circle spark = new Circle(8 + random.nextDouble() * 3, ELECTRIC_YELLOW);
            spark.setCenterX(tx);
            spark.setCenterY(ty);
            spark.setOpacity(0);
            spark.setEffect(new GaussianBlur(3));
            prepareTransientNode(spark);
            battleField.getChildren().add(spark);

            int delay = 40 + i * 35;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),      new KeyValue(spark.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(delay + 90), new KeyValue(spark.opacityProperty(), 0)));
            registerCleanup(tl, spark);
        }
    }

    // RANGED – discharge: multiple bolts radiating from attacker

    private void addDischarge(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        int boltCount = 15;
        double spread = 110;
        for (int i = 0; i < boltCount; i++) {
            double laneOffset = (i - boltCount / 2.0) * (spread / boltCount);
            Polyline bolt = createBolt(sx, sy, ex + laneOffset * 0.3, ey + laneOffset, 6, 20 + power / 14.0);
            battleField.getChildren().add(bolt);

            int delay = i * 35;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(bolt.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 30),  new KeyValue(bolt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 120), new KeyValue(bolt.opacityProperty(), 0)));
            registerCleanup(tl, bolt);
        }
    }

    // RANGED – electroweb: criss-crossing electric net at defender

    private void addElectroweb(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        double webRadius = 80 + power / 6.0;
        int lineCount = 16;

        // Radial web lines
        for (int i = 0; i < lineCount; i++) {
            double angle = (i / (double) lineCount) * 2 * Math.PI;
            Line webLine = new Line(ex, ey, ex + Math.cos(angle) * webRadius, ey + Math.sin(angle) * webRadius);
            webLine.setStroke(ELECTRIC_YELLOW);
            webLine.setStrokeWidth(8);
            webLine.setStrokeLineCap(StrokeLineCap.ROUND);
            webLine.setEffect(new DropShadow(10, ELECTRIC_GOLD));
            webLine.setOpacity(0);
            prepareTransientNode(webLine);
            battleField.getChildren().add(webLine);

            int delay = 60 + i * 18;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(webLine.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 40),  new KeyValue(webLine.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(delay + 220), new KeyValue(webLine.opacityProperty(), 0)));
            registerCleanup(tl, webLine);
        }

        // Concentric ring arcs
        for (int r = 1; r <= 7; r++) {
            double ringR = webRadius * r / 3.5;
            Circle ring = new Circle(ringR, Color.TRANSPARENT);
            ring.setCenterX(ex);
            ring.setCenterY(ey);
            ring.setStroke(ELECTRIC_YELLOW);
            ring.setStrokeWidth(8);
            ring.setEffect(new DropShadow(6, ELECTRIC_GOLD));
            ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            int delay = 80 + r * 30;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(ring.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 40),  new KeyValue(ring.opacityProperty(), 0.85)),
                new KeyFrame(Duration.millis(delay + 240), new KeyValue(ring.opacityProperty(), 0)));
            registerCleanup(tl, ring);
        }
    }

    // RANGED – charge-beam: thin concentrated beam

    private void addChargeBeam(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        Line beam = new Line(sx, sy, ex, ey);
        beam.setStroke(ELECTRIC_YELLOW);
        beam.setStrokeWidth(10);
        beam.setStrokeLineCap(StrokeLineCap.ROUND);
        beam.setEffect(new DropShadow(18, ELECTRIC_GOLD));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(20),  new KeyValue(beam.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(80),  new KeyValue(beam.opacityProperty(), 0.9)),
            new KeyFrame(Duration.millis(160), new KeyValue(beam.opacityProperty(), 0)));
        registerCleanup(tl, beam);

        // Small sparks along beam
        for (int i = 0; i < 8; i++) {
            double frac = (i + 1.0) / 5.0;
            double px = sx + (ex - sx) * frac + (random.nextDouble() - 0.5) * 12;
            double py = sy + (ey - sy) * frac + (random.nextDouble() - 0.5) * 12;

            Circle dot = new Circle(5 + random.nextDouble() * 2, ELECTRIC_WHITE);
            dot.setCenterX(px);
            dot.setCenterY(py);
            dot.setOpacity(0);
            dot.setEffect(new GaussianBlur(2));
            prepareTransientNode(dot);
            battleField.getChildren().add(dot);

            int delay = 30 + i * 25;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),      new KeyValue(dot.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(delay + 80), new KeyValue(dot.opacityProperty(), 0)));
            registerCleanup(tl, dot);
        }
    }

    // RANGED – rising-voltage: bolts erupting from ground beneath defender

    private void addRisingVoltage(double ex, double ey, int power, Timeline tl) {
        double groundY = ey + 60;
        int boltCount = 10 + power / 25;

        for (int i = 0; i < boltCount; i++) {
            double bx = ex + (random.nextDouble() - 0.5) * 80;
            Polyline bolt = createBolt(bx, groundY, bx + (random.nextDouble() - 0.5) * 30,
                    ey - 30 - random.nextDouble() * 40, 6, 16 + power / 16.0);
            battleField.getChildren().add(bolt);

            int delay = i * 40;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(bolt.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 25),  new KeyValue(bolt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 130), new KeyValue(bolt.opacityProperty(), 0)));
            registerCleanup(tl, bolt);
        }

        // Ground flash
        Circle glow = new Circle(75, Color.rgb(255, 229, 92, 0.35));
        glow.setCenterX(ex);
        glow.setCenterY(groundY);
        glow.setOpacity(0);
        glow.setEffect(new GaussianBlur(18));
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);
        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(10),  new KeyValue(glow.opacityProperty(), 0.8)),
            new KeyFrame(Duration.millis(200), new KeyValue(glow.opacityProperty(), 0)));
        registerCleanup(tl, glow);
    }

    // RANGED – thunder-shock: small bolt attacker → defender

    private void addThunderShock(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        Polyline bolt = createBolt(sx, sy, ex, ey, 15, 14 + power / 16.0);
        bolt.setStrokeWidth(10);
        battleField.getChildren().add(bolt);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0),   new KeyValue(bolt.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(30),  new KeyValue(bolt.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(110), new KeyValue(bolt.opacityProperty(), 0)));
        registerCleanup(tl, bolt);
    }

    // RANGED – shock-wave: fast expanding arc from attacker to defender

    private void addShockWave(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        // Expanding arc ring travelling toward defender
        Circle wave = new Circle(15, Color.TRANSPARENT);
        wave.setCenterX(sx);
        wave.setCenterY(sy);
        wave.setStroke(ELECTRIC_YELLOW);
        wave.setStrokeWidth(8);
        wave.setEffect(new DropShadow(14, ELECTRIC_GOLD));
        wave.setOpacity(0);
        prepareTransientNode(wave);
        battleField.getChildren().add(wave);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0),
                new KeyValue(wave.opacityProperty(), 0.9)),
            new KeyFrame(Duration.millis(140),
                new KeyValue(wave.centerXProperty(), ex),
                new KeyValue(wave.centerYProperty(), ey),
                new KeyValue(wave.radiusProperty(), 50.0),
                new KeyValue(wave.opacityProperty(), 0.7)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(wave.opacityProperty(), 0)));
        registerCleanup(tl, wave);

        // Quick bolt behind the wave
        Polyline bolt = createBolt(sx, sy, ex, ey, 15, 12);
        bolt.setStrokeWidth(8);
        battleField.getChildren().add(bolt);
        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(10),  new KeyValue(bolt.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(40),  new KeyValue(bolt.opacityProperty(), 0.8)),
            new KeyFrame(Duration.millis(150), new KeyValue(bolt.opacityProperty(), 0)));
        registerCleanup(tl, bolt);
    }

    // RANGED – volt-switch: quick bolt then flash

    private void addVoltSwitch(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        // Fast single bolt
        Polyline bolt = createBolt(sx, sy, ex, ey, 15, 16);
        bolt.setStrokeWidth(10);
        battleField.getChildren().add(bolt);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0),  new KeyValue(bolt.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(20), new KeyValue(bolt.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(90), new KeyValue(bolt.opacityProperty(), 0)));
        registerCleanup(tl, bolt);

        // Quick flash at defender
        Circle flash = new Circle(35, ELECTRIC_YELLOW);
        flash.setCenterX(ex);
        flash.setCenterY(ey);
        flash.setOpacity(0);
        flash.setEffect(new GaussianBlur(16));
        prepareTransientNode(flash);
        battleField.getChildren().add(flash);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(30),  new KeyValue(flash.opacityProperty(), 0.95)),
            new KeyFrame(Duration.millis(120), new KeyValue(flash.opacityProperty(), 0),
                new KeyValue(flash.radiusProperty(), 55.0)));
        registerCleanup(tl, flash);
    }

    // RANGED – default: generic bolts (fallback)

    private void addDefaultRangedBolts(double sx, double sy, double ex, double ey, int power, Timeline tl) {
        addSourceBurst(sx, sy, power, tl);

        for (int i = 0; i < 5; i++) {
            double laneOffset = (random.nextDouble() - 0.5) * 90;
            Polyline bolt = createBolt(sx, sy,
                    ex + laneOffset * 0.22, ey + laneOffset, 6, 20 + power / 14.0);
            battleField.getChildren().add(bolt);

            int delay = i * 55;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(bolt.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 35),  new KeyValue(bolt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 110), new KeyValue(bolt.opacityProperty(), 0)));
            registerCleanup(tl, bolt);
        }
    }

    // IMPACT – thunder-punch: radial zaps with punch emphasis

    private void addPunchZaps(double x, double y, int power, Timeline tl) {
        // Central punch flash
        Circle flash = new Circle(30, ELECTRIC_YELLOW);
        flash.setCenterX(x);
        flash.setCenterY(y);
        flash.setOpacity(0);
        flash.setEffect(new DropShadow(20, ELECTRIC_GOLD));
        prepareTransientNode(flash);
        battleField.getChildren().add(flash);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(10),  new KeyValue(flash.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(80),  new KeyValue(flash.opacityProperty(), 0.5)),
            new KeyFrame(Duration.millis(140), new KeyValue(flash.opacityProperty(), 0),
                new KeyValue(flash.radiusProperty(), 40.0)));
        registerCleanup(tl, flash);

        // Radial zaps
        int zapCount = 20 + power / 15;
        for (int i = 0; i < zapCount; i++) {
            double angle = (i / (double) zapCount) * 2 * Math.PI + random.nextDouble() * 0.4;
            double length = 70 + random.nextDouble() * 40 + power / 4.0;

            Polyline zap = createBolt(x, y, x + Math.cos(angle) * length, y + Math.sin(angle) * length,
                    14, 14 + power / 18.0);
            battleField.getChildren().add(zap);

            int delay = i * 12;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(zap.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 20),  new KeyValue(zap.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 80),  new KeyValue(zap.opacityProperty(), 0)));
            registerCleanup(tl, zap);
        }
    }

    // IMPACT – supercell-slam: wide electric ground-slam burst

    private void addGroundSlamBurst(double x, double y, int power, Timeline tl) {
        // Expanding shockwave ring
        Circle ring = new Circle(16, Color.TRANSPARENT);
        ring.setCenterX(x);
        ring.setCenterY(y);
        ring.setStroke(ELECTRIC_YELLOW);
        ring.setStrokeWidth(10);
        ring.setEffect(new DropShadow(20, ELECTRIC_GOLD));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        double maxRadius = 80 + power / 3.0;
        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0),   new KeyValue(ring.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(ring.radiusProperty(), maxRadius),
                new KeyValue(ring.opacityProperty(), 0.6)),
            new KeyFrame(Duration.millis(320),
                new KeyValue(ring.opacityProperty(), 0)));
        registerCleanup(tl, ring);

        // Central bright flash
        Circle flash = new Circle(40, Color.rgb(255, 255, 255, 0.7));
        flash.setCenterX(x);
        flash.setCenterY(y);
        flash.setOpacity(0);
        flash.setEffect(new GaussianBlur(20));
        prepareTransientNode(flash);
        battleField.getChildren().add(flash);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0),   new KeyValue(flash.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(140), new KeyValue(flash.opacityProperty(), 0)));
        registerCleanup(tl, flash);

        // Wide ground-level bolts
        int boltCount = 18 + power / 20;
        for (int i = 0; i < boltCount; i++) {
            double angle = (i / (double) boltCount) * 2 * Math.PI;
            double length = maxRadius + random.nextDouble() * 30;

            Polyline bolt = createBolt(x, y, x + Math.cos(angle) * length, y + Math.sin(angle) * length,
                    16, 22 + power / 14.0);
            bolt.setStrokeWidth(10);
            battleField.getChildren().add(bolt);

            int delay = i * 18;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(bolt.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 30),  new KeyValue(bolt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 120), new KeyValue(bolt.opacityProperty(), 0)));
            registerCleanup(tl, bolt);
        }
    }

    // IMPACT – volt-tackle / wild-charge: massive electric explosion

    private void addExplosionBurst(double x, double y, int power, Timeline tl) {
        // Bright explosion core
        Circle core = new Circle(35, ELECTRIC_YELLOW);
        core.setCenterX(x);
        core.setCenterY(y);
        core.setOpacity(0);
        core.setEffect(new DropShadow(30, ELECTRIC_GOLD));
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(0), new KeyValue(core.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(60),
                new KeyValue(core.radiusProperty(), 55.0),
                new KeyValue(core.opacityProperty(), 0.85)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(core.radiusProperty(), 70.0),
                new KeyValue(core.opacityProperty(), 0)));
        registerCleanup(tl, core);

        // Dense radial zaps
        int zapCount = 20 + power / 12;
        for (int i = 0; i < zapCount; i++) {
            double angle = (i / (double) zapCount) * 2 * Math.PI + random.nextDouble() * 0.3;
            double length = 90 + random.nextDouble() * 60 + power / 3.0;

            Polyline zap = createBolt(x, y, x + Math.cos(angle) * length, y + Math.sin(angle) * length,
                    16, 24 + power / 12.0);
            zap.setStrokeWidth(12);
            battleField.getChildren().add(zap);

            int delay = i * 14;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay), new KeyValue(zap.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 20), new KeyValue(zap.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 50), new KeyValue(zap.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 80), new KeyValue(zap.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(delay + 110), new KeyValue(zap.opacityProperty(), 0)));
            registerCleanup(tl, zap);
        }
    }

    // IMPACT – nuzzle: small gentle sparks

    private void addGentleSparks(double x, double y, Timeline tl) {
        int sparkCount = 20;
        for (int i = 0; i < sparkCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double length = 35 + random.nextDouble() * 20;

            Polyline spark = createBolt(x, y, x + Math.cos(angle) * length, y + Math.sin(angle) * length,
                    10, 8);
            spark.setStrokeWidth(3);
            battleField.getChildren().add(spark);

            int delay = i * 35;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(spark.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 30),  new KeyValue(spark.opacityProperty(), 0.8)),
                new KeyFrame(Duration.millis(delay + 100), new KeyValue(spark.opacityProperty(), 0)));
            registerCleanup(tl, spark);
        }

        // Tiny warm glow
        Circle glow = new Circle(20, Color.rgb(255, 229, 92, 0.4));
        glow.setCenterX(x);
        glow.setCenterY(y);
        glow.setOpacity(0);
        glow.setEffect(new GaussianBlur(8));
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);

        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(20),  new KeyValue(glow.opacityProperty(), 0.7)),
            new KeyFrame(Duration.millis(180), new KeyValue(glow.opacityProperty(), 0)));
        registerCleanup(tl, glow);
    }

    // IMPACT – spark: moderate radial sparks

    private void addRadialSparks(double x, double y, int power, Timeline tl) {
        int zapCount = 20;
        for (int i = 0; i < zapCount; i++) {
            double angle = (i / (double) zapCount) * 2 * Math.PI + random.nextDouble() * 0.5;
            double length = 65 + random.nextDouble() * 35 + power / 5.0;

            Polyline zap = createBolt(x, y, x + Math.cos(angle) * length, y + Math.sin(angle) * length,
                    12, 14 + power / 18.0);
            battleField.getChildren().add(zap);

            int delay = i * 18;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(zap.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 25),  new KeyValue(zap.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 60),  new KeyValue(zap.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 90),  new KeyValue(zap.opacityProperty(), 0.7)),
                new KeyFrame(Duration.millis(delay + 120), new KeyValue(zap.opacityProperty(), 0)));
            registerCleanup(tl, zap);
        }
    }

    // IMPACT – default: generic electric zaps (fallback)

    private void addDefaultZaps(double x, double y, int power, Timeline tl) {
        int zapCount = Math.min(24 + power / 20, 28);

        for (int i = 0; i < zapCount; i++) {
            double angle = (i / (double) zapCount) * 2 * Math.PI + random.nextDouble() * 0.5;
            double length = 80 + random.nextDouble() * 50 + power / 3.0;

            Polyline zap = createBolt(x, y, x + Math.cos(angle) * length, y + Math.sin(angle) * length,
                    15, 18 + power / 16.0);
            battleField.getChildren().add(zap);

            int delay = i * 20;
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(delay),       new KeyValue(zap.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 25),  new KeyValue(zap.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(delay + 50),  new KeyValue(zap.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 90),  new KeyValue(zap.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(delay + 115), new KeyValue(zap.opacityProperty(), 0)));
            registerCleanup(tl, zap);
        }
    }

    // SHARED HELPERS

    private void addSourceBurst(double x, double y, int movePower, Timeline timeline) {
        Circle flare = new Circle(25 + movePower / 16.0, Color.rgb(255, 245, 160, 0.85));
        flare.setCenterX(x);
        flare.setCenterY(y);
        flare.setOpacity(0);
        flare.setEffect(new DropShadow(24, ELECTRIC_GOLD));
        prepareTransientNode(flare);

        battleField.getChildren().add(flare);

        KeyFrame appear = new KeyFrame(Duration.millis(30),
            new KeyValue(flare.opacityProperty(), 1.0),
            new KeyValue(flare.radiusProperty(), flare.getRadius() * 1.15));
        KeyFrame fade = new KeyFrame(Duration.millis(140),
            new KeyValue(flare.opacityProperty(), 0),
            new KeyValue(flare.radiusProperty(), flare.getRadius() * 1.8));

        timeline.getKeyFrames().addAll(appear, fade);
        registerCleanup(timeline, flare);
    }

    private void addFangVisual(double x, double y, Timeline timeline) {
        for (int i = 0; i < 2; i++) {
            Polygon fang = new Polygon();
            fang.getPoints().addAll(
                0.0, 0.0,
                -25.0, -30.0,
                0.0, -65.0,
                25.0, -30.0
            );

            fang.setFill(ELECTRIC_YELLOW);
            fang.setStroke(ELECTRIC_GOLD);
            fang.setStrokeWidth(10);
            fang.setEffect(new DropShadow(20, ELECTRIC_GOLD));

            double xOffset = i == 0 ? -20 : 20;
            fang.setLayoutX(x + xOffset);
            fang.setLayoutY(y);
            fang.setOpacity(0);
            fang.setRotate(i == 0 ? -20 : 20);
            prepareTransientNode(fang);

            battleField.getChildren().add(fang);

            KeyFrame appear = new KeyFrame(Duration.millis(50),
                new KeyValue(fang.opacityProperty(), 1.0),
                new KeyValue(fang.scaleXProperty(), 1.0),
                new KeyValue(fang.scaleYProperty(), 1.0));
            KeyFrame bite = new KeyFrame(Duration.millis(100),
                new KeyValue(fang.scaleXProperty(), 1.4),
                new KeyValue(fang.scaleYProperty(), 1.4));
            KeyFrame disappear = new KeyFrame(Duration.millis(200),
                new KeyValue(fang.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, bite, disappear);

            registerCleanup(timeline, fang);
        }
    }

    private Polyline createBolt(double startX, double startY, double endX, double endY,
            int segmentCount, double maxOffset) {
        Polyline bolt = new Polyline();
        bolt.setStroke(ELECTRIC_YELLOW);
        bolt.setStrokeWidth(12);
        bolt.setStrokeLineCap(StrokeLineCap.ROUND);
        bolt.setEffect(new DropShadow(24, ELECTRIC_GOLD));
        bolt.setOpacity(0);
        bolt.setFill(null);

        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double px = -(dy / distance);
        double py = dx / distance;

        bolt.getPoints().addAll(startX, startY);
        for (int i = 1; i < segmentCount; i++) {
            double progress = i / (double) segmentCount;
            double offset = (random.nextDouble() - 0.5) * maxOffset;
            double pointX = startX + dx * progress + px * offset;
            double pointY = startY + dy * progress + py * offset;
            bolt.getPoints().addAll(pointX, pointY);
        }
        bolt.getPoints().addAll(endX, endY);

        prepareTransientNode(bolt);
        return bolt;
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