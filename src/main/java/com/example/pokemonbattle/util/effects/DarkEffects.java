// DarkEffects.java
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

public class DarkEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Dark colour palette
    private static final Color DARK_BLACK   = Color.web("#212121");
    private static final Color DARK_PURPLE  = Color.web("#4A148C");
    private static final Color DARK_CRIMSON = Color.web("#B71C1C");
    private static final Color DARK_GREY    = Color.web("#424242");
    private static final Color DARK_SHADOW  = Color.web("#0D0D0D");
    private static final Color DARK_SMOKE   = Color.web("#616161");
    private static final Color DARK_RED     = Color.web("#D32F2F");

    public DarkEffects(Pane battleField) {
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
    // Public API – full signature (all dark moves)
    // -----------------------------------------------------------------

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.4, 1.8);

        switch (moveName) {
            // Fast melee lunge with black-purple slash trails
            case "bite"         -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "foul-play"    -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "knock-off"    -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "lash-out"     -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "night-slash"  -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "pursuit"      -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "sucker-punch" -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "thief"        -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);
            case "throat-chop"  -> addDarkSlashLunge(startX, startY, endX, endY, intensity, timeline);

            // Dark aura burst with drifting smoke particles
            case "brutal-swing"   -> addDarkAuraBurst(endX, endY, intensity, timeline);
            case "crunch"         -> addDarkAuraBurst(endX, endY, intensity, timeline);
            case "darkest-lariat" -> addDarkAuraBurst(endX, endY, intensity, timeline);
            case "feint-attack"   -> addDarkAuraBurst(endX, endY, intensity, timeline);

            // Shadow pulse ring with smoky shockwave
            case "dark-pulse" -> addShadowPulseRing(startX, startY, endX, endY, intensity, timeline);
            case "snarl"      -> addShadowPulseRing(startX, startY, endX, endY, intensity, timeline);

            // Delayed retaliation burst
            case "assurance"  -> addRetaliationBurst(startX, startY, endX, endY, intensity, timeline);
            case "comeuppance" -> addRetaliationBurst(startX, startY, endX, endY, intensity, timeline);
            case "payback"    -> addRetaliationBurst(startX, startY, endX, endY, intensity, timeline);
            case "power-trip" -> addRetaliationBurst(startX, startY, endX, endY, intensity, timeline);

            default           -> addDefaultDarkBurst(endX, endY, intensity, timeline);
        }
    }

    // -----------------------------------------------------------------
    // Public API – ranged lead effect (projectile from attacker to target)
    // -----------------------------------------------------------------

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.4, 1.8);
        addShadowPulseRing(startX, startY, endX, endY, intensity, timeline);
    }

    // =================================================================
    // Fast melee lunge – dark slash marks at impact, purple-black trails
    // =================================================================

    private void addDarkSlashLunge(double startX, double startY,
                                   double endX, double endY,
                                   double intensity, Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;

        // Purple-black energy trails along the lunge path
        int trailCount = (int) (5 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = startX + dx * t + (random.nextDouble() - 0.5) * 14;
            double ty = startY + dy * t + (random.nextDouble() - 0.5) * 14;

            Circle trail = new Circle(3 + random.nextDouble() * 3,
                    i % 2 == 0 ? DARK_PURPLE : DARK_BLACK);
            trail.setEffect(new GaussianBlur(4));
            trail.setCenterX(tx);
            trail.setCenterY(ty);
            trail.setOpacity(0);
            prepareTransientNode(trail);
            battleField.getChildren().add(trail);

            int delay = (int) (t * 140);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(trail.opacityProperty(), 0.75));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(trail.centerYProperty(), ty - 8 - random.nextDouble() * 10),
                    new KeyValue(trail.radiusProperty(), trail.getRadius() * 1.5),
                    new KeyValue(trail.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, trail);
        }

        // Slash marks at impact point
        int slashCount = (int) (2 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle = -30 + i * (60.0 / Math.max(slashCount - 1, 1));
            double rad = Math.toRadians(angle);
            double slashLen = 22 + 14 * intensity;
            double sx = endX - Math.cos(rad) * slashLen * 0.5;
            double sy = endY - Math.sin(rad) * slashLen * 0.5;
            double ex = endX + Math.cos(rad) * slashLen * 0.5;
            double ey = endY + Math.sin(rad) * slashLen * 0.5;

            Line slash = new Line(sx, sy, ex, ey);
            slash.setStroke(i % 2 == 0 ? DARK_PURPLE : DARK_CRIMSON);
            slash.setStrokeWidth(3 + 1.5 * intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(8 + 4 * intensity, DARK_PURPLE));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = 100 + i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(slash.opacityProperty(), 0.95));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 80),
                    new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.6),
                    new KeyValue(slash.opacityProperty(), 0.9));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(slash.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, slash);
        }

        // Dark impact flash
        addDarkFlash(endX, endY, 16 + 10 * intensity, DARK_CRIMSON, 90, 200, timeline);
    }

    // =================================================================
    // Dark aura burst – expanding aura circle, smoke/shadow particles
    // =================================================================

    private void addDarkAuraBurst(double x, double y, double intensity,
                                  Timeline timeline) {
        // Expanding dark aura circle
        Circle aura = new Circle(0, DARK_SHADOW.deriveColor(0, 1, 1, 0.6));
        aura.setStroke(DARK_PURPLE.deriveColor(0, 1, 1, 0.7));
        aura.setStrokeWidth(3 + 1.5 * intensity);
        aura.setCenterX(x);
        aura.setCenterY(y);
        aura.setEffect(new GaussianBlur(8 + 4 * intensity));
        aura.setOpacity(0);
        prepareTransientNode(aura);
        battleField.getChildren().add(aura);

        double auraRadius = 30 + 22 * intensity;
        KeyFrame aAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(aura.opacityProperty(), 0.85));
        KeyFrame aExpand = new KeyFrame(Duration.millis(200),
                new KeyValue(aura.radiusProperty(), auraRadius),
                new KeyValue(aura.opacityProperty(), 0.5));
        KeyFrame aFade = new KeyFrame(Duration.millis(360),
                new KeyValue(aura.radiusProperty(), auraRadius * 1.4),
                new KeyValue(aura.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(aAppear, aExpand, aFade);
        registerCleanup(timeline, aura);

        // Inner crimson flicker
        Circle innerFlicker = new Circle(0, DARK_CRIMSON.deriveColor(0, 1, 1, 0.5));
        innerFlicker.setEffect(new GaussianBlur(6));
        innerFlicker.setCenterX(x);
        innerFlicker.setCenterY(y);
        innerFlicker.setOpacity(0);
        prepareTransientNode(innerFlicker);
        battleField.getChildren().add(innerFlicker);

        KeyFrame fAppear = new KeyFrame(Duration.millis(30),
                new KeyValue(innerFlicker.opacityProperty(), 0.7));
        KeyFrame fExpand = new KeyFrame(Duration.millis(160),
                new KeyValue(innerFlicker.radiusProperty(), auraRadius * 0.5),
                new KeyValue(innerFlicker.opacityProperty(), 0.3));
        KeyFrame fFade = new KeyFrame(Duration.millis(280),
                new KeyValue(innerFlicker.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(fAppear, fExpand, fFade);
        registerCleanup(timeline, innerFlicker);

        // Smoke/shadow particles drifting outward
        addSmokeParticles(x, y, intensity, 0, timeline);
    }

    /** Drifting dark smoke particles expanding outward from a point. */
    private void addSmokeParticles(double x, double y, double intensity,
                                   int startDelay, Timeline timeline) {
        int count = (int) (7 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            double r = 5 + random.nextDouble() * 5;
            Color smokeColor = i % 3 == 0 ? DARK_SMOKE : i % 3 == 1 ? DARK_GREY : DARK_BLACK;
            Rectangle smoke = new Rectangle(r * 2, r * 2);
            smoke.setFill(smokeColor.deriveColor(0, 1, 1, 0.5));
            smoke.setArcWidth(r);
            smoke.setArcHeight(r);
            smoke.setEffect(new GaussianBlur(5 + 2 * intensity));
            smoke.setX(x - r + (random.nextDouble() - 0.5) * 16);
            smoke.setY(y - r + (random.nextDouble() - 0.5) * 16);
            smoke.setOpacity(0);
            smoke.setRotate(random.nextDouble() * 360);
            prepareTransientNode(smoke);
            battleField.getChildren().add(smoke);

            double angle = (i / (double) count) * 2 * Math.PI + random.nextDouble() * 0.5;
            double driftR = 28 + 18 * intensity;
            int delay = startDelay + i * 25;

            KeyFrame appear = new KeyFrame(Duration.millis(delay + 40),
                    new KeyValue(smoke.opacityProperty(), 0.6));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(smoke.xProperty(), smoke.getX() + Math.cos(angle) * driftR),
                    new KeyValue(smoke.yProperty(), smoke.getY() + Math.sin(angle) * driftR),
                    new KeyValue(smoke.scaleXProperty(), 1.8),
                    new KeyValue(smoke.scaleYProperty(), 1.8),
                    new KeyValue(smoke.rotateProperty(), smoke.getRotate() + 45),
                    new KeyValue(smoke.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, smoke);
        }
    }

    // =================================================================
    // Shadow pulse ring – dark ring projectile with smoky shockwave
    // =================================================================

    private void addShadowPulseRing(double startX, double startY,
                                    double endX, double endY,
                                    double intensity, Timeline timeline) {
        // Dark ring projectile traveling from attacker to defender
        double ringRadius = 10 + 6 * intensity;

        Circle ring = new Circle(ringRadius, Color.TRANSPARENT);
        ring.setStroke(DARK_PURPLE);
        ring.setStrokeWidth(4 + 2 * intensity);
        ring.setEffect(new DropShadow(14 + 6 * intensity, DARK_SHADOW));
        ring.setCenterX(startX);
        ring.setCenterY(startY);
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        // Inner dark fill for the ring
        Circle ringCore = new Circle(ringRadius * 0.6, DARK_BLACK.deriveColor(0, 1, 1, 0.7));
        ringCore.setEffect(new GaussianBlur(5));
        ringCore.setCenterX(startX);
        ringCore.setCenterY(startY);
        ringCore.setOpacity(0);
        prepareTransientNode(ringCore);
        battleField.getChildren().add(ringCore);

        KeyFrame rAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(ring.opacityProperty(), 0.95),
                new KeyValue(ringCore.opacityProperty(), 0.8));
        KeyFrame rTravel = new KeyFrame(Duration.millis(240),
                new KeyValue(ring.centerXProperty(), endX),
                new KeyValue(ring.centerYProperty(), endY),
                new KeyValue(ringCore.centerXProperty(), endX),
                new KeyValue(ringCore.centerYProperty(), endY));
        KeyFrame rFade = new KeyFrame(Duration.millis(320),
                new KeyValue(ring.opacityProperty(), 0),
                new KeyValue(ringCore.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(rAppear, rTravel, rFade);
        registerCleanup(timeline, ring);
        registerCleanup(timeline, ringCore);

        // Wispy trail behind the ring
        addPulseTrail(startX, startY, endX, endY, intensity, timeline);

        // Smoky shockwave on impact
        addSmokyShockwave(endX, endY, intensity, timeline);
    }

    /** Wispy dark particles trailing the shadow pulse ring. */
    private void addPulseTrail(double startX, double startY,
                               double endX, double endY,
                               double intensity, Timeline timeline) {
        int trailCount = (int) (7 + 5 * intensity);
        double dx = endX - startX;
        double dy = endY - startY;

        for (int i = 0; i < trailCount; i++) {
            Circle wisp = new Circle(3 + random.nextDouble() * 3,
                    i % 2 == 0 ? DARK_PURPLE : DARK_SMOKE);
            wisp.setEffect(new GaussianBlur(4));

            double t = (i + random.nextDouble()) / trailCount * 0.8;
            double wx = startX + dx * t + (random.nextDouble() - 0.5) * 18;
            double wy = startY + dy * t + (random.nextDouble() - 0.5) * 18;
            wisp.setCenterX(wx);
            wisp.setCenterY(wy);
            wisp.setOpacity(0);
            prepareTransientNode(wisp);
            battleField.getChildren().add(wisp);

            int delay = (int) (t * 220) + 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(wisp.opacityProperty(), 0.65));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(wisp.centerYProperty(), wy - 10 - random.nextDouble() * 12),
                    new KeyValue(wisp.radiusProperty(), wisp.getRadius() * 1.4),
                    new KeyValue(wisp.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, wisp);
        }
    }

    /** Expanding smoky shockwave at the impact point. */
    private void addSmokyShockwave(double x, double y, double intensity,
                                   Timeline timeline) {
        // Central dark shockwave ring
        Circle shockwave = new Circle(0, Color.TRANSPARENT);
        shockwave.setStroke(DARK_BLACK.deriveColor(0, 1, 1, 0.8));
        shockwave.setStrokeWidth(5 + 2 * intensity);
        shockwave.setEffect(new GaussianBlur(6 + 3 * intensity));
        shockwave.setCenterX(x);
        shockwave.setCenterY(y);
        shockwave.setOpacity(0);
        prepareTransientNode(shockwave);
        battleField.getChildren().add(shockwave);

        double shockRadius = 35 + 20 * intensity;
        KeyFrame sAppear = new KeyFrame(Duration.millis(220),
                new KeyValue(shockwave.opacityProperty(), 0.85));
        KeyFrame sExpand = new KeyFrame(Duration.millis(380),
                new KeyValue(shockwave.radiusProperty(), shockRadius),
                new KeyValue(shockwave.strokeWidthProperty(), 2.0),
                new KeyValue(shockwave.opacityProperty(), 0.3));
        KeyFrame sFade = new KeyFrame(Duration.millis(480),
                new KeyValue(shockwave.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(sAppear, sExpand, sFade);
        registerCleanup(timeline, shockwave);

        // Smoke debris from shockwave
        addSmokeParticles(x, y, intensity * 0.7, 240, timeline);
    }

    // =================================================================
    // Delayed retaliation burst – charge at user, then explosive burst
    // =================================================================

    private void addRetaliationBurst(double startX, double startY,
                                     double endX, double endY,
                                     double intensity, Timeline timeline) {
        // Phase 1: Brief dark aura charging at the attacker position
        Ellipse chargeAura = new Ellipse(16 + 8 * intensity, 20 + 10 * intensity);
        chargeAura.setFill(DARK_SHADOW.deriveColor(0, 1, 1, 0.5));
        chargeAura.setStroke(DARK_PURPLE.deriveColor(0, 1, 1, 0.6));
        chargeAura.setStrokeWidth(2);
        chargeAura.setEffect(new GaussianBlur(8 + 3 * intensity));
        chargeAura.setCenterX(startX);
        chargeAura.setCenterY(startY);
        chargeAura.setOpacity(0);
        prepareTransientNode(chargeAura);
        battleField.getChildren().add(chargeAura);

        // Charge pulses converging toward attacker
        int pulseCount = (int) (4 + 3 * intensity);
        for (int i = 0; i < pulseCount; i++) {
            double angle = (i / (double) pulseCount) * 2 * Math.PI;
            double orbitR = 30 + 12 * intensity;
            double px = startX + Math.cos(angle) * orbitR;
            double py = startY + Math.sin(angle) * orbitR;

            Circle pulse = new Circle(3 + random.nextDouble() * 3,
                    i % 2 == 0 ? DARK_PURPLE : DARK_CRIMSON);
            pulse.setEffect(new DropShadow(6, DARK_PURPLE));
            pulse.setCenterX(px);
            pulse.setCenterY(py);
            pulse.setOpacity(0);
            prepareTransientNode(pulse);
            battleField.getChildren().add(pulse);

            int delay = i * 20;
            KeyFrame pAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(pulse.opacityProperty(), 0.8));
            KeyFrame pConverge = new KeyFrame(Duration.millis(delay + 140),
                    new KeyValue(pulse.centerXProperty(), startX),
                    new KeyValue(pulse.centerYProperty(), startY),
                    new KeyValue(pulse.radiusProperty(), pulse.getRadius() * 0.4),
                    new KeyValue(pulse.opacityProperty(), 0.4));
            KeyFrame pFade = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(pulse.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(pAppear, pConverge, pFade);
            registerCleanup(timeline, pulse);
        }

        // Charge aura animation
        KeyFrame cAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(chargeAura.opacityProperty(), 0.7));
        KeyFrame cPulse = new KeyFrame(Duration.millis(120),
                new KeyValue(chargeAura.scaleXProperty(), 1.3),
                new KeyValue(chargeAura.scaleYProperty(), 1.3),
                new KeyValue(chargeAura.opacityProperty(), 0.9));
        KeyFrame cShrink = new KeyFrame(Duration.millis(180),
                new KeyValue(chargeAura.scaleXProperty(), 0.6),
                new KeyValue(chargeAura.scaleYProperty(), 0.6),
                new KeyValue(chargeAura.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(cAppear, cPulse, cShrink);
        registerCleanup(timeline, chargeAura);

        // Phase 2: Explosive dark energy burst at the target
        addExplosiveDarkBurst(endX, endY, intensity, 200, timeline);
    }

    /** Explosive dark energy burst – dark particles and flash at target. */
    private void addExplosiveDarkBurst(double x, double y, double intensity,
                                       int startDelay, Timeline timeline) {
        // Central explosion flash
        Circle explosion = new Circle(0, DARK_CRIMSON.deriveColor(0, 1, 1, 0.7));
        explosion.setCenterX(x);
        explosion.setCenterY(y);
        explosion.setEffect(new DropShadow(16 + 8 * intensity, DARK_RED));
        explosion.setOpacity(0);
        prepareTransientNode(explosion);
        battleField.getChildren().add(explosion);

        double burstRadius = 24 + 16 * intensity;
        KeyFrame eAppear = new KeyFrame(Duration.millis(startDelay),
                new KeyValue(explosion.opacityProperty(), 0.9));
        KeyFrame eExpand = new KeyFrame(Duration.millis(startDelay + 120),
                new KeyValue(explosion.radiusProperty(), burstRadius),
                new KeyValue(explosion.opacityProperty(), 0.6));
        KeyFrame eFade = new KeyFrame(Duration.millis(startDelay + 260),
                new KeyValue(explosion.radiusProperty(), burstRadius * 1.3),
                new KeyValue(explosion.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(eAppear, eExpand, eFade);
        registerCleanup(timeline, explosion);

        // Dark energy shards bursting outward
        int shardCount = (int) (8 + 6 * intensity);
        for (int i = 0; i < shardCount; i++) {
            Polygon shard = buildDarkShardPolygon(
                    8 + random.nextDouble() * 6 * intensity,
                    i % 3 == 0 ? DARK_PURPLE : i % 3 == 1 ? DARK_CRIMSON : DARK_BLACK);
            double angle = (i / (double) shardCount) * 2 * Math.PI;
            shard.setLayoutX(x);
            shard.setLayoutY(y);
            shard.setRotate(Math.toDegrees(angle));
            shard.setOpacity(0);
            shard.setEffect(new DropShadow(6, DARK_PURPLE));
            prepareTransientNode(shard);
            battleField.getChildren().add(shard);

            double shardR = 30 + 20 * intensity;
            int delay = startDelay + 40 + i * 18;
            KeyFrame sAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(shard.opacityProperty(), 0.9));
            KeyFrame sBurst = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(shard.layoutXProperty(), x + Math.cos(angle) * shardR),
                    new KeyValue(shard.layoutYProperty(), y + Math.sin(angle) * shardR),
                    new KeyValue(shard.scaleXProperty(), 1.3),
                    new KeyValue(shard.scaleYProperty(), 1.3));
            KeyFrame sFade = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(shard.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(sAppear, sBurst, sFade);
            registerCleanup(timeline, shard);
        }

        // Dark smoke aftermath
        addSmokeParticles(x, y, intensity * 0.6, startDelay + 80, timeline);
    }

    // =================================================================
    // Default dark burst – generic fallback
    // =================================================================

    private void addDefaultDarkBurst(double x, double y, double intensity,
                                     Timeline timeline) {
        int count = (int) (6 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Circle particle = new Circle(5 + random.nextDouble() * 5,
                    i % 3 == 0 ? DARK_PURPLE : i % 3 == 1 ? DARK_BLACK : DARK_CRIMSON);
            particle.setEffect(new DropShadow(8, DARK_SHADOW));
            double angle = (i / (double) count) * 2 * Math.PI;
            particle.setCenterX(x);
            particle.setCenterY(y);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);

            double burstR = 25 + 15 * intensity;
            int delay = i * 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(particle.opacityProperty(), 0.8));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(particle.centerXProperty(), x + Math.cos(angle) * burstR),
                    new KeyValue(particle.centerYProperty(), y + Math.sin(angle) * burstR),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst);
            registerCleanup(timeline, particle);
        }

        addDarkFlash(x, y, 20 + 10 * intensity, DARK_PURPLE, 0, 260, timeline);
    }

    // =================================================================
    // Shared helper – dark energy shard polygon
    // =================================================================

    /** Builds a jagged dark energy shard polygon scaled by size. */
    private Polygon buildDarkShardPolygon(double size, Color color) {
        double s = size / 10.0;
        Polygon shard = new Polygon();
        shard.getPoints().addAll(
                 0.0 * s, -14.0 * s,
                 5.0 * s,  -4.0 * s,
                 3.0 * s,   2.0 * s,
                 0.0 * s,  10.0 * s,
                -3.0 * s,   2.0 * s,
                -5.0 * s,  -4.0 * s
        );
        shard.setFill(color);
        shard.setStroke(DARK_PURPLE.deriveColor(0, 1, 1, 0.4));
        shard.setStrokeWidth(1);
        return shard;
    }

    // =================================================================
    // Shared helper – expanding dark flash circle
    // =================================================================

    private void addDarkFlash(double x, double y, double radius, Color color,
                              int startDelay, int fadeDuration,
                              Timeline timeline) {
        Circle flash = new Circle(0, color.deriveColor(0, 1, 1, 0.7));
        flash.setCenterX(x);
        flash.setCenterY(y);
        flash.setEffect(new GaussianBlur(radius * 0.35));
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
