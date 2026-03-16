// PoisonEffects.java
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class PoisonEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Poison colour palette
    private static final Color POISON_PURPLE   = Color.web("#7B1FA2");
    private static final Color POISON_VIOLET   = Color.web("#AB47BC");
    private static final Color POISON_DARK     = Color.web("#4A148C");
    private static final Color POISON_GREEN    = Color.web("#76FF03");
    private static final Color POISON_SLUDGE   = Color.web("#558B2F");
    private static final Color POISON_ACID     = Color.web("#C6FF00");
    private static final Color POISON_TOXIC    = Color.web("#AEEA00");
    private static final Color POISON_GREY     = Color.web("#757575");
        private static final String FANG_ASSET     = "fang.gif";

    public PoisonEffects(Pane battleField) {
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
    // Public API – full signature (all poison moves)
    // -----------------------------------------------------------------

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.4, 1.8);

        switch (moveName) {
            // Venom-sting melee jabs
            case "poison-sting"   -> addVenomStrike(startX, startY, endX, endY, intensity, timeline);
                        case "poison-fang"    -> {
                                addFangImage(endX, endY, timeline);
                                addVenomStrike(startX, startY, endX, endY, intensity, timeline);
                        }
            case "poison-tail"    -> addVenomStrike(startX, startY, endX, endY, intensity * 0.9, timeline);
            case "barb-barrage"   -> addBarbBarrage(startX, startY, endX, endY, intensity, timeline);

            // Toxic jab melee
            case "poison-jab"     -> addToxicJab(endX, endY, intensity, timeline);
            case "cross-poison"   -> addToxicJab(endX, endY, intensity, timeline);

            // Sludge splatter (ranged)
            case "sludge"         -> addSludgeSplatter(startX, startY, endX, endY, intensity, timeline);
            case "sludge-bomb"    -> addSludgeBomb(startX, startY, endX, endY, intensity, timeline);
            case "sludge-wave"    -> addSludgeWave(endX, endY, intensity, timeline);
            case "gunk-shot"      -> addSludgeBomb(startX, startY, endX, endY, intensity * 1.2, timeline);

            // Acid spray (ranged)
            case "acid"           -> addAcidSpray(startX, startY, endX, endY, intensity, timeline);
            case "acid-spray"     -> addAcidSpray(startX, startY, endX, endY, intensity, timeline);
            case "venoshock"      -> addAcidSpray(startX, startY, endX, endY, intensity * 1.1, timeline);

            default               -> addDefaultPoisonBurst(endX, endY, intensity, timeline);
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
        switch (moveName) {
            case "sludge-bomb", "gunk-shot" ->
                    addSludgeBomb(startX, startY, endX, endY, intensity, timeline);
            case "sludge-wave" ->
                    addSludgeWave(endX, endY, intensity, timeline);
            default ->
                    addAcidSpray(startX, startY, endX, endY, intensity, timeline);
        }
    }

    // =================================================================
    // Venom strike – fang/stinger jab with dripping poison droplets
    // =================================================================

    private void addVenomStrike(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        // Purple trail drops along approach
        int trailCount = (int) (5 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 14;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 14;

            Circle drop = new Circle(3 + random.nextDouble() * 3,
                    i % 2 == 0 ? POISON_PURPLE : POISON_VIOLET);
            drop.setEffect(new GaussianBlur(3));
            drop.setCenterX(tx);
            drop.setCenterY(ty);
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            int delay = (int) (t * 130);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(drop.opacityProperty(), 0.8));
            KeyFrame drip = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(drop.centerYProperty(), ty + 8 + random.nextDouble() * 10),
                    new KeyValue(drop.radiusProperty(), drop.getRadius() * 1.4),
                    new KeyValue(drop.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drip);
            registerCleanup(timeline, drop);
        }

        // Stinger impact lines at target
        int stingCount = (int) (2 + 2 * intensity);
        for (int i = 0; i < stingCount; i++) {
            double angle = -35 + i * (70.0 / Math.max(stingCount - 1, 1));
            double rad = Math.toRadians(angle);
            double len = 20 + 12 * intensity;

            Line sting = new Line(
                    ex - Math.cos(rad) * len * 0.5,
                    ey - Math.sin(rad) * len * 0.5,
                    ex + Math.cos(rad) * len * 0.5,
                    ey + Math.sin(rad) * len * 0.5);
            sting.setStroke(i % 2 == 0 ? POISON_VIOLET : POISON_GREEN);
            sting.setStrokeWidth(2.5 + intensity);
            sting.setOpacity(0);
            sting.setEffect(new DropShadow(8 + 3 * intensity, POISON_PURPLE));
            prepareTransientNode(sting);
            battleField.getChildren().add(sting);

            int delay = 100 + i * 28;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(sting.opacityProperty(), 0.9));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 70),
                    new KeyValue(sting.strokeWidthProperty(), sting.getStrokeWidth() * 1.6),
                    new KeyValue(sting.opacityProperty(), 0.85));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(sting.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, sting);
        }

        addPoisonFlash(ex, ey, 15 + 10 * intensity, POISON_VIOLET, 90, 200, timeline);
    }

    // =================================================================
    // Barb barrage – thorn projectiles flying at target
    // =================================================================

    private void addBarbBarrage(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {
        int count = (int) (4 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            // Needle-like polygon for each barb
            Polygon barb = buildBarbPolygon(6 + random.nextDouble() * 5 * intensity);
            barb.setFill(i % 2 == 0 ? POISON_VIOLET : POISON_PURPLE);
            barb.setEffect(new DropShadow(5, POISON_DARK));
            barb.setLayoutX(sx + (random.nextDouble() - 0.5) * 20);
            barb.setLayoutY(sy + (random.nextDouble() - 0.5) * 20);
            barb.setOpacity(0);

            double targetAngle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
            barb.setRotate(targetAngle + (random.nextDouble() - 0.5) * 15);
            prepareTransientNode(barb);
            battleField.getChildren().add(barb);

            int delay = i * 45;
            double tx = ex + (random.nextDouble() - 0.5) * 22;
            double ty = ey + (random.nextDouble() - 0.5) * 22;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(barb.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(barb.layoutXProperty(), tx),
                    new KeyValue(barb.layoutYProperty(), ty));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(barb.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, barb);
        }

        addPoisonFlash(ex, ey, 14 + 8 * intensity, POISON_GREEN, 140, 180, timeline);
    }

    /** Build a narrow diamond/spike polygon for a barb. */
    private Polygon buildBarbPolygon(double size) {
        return new Polygon(0, -size, size * 0.3, 0, 0, size * 0.4, -size * 0.3, 0);
    }

    // =================================================================
    // Toxic jab – purple aura burst on contact
    // =================================================================

    private void addToxicJab(double x, double y, double intensity, Timeline timeline) {
        // Expanding toxic aura
        Circle aura = new Circle(0, POISON_DARK.deriveColor(0, 1, 1, 0.55));
        aura.setStroke(POISON_VIOLET.deriveColor(0, 1, 1, 0.7));
        aura.setStrokeWidth(3 + 1.5 * intensity);
        aura.setCenterX(x);
        aura.setCenterY(y);
        aura.setEffect(new GaussianBlur(8 + 4 * intensity));
        aura.setOpacity(0);
        prepareTransientNode(aura);
        battleField.getChildren().add(aura);

        double auraR = 28 + 20 * intensity;
        KeyFrame aAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(aura.opacityProperty(), 0.85));
        KeyFrame aExpand = new KeyFrame(Duration.millis(180),
                new KeyValue(aura.radiusProperty(), auraR),
                new KeyValue(aura.opacityProperty(), 0.45));
        KeyFrame aFade = new KeyFrame(Duration.millis(320),
                new KeyValue(aura.radiusProperty(), auraR * 1.4),
                new KeyValue(aura.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(aAppear, aExpand, aFade);
        registerCleanup(timeline, aura);

        // Poison droplets scattering outward
        addPoisonDroplets(x, y, intensity, 0, timeline);
        addPoisonFlash(x, y, 18 + 10 * intensity, POISON_GREEN, 0, 160, timeline);
    }

    // =================================================================
    // Sludge splatter – blob that arcs and splatters
    // =================================================================

    private void addSludgeSplatter(double sx, double sy, double ex, double ey,
                                   double intensity, Timeline timeline) {
        // Main sludge blob
        Circle blob = new Circle(10 + 5 * intensity, POISON_SLUDGE);
        blob.setEffect(new DropShadow(10, POISON_DARK));
        blob.setCenterX(sx);
        blob.setCenterY(sy);
        blob.setOpacity(0);
        prepareTransientNode(blob);
        battleField.getChildren().add(blob);

        double midX = (sx + ex) / 2;
        double midY = Math.min(sy, ey) - 30 - 10 * intensity;

        KeyFrame bAppear = new KeyFrame(Duration.millis(30),
                new KeyValue(blob.opacityProperty(), 0.9));
        KeyFrame bArc = new KeyFrame(Duration.millis(180),
                new KeyValue(blob.centerXProperty(), midX),
                new KeyValue(blob.centerYProperty(), midY));
        KeyFrame bLand = new KeyFrame(Duration.millis(300),
                new KeyValue(blob.centerXProperty(), ex),
                new KeyValue(blob.centerYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(380),
                new KeyValue(blob.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bArc, bLand, bFade);
        registerCleanup(timeline, blob);

        // Splatter droplets on impact
        addSplatDroplets(ex, ey, intensity, 280, timeline);
    }

    // =================================================================
    // Sludge bomb – large toxic explosion on impact
    // =================================================================

    private void addSludgeBomb(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        // Core sludge projectile
        Circle bomb = new Circle(8 + 4 * intensity, POISON_SLUDGE);
        bomb.setStroke(POISON_VIOLET.deriveColor(0, 1, 1, 0.6));
        bomb.setStrokeWidth(2);
        bomb.setEffect(new DropShadow(12, POISON_DARK));
        bomb.setCenterX(sx);
        bomb.setCenterY(sy);
        bomb.setOpacity(0);
        prepareTransientNode(bomb);
        battleField.getChildren().add(bomb);

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(bomb.opacityProperty(), 0.9));
        KeyFrame bTravel = new KeyFrame(Duration.millis(220),
                new KeyValue(bomb.centerXProperty(), ex),
                new KeyValue(bomb.centerYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(280),
                new KeyValue(bomb.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bTravel, bFade);
        registerCleanup(timeline, bomb);

        // Explosion burst at impact
        Circle explosion = new Circle(0, POISON_SLUDGE.deriveColor(0, 1, 1, 0.7));
        explosion.setEffect(new DropShadow(18 + 8 * intensity, POISON_GREEN));
        explosion.setCenterX(ex);
        explosion.setCenterY(ey);
        explosion.setOpacity(0);
        prepareTransientNode(explosion);
        battleField.getChildren().add(explosion);

        double exR = 30 + 18 * intensity;
        KeyFrame eAppear = new KeyFrame(Duration.millis(230),
                new KeyValue(explosion.opacityProperty(), 1.0),
                new KeyValue(explosion.radiusProperty(), exR * 0.3));
        KeyFrame ePeak = new KeyFrame(Duration.millis(320),
                new KeyValue(explosion.radiusProperty(), exR),
                new KeyValue(explosion.opacityProperty(), 0.5));
        KeyFrame eFade = new KeyFrame(Duration.millis(440),
                new KeyValue(explosion.radiusProperty(), exR * 1.5),
                new KeyValue(explosion.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(eAppear, ePeak, eFade);
        registerCleanup(timeline, explosion);

        addSplatDroplets(ex, ey, intensity, 260, timeline);
    }

    // =================================================================
    // Sludge wave – wide toxic wave spreading at target
    // =================================================================

    private void addSludgeWave(double x, double y, double intensity, Timeline timeline) {
        int waveCount = (int) (3 + 2 * intensity);
        for (int i = 0; i < waveCount; i++) {
            Ellipse wave = new Ellipse(0, 0);
            wave.setFill(Color.TRANSPARENT);
            wave.setStroke((i % 2 == 0 ? POISON_SLUDGE : POISON_VIOLET).deriveColor(0, 1, 1, 0.65));
            wave.setStrokeWidth(4 - i * 0.5);
            wave.setEffect(new GaussianBlur(4 + i));
            wave.setCenterX(x);
            wave.setCenterY(y);
            wave.setOpacity(0);
            prepareTransientNode(wave);
            battleField.getChildren().add(wave);

            int delay = i * 60;
            double maxRX = 50 + 24 * intensity;
            double maxRY = 20 + 10 * intensity;

            KeyFrame wAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(wave.opacityProperty(), 0.75));
            KeyFrame wExpand = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(wave.radiusXProperty(), maxRX),
                    new KeyValue(wave.radiusYProperty(), maxRY),
                    new KeyValue(wave.opacityProperty(), 0.3));
            KeyFrame wFade = new KeyFrame(Duration.millis(delay + 380),
                    new KeyValue(wave.radiusXProperty(), maxRX * 1.3),
                    new KeyValue(wave.radiusYProperty(), maxRY * 1.3),
                    new KeyValue(wave.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(wAppear, wExpand, wFade);
            registerCleanup(timeline, wave);
        }

        addSplatDroplets(x, y, intensity * 0.85, 0, timeline);
    }

    // =================================================================
    // Acid spray – streaks of corrosive liquid
    // =================================================================

    private void addAcidSpray(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int streamCount = (int) (4 + 4 * intensity);
        for (int i = 0; i < streamCount; i++) {
            Circle drop = new Circle(3 + random.nextDouble() * 3.5,
                    i % 3 == 0 ? POISON_ACID : i % 3 == 1 ? POISON_TOXIC : POISON_GREEN);
            drop.setEffect(new GaussianBlur(2));
            drop.setCenterX(sx + (random.nextDouble() - 0.5) * 16);
            drop.setCenterY(sy + (random.nextDouble() - 0.5) * 16);
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            int delay = i * 35;
            double tx = ex + (random.nextDouble() - 0.5) * 24;
            double ty = ey + (random.nextDouble() - 0.5) * 24;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(drop.opacityProperty(), 0.9));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(drop.centerXProperty(), tx),
                    new KeyValue(drop.centerYProperty(), ty),
                    new KeyValue(drop.radiusProperty(), drop.getRadius() * 1.4));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(drop.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, drop);
        }

        // Corrosive splash on impact
        addPoisonFlash(ex, ey, 18 + 10 * intensity, POISON_ACID, streamCount * 35, 200, timeline);
    }

    // =================================================================
    // Default poison burst – aura + droplets
    // =================================================================

    private void addDefaultPoisonBurst(double x, double y, double intensity, Timeline timeline) {
        addToxicJab(x, y, intensity, timeline);
    }

    // =================================================================
    // Shared helpers – droplet scatter, splat
    // =================================================================

    private void addPoisonDroplets(double x, double y, double intensity,
                                   int startDelay, Timeline timeline) {
        int count = (int) (7 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Circle drop = new Circle(3 + random.nextDouble() * 3,
                    i % 3 == 0 ? POISON_VIOLET : i % 3 == 1 ? POISON_GREEN : POISON_PURPLE);
            drop.setEffect(new GaussianBlur(3));
            double angle = (i / (double) count) * 2 * Math.PI;
            drop.setCenterX(x);
            drop.setCenterY(y);
            drop.setOpacity(0);
            prepareTransientNode(drop);
            battleField.getChildren().add(drop);

            double dist = 18 + random.nextDouble() * 20 * intensity;
            int delay = startDelay + i * 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(drop.opacityProperty(), 0.8));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(drop.centerXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(drop.centerYProperty(), y + Math.sin(angle) * dist),
                    new KeyValue(drop.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst);
            registerCleanup(timeline, drop);
        }
    }

    private void addSplatDroplets(double x, double y, double intensity,
                                  int startDelay, Timeline timeline) {
        int count = (int) (5 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            Circle splat = new Circle(3 + random.nextDouble() * 4,
                    i % 2 == 0 ? POISON_SLUDGE : POISON_VIOLET);
            splat.setEffect(new GaussianBlur(3));
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 10 + random.nextDouble() * 18 * intensity;
            splat.setCenterX(x);
            splat.setCenterY(y);
            splat.setOpacity(0);
            prepareTransientNode(splat);
            battleField.getChildren().add(splat);

            int delay = startDelay + i * 18;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(splat.opacityProperty(), 0.85));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 140),
                    new KeyValue(splat.centerXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(splat.centerYProperty(), y + Math.sin(angle) * dist),
                    new KeyValue(splat.radiusProperty(), splat.getRadius() * 1.6));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(splat.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, splat);
        }
    }

    // =================================================================
    // Flash circle helper
    // =================================================================

    private void addPoisonFlash(double x, double y, double radius, Color color,
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

        private void addFangImage(double x, double y, Timeline timeline) {
                try {
                        Image image = MediaCache.getImage(FANG_ASSET);
                        if (image == null) {
                                return;
                        }

                        ImageView imageView = new ImageView(image);
                        imageView.setFitWidth(190);
                        imageView.setFitHeight(190);
                        imageView.setPreserveRatio(true);
                        imageView.setLayoutX(x - 95);
                        imageView.setLayoutY(y - 108);
                        imageView.setOpacity(0);
                        imageView.setScaleX(0.55);
                        imageView.setScaleY(0.55);
                        prepareTransientNode(imageView);
                        battleField.getChildren().add(imageView);

                        KeyFrame appear = new KeyFrame(Duration.millis(35),
                                new KeyValue(imageView.opacityProperty(), 1.0),
                                new KeyValue(imageView.scaleXProperty(), 1.25),
                                new KeyValue(imageView.scaleYProperty(), 1.25));
                        KeyFrame settle = new KeyFrame(Duration.millis(115),
                                new KeyValue(imageView.scaleXProperty(), 1.0),
                                new KeyValue(imageView.scaleYProperty(), 1.0));
                        KeyFrame fade = new KeyFrame(Duration.millis(330),
                                new KeyValue(imageView.opacityProperty(), 0.0));

                        timeline.getKeyFrames().addAll(appear, settle, fade);
                        registerCleanup(timeline, imageView);
                } catch (Exception ignored) {
                        // Overlay is optional; the core move effect should still play.
                }
        }

    private void registerCleanup(Timeline timeline, Node node) {
        EventHandler<ActionEvent> prev = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (prev != null) prev.handle(e);
        });
    }
}
