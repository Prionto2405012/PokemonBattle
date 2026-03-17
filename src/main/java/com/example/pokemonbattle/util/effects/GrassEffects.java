// GrassEffects.java
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
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class GrassEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Grass colour palette
    private static final Color GRASS_GREEN   = Color.web("#2E7D32");
    private static final Color GRASS_LIGHT   = Color.web("#66BB6A");
    private static final Color GRASS_LIME    = Color.web("#C5E1A5");
    private static final Color GRASS_DARK    = Color.web("#1B5E20");
    private static final Color GRASS_YELLOW  = Color.web("#F9A825");
    private static final Color GRASS_BROWN   = Color.web("#5D4037");
    private static final Color GRASS_TEAL    = Color.web("#00897B");
    private static final Color GRASS_FRESH   = Color.web("#A5D6A7");

    public GrassEffects(Pane battleField) {
        this.battleField = battleField;
    }

    // Public API – single-point overload (melee / contact moves)

    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        createImpactEffect(x, y, x, y, moveName, movePower, timeline);
    }

    // Public API – full signature (all grass moves)

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.8, 2.4);

        switch (moveName) {
            // Whip / vine melee
            case "vine-whip"      -> addVineWhip(startX, startY, endX, endY, intensity, timeline);
            case "power-whip"     -> addVineWhip(startX, startY, endX, endY, intensity * 1.2, timeline);
            case "wood-hammer"    -> addWoodHammer(endX, endY, intensity, timeline);

            // Leaf slash melee
            case "leaf-blade"     -> addLeafSlash(startX, startY, endX, endY, intensity, timeline);
            case "petal-dance"    -> addLeafSlash(startX, startY, endX, endY, intensity * 0.9, timeline);

            // Ranged leaf / seed projectiles
            case "razor-leaf"     -> addRazorLeaf(startX, startY, endX, endY, intensity, timeline);
            case "bullet-seed"    -> addBulletSeed(startX, startY, endX, endY, intensity, timeline);
            case "seed-bomb"      -> addSeedBomb(startX, startY, endX, endY, intensity, timeline);
            case "magical-leaf"   -> addRazorLeaf(startX, startY, endX, endY, intensity * 0.9, timeline);
            case "petal-blizzard" -> addPetalBlizzard(startX, startY, endX, endY, intensity, timeline);

            // Ranged energy
            case "energy-ball"    -> addEnergyBall(startX, startY, endX, endY, intensity, timeline);
            case "leaf-storm"     -> addLeafStorm(startX, startY, endX, endY, intensity, timeline);
            case "solar-beam"     -> addSolarBeam(startX, startY, endX, endY, intensity, timeline);
            case "seed-flare"     -> addEnergyBall(startX, startY, endX, endY, intensity * 1.1, timeline);
            case "frenzy-plant"   -> addFrenzyPlant(endX, endY, intensity, timeline);

            default               -> addDefaultGrassBurst(endX, endY, intensity, timeline);
        }
    }

    // Public API – ranged lead effect

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.8, 2.4);
        switch (moveName) {
            case "solar-beam"     -> addSolarBeam(startX, startY, endX, endY, intensity, timeline);
            case "energy-ball", "seed-flare" ->
                    addEnergyBall(startX, startY, endX, endY, intensity, timeline);
            case "bullet-seed"    -> addBulletSeed(startX, startY, endX, endY, intensity, timeline);
            case "frenzy-plant"   -> addFrenzyPlant(endX, endY, intensity, timeline);
            default               -> addRazorLeaf(startX, startY, endX, endY, intensity, timeline);
        }
    }

    // Vine whip – long lashing vines

    private void addVineWhip(double sx, double sy, double ex, double ey, double intensity, Timeline timeline) {
        int vineCount = (int) (8 + intensity);
        for (int v = 0; v < vineCount; v++) {
            double offY = (v - (vineCount - 1) / 1.2) * 8;

            Line vine = new Line(sx, sy + offY, sx, sy + offY);
            vine.setStroke(v % 2 == 0 ? GRASS_GREEN : GRASS_LIGHT);
            vine.setStrokeWidth(6 + 1.5 * intensity);
            vine.setEffect(new DropShadow(6 + 2 * intensity, GRASS_DARK));
            vine.setOpacity(0);
            prepareTransientNode(vine);
            battleField.getChildren().add(vine);

            int delay = v * 35;
            KeyFrame vAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(vine.opacityProperty(), 0.9));
            KeyFrame vExtend = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(vine.endXProperty(), ex),
                    new KeyValue(vine.endYProperty(), ey + offY));
            KeyFrame vFade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(vine.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(vAppear, vExtend, vFade);
            registerCleanup(timeline, vine);
        }

        // Leaf particles at impact
        addLeafParticles(ex, ey, intensity, vineCount * 35 + 120, timeline);
        addGrassFlash(ex, ey, 20 + 10 * intensity, GRASS_LIGHT, vineCount * 35 + 160, 180, timeline);
    }

    // Wood hammer – heavy wooden slam with bark debris

    private void addWoodHammer(double x, double y, double intensity, Timeline timeline) {
        // Shockwave ring from slam
        Circle ring = new Circle(0, Color.TRANSPARENT);
        ring.setStroke(GRASS_BROWN.deriveColor(0, 1, 1, 0.7));
        ring.setStrokeWidth(8 + 1.5 * intensity);
        ring.setCenterX(x);
        ring.setCenterY(y);
        ring.setEffect(new GaussianBlur(5));
        ring.setOpacity(0);
        prepareTransientNode(ring);
        battleField.getChildren().add(ring);

        double ringR = 35 + 20 * intensity;
        KeyFrame rAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(ring.opacityProperty(), 0.85));
        KeyFrame rExpand = new KeyFrame(Duration.millis(200),
                new KeyValue(ring.radiusProperty(), ringR),
                new KeyValue(ring.opacityProperty(), 0.4));
        KeyFrame rFade = new KeyFrame(Duration.millis(320),
                new KeyValue(ring.radiusProperty(), ringR * 1.4),
                new KeyValue(ring.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(rAppear, rExpand, rFade);
        registerCleanup(timeline, ring);

        // Bark/wood debris
        int debrisCount = (int) (16 + 4 * intensity);
        for (int i = 0; i < debrisCount; i++) {
            Rectangle bark = new Rectangle(
                    9 + random.nextDouble() * 6 * intensity,
                    8 + random.nextDouble() * 4 * intensity);
            bark.setFill(i % 2 == 0 ? GRASS_BROWN : GRASS_DARK);
            bark.setX(x - bark.getWidth() / 2);
            bark.setY(y - bark.getHeight() / 2);
            bark.setRotate(random.nextDouble() * 360);
            bark.setOpacity(0);
            prepareTransientNode(bark);
            battleField.getChildren().add(bark);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 14 + random.nextDouble() * 22 * intensity;
            int delay = i * 22;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(bark.opacityProperty(), 0.9));
            KeyFrame scatter = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(bark.xProperty(), x + Math.cos(angle) * dist - bark.getWidth() / 2),
                    new KeyValue(bark.yProperty(), y + Math.sin(angle) * dist - bark.getHeight() / 2),
                    new KeyValue(bark.rotateProperty(), bark.getRotate() + 200));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(bark.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, scatter, fade);
            registerCleanup(timeline, bark);
        }

        addGrassFlash(x, y, 28 + 12 * intensity, GRASS_LIGHT, 0, 200, timeline);
    }

    // Leaf slash / leaf blade – sharp leaf cuts

    private void addLeafSlash(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        double dx = ex - sx;
        double dy = ey - sy;

        // Leaf trail along approach
        int trailCount = (int) (15 + 4 * intensity);
        for (int i = 0; i < trailCount; i++) {
            double t = (i + 0.5) / trailCount;
            double tx = sx + dx * t + (random.nextDouble() - 0.5) * 14;
            double ty = sy + dy * t + (random.nextDouble() - 0.5) * 14;

            Ellipse leaf = new Ellipse(9 + random.nextDouble() * 3, 13 + random.nextDouble() * 5);
            leaf.setFill((i % 2 == 0 ? GRASS_GREEN : GRASS_LIGHT).deriveColor(0, 1, 1, 0.7));
            leaf.setEffect(new GaussianBlur(2));
            leaf.setCenterX(tx);
            leaf.setCenterY(ty);
            leaf.setRotate(random.nextDouble() * 60 - 30);
            leaf.setOpacity(0);
            prepareTransientNode(leaf);
            battleField.getChildren().add(leaf);

            int delay = (int) (t * 130);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(leaf.opacityProperty(), 0.8));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(leaf.centerYProperty(), ty - 10 - random.nextDouble() * 10),
                    new KeyValue(leaf.rotateProperty(), leaf.getRotate() + 50),
                    new KeyValue(leaf.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, leaf);
        }

        // Slash marks at impact
        int slashCount = (int) (10 + 2 * intensity);
        for (int i = 0; i < slashCount; i++) {
            double angle = -45 + i * (90.0 / Math.max(slashCount - 1, 1));
            double rad = Math.toRadians(angle);
            double slashLen = 24 + 14 * intensity;

            Line slash = new Line(
                    ex - Math.cos(rad) * slashLen * 0.5,
                    ey - Math.sin(rad) * slashLen * 0.5,
                    ex + Math.cos(rad) * slashLen * 0.5,
                    ey + Math.sin(rad) * slashLen * 0.5);
            slash.setStroke(i % 2 == 0 ? GRASS_LIGHT : GRASS_GREEN);
            slash.setStrokeWidth(5 + intensity);
            slash.setOpacity(0);
            slash.setEffect(new DropShadow(8 + 3 * intensity, GRASS_GREEN));
            prepareTransientNode(slash);
            battleField.getChildren().add(slash);

            int delay = 90 + i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(slash.opacityProperty(), 1.0));
            KeyFrame flare = new KeyFrame(Duration.millis(delay + 70),
                    new KeyValue(slash.strokeWidthProperty(), slash.getStrokeWidth() * 1.6),
                    new KeyValue(slash.opacityProperty(), 0.85));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(slash.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, flare, fade);
            registerCleanup(timeline, slash);
        }

        addGrassFlash(ex, ey, 20 + 10 * intensity, GRASS_FRESH, 90, 200, timeline);
    }

    // Razor leaf – spinning sharp leaves flying to target

    private void addRazorLeaf(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int leafCount = (int) (14 + 4 * intensity);
        for (int i = 0; i < leafCount; i++) {
            Ellipse leaf = new Ellipse(10 + random.nextDouble() * 4, 15 + random.nextDouble() * 6);
            leaf.setFill((i % 2 == 0 ? GRASS_GREEN : GRASS_LIGHT).deriveColor(0, 1, 1, 0.85));
            leaf.setStroke(GRASS_DARK.deriveColor(0, 1, 1, 0.4));
            leaf.setStrokeWidth(1.2);
            leaf.setCenterX(sx + (random.nextDouble() - 0.5) * 14);
            leaf.setCenterY(sy + (random.nextDouble() - 0.5) * 14);
            leaf.setRotate(random.nextDouble() * 360);
            leaf.setOpacity(0);
            prepareTransientNode(leaf);
            battleField.getChildren().add(leaf);

            int delay = i * 45;
            double tx = ex + (random.nextDouble() - 0.5) * 18;
            double ty = ey + (random.nextDouble() - 0.5) * 18;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(leaf.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(leaf.centerXProperty(), tx),
                    new KeyValue(leaf.centerYProperty(), ty),
                    new KeyValue(leaf.rotateProperty(), leaf.getRotate() + 300));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(leaf.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, leaf);
        }

        addGrassFlash(ex, ey, 20 + 8 * intensity, GRASS_LIGHT, leafCount * 45, 180, timeline);
    }

    // Bullet seed – rapid-fire seed pellets

    private void addBulletSeed(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        int seedCount = (int) (14 + 4 * intensity);
        for (int i = 0; i < seedCount; i++) {
            Circle seed = new Circle(8 + random.nextDouble() * 2.5, GRASS_YELLOW);
            seed.setStroke(GRASS_GREEN.deriveColor(0, 1, 1, 0.5));
            seed.setStrokeWidth(1);
            seed.setEffect(new DropShadow(5, GRASS_DARK));
            seed.setCenterX(sx + (random.nextDouble() - 0.5) * 10);
            seed.setCenterY(sy + (random.nextDouble() - 0.5) * 10);
            seed.setOpacity(0);
            prepareTransientNode(seed);
            battleField.getChildren().add(seed);

            int delay = i * 50;
            double tx = ex + (random.nextDouble() - 0.5) * 16;
            double ty = ey + (random.nextDouble() - 0.5) * 16;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(seed.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(seed.centerXProperty(), tx),
                    new KeyValue(seed.centerYProperty(), ty));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(seed.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, seed);
        }

        addGrassFlash(ex, ey, 20 + 8 * intensity, GRASS_YELLOW, seedCount * 50, 180, timeline);
    }

    // Seed bomb – large bouncing seed that explodes on impact

    private void addSeedBomb(double sx, double sy, double ex, double ey,
                             double intensity, Timeline timeline) {
        Circle bomb = new Circle(15 + 4 * intensity, GRASS_DARK);
        bomb.setStroke(GRASS_GREEN);
        bomb.setStrokeWidth(2.5);
        bomb.setEffect(new DropShadow(10, GRASS_DARK));
        bomb.setCenterX(sx);
        bomb.setCenterY(sy);
        bomb.setOpacity(0);
        prepareTransientNode(bomb);
        battleField.getChildren().add(bomb);

        double midX = (sx + ex) / 2;
        double midY = Math.min(sy, ey) - 36 - 10 * intensity;

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(bomb.opacityProperty(), 0.95));
        KeyFrame bArc = new KeyFrame(Duration.millis(160),
                new KeyValue(bomb.centerXProperty(), midX),
                new KeyValue(bomb.centerYProperty(), midY));
        KeyFrame bLand = new KeyFrame(Duration.millis(280),
                new KeyValue(bomb.centerXProperty(), ex),
                new KeyValue(bomb.centerYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(360),
                new KeyValue(bomb.opacityProperty(), 0),
                new KeyValue(bomb.radiusProperty(), bomb.getRadius() * 2));

        timeline.getKeyFrames().addAll(bAppear, bArc, bLand, bFade);
        registerCleanup(timeline, bomb);

        addLeafParticles(ex, ey, intensity, 280, timeline);
        addGrassFlash(ex, ey, 25 + 12 * intensity, GRASS_LIGHT, 280, 200, timeline);
    }

    // Petal blizzard – storm of petals from all directions

    private void addPetalBlizzard(double sx, double sy, double ex, double ey,
                                  double intensity, Timeline timeline) {
        int count = (int) (18 + 7 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            double startR = 46 + 14 * intensity;

            Ellipse petal = new Ellipse(9 + random.nextDouble() * 3, 14 + random.nextDouble() * 5);
            petal.setFill((i % 3 == 0 ? GRASS_FRESH : i % 3 == 1 ? GRASS_LIGHT : GRASS_LIME)
                    .deriveColor(0, 1, 1, 0.8));
            petal.setCenterX(ex + Math.cos(angle) * startR);
            petal.setCenterY(ey + Math.sin(angle) * startR);
            petal.setRotate(Math.toDegrees(angle));
            petal.setOpacity(0);
            prepareTransientNode(petal);
            battleField.getChildren().add(petal);

            int delay = i * 22;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(petal.opacityProperty(), 0.85));
            KeyFrame converge = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(petal.centerXProperty(), ex + (random.nextDouble() - 0.5) * 18),
                    new KeyValue(petal.centerYProperty(), ey + (random.nextDouble() - 0.5) * 18),
                    new KeyValue(petal.rotateProperty(), petal.getRotate() + 120));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 340),
                    new KeyValue(petal.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, converge, fade);
            registerCleanup(timeline, petal);
        }

        addGrassFlash(ex, ey, 22 + 10 * intensity, GRASS_FRESH, count * 22 / 2, 200, timeline);
    }

    // Energy ball – green orb hurled at target

    private void addEnergyBall(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        double orbRadius = 17 + 6 * intensity;

        Circle orb = new Circle(orbRadius, GRASS_TEAL.deriveColor(0, 1, 1, 0.85));
        orb.setStroke(GRASS_LIGHT);
        orb.setStrokeWidth(3.5);
        orb.setEffect(new DropShadow(18 + 7 * intensity, GRASS_GREEN));
        orb.setCenterX(sx);
        orb.setCenterY(sy);
        orb.setOpacity(0);
        prepareTransientNode(orb);
        battleField.getChildren().add(orb);

        Circle glow = new Circle(orbRadius * 0.75, GRASS_FRESH.deriveColor(0, 1, 1, 0.65));
        glow.setEffect(new GaussianBlur(5));
        glow.setCenterX(sx);
        glow.setCenterY(sy);
        glow.setOpacity(0);
        prepareTransientNode(glow);
        battleField.getChildren().add(glow);

        double midX = (sx + ex) / 2;
        double midY = Math.min(sy, ey) - 35 - 10 * intensity;

        KeyFrame appear = new KeyFrame(Duration.millis(30),
                new KeyValue(orb.opacityProperty(), 1.0),
                new KeyValue(glow.opacityProperty(), 0.9));
        KeyFrame arc = new KeyFrame(Duration.millis(160),
                new KeyValue(orb.centerXProperty(), midX),
                new KeyValue(orb.centerYProperty(), midY),
                new KeyValue(glow.centerXProperty(), midX),
                new KeyValue(glow.centerYProperty(), midY));
        KeyFrame impact = new KeyFrame(Duration.millis(280),
                new KeyValue(orb.centerXProperty(), ex),
                new KeyValue(orb.centerYProperty(), ey),
                new KeyValue(glow.centerXProperty(), ex),
                new KeyValue(glow.centerYProperty(), ey));
        KeyFrame burst = new KeyFrame(Duration.millis(370),
                new KeyValue(orb.opacityProperty(), 0),
                new KeyValue(glow.opacityProperty(), 0),
                new KeyValue(orb.radiusProperty(), orbRadius * 2));

        timeline.getKeyFrames().addAll(appear, arc, impact, burst);
        registerCleanup(timeline, orb);
        registerCleanup(timeline, glow);

        addLeafParticles(ex, ey, intensity, 280, timeline);
    }

    // Leaf storm – a spiralling storm of leaves

    private void addLeafStorm(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int count = (int) (20 + 7 * intensity);
        double dx = ex - sx;
        double dy = ey - sy;

        for (int i = 0; i < count; i++) {
            double t = (i + random.nextDouble()) / count;
            double angle = t * 3 * Math.PI;
            double spiralR = 14 + 8 * intensity * (1 - t);

            double px = sx + dx * t + Math.cos(angle) * spiralR;
            double py = sy + dy * t + Math.sin(angle) * spiralR;

            Ellipse leaf = new Ellipse(10 + random.nextDouble() * 3, 15 + random.nextDouble() * 5);
            leaf.setFill((i % 2 == 0 ? GRASS_GREEN : GRASS_LIGHT).deriveColor(0, 1, 1, 0.8));
            leaf.setEffect(new GaussianBlur(2));
            leaf.setCenterX(px);
            leaf.setCenterY(py);
            leaf.setRotate(Math.toDegrees(angle));
            leaf.setOpacity(0);
            prepareTransientNode(leaf);
            battleField.getChildren().add(leaf);

            int delay = (int) (t * 220);
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(leaf.opacityProperty(), 0.8));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(leaf.rotateProperty(), leaf.getRotate() + 80),
                    new KeyValue(leaf.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, drift);
            registerCleanup(timeline, leaf);
        }

        addGrassFlash(ex, ey, 25 + 12 * intensity, GRASS_LIME, count / 2 * 22, 200, timeline);
    }

    // Solar beam – charging bright beam

    private void addSolarBeam(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        // Charging glow at attacker
        Circle charge = new Circle(0, GRASS_YELLOW.deriveColor(0, 1, 1, 0.6));
        charge.setEffect(new GaussianBlur(12 + 4 * intensity));
        charge.setCenterX(sx);
        charge.setCenterY(sy);
        charge.setOpacity(0);
        prepareTransientNode(charge);
        battleField.getChildren().add(charge);

        double chargeR = 24 + 14 * intensity;
        KeyFrame cAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(charge.opacityProperty(), 0.75),
                new KeyValue(charge.radiusProperty(), chargeR));
        KeyFrame cFade = new KeyFrame(Duration.millis(80),
                new KeyValue(charge.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(cAppear, cFade);
        registerCleanup(timeline, charge);

        // Bright beam lancing forward
        Line beam = new Line(sx, sy, sx, sy);
        beam.setStroke(GRASS_YELLOW.deriveColor(0, 1, 1, 0.9));
        beam.setStrokeWidth(12 + 3 * intensity);
        beam.setEffect(new DropShadow(18 + 7 * intensity, GRASS_LIGHT));
        beam.setOpacity(0);
        prepareTransientNode(beam);
        battleField.getChildren().add(beam);

        Line core = new Line(sx, sy, sx, sy);
        core.setStroke(Color.WHITE);
        core.setStrokeWidth(8 + intensity);
        core.setEffect(new GaussianBlur(2));
        core.setOpacity(0);
        prepareTransientNode(core);
        battleField.getChildren().add(core);

        KeyFrame bAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(beam.opacityProperty(), 0.9),
                new KeyValue(core.opacityProperty(), 1.0));
        KeyFrame bExtend = new KeyFrame(Duration.millis(210),
                new KeyValue(beam.endXProperty(), ex),
                new KeyValue(beam.endYProperty(), ey),
                new KeyValue(core.endXProperty(), ex),
                new KeyValue(core.endYProperty(), ey));
        KeyFrame bFade = new KeyFrame(Duration.millis(360),
                new KeyValue(beam.opacityProperty(), 0),
                new KeyValue(core.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(bAppear, bExtend, bFade);
        registerCleanup(timeline, beam);
        registerCleanup(timeline, core);

        addGrassFlash(ex, ey, 30 + 14 * intensity, GRASS_YELLOW, 200, 220, timeline);
    }

    // Frenzy plant – roots erupting from the ground

    private void addFrenzyPlant(double x, double y, double intensity, Timeline timeline) {
        int rootCount = (int) (14 + 3 * intensity);
        for (int i = 0; i < rootCount; i++) {
            double angle = -60 + (i / (double) (rootCount - 1)) * 120;
            double rad = Math.toRadians(angle);
            double rootLen = 28 + 18 * intensity;

            Line root = new Line(x, y + 10, x, y + 10);
            root.setStroke(i % 2 == 0 ? GRASS_BROWN : GRASS_DARK);
            root.setStrokeWidth(8 + 1.5 * intensity);
            root.setEffect(new DropShadow(6 + 2 * intensity, GRASS_DARK));
            root.setOpacity(0);
            prepareTransientNode(root);
            battleField.getChildren().add(root);

            int delay = i * 40;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(root.opacityProperty(), 0.9));
            KeyFrame extend = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(root.endXProperty(), x + Math.cos(rad) * rootLen),
                    new KeyValue(root.endYProperty(), y - Math.abs(Math.sin(rad)) * rootLen * 0.6));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 360),
                    new KeyValue(root.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, extend, fade);
            registerCleanup(timeline, root);
        }

        // Ground crack ripple
        Ellipse crack = new Ellipse(0, 0);
        crack.setFill(Color.TRANSPARENT);
        crack.setStroke(GRASS_BROWN.deriveColor(0, 1, 1, 0.65));
        crack.setStrokeWidth(6 + intensity);
        crack.setEffect(new GaussianBlur(3));
        crack.setCenterX(x);
        crack.setCenterY(y + 10);
        crack.setOpacity(0);
        prepareTransientNode(crack);
        battleField.getChildren().add(crack);

        double maxRX = 48 + 28 * intensity;
        double maxRY = 16 + 8 * intensity;
        KeyFrame cAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(crack.opacityProperty(), 0.75));
        KeyFrame cExpand = new KeyFrame(Duration.millis(220),
                new KeyValue(crack.radiusXProperty(), maxRX),
                new KeyValue(crack.radiusYProperty(), maxRY),
                new KeyValue(crack.opacityProperty(), 0.35));
        KeyFrame cFade = new KeyFrame(Duration.millis(360),
                new KeyValue(crack.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(cAppear, cExpand, cFade);
        registerCleanup(timeline, crack);

        addGrassFlash(x, y, 26 + 14 * intensity, GRASS_GREEN, 0, 200, timeline);
    }

    // Default grass burst – leaf particles + flash

    private void addDefaultGrassBurst(double x, double y, double intensity, Timeline timeline) {
        addLeafParticles(x, y, intensity, 0, timeline);
        addGrassFlash(x, y, 22 + 10 * intensity, GRASS_FRESH, 0, 200, timeline);
    }

    // Shared helpers – leaf particles

    private void addLeafParticles(double x, double y, double intensity,
                                  int startDelay, Timeline timeline) {
        int count = (int) (16 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Ellipse leaf = new Ellipse(8 + random.nextDouble() * 3, 14 + random.nextDouble() * 5);
            leaf.setFill((i % 2 == 0 ? GRASS_GREEN : GRASS_LIGHT).deriveColor(0, 1, 1, 0.7));
            leaf.setEffect(new GaussianBlur(2));
            double angle = random.nextDouble() * 2 * Math.PI;
            leaf.setCenterX(x);
            leaf.setCenterY(y);
            leaf.setRotate(random.nextDouble() * 360);
            leaf.setOpacity(0);
            prepareTransientNode(leaf);
            battleField.getChildren().add(leaf);

            double dist = 14 + random.nextDouble() * 20 * intensity;
            int delay = startDelay + i * 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(leaf.opacityProperty(), 0.8));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(leaf.centerXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(leaf.centerYProperty(), y + Math.sin(angle) * dist),
                    new KeyValue(leaf.rotateProperty(), leaf.getRotate() + 80));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(leaf.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, leaf);
        }
    }

    // Flash circle helper

    private void addGrassFlash(double x, double y, double radius, Color color, int startDelay, int fadeDuration, Timeline timeline) {
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
