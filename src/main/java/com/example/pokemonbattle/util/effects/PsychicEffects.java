// PsychicEffects.java
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

public class PsychicEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Psychic colour palette
    private static final Color PSY_PINK   = Color.web("#F06292");
    private static final Color PSY_PURPLE = Color.web("#AB47BC");
    private static final Color PSY_DEEP   = Color.web("#7B1FA2");
    private static final Color PSY_LIGHT  = Color.web("#F8BBD0");
    private static final Color PSY_CYAN   = Color.web("#80CBC4");
    private static final Color PSY_WHITE  = Color.WHITE;
    private static final Color PSY_INDIGO = Color.web("#5C6BC0");

    public PsychicEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // Public API – single-point overload (melee / contact moves)

    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    // Public API – full signature (all psychic moves)

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.8, 2.4);

        switch (moveName) {
            // Concentric psychic rings
            case "confusion"        -> addConcentricRings(endX, endY, intensity, timeline);
            case "expanding-force"  -> addConcentricRings(endX, endY, intensity, timeline);
            case "extrasensory"     -> addConcentricRings(endX, endY, intensity, timeline);
            case "luster-purge"     -> addConcentricRings(endX, endY, intensity, timeline);
            case "mist-ball"        -> addConcentricRings(endX, endY, intensity, timeline);
            case "mystical-power"   -> addConcentricRings(endX, endY, intensity, timeline);
            case "psychic"          -> addConcentricRings(endX, endY, intensity, timeline);
            case "psychic-noise"    -> addConcentricRings(endX, endY, intensity, timeline);
            case "psycho-boost"     -> addConcentricRings(endX, endY, intensity, timeline);
            case "psyshock"         -> addConcentricRings(endX, endY, intensity, timeline);
            case "synchronoise"     -> addConcentricRings(endX, endY, intensity, timeline);

            // Straight psionic beam
            case "psybeam"          -> addPsionicBeam(startX, startY, endX, endY, intensity, timeline);
            case "twin-beam"        -> addPsionicBeam(startX, startY, endX, endY, intensity, timeline);

            // Melee telekinetic overlay
            case "heart-stamp"      -> addTelekineticSlash(endX, endY, intensity, timeline);
            case "psychic-fangs"    -> addTelekineticSlash(endX, endY, intensity, timeline);
            case "psycho-cut"       -> addTelekineticSlash(endX, endY, intensity, timeline);
            case "psyshield-bash"   -> addTelekineticSlash(endX, endY, intensity, timeline);
            case "psystrike"        -> addTelekineticSlash(endX, endY, intensity, timeline);
            case "zen-headbutt"     -> addTelekineticSlash(endX, endY, intensity, timeline);

            // Charge-up aura burst
            case "dream-eater"      -> addAuraBurst(startX, startY, endX, endY, intensity, timeline);
            case "future-sight"     -> addAuraBurst(startX, startY, endX, endY, intensity, timeline);
            case "lunar-blessing"   -> addAuraBurst(startX, startY, endX, endY, intensity, timeline);
            case "stored-power"     -> addAuraBurst(startX, startY, endX, endY, intensity, timeline);
            case "take-heart"       -> addAuraBurst(startX, startY, endX, endY, intensity, timeline);

            default                 -> addDefaultPsychicBurst(endX, endY, intensity, timeline);
        }
    }

    // Public API – ranged lead effect (projectile from attacker to target)

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        addPsionicBeam(startX, startY, endX, endY, intensity, timeline);
    }

    // Concentric psychic rings – expanding rings of pink/purple energy with mindwave distortion particles

    private void addConcentricRings(double x, double y, double intensity, Timeline timeline) {
        int ringCount = (int) (14 + 4 * intensity);

        for (int i = 0; i < ringCount; i++) {
            double maxRadius = 30 + 30 * intensity;
            Circle ring = new Circle(0);
            Color color = i % 3 == 0 ? PSY_PINK : i % 3 == 1 ? PSY_PURPLE : PSY_LIGHT;
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(color.deriveColor(0, 1, 1, 0.75));
            ring.setStrokeWidth(4.5 + intensity);
            ring.setCenterX(x);
            ring.setCenterY(y);
            ring.setOpacity(0);
            ring.setEffect(new GaussianBlur(4 + 2 * intensity));
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            int delay = i * 55;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ring.opacityProperty(), 0.9));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(ring.radiusProperty(), maxRadius * (0.5 + 0.5 * ((i + 1.0) / ringCount))),
                    new KeyValue(ring.opacityProperty(), 0.6),
                    new KeyValue(ring.strokeWidthProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 400),
                    new KeyValue(ring.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand, fade);
            registerCleanup(timeline, ring);
        }

        // Mindwave distortion particles – small warping ellipses drifting outward
        int particleCount = (int) (16 + 5 * intensity);
        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * 2 * Math.PI + random.nextDouble() * 0.4;
            double dist = 10 + random.nextDouble() * 15;
            double endDist = 35 + 25 * intensity + random.nextDouble() * 15;

            Ellipse mote = new Ellipse(10 + random.nextDouble() * 3, 15 + random.nextDouble() * 4);
            Color moteColor = i % 2 == 0 ? PSY_CYAN : PSY_INDIGO;
            mote.setFill(moteColor.deriveColor(0, 1, 1, 0.6));
            mote.setStroke(null);
            mote.setRotate(Math.toDegrees(angle));
            mote.setCenterX(x + Math.cos(angle) * dist);
            mote.setCenterY(y + Math.sin(angle) * dist);
            mote.setOpacity(0);
            mote.setEffect(new GaussianBlur(3));
            prepareTransientNode(mote);
            battleField.getChildren().add(mote);

            int delay = 40 + i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(mote.opacityProperty(), 0.8));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(mote.centerXProperty(), x + Math.cos(angle) * endDist),
                    new KeyValue(mote.centerYProperty(), y + Math.sin(angle) * endDist),
                    new KeyValue(mote.scaleXProperty(), 1.6),
                    new KeyValue(mote.scaleYProperty(), 0.5));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 380),
                    new KeyValue(mote.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift, fade);
            registerCleanup(timeline, mote);
        }

        // Central pulse flash
        addPsychicFlash(x, y, 30 * intensity, PSY_PINK, 0, 220, timeline);
    }

    // Psionic beam – chromatic rainbow-tinted beam from attacker to defender with prismatic trail particles

    private void addPsionicBeam(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;
        double distance = Math.hypot(dx, dy);
        if (distance < 1) return;
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        // Core beam lines – overlapping coloured lines forming a chromatic band
        Color[] beamColors = { PSY_PINK, PSY_PURPLE, PSY_CYAN, PSY_INDIGO, PSY_LIGHT };
        for (int b = 0; b < beamColors.length; b++) {
            double offset = (b - 2) * (2.5 + intensity);
            Line beam = new Line(
                    sx + px * offset, sy + py * offset,
                    sx + px * offset, sy + py * offset);
            beam.setStroke(beamColors[b].deriveColor(0, 1, 1, 0.7));
            beam.setStrokeWidth(5 + 2 * intensity);
            beam.setOpacity(0);
            beam.setEffect(new GaussianBlur(4 + intensity));
            prepareTransientNode(beam);
            battleField.getChildren().add(beam);

            KeyFrame appear = new KeyFrame(Duration.millis(0),
                    new KeyValue(beam.opacityProperty(), 0.85));
            KeyFrame extend = new KeyFrame(Duration.millis(220),
                    new KeyValue(beam.endXProperty(), ex + px * offset),
                    new KeyValue(beam.endYProperty(), ey + py * offset));
            KeyFrame hold = new KeyFrame(Duration.millis(320),
                    new KeyValue(beam.opacityProperty(), 0.7));
            KeyFrame fade = new KeyFrame(Duration.millis(420),
                    new KeyValue(beam.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, extend, hold, fade);
            registerCleanup(timeline, beam);
        }

        // Prismatic trail particles along the beam path
        int trailCount = (int) (18 + 6 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double spread = (random.nextDouble() - 0.5) * (12 + 8 * intensity);
            double cx = sx + dx * t + px * spread;
            double cy = sy + dy * t + py * spread;

            Circle sparkle = new Circle(10 + random.nextDouble() * 2.5, PSY_WHITE);
            sparkle.setCenterX(cx);
            sparkle.setCenterY(cy);
            sparkle.setOpacity(0);
            sparkle.setEffect(new DropShadow(6, beamColors[i % beamColors.length]));
            prepareTransientNode(sparkle);
            battleField.getChildren().add(sparkle);

            int delay = (int) (t * 220);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(sparkle.opacityProperty(), 0.95));
            KeyFrame shimmer = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(sparkle.scaleXProperty(), 1.8),
                    new KeyValue(sparkle.scaleYProperty(), 1.8),
                    new KeyValue(sparkle.opacityProperty(), 0.5));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(sparkle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, shimmer, fade);
            registerCleanup(timeline, sparkle);
        }

        // Impact flash at target
        addPsychicFlash(ex, ey, 25 * intensity, PSY_PURPLE, 230, 180, timeline);
    }

    // Telekinetic slash – purple slash marks and energy edges at impact

    private void addTelekineticSlash(double x, double y, double intensity, Timeline timeline) {
        int slashCount = (int) (13 + 3 * intensity);

        for (int i = 0; i < slashCount; i++) {
            double angle = random.nextDouble() * Math.PI - Math.PI / 2;
            double len = 22 + 14 * intensity;

            // Slash line – a bold stroke cutting across the impact area
            Line slash = new Line(
                    x - Math.cos(angle) * len, y - Math.sin(angle) * len,
                    x - Math.cos(angle) * len, y - Math.sin(angle) * len);
            Color slashColor = i % 2 == 0 ? PSY_PURPLE : PSY_DEEP;
            slash.setStroke(slashColor.deriveColor(0, 1, 1, 0.85));
            slash.setStrokeWidth(5 + 2 * intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(8, PSY_PINK));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = i * 50;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(slash.opacityProperty(), 0.95));
            KeyFrame extend = new KeyFrame(Duration.millis(delay + 120),
                    new KeyValue(slash.endXProperty(), x + Math.cos(angle) * len),
                    new KeyValue(slash.endYProperty(), y + Math.sin(angle) * len));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(slash.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, extend, fade);
            registerCleanup(timeline, slash);
        }

        // Energy edge fragments – small diamond-shaped polygons bursting outward
        int edgeCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < edgeCount; i++) {
            double angle = (i / (double) edgeCount) * 2 * Math.PI;
            double size = 10 + random.nextDouble() * 4 * intensity;

            Polygon diamond = buildDiamondPolygon(size,
                    i % 3 == 0 ? PSY_PURPLE : i % 3 == 1 ? PSY_PINK : PSY_INDIGO);
            diamond.setLayoutX(x);
            diamond.setLayoutY(y);
            diamond.setRotate(Math.toDegrees(angle));
            diamond.setOpacity(0);
            diamond.setEffect(new DropShadow(5, PSY_DEEP));
            prepareTransientNode(diamond);
            battleField.getChildren().add(diamond);

            double burstR = 28 + 18 * intensity;
            int delay = 30 + i * 25;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(diamond.opacityProperty(), 0.9));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(diamond.layoutXProperty(), x + Math.cos(angle) * burstR),
                    new KeyValue(diamond.layoutYProperty(), y + Math.sin(angle) * burstR),
                    new KeyValue(diamond.rotateProperty(), diamond.getRotate() + 90));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(diamond.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, diamond);
        }

        // Purple impact ripple
        addPsychicFlash(x, y, 22 * intensity, PSY_DEEP, 0, 200, timeline);
    }

    // Aura burst – charging glow at caster position, then delayed burst at the target

    private void addAuraBurst(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        // Phase 1: Charge-up aura at caster position
        int chargeCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < chargeCount; i++) {
            double angle = (i / (double) chargeCount) * 2 * Math.PI;
            double orbitR = 25 + 12 * intensity;

            Circle aura = new Circle(10 + random.nextDouble() * 3 * intensity);
            Color auraColor = i % 3 == 0 ? PSY_PINK : i % 3 == 1 ? PSY_LIGHT : PSY_CYAN;
            aura.setFill(auraColor.deriveColor(0, 1, 1, 0.65));
            aura.setCenterX(sx + Math.cos(angle) * orbitR);
            aura.setCenterY(sy + Math.sin(angle) * orbitR);
            aura.setOpacity(0);
            aura.setEffect(new GaussianBlur(5));
            prepareTransientNode(aura);
            battleField.getChildren().add(aura);

            int delay = i * 30;
            // Converge toward caster centre to represent energy gathering
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(aura.opacityProperty(), 0.85));
            KeyFrame converge = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(aura.centerXProperty(), sx + Math.cos(angle) * 4),
                    new KeyValue(aura.centerYProperty(), sy + Math.sin(angle) * 4),
                    new KeyValue(aura.scaleXProperty(), 1.4),
                    new KeyValue(aura.scaleYProperty(), 1.4));
            KeyFrame collapse = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(aura.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, converge, collapse);
            registerCleanup(timeline, aura);
        }

        // Central charge glow at caster
        Circle chargeGlow = new Circle(0, PSY_PINK.deriveColor(0, 1, 1, 0.6));
        chargeGlow.setCenterX(sx);
        chargeGlow.setCenterY(sy);
        chargeGlow.setOpacity(0);
        chargeGlow.setEffect(new GaussianBlur(10 + 4 * intensity));
        prepareTransientNode(chargeGlow);
        battleField.getChildren().add(chargeGlow);

        KeyFrame chargeStart = new KeyFrame(Duration.millis(0),
                new KeyValue(chargeGlow.opacityProperty(), 0.7),
                new KeyValue(chargeGlow.radiusProperty(), 6.0));
        KeyFrame chargePeak = new KeyFrame(Duration.millis(280),
                new KeyValue(chargeGlow.radiusProperty(), 18.0 * intensity),
                new KeyValue(chargeGlow.opacityProperty(), 0.9));
        KeyFrame chargeRelease = new KeyFrame(Duration.millis(340),
                new KeyValue(chargeGlow.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(chargeStart, chargePeak, chargeRelease);
        registerCleanup(timeline, chargeGlow);

        // Phase 2: Delayed burst at target
        int burstDelay = 320;

        // Travelling orb from caster to target
        Circle orb = new Circle(12 + 4 * intensity, PSY_PURPLE.deriveColor(0, 1, 1, 0.8));
        orb.setCenterX(sx);
        orb.setCenterY(sy);
        orb.setOpacity(0);
        orb.setEffect(new DropShadow(14, PSY_PINK));
        prepareTransientNode(orb);
        battleField.getChildren().add(orb);

        KeyFrame orbAppear = new KeyFrame(Duration.millis(burstDelay),
                new KeyValue(orb.opacityProperty(), 0.95));
        KeyFrame orbTravel = new KeyFrame(Duration.millis(burstDelay + 200),
                new KeyValue(orb.centerXProperty(), ex),
                new KeyValue(orb.centerYProperty(), ey));
        KeyFrame orbFade = new KeyFrame(Duration.millis(burstDelay + 260),
                new KeyValue(orb.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(orbAppear, orbTravel, orbFade);
        registerCleanup(timeline, orb);

        // Burst particles at target upon arrival
        int burstCount = (int) (16 + 5 * intensity);
        for (int i = 0; i < burstCount; i++) {
            double angle = (i / (double) burstCount) * 2 * Math.PI;
            double burstR = 28 + 20 * intensity;

            Circle particle = new Circle(10 + random.nextDouble() * 3);
            Color pColor = i % 3 == 0 ? PSY_PINK : i % 3 == 1 ? PSY_PURPLE : PSY_CYAN;
            particle.setFill(pColor.deriveColor(0, 1, 1, 0.7));
            particle.setCenterX(ex);
            particle.setCenterY(ey);
            particle.setOpacity(0);
            particle.setEffect(new GaussianBlur(3));
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);

            int pDelay = burstDelay + 200 + i * 20;
            KeyFrame pAppear = new KeyFrame(Duration.millis(pDelay),
                    new KeyValue(particle.opacityProperty(), 0.85));
            KeyFrame pBurst = new KeyFrame(Duration.millis(pDelay + 200),
                    new KeyValue(particle.centerXProperty(), ex + Math.cos(angle) * burstR),
                    new KeyValue(particle.centerYProperty(), ey + Math.sin(angle) * burstR),
                    new KeyValue(particle.scaleXProperty(), 0.4),
                    new KeyValue(particle.scaleYProperty(), 0.4));
            KeyFrame pFade = new KeyFrame(Duration.millis(pDelay + 320),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(pAppear, pBurst, pFade);
            registerCleanup(timeline, particle);
        }

        // Impact flash at target
        addPsychicFlash(ex, ey, 28 * intensity, PSY_PURPLE, burstDelay + 200, 200, timeline);
    }

    // Default psychic burst – generic fallback with expanding rings and scattering motes

    private void addDefaultPsychicBurst(double x, double y, double intensity, Timeline timeline) {
        // Two concentric pulse rings
        for (int r = 0; r < 2; r++) {
            double maxR = (22 + 16 * intensity) * (r + 1);
            Circle ring = new Circle(0);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(r == 0 ? PSY_PINK.deriveColor(0, 1, 1, 0.7)
                                  : PSY_PURPLE.deriveColor(0, 1, 1, 0.6));
            ring.setStrokeWidth(5 + intensity);
            ring.setCenterX(x);
            ring.setCenterY(y);
            ring.setOpacity(0);
            ring.setEffect(new GaussianBlur(5));
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            int delay = r * 70;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ring.opacityProperty(), 0.8));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 250),
                    new KeyValue(ring.radiusProperty(), maxR),
                    new KeyValue(ring.opacityProperty(), 0.3));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 360),
                    new KeyValue(ring.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand, fade);
            registerCleanup(timeline, ring);
        }

        // Scattering motes
        int moteCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < moteCount; i++) {
            double angle = (i / (double) moteCount) * 2 * Math.PI;
            Circle mote = new Circle(8 + random.nextDouble() * 2,
                    i % 2 == 0 ? PSY_LIGHT : PSY_CYAN);
            mote.setCenterX(x);
            mote.setCenterY(y);
            mote.setOpacity(0);
            mote.setEffect(new GaussianBlur(2));
            prepareTransientNode(mote);
            battleField.getChildren().add(mote);

            double drift = 18 + 14 * intensity;
            int delay = 20 + i * 25;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(mote.opacityProperty(), 0.8));
            KeyFrame move = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(mote.centerXProperty(), x + Math.cos(angle) * drift),
                    new KeyValue(mote.centerYProperty(), y + Math.sin(angle) * drift));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(mote.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, move, fade);
            registerCleanup(timeline, mote);
        }

        addPsychicFlash(x, y, 20 * intensity, PSY_PINK, 0, 180, timeline);
    }

    // Shape builder – diamond polygon for energy edge fragments

    private Polygon buildDiamondPolygon(double size, Color fill) {
        Polygon diamond = new Polygon(
                0, -size,
                size * 0.75, 0,
                0, size,
                -size * 0.75, 0);
        diamond.setFill(fill);
        diamond.setStroke(PSY_LIGHT.deriveColor(0, 1, 1, 0.5));
        diamond.setStrokeWidth(4);
        return diamond;
    }

    // Shared helper – expanding psychic flash circle

    private void addPsychicFlash(double x, double y, double radius, Color color, int startDelay, int fadeDuration, Timeline timeline) {
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
