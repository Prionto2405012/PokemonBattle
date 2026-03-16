// GhostEffects.java
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

public class GhostEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Ghost colour palette
    private static final Color GHOST_PURPLE   = Color.web("#7B1FA2");
    private static final Color GHOST_DARK     = Color.web("#4A148C");
    private static final Color GHOST_LAVENDER = Color.web("#CE93D8");
    private static final Color GHOST_WISP     = Color.web("#E1BEE7");
    private static final Color GHOST_SHADOW   = Color.web("#1A0033");
    private static final Color GHOST_GREEN    = Color.web("#69F0AE");
    private static final Color GHOST_BLUE     = Color.web("#B388FF");

    public GhostEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // Public API – single-point overload (melee / contact moves)

    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    // Public API – full signature (all ghost moves)

    public void createImpactEffect(double startX, double startY, double endX, double endY, String moveName, int movePower, Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.7, 2.4);

        switch (moveName) {
            // Phasing melee strikes with afterimage and ghost trail
            case "astonish"       -> addPhasingStrike(startX, startY, endX, endY, intensity, timeline);
            case "lick"           -> addPhasingStrike(startX, startY, endX, endY, intensity, timeline);
            case "phantom-force"  -> addPhasingStrike(startX, startY, endX, endY, intensity, timeline);
            case "shadow-claw"    -> addPhasingStrike(startX, startY, endX, endY, intensity, timeline);
            case "shadow-force"   -> addPhasingStrike(startX, startY, endX, endY, intensity, timeline);
            case "shadow-punch"   -> addPhasingStrike(startX, startY, endX, endY, intensity, timeline);
            case "shadow-sneak"   -> addPhasingStrike(startX, startY, endX, endY, intensity, timeline);

            // Eerie fade-in apparition plus ghostly impact pulse
            case "hex"            -> addEerieApparition(endX, endY, intensity, timeline);
            case "poltergeist"    -> addEerieApparition(endX, endY, intensity, timeline);
            case "rage-fist"      -> addEerieApparition(endX, endY, intensity, timeline);

            // Spiral haunted wind with translucent rings
            case "ominous-wind"   -> addHauntedWind(startX, startY, endX, endY, intensity, timeline);

            // Spectral orb projectile (also reachable via createRangedEffect)
            case "shadow-ball"    -> addSpectralOrb(startX, startY, endX, endY, intensity, timeline);

            default -> addDefaultGhostBurst(endX, endY, intensity, timeline);
        }
    }

    // Public API – ranged lead effect (projectile from attacker to target)

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        addSpectralOrb(startX, startY, endX, endY, intensity, timeline);
    }

    // Phasing melee strike – ghostly afterimages that fade through

    private void addPhasingStrike(double startX, double startY, double endX, double endY, double intensity, Timeline timeline) {
        int afterimageCount = (int) (10 + 4 * intensity);
        double dx = endX - startX;
        double dy = endY - startY;

        // Afterimage silhouettes phasing from attacker to defender
        for (int i = 0; i < afterimageCount; i++) {
            Ellipse afterimage = new Ellipse(20 + 6 * intensity, 28 + 8 * intensity);
            Color color = i % 3 == 0 ? GHOST_PURPLE : i % 3 == 1 ? GHOST_LAVENDER : GHOST_BLUE;
            afterimage.setFill(color.deriveColor(0, 1, 1, 0.55));
            afterimage.setStroke(GHOST_WISP.deriveColor(0, 1, 1, 0.3));
            afterimage.setStrokeWidth(4.5);
            afterimage.setEffect(new GaussianBlur(6 + 2 * intensity));

            double t = (i + 0.5) / afterimageCount;
            double px = startX + dx * t + (random.nextDouble() - 0.5) * 18;
            double py = startY + dy * t + (random.nextDouble() - 0.5) * 18;
            afterimage.setCenterX(px);
            afterimage.setCenterY(py);
            afterimage.setOpacity(0);
            prepareTransientNode(afterimage);
            battleField.getChildren().add(afterimage);

            int delay = i * 35;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(afterimage.opacityProperty(), 0.75));
            KeyFrame phase = new KeyFrame(Duration.millis(delay + 140),
                    new KeyValue(afterimage.centerXProperty(), px + dx * 0.12),
                    new KeyValue(afterimage.centerYProperty(), py + dy * 0.12),
                    new KeyValue(afterimage.scaleXProperty(), 1.3),
                    new KeyValue(afterimage.scaleYProperty(), 0.7));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(afterimage.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, phase, fade);
            registerCleanup(timeline, afterimage);
        }

        // Ghost trail wisps along the path
        addGhostTrail(startX, startY, endX, endY, intensity, timeline);

        // Impact flash at defender
        addGhostFlash(endX, endY, 22 + 12 * intensity, GHOST_GREEN, 80, 220, timeline);
    }

    // Spectral orb projectile – dark purple orb arcing with wispy trails

    private void addSpectralOrb(double startX, double startY, double endX, double endY, double intensity, Timeline timeline) {
        double orbRadius = 15 + 8 * intensity;

        // Main orb
        Circle orb = new Circle(orbRadius, GHOST_DARK);
        orb.setStroke(GHOST_PURPLE);
        orb.setStrokeWidth(8);
        orb.setEffect(new DropShadow(20 + 8 * intensity, GHOST_PURPLE));
        orb.setCenterX(startX);
        orb.setCenterY(startY);
        orb.setOpacity(0);
        prepareTransientNode(orb);
        battleField.getChildren().add(orb);

        // Inner glow
        Circle glow = new Circle(orbRadius * 0.75, GHOST_LAVENDER.deriveColor(0, 1, 1, 0.6));
        glow.setEffect(new GaussianBlur(6));
        glow.setCenterX(startX);
        glow.setCenterY(startY);
        glow.setOpacity(0);
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);

        // Halo ring
        Circle halo = new Circle(orbRadius * 3.5, Color.color(0.48, 0.12, 0.64, 0.18));
        halo.setStroke(GHOST_WISP.deriveColor(0, 1, 1, 0.25));
        halo.setStrokeWidth(8);
        halo.setEffect(new GaussianBlur(10));
        halo.setCenterX(startX);
        halo.setCenterY(startY);
        halo.setOpacity(0);
        prepareTransientNode(halo);
        battleField.getChildren().add(halo);

        // Arc path – slight upward arc
        double midX = (startX + endX) / 2.0;
        double midY = Math.min(startY, endY) - 55 - 15 * intensity;

        KeyFrame appear = new KeyFrame(Duration.millis(40),
                new KeyValue(orb.opacityProperty(), 1.0),
                new KeyValue(glow.opacityProperty(), 0.9),
                new KeyValue(halo.opacityProperty(), 0.85));
        KeyFrame arcMid = new KeyFrame(Duration.millis(160),
                new KeyValue(orb.centerXProperty(), midX),
                new KeyValue(orb.centerYProperty(), midY),
                new KeyValue(glow.centerXProperty(), midX),
                new KeyValue(glow.centerYProperty(), midY),
                new KeyValue(halo.centerXProperty(), midX),
                new KeyValue(halo.centerYProperty(), midY));
        KeyFrame impact = new KeyFrame(Duration.millis(300),
                new KeyValue(orb.centerXProperty(), endX),
                new KeyValue(orb.centerYProperty(), endY),
                new KeyValue(glow.centerXProperty(), endX),
                new KeyValue(glow.centerYProperty(), endY),
                new KeyValue(halo.centerXProperty(), endX),
                new KeyValue(halo.centerYProperty(), endY),
                new KeyValue(halo.radiusProperty(), halo.getRadius() * 2.0));
        KeyFrame burst = new KeyFrame(Duration.millis(380),
                new KeyValue(orb.opacityProperty(), 0),
                new KeyValue(glow.opacityProperty(), 0),
                new KeyValue(halo.opacityProperty(), 0),
                new KeyValue(orb.radiusProperty(), orbRadius * 0.5));

        timeline.getKeyFrames().addAll(appear, arcMid, impact, burst);
        registerCleanup(timeline, orb);
        registerCleanup(timeline, glow);
        registerCleanup(timeline, halo);

        // Trailing wisps behind the orb
        addOrbWisps(startX, startY, endX, endY, intensity, timeline);

        // Shadow burst at impact
        addShadowBurst(endX, endY, intensity, timeline);
    }

    /** Trailing wisp particles behind the spectral orb. */
    private void addOrbWisps(double startX, double startY, double endX, double endY, double intensity, Timeline timeline) {
        int wispCount = (int) (18 + 6 * intensity);
        double dx = endX - startX;
        double dy = endY - startY;

        for (int i = 0; i < wispCount; i++) {
            Circle wisp = new Circle(10 + random.nextDouble() * 4, i % 2 == 0 ? GHOST_WISP : GHOST_LAVENDER);
            wisp.setEffect(new GaussianBlur(4));

            double t = (i + random.nextDouble()) / wispCount * 0.85;
            double wx = startX + dx * t + (random.nextDouble() - 0.5) * 22;
            double wy = startY + dy * t + (random.nextDouble() - 0.5) * 22;
            wisp.setCenterX(wx);
            wisp.setCenterY(wy);
            wisp.setOpacity(0);
            prepareTransientNode(wisp);
            battleField.getChildren().add(wisp);

            int delay = (int) (t * 260) + 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(wisp.opacityProperty(), 0.7));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(wisp.centerYProperty(), wy - 12 - random.nextDouble() * 14),
                    new KeyValue(wisp.radiusProperty(), wisp.getRadius() * 1.6),
                    new KeyValue(wisp.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, wisp);
        }
    }

    /** Dark particles bursting on impact for the spectral orb. */
    private void addShadowBurst(double x, double y, double intensity, Timeline timeline) {
        int count = (int) (16 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Circle particle = new Circle(8 + random.nextDouble() * 4, i % 2 == 0 ? GHOST_SHADOW : GHOST_DARK);
            particle.setEffect(new DropShadow(6, GHOST_PURPLE));
            double angle = (i / (double) count) * 2 * Math.PI;
            particle.setCenterX(x);
            particle.setCenterY(y);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);

            double burstR = 30 + 18 * intensity;
            int delay = 280 + i * 15;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(particle.opacityProperty(), 0.85));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(particle.centerXProperty(), x + Math.cos(angle) * burstR),
                    new KeyValue(particle.centerYProperty(), y + Math.sin(angle) * burstR),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand);
            registerCleanup(timeline, particle);
        }
    }

    // Eerie apparition – translucent ghost shapes materializing at defender

    private void addEerieApparition(double x, double y, double intensity, Timeline timeline) {
        // Ghostly silhouette polygon (rough spectre shape)
        Polygon spectre = buildSpectrePolygon(intensity);
        spectre.setFill(GHOST_PURPLE.deriveColor(0, 1, 1, 0.35));
        spectre.setStroke(GHOST_LAVENDER.deriveColor(0, 1, 1, 0.45));
        spectre.setStrokeWidth(8);
        spectre.setEffect(new GaussianBlur(8 + 3 * intensity));
        spectre.setLayoutX(x);
        spectre.setLayoutY(y + 20);
        spectre.setOpacity(0);
        spectre.setScaleX(0.5);
        spectre.setScaleY(0.5);
        prepareTransientNode(spectre);
        battleField.getChildren().add(spectre);

        // Spectre fades in, rises, then pulses out
        KeyFrame materialize = new KeyFrame(Duration.millis(60),
                new KeyValue(spectre.opacityProperty(), 0.7),
                new KeyValue(spectre.scaleXProperty(), 1.0),
                new KeyValue(spectre.scaleYProperty(), 1.0));
        KeyFrame rise = new KeyFrame(Duration.millis(200),
                new KeyValue(spectre.layoutYProperty(), y - 10),
                new KeyValue(spectre.opacityProperty(), 0.85));
        KeyFrame pulse = new KeyFrame(Duration.millis(300),
                new KeyValue(spectre.scaleXProperty(), 1.25),
                new KeyValue(spectre.scaleYProperty(), 1.25));
        KeyFrame vanish = new KeyFrame(Duration.millis(420),
                new KeyValue(spectre.opacityProperty(), 0),
                new KeyValue(spectre.scaleXProperty(), 1.5),
                new KeyValue(spectre.scaleYProperty(), 0.3));

        timeline.getKeyFrames().addAll(materialize, rise, pulse, vanish);
        registerCleanup(timeline, spectre);

        // Ghostly impact pulse rings
        addImpactPulse(x, y, intensity, timeline);

        // Floating wisp particles around the spectre
        addApparitionWisps(x, y, intensity, timeline);
    }

    /** Concentric ghostly pulse rings expanding from the impact point. */
    private void addImpactPulse(double x, double y, double intensity, Timeline timeline) {
        int ringCount = (int) (7 + 2 * intensity);
        for (int i = 0; i < ringCount; i++) {
            Circle ring = new Circle(10 + random.nextDouble() * 20, Color.TRANSPARENT);
            ring.setStroke(i % 2 == 0 ? GHOST_LAVENDER : GHOST_GREEN);
            ring.setStrokeWidth(5.5);
            ring.setCenterX(x);
            ring.setCenterY(y);
            ring.setOpacity(0);
            ring.setEffect(new GaussianBlur(3));
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            double maxRadius = 40 + 20 * intensity;
            int delay = 180 + i * 70;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ring.opacityProperty(), 0.8));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(ring.radiusProperty(), maxRadius),
                    new KeyValue(ring.strokeWidthProperty(), 0.5),
                    new KeyValue(ring.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand);
            registerCleanup(timeline, ring);
        }
    }

    /** Wisps floating around the apparition site. */
    private void addApparitionWisps(double x, double y, double intensity, Timeline timeline) {
        int count = (int) (15 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            Circle wisp = new Circle(8 + random.nextDouble() * 3,
                    i % 3 == 0 ? GHOST_WISP : i % 3 == 1 ? GHOST_GREEN : GHOST_BLUE);
            wisp.setEffect(new GaussianBlur(4));

            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 17 + random.nextDouble() * 20;
            wisp.setCenterX(x + Math.cos(angle) * radius);
            wisp.setCenterY(y + Math.sin(angle) * radius);
            wisp.setOpacity(0);
            prepareTransientNode(wisp);
            battleField.getChildren().add(wisp);

            int delay = 40 + i * 35;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(wisp.opacityProperty(), 0.7));
            KeyFrame driftAway = new KeyFrame(Duration.millis(delay + 250),
                    new KeyValue(wisp.centerYProperty(), wisp.getCenterY() - 18 - random.nextDouble() * 12),
                    new KeyValue(wisp.centerXProperty(), wisp.getCenterX() + (random.nextDouble() - 0.5) * 20),
                    new KeyValue(wisp.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, driftAway);
            registerCleanup(timeline, wisp);
        }
    }

    // Haunted wind – spiral translucent rings with ghostly particles

    private void addHauntedWind(double startX, double startY, double endX, double endY, double intensity, Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        double corridor = Math.max(140.0, safeBattleHeight() * 0.8);

        // Spiral translucent rings moving from attacker to defender
        int ringCount = (int) (16 + 5 * intensity);
        for (int i = 0; i < ringCount; i++) {
            Ellipse ring = new Ellipse(22 + 8 * intensity, 18 + 4 * intensity);
            ring.setFill(Color.TRANSPARENT);
            Color ringColor = i % 3 == 0 ? GHOST_PURPLE : i % 3 == 1 ? GHOST_LAVENDER : GHOST_BLUE;
            ring.setStroke(ringColor.deriveColor(0, 1, 1, 0.5));
            ring.setStrokeWidth(5.5);
            ring.setEffect(new GaussianBlur(4));

            double progress = (i + random.nextDouble()) / ringCount;
            double spiralOffset = Math.sin(progress * 4 * Math.PI) * corridor * 0.3;
            double sx = startX + ux * distance * progress + px * spiralOffset;
            double sy = startY + uy * distance * progress + py * spiralOffset;
            ring.setCenterX(sx);
            ring.setCenterY(sy);
            ring.setRotate(progress * 360);
            ring.setOpacity(0);
            prepareTransientNode(ring);
            battleField.getChildren().add(ring);

            double travel = Math.min(distance * 0.35, 160.0);
            int delay = i * 40;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(ring.opacityProperty(), 0.7));
            KeyFrame move = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(ring.centerXProperty(), sx + ux * travel),
                    new KeyValue(ring.centerYProperty(), sy + uy * travel),
                    new KeyValue(ring.rotateProperty(), ring.getRotate() + 180),
                    new KeyValue(ring.radiusXProperty(), ring.getRadiusX() * 1.5),
                    new KeyValue(ring.radiusYProperty(), ring.getRadiusY() * 1.5),
                    new KeyValue(ring.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, move);
            registerCleanup(timeline, ring);
        }

        // Wind lines with ghostly coloring
        int lineCount = (int) (20 + 6 * intensity);
        for (int i = 0; i < lineCount; i++) {
            Line windLine = new Line();
            Color lineColor = i % 2 == 0 ? GHOST_WISP : GHOST_LAVENDER;
            windLine.setStroke(lineColor.deriveColor(0, 1, 1, 0.4));
            windLine.setStrokeWidth(15 + 3 * intensity);
            windLine.setOpacity(0);
            windLine.setEffect(new GaussianBlur(3));

            double lane = (i + random.nextDouble()) / lineCount;
            double spreadOffset = (random.nextDouble() - 0.5) * corridor;
            double segLen = Math.min(Math.max(100.0, distance * 0.6), distance + 86.0);
            double jitter = (random.nextDouble() - 0.5) * 18.0;

            double sx = startX + ux * distance * lane + px * spreadOffset;
            double sy = startY + uy * distance * lane + py * spreadOffset;
            windLine.setStartX(sx);
            windLine.setStartY(sy);
            windLine.setEndX(sx + ux * segLen + px * jitter);
            windLine.setEndY(sy + uy * segLen + py * jitter);
            prepareTransientNode(windLine);
            battleField.getChildren().add(windLine);

            double targetSpread = (random.nextDouble() - 0.5) * corridor;
            double tx = startX + ux * distance * Math.min(1.0, lane + 0.3) + px * targetSpread;
            double ty = startY + uy * distance * Math.min(1.0, lane + 0.3) + py * targetSpread;

            int delay = i * 25;
            KeyFrame appear = new KeyFrame(Duration.millis(delay + 50),
                    new KeyValue(windLine.opacityProperty(), 0.55));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(windLine.startXProperty(), tx),
                    new KeyValue(windLine.startYProperty(), ty),
                    new KeyValue(windLine.endXProperty(), tx + ux * segLen + px * jitter),
                    new KeyValue(windLine.endYProperty(), ty + uy * segLen + py * jitter),
                    new KeyValue(windLine.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, windLine);
        }

        // Ghostly particles swirling in the wind
        addWindParticles(startX, startY, endX, endY, intensity, timeline);
    }

    /** Ghostly particles carried by the haunted wind. */
    private void addWindParticles(double startX, double startY, double endX, double endY, double intensity, Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        int count = (int) (20 + 8 * intensity);
        for (int i = 0; i < count; i++) {
            Circle particle = new Circle(8 + random.nextDouble() * 3,
                    i % 3 == 0 ? GHOST_GREEN : i % 3 == 1 ? GHOST_WISP : GHOST_BLUE);
            particle.setEffect(new GaussianBlur(3));

            double progress = (i + random.nextDouble()) / count;
            double lateral = (random.nextDouble() - 0.5) * Math.max(100.0, safeBattleHeight() * 0.6);
            particle.setCenterX(startX + ux * distance * progress + px * lateral);
            particle.setCenterY(startY + uy * distance * progress + py * lateral);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);

            double travel = Math.min(distance * 0.4, 180.0);
            double drift = (random.nextDouble() - 0.5) * 40.0;
            int delay = i * 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(particle.opacityProperty(), 0.65));
            KeyFrame move = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(particle.centerXProperty(),
                            particle.getCenterX() + ux * travel + px * drift),
                    new KeyValue(particle.centerYProperty(),
                            particle.getCenterY() + uy * travel + py * drift),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, move);
            registerCleanup(timeline, particle);
        }
    }

    // Ghost trail – dark smoky wisps trailing along a path

    private void addGhostTrail(double startX, double startY, double endX, double endY, double intensity, Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        int trailCount = (int) (16 + 5 * intensity);

        for (int i = 0; i < trailCount; i++) {
            Rectangle smoke = new Rectangle(12 + random.nextDouble() * 6,
                    10 + random.nextDouble() * 5);
            smoke.setArcWidth(7);
            smoke.setArcHeight(7);
            Color color = i % 2 == 0 ? GHOST_SHADOW : GHOST_DARK;
            smoke.setFill(color.deriveColor(0, 1, 1, 0.45));
            smoke.setEffect(new GaussianBlur(5));

            double t = (i + random.nextDouble()) / trailCount;
            smoke.setX(startX + dx * t + (random.nextDouble() - 0.5) * 14);
            smoke.setY(startY + dy * t + (random.nextDouble() - 0.5) * 14);
            smoke.setOpacity(0);
            smoke.setRotate(random.nextDouble() * 360);
            prepareTransientNode(smoke);
            battleField.getChildren().add(smoke);

            int delay = (int) (t * 220);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(smoke.opacityProperty(), 0.6));
            KeyFrame dissipate = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(smoke.yProperty(), smoke.getY() - 10 - random.nextDouble() * 12),
                    new KeyValue(smoke.scaleXProperty(), 1.8),
                    new KeyValue(smoke.scaleYProperty(), 1.8),
                    new KeyValue(smoke.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, dissipate);
            registerCleanup(timeline, smoke);
        }
    }

    // Default ghost burst – generic fallback

    private void addDefaultGhostBurst(double x, double y, double intensity, Timeline timeline) {
        int count = (int) (18 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Circle particle = new Circle(8 + random.nextDouble() * 3,
                    i % 3 == 0 ? GHOST_PURPLE : i % 3 == 1 ? GHOST_LAVENDER : GHOST_DARK);
            particle.setEffect(new DropShadow(8, GHOST_PURPLE));
            double angle = (i / (double) count) * 2 * Math.PI;
            particle.setCenterX(x);
            particle.setCenterY(y);
            particle.setOpacity(0);
            prepareTransientNode(particle);
            battleField.getChildren().add(particle);

            double burstR = 30 + 15 * intensity;
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

        addGhostFlash(x, y, 27 + 10 * intensity, GHOST_PURPLE, 0, 260, timeline);
    }

    // Shared helper – spectre polygon shape

    /** Builds a rough ghost/spectre silhouette polygon scaled by intensity. */
    private Polygon buildSpectrePolygon(double intensity) {
        double s = 0.8 + 0.6 * intensity;
        Polygon spectre = new Polygon();
        spectre.getPoints().addAll(
                0.0 * s,    0.0 * s,
               -16.0 * s,  -8.0 * s,
               -22.0 * s, -28.0 * s,
               -14.0 * s, -42.0 * s,
                0.0 * s,  -50.0 * s,
                14.0 * s, -42.0 * s,
                22.0 * s, -28.0 * s,
                16.0 * s,  -8.0 * s,
                10.0 * s,   4.0 * s,
                 0.0 * s,  10.0 * s,
               -10.0 * s,   4.0 * s
        );
        return spectre;
    }

    // Shared helper – expanding flash circle

    private void addGhostFlash(double x, double y, double radius, Color color, int startDelay, int fadeDuration, Timeline timeline) {
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

    private double safeBattleHeight() {
        double h = battleField.getHeight();
        return h > 0 ? h : 700.0;
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
