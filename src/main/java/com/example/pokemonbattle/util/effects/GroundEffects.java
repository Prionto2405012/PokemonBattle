// GroundEffects.java
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

public class GroundEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Ground colour palette
    private static final Color GROUND_BROWN = Color.web("#795548");
    private static final Color GROUND_TAN   = Color.web("#A1887F");
    private static final Color GROUND_DARK  = Color.web("#4E342E");
    private static final Color GROUND_SAND  = Color.web("#D7CCC8");
    private static final Color GROUND_DUST  = Color.web("#EFEBE9");
    private static final Color GROUND_MUD   = Color.web("#6D4C41");
    private static final Color GROUND_CLAY  = Color.web("#8D6E63");

    public GroundEffects(Pane battleField) {
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
    // Public API – full signature (ranged / projectile moves)
    // -----------------------------------------------------------------

    public void createImpactEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {

        double intensity = clamp(movePower / 100.0, 0.4, 1.8);

        switch (moveName) {
            // Screen shake + ground cracks
            case "bone-rush", "bonemerang"
                         -> addBoneStrike(startX, startY, endX, endY, intensity, timeline);
            case "bulldoze"
                         -> addBulldoze(endX, endY, intensity, timeline);
            case "drill-run"
                         -> addDrillRun(startX, startY, endX, endY, intensity, timeline);
            case "earthquake"
                         -> addEarthquake(endX, endY, intensity, timeline);
            case "high-horsepower"
                         -> addHighHorsepower(endX, endY, intensity, timeline);
            case "stomping-tantrum"
                         -> addStompingTantrum(endX, endY, intensity, timeline);

            // Eruption pillars from below
            case "earth-power"
                         -> addEarthPower(endX, endY, intensity, timeline);
            case "land's-wrath"
                         -> addLandsWrath(endX, endY, intensity, timeline);
            case "precipice-blades"
                         -> addPrecipiceBlades(endX, endY, intensity, timeline);

            // Mud projectile impact at target
            case "mud-bomb"
                         -> addMudSplatter(endX, endY, intensity, timeline);
            case "mud-shot"
                         -> addMudSplatter(endX, endY, intensity * 0.8, timeline);
            case "sand-attack"
                         -> addSandSplatter(endX, endY, intensity, timeline);

            // Sand swirl at defender
            case "sand-tomb"
                         -> addSandTomb(endX, endY, intensity, timeline);
            case "sandstorm"
                         -> addSandstorm(endX, endY, intensity, timeline);
            case "scorching-sands"
                         -> addScorchingSands(endX, endY, intensity, timeline);

            // Dust cloud at target
            case "dig"   -> addDig(endX, endY, intensity, timeline);

            default      -> addDefaultGround(endX, endY, intensity, timeline);
        }
    }

    // -----------------------------------------------------------------
    // Public API – ranged lead effect (mud / sand projectile to target)
    // -----------------------------------------------------------------

    public void createRangedEffect(double startX, double startY,
                                   double endX, double endY,
                                   String moveName, int movePower,
                                   Timeline timeline) {
        double intensity = clamp(movePower / 100.0, 0.4, 1.8);
        int count = (int) (3 + 4 * intensity);

        for (int i = 0; i < count; i++) {
            Circle blob = buildMudBlob(6 + random.nextDouble() * 6 * intensity,
                    i % 3 == 0 ? GROUND_MUD : i % 3 == 1 ? GROUND_BROWN : GROUND_CLAY);
            blob.setCenterX(startX + (random.nextDouble() - 0.5) * 20);
            blob.setCenterY(startY + (random.nextDouble() - 0.5) * 20);
            blob.setOpacity(0);
            blob.setEffect(new DropShadow(6, GROUND_DARK));
            prepareTransientNode(blob);
            battleField.getChildren().add(blob);

            int delay = i * 40;
            double tx = endX + (random.nextDouble() - 0.5) * 30;
            double ty = endY + (random.nextDouble() - 0.5) * 30;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(blob.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(blob.centerXProperty(), tx),
                    new KeyValue(blob.centerYProperty(), ty),
                    new KeyValue(blob.radiusProperty(), blob.getRadius() * 1.3));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(blob.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, blob);
        }

        // Splatter circles on impact
        int splatters = (int) (3 + 2 * intensity);
        int baseDelay = count * 40;
        for (int i = 0; i < splatters; i++) {
            Circle splat = new Circle(3 + random.nextDouble() * 4,
                    GROUND_MUD.deriveColor(0, 1, 1, 0.7));
            splat.setCenterX(endX);
            splat.setCenterY(endY);
            splat.setOpacity(0);
            splat.setEffect(new GaussianBlur(2));
            prepareTransientNode(splat);
            battleField.getChildren().add(splat);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 10 + random.nextDouble() * 16;
            int delay = baseDelay + i * 15;

            KeyFrame sAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(splat.opacityProperty(), 0.8));
            KeyFrame sBurst = new KeyFrame(Duration.millis(delay + 120),
                    new KeyValue(splat.centerXProperty(), endX + Math.cos(angle) * dist),
                    new KeyValue(splat.centerYProperty(), endY + Math.sin(angle) * dist),
                    new KeyValue(splat.radiusProperty(), splat.getRadius() * 1.8));
            KeyFrame sFade = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(splat.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(sAppear, sBurst, sFade);
            registerCleanup(timeline, splat);
        }
    }

    // =================================================================
    // Screen shake + ground crack animations
    // =================================================================

    /** bone-rush / bonemerang – bone projectiles hurled with rapid rotation. */
    private void addBoneStrike(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        int count = (int) (2 + 2 * intensity);
        for (int i = 0; i < count; i++) {
            Rectangle bone = new Rectangle(22 * intensity, 5 * intensity);
            bone.setFill(GROUND_SAND);
            bone.setStroke(GROUND_TAN);
            bone.setStrokeWidth(1);
            bone.setArcWidth(3);
            bone.setArcHeight(3);
            bone.setLayoutX(sx + (random.nextDouble() - 0.5) * 14);
            bone.setLayoutY(sy + (random.nextDouble() - 0.5) * 14);
            bone.setOpacity(0);
            bone.setEffect(new DropShadow(4, GROUND_DARK));
            prepareTransientNode(bone);
            battleField.getChildren().add(bone);

            int delay = i * 70;
            double tx = ex + (random.nextDouble() - 0.5) * 20;
            double ty = ey + (random.nextDouble() - 0.5) * 20;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(bone.opacityProperty(), 1.0));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(bone.layoutXProperty(), tx),
                    new KeyValue(bone.layoutYProperty(), ty),
                    new KeyValue(bone.rotateProperty(), 720));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 270),
                    new KeyValue(bone.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, bone);
        }

        addCrackLines(ex, ey, intensity, 180, timeline);
        addDustCloud(ex, ey, intensity, 200, timeline);
    }

    /** bulldoze – expanding shockwave with ground cracks radiating outward. */
    private void addBulldoze(double x, double y, double intensity,
                             Timeline timeline) {
        // Shockwave ring
        Ellipse wave = new Ellipse(0, 0);
        wave.setCenterX(x);
        wave.setCenterY(y);
        wave.setFill(Color.TRANSPARENT);
        wave.setStroke(GROUND_BROWN);
        wave.setStrokeWidth(3 * intensity);
        wave.setOpacity(0);
        wave.setEffect(new GaussianBlur(3));
        prepareTransientNode(wave);
        battleField.getChildren().add(wave);

        double waveR = 40 + 25 * intensity;
        KeyFrame wAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(wave.opacityProperty(), 0.8));
        KeyFrame wExpand = new KeyFrame(Duration.millis(250),
                new KeyValue(wave.radiusXProperty(), waveR),
                new KeyValue(wave.radiusYProperty(), waveR * 0.5),
                new KeyValue(wave.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(wAppear, wExpand);
        registerCleanup(timeline, wave);

        addCrackLines(x, y, intensity, 0, timeline);
        addDustCloud(x, y, intensity * 1.2, 80, timeline);
        addFlashCircle(x, y, 20 * intensity, GROUND_TAN, 0, 180, timeline);
    }

    /** drill-run – spinning drill projectile charges from attacker to target. */
    private void addDrillRun(double sx, double sy, double ex, double ey,
                             double intensity, Timeline timeline) {
        Polygon drill = buildDrillPolygon(16 * intensity, GROUND_BROWN);
        drill.setLayoutX(sx);
        drill.setLayoutY(sy);
        drill.setOpacity(0);
        drill.setEffect(new DropShadow(8, GROUND_DARK));
        prepareTransientNode(drill);
        battleField.getChildren().add(drill);

        KeyFrame dAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(drill.opacityProperty(), 1.0));
        KeyFrame dTravel = new KeyFrame(Duration.millis(280),
                new KeyValue(drill.layoutXProperty(), ex),
                new KeyValue(drill.layoutYProperty(), ey),
                new KeyValue(drill.rotateProperty(), 1080));
        KeyFrame dFade = new KeyFrame(Duration.millis(350),
                new KeyValue(drill.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(dAppear, dTravel, dFade);
        registerCleanup(timeline, drill);

        // Dust trail along path
        int trails = (int) (5 * intensity);
        for (int i = 0; i < trails; i++) {
            Circle dust = new Circle(3 + random.nextDouble() * 3, GROUND_DUST);
            double frac = (i + 1.0) / (trails + 1);
            double mx = sx + (ex - sx) * frac + (random.nextDouble() - 0.5) * 14;
            double my = sy + (ey - sy) * frac + (random.nextDouble() - 0.5) * 14;
            dust.setCenterX(mx);
            dust.setCenterY(my);
            dust.setOpacity(0);
            dust.setEffect(new GaussianBlur(3));
            prepareTransientNode(dust);
            battleField.getChildren().add(dust);

            int delay = 50 + i * 40;
            KeyFrame tAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(dust.opacityProperty(), 0.6));
            KeyFrame tFade = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(dust.opacityProperty(), 0),
                    new KeyValue(dust.radiusProperty(), dust.getRadius() * 2.2));
            timeline.getKeyFrames().addAll(tAppear, tFade);
            registerCleanup(timeline, dust);
        }

        addCrackLines(ex, ey, intensity * 0.8, 260, timeline);
        addFlashCircle(ex, ey, 18 * intensity, GROUND_CLAY, 260, 120, timeline);
    }

    /** earthquake – heavy ground vibration with extensive cracks and debris. */
    private void addEarthquake(double x, double y, double intensity,
                               Timeline timeline) {
        // Multiple shockwave rings
        for (int w = 0; w < 3; w++) {
            Ellipse wave = new Ellipse(0, 0);
            wave.setCenterX(x);
            wave.setCenterY(y);
            wave.setFill(Color.TRANSPARENT);
            wave.setStroke(w == 0 ? GROUND_BROWN : w == 1 ? GROUND_CLAY : GROUND_TAN);
            wave.setStrokeWidth(2.5 * intensity);
            wave.setOpacity(0);
            wave.setEffect(new GaussianBlur(2));
            prepareTransientNode(wave);
            battleField.getChildren().add(wave);

            double waveR = 50 + 30 * intensity;
            int delay = w * 60;
            KeyFrame wAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(wave.opacityProperty(), 0.75));
            KeyFrame wExpand = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(wave.radiusXProperty(), waveR),
                    new KeyValue(wave.radiusYProperty(), waveR * 0.45),
                    new KeyValue(wave.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(wAppear, wExpand);
            registerCleanup(timeline, wave);
        }

        // Ground debris fragments
        int debris = (int) (8 + 6 * intensity);
        for (int i = 0; i < debris; i++) {
            Polygon frag = buildRockChunk(4 + random.nextDouble() * 6,
                    i % 3 == 0 ? GROUND_BROWN : i % 3 == 1 ? GROUND_DARK : GROUND_CLAY);
            frag.setLayoutX(x + (random.nextDouble() - 0.5) * 30);
            frag.setLayoutY(y);
            frag.setOpacity(0);
            prepareTransientNode(frag);
            battleField.getChildren().add(frag);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 20 + random.nextDouble() * 35 * intensity;
            int delay = 40 + i * 18;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(frag.opacityProperty(), 0.9));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(frag.layoutXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(frag.layoutYProperty(),
                            y + Math.sin(angle) * dist * 0.6 - 15),
                    new KeyValue(frag.rotateProperty(), random.nextDouble() * 360));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 340),
                    new KeyValue(frag.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, frag);
        }

        addCrackLines(x, y, intensity * 1.4, 0, timeline);
        addDustCloud(x, y, intensity * 1.5, 60, timeline);
        addFlashCircle(x, y, 30 * intensity, GROUND_TAN, 0, 220, timeline);
    }

    /** high-horsepower – massive stomp impact with shockwave and debris. */
    private void addHighHorsepower(double x, double y, double intensity,
                                   Timeline timeline) {
        // Central impact flash
        addFlashCircle(x, y, 28 * intensity, GROUND_CLAY, 0, 200, timeline);

        // Stomp shockwave
        Ellipse shockwave = new Ellipse(0, 0);
        shockwave.setCenterX(x);
        shockwave.setCenterY(y);
        shockwave.setFill(Color.TRANSPARENT);
        shockwave.setStroke(GROUND_BROWN);
        shockwave.setStrokeWidth(4 * intensity);
        shockwave.setOpacity(0);
        prepareTransientNode(shockwave);
        battleField.getChildren().add(shockwave);

        double waveR = 45 + 20 * intensity;
        KeyFrame sAppear = new KeyFrame(Duration.millis(20),
                new KeyValue(shockwave.opacityProperty(), 0.85));
        KeyFrame sExpand = new KeyFrame(Duration.millis(220),
                new KeyValue(shockwave.radiusXProperty(), waveR),
                new KeyValue(shockwave.radiusYProperty(), waveR * 0.5),
                new KeyValue(shockwave.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(sAppear, sExpand);
        registerCleanup(timeline, shockwave);

        // Flying debris
        int count = (int) (8 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon chunk = buildRockChunk(5 + random.nextDouble() * 6,
                    i % 2 == 0 ? GROUND_BROWN : GROUND_DARK);
            chunk.setLayoutX(x);
            chunk.setLayoutY(y);
            chunk.setOpacity(0);
            prepareTransientNode(chunk);
            battleField.getChildren().add(chunk);

            double angle = (i / (double) count) * 2 * Math.PI;
            double dist = 25 + random.nextDouble() * 25 * intensity;
            int delay = i * 15;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(chunk.opacityProperty(), 0.9));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(chunk.layoutXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(chunk.layoutYProperty(),
                            y + Math.sin(angle) * dist * 0.6 - 10),
                    new KeyValue(chunk.rotateProperty(), random.nextDouble() * 300));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(chunk.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, chunk);
        }

        addCrackLines(x, y, intensity, 20, timeline);
        addDustCloud(x, y, intensity * 1.3, 80, timeline);
    }

    /** stomping-tantrum – rapid repeated stomps with expanding cracks. */
    private void addStompingTantrum(double x, double y, double intensity,
                                   Timeline timeline) {
        int stomps = (int) (3 + 2 * intensity);
        for (int s = 0; s < stomps; s++) {
            int baseDelay = s * 100;

            // Stomp flash for each impact
            addFlashCircle(x, y, 16 * intensity, GROUND_CLAY, baseDelay, 90, timeline);

            // Small debris per stomp
            int frags = (int) (3 + 2 * intensity);
            for (int i = 0; i < frags; i++) {
                Polygon chunk = buildRockChunk(3 + random.nextDouble() * 5,
                        i % 2 == 0 ? GROUND_BROWN : GROUND_TAN);
                chunk.setLayoutX(x + (random.nextDouble() - 0.5) * 16);
                chunk.setLayoutY(y);
                chunk.setOpacity(0);
                prepareTransientNode(chunk);
                battleField.getChildren().add(chunk);

                double angle = random.nextDouble() * 2 * Math.PI;
                double dist = 15 + random.nextDouble() * 20 * intensity;
                int delay = baseDelay + i * 12;

                KeyFrame appear = new KeyFrame(Duration.millis(delay),
                        new KeyValue(chunk.opacityProperty(), 0.85));
                KeyFrame burst = new KeyFrame(Duration.millis(delay + 120),
                        new KeyValue(chunk.layoutXProperty(),
                                x + Math.cos(angle) * dist),
                        new KeyValue(chunk.layoutYProperty(),
                                y + Math.sin(angle) * dist * 0.5 - 8),
                        new KeyValue(chunk.rotateProperty(),
                                random.nextDouble() * 270));
                KeyFrame fade = new KeyFrame(Duration.millis(delay + 200),
                        new KeyValue(chunk.opacityProperty(), 0));

                timeline.getKeyFrames().addAll(appear, burst, fade);
                registerCleanup(timeline, chunk);
            }
        }

        addCrackLines(x, y, intensity * 1.2, 40, timeline);
        addDustCloud(x, y, intensity, 60, timeline);
    }

    // =================================================================
    // Eruption pillar animations
    // =================================================================

    /** earth-power – brown/tan pillars erupt upward from beneath the defender. */
    private void addEarthPower(double x, double y, double intensity,
                               Timeline timeline) {
        int count = (int) (4 + 3 * intensity);
        for (int i = 0; i < count; i++) {
            double w = 10 + random.nextDouble() * 8 * intensity;
            double h = 30 + random.nextDouble() * 25 * intensity;
            Rectangle pillar = new Rectangle(w, h);
            pillar.setFill(i % 2 == 0 ? GROUND_BROWN : GROUND_TAN);
            pillar.setStroke(GROUND_DARK);
            pillar.setStrokeWidth(1);
            pillar.setArcWidth(4);
            pillar.setArcHeight(4);
            double offsetX = (i - count / 2.0) * 16;
            pillar.setLayoutX(x + offsetX - w / 2);
            pillar.setLayoutY(y + 10);
            pillar.setOpacity(0);
            pillar.setScaleY(0.1);
            pillar.setEffect(new DropShadow(6, GROUND_DARK));
            prepareTransientNode(pillar);
            battleField.getChildren().add(pillar);

            int delay = i * 40;
            KeyFrame emerge = new KeyFrame(Duration.millis(delay),
                    new KeyValue(pillar.opacityProperty(), 1.0));
            KeyFrame full = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(pillar.scaleYProperty(), 1.2),
                    new KeyValue(pillar.layoutYProperty(), y - h * 0.6));
            KeyFrame settle = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(pillar.scaleYProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 400),
                    new KeyValue(pillar.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(emerge, full, settle, fade);
            registerCleanup(timeline, pillar);
        }

        addDustCloud(x, y, intensity, 60, timeline);
        addFlashCircle(x, y, 22 * intensity, GROUND_CLAY, 0, 180, timeline);
    }

    /** land's-wrath – wide line of earth pillars erupting across the target zone. */
    private void addLandsWrath(double x, double y, double intensity,
                               Timeline timeline) {
        int count = (int) (6 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            double w = 8 + random.nextDouble() * 7;
            double h = 25 + random.nextDouble() * 30 * intensity;
            Polygon pillar = buildPillarPolygon(w, h,
                    i % 3 == 0 ? GROUND_BROWN : i % 3 == 1 ? GROUND_CLAY : GROUND_TAN);
            double spread = (i - count / 2.0) * 14;
            pillar.setLayoutX(x + spread);
            pillar.setLayoutY(y + 15);
            pillar.setOpacity(0);
            pillar.setScaleY(0.05);
            pillar.setEffect(new DropShadow(5, GROUND_DARK));
            prepareTransientNode(pillar);
            battleField.getChildren().add(pillar);

            int delay = i * 30;
            KeyFrame emerge = new KeyFrame(Duration.millis(delay),
                    new KeyValue(pillar.opacityProperty(), 0.95));
            KeyFrame rise = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(pillar.scaleYProperty(), 1.3),
                    new KeyValue(pillar.layoutYProperty(), y - h * 0.5));
            KeyFrame settle = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(pillar.scaleYProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 420),
                    new KeyValue(pillar.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(emerge, rise, settle, fade);
            registerCleanup(timeline, pillar);
        }

        addDustCloud(x, y, intensity * 1.3, 40, timeline);
        addFlashCircle(x, y, 26 * intensity, GROUND_TAN, 0, 200, timeline);
    }

    /** precipice-blades – tall sharp blades of earth erupt violently. */
    private void addPrecipiceBlades(double x, double y, double intensity,
                                    Timeline timeline) {
        int count = (int) (5 + 3 * intensity);
        for (int i = 0; i < count; i++) {
            double size = 35 + random.nextDouble() * 25 * intensity;
            Polygon blade = buildBladePolygon(size,
                    i % 2 == 0 ? GROUND_DARK : GROUND_BROWN);
            double offset = (i - count / 2.0) * 18;
            blade.setLayoutX(x + offset);
            blade.setLayoutY(y + 20);
            blade.setOpacity(0);
            blade.setScaleY(0.05);
            blade.setRotate((random.nextDouble() - 0.5) * 15);
            blade.setEffect(new DropShadow(8, GROUND_DARK));
            prepareTransientNode(blade);
            battleField.getChildren().add(blade);

            int delay = i * 45;
            KeyFrame emerge = new KeyFrame(Duration.millis(delay),
                    new KeyValue(blade.opacityProperty(), 1.0));
            KeyFrame rise = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(blade.scaleYProperty(), 1.4),
                    new KeyValue(blade.layoutYProperty(), y - size * 0.5));
            KeyFrame settle = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(blade.scaleYProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 440),
                    new KeyValue(blade.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(emerge, rise, settle, fade);
            registerCleanup(timeline, blade);
        }

        addDustCloud(x, y, intensity * 1.5, 40, timeline);
        addFlashCircle(x, y, 28 * intensity, GROUND_CLAY, 0, 200, timeline);
    }

    // =================================================================
    // Sand swirl animations
    // =================================================================

    /** sand-tomb – spinning vortex of sand trapping the target. */
    private void addSandTomb(double x, double y, double intensity,
                             Timeline timeline) {
        int count = (int) (12 + 8 * intensity);
        for (int i = 0; i < count; i++) {
            Circle grain = new Circle(2 + random.nextDouble() * 3,
                    i % 3 == 0 ? GROUND_SAND : i % 3 == 1 ? GROUND_TAN : GROUND_CLAY);
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 10 + random.nextDouble() * 8;
            grain.setCenterX(x + Math.cos(angle) * radius);
            grain.setCenterY(y + Math.sin(angle) * radius * 0.5);
            grain.setOpacity(0);
            grain.setEffect(new GaussianBlur(1.5));
            prepareTransientNode(grain);
            battleField.getChildren().add(grain);

            double spiralR = 30 + 20 * intensity;
            double endAngle = angle + Math.PI * 3;
            int delay = i * 20;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(grain.opacityProperty(), 0.85));
            KeyFrame spiral = new KeyFrame(Duration.millis(delay + 350),
                    new KeyValue(grain.centerXProperty(),
                            x + Math.cos(endAngle) * spiralR),
                    new KeyValue(grain.centerYProperty(),
                            y + Math.sin(endAngle) * spiralR * 0.5 - 15));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 450),
                    new KeyValue(grain.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, spiral, fade);
            registerCleanup(timeline, grain);
        }

        // Central swirl haze
        Ellipse haze = new Ellipse(10, 6);
        haze.setCenterX(x);
        haze.setCenterY(y);
        haze.setFill(GROUND_SAND.deriveColor(0, 1, 1, 0.4));
        haze.setEffect(new GaussianBlur(8));
        haze.setOpacity(0);
        prepareTransientNode(haze);
        battleField.getChildren().add(haze);

        KeyFrame hAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(haze.opacityProperty(), 0.6));
        KeyFrame hExpand = new KeyFrame(Duration.millis(300),
                new KeyValue(haze.radiusXProperty(), 35 * intensity),
                new KeyValue(haze.radiusYProperty(), 20 * intensity));
        KeyFrame hFade = new KeyFrame(Duration.millis(480),
                new KeyValue(haze.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(hAppear, hExpand, hFade);
        registerCleanup(timeline, haze);
    }

    /** sandstorm – swirling sand particles engulfing the target area. */
    private void addSandstorm(double x, double y, double intensity,
                              Timeline timeline) {
        int count = (int) (16 + 10 * intensity);
        for (int i = 0; i < count; i++) {
            Circle sand = new Circle(1.5 + random.nextDouble() * 2.5,
                    i % 4 == 0 ? GROUND_SAND : i % 4 == 1 ? GROUND_TAN
                    : i % 4 == 2 ? GROUND_DUST : GROUND_CLAY);
            double startAngle = random.nextDouble() * 2 * Math.PI;
            double startR = 5 + random.nextDouble() * 15;
            sand.setCenterX(x + Math.cos(startAngle) * startR);
            sand.setCenterY(y + Math.sin(startAngle) * startR * 0.5);
            sand.setOpacity(0);
            prepareTransientNode(sand);
            battleField.getChildren().add(sand);

            double endAngle = startAngle + Math.PI * 2.5
                    + random.nextDouble() * Math.PI;
            double endR = 25 + random.nextDouble() * 30 * intensity;
            int delay = i * 18;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(sand.opacityProperty(), 0.75));
            KeyFrame mid = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(sand.centerXProperty(),
                            x + Math.cos(startAngle + Math.PI) * endR * 0.7),
                    new KeyValue(sand.centerYProperty(),
                            y + Math.sin(startAngle + Math.PI) * endR * 0.4 - 10));
            KeyFrame end = new KeyFrame(Duration.millis(delay + 400),
                    new KeyValue(sand.centerXProperty(),
                            x + Math.cos(endAngle) * endR),
                    new KeyValue(sand.centerYProperty(),
                            y + Math.sin(endAngle) * endR * 0.5 - 20),
                    new KeyValue(sand.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, mid, end);
            registerCleanup(timeline, sand);
        }

        addFlashCircle(x, y, 20 * intensity, GROUND_SAND, 0, 300, timeline);
    }

    /** scorching-sands – hot sand swirl with reddish glow. */
    private void addScorchingSands(double x, double y, double intensity,
                                   Timeline timeline) {
        Color scorchGlow = Color.web("#FF8A65");
        int count = (int) (14 + 8 * intensity);
        for (int i = 0; i < count; i++) {
            Circle grain = new Circle(2 + random.nextDouble() * 3,
                    i % 3 == 0 ? GROUND_SAND : i % 3 == 1 ? scorchGlow : GROUND_TAN);
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 8 + random.nextDouble() * 10;
            grain.setCenterX(x + Math.cos(angle) * radius);
            grain.setCenterY(y + Math.sin(angle) * radius * 0.5);
            grain.setOpacity(0);
            grain.setEffect(i % 3 == 1
                    ? new DropShadow(4, scorchGlow) : new GaussianBlur(1));
            prepareTransientNode(grain);
            battleField.getChildren().add(grain);

            double spiralR = 28 + 18 * intensity;
            double endAngle = angle + Math.PI * 2.8;
            int delay = i * 20;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(grain.opacityProperty(), 0.9));
            KeyFrame spiral = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(grain.centerXProperty(),
                            x + Math.cos(endAngle) * spiralR),
                    new KeyValue(grain.centerYProperty(),
                            y + Math.sin(endAngle) * spiralR * 0.5 - 12));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 420),
                    new KeyValue(grain.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, spiral, fade);
            registerCleanup(timeline, grain);
        }

        addFlashCircle(x, y, 22 * intensity, scorchGlow, 0, 250, timeline);
    }

    // =================================================================
    // Mud / sand impact animations
    // =================================================================

    /** mud-bomb / mud-shot – mud splatters outward on impact. */
    private void addMudSplatter(double x, double y, double intensity,
                                Timeline timeline) {
        int count = (int) (6 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Circle blob = new Circle(4 + random.nextDouble() * 5 * intensity,
                    i % 3 == 0 ? GROUND_MUD : i % 3 == 1 ? GROUND_BROWN : GROUND_CLAY);
            blob.setCenterX(x);
            blob.setCenterY(y);
            blob.setOpacity(0);
            blob.setEffect(new GaussianBlur(2));
            prepareTransientNode(blob);
            battleField.getChildren().add(blob);

            double angle = (i / (double) count) * 2 * Math.PI
                    + (random.nextDouble() - 0.5) * 0.5;
            double dist = 15 + random.nextDouble() * 25 * intensity;
            int delay = i * 15;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(blob.opacityProperty(), 0.9));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(blob.centerXProperty(),
                            x + Math.cos(angle) * dist),
                    new KeyValue(blob.centerYProperty(),
                            y + Math.sin(angle) * dist * 0.6),
                    new KeyValue(blob.radiusProperty(),
                            blob.getRadius() * 1.6));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(blob.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, blob);
        }

        addFlashCircle(x, y, 18 * intensity, GROUND_MUD, 0, 160, timeline);
        addDustCloud(x, y, intensity * 0.7, 100, timeline);
    }

    /** sand-attack – sand burst at target with lingering haze. */
    private void addSandSplatter(double x, double y, double intensity,
                                 Timeline timeline) {
        int count = (int) (10 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            Circle grain = new Circle(1.5 + random.nextDouble() * 2,
                    i % 3 == 0 ? GROUND_SAND : i % 3 == 1 ? GROUND_TAN : GROUND_DUST);
            grain.setCenterX(x + (random.nextDouble() - 0.5) * 10);
            grain.setCenterY(y + (random.nextDouble() - 0.5) * 10);
            grain.setOpacity(0);
            prepareTransientNode(grain);
            battleField.getChildren().add(grain);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 12 + random.nextDouble() * 20 * intensity;
            int delay = i * 12;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(grain.opacityProperty(), 0.8));
            KeyFrame scatter = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(grain.centerXProperty(),
                            x + Math.cos(angle) * dist),
                    new KeyValue(grain.centerYProperty(),
                            y + Math.sin(angle) * dist * 0.5));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(grain.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, scatter, fade);
            registerCleanup(timeline, grain);
        }

        addFlashCircle(x, y, 14 * intensity, GROUND_SAND, 0, 180, timeline);
    }

    // =================================================================
    // Dust cloud animation
    // =================================================================

    /** dig – underground approach followed by erupting dust cloud at target. */
    private void addDig(double x, double y, double intensity,
                        Timeline timeline) {
        // Dust burst upward
        int count = (int) (8 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            Circle dust = new Circle(4 + random.nextDouble() * 5,
                    i % 3 == 0 ? GROUND_DUST : i % 3 == 1 ? GROUND_SAND : GROUND_TAN);
            dust.setCenterX(x + (random.nextDouble() - 0.5) * 24);
            dust.setCenterY(y);
            dust.setOpacity(0);
            dust.setEffect(new GaussianBlur(4));
            prepareTransientNode(dust);
            battleField.getChildren().add(dust);

            double riseHeight = 30 + random.nextDouble() * 35 * intensity;
            double drift = (random.nextDouble() - 0.5) * 30;
            int delay = i * 22;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(dust.opacityProperty(), 0.8));
            KeyFrame rise = new KeyFrame(Duration.millis(delay + 250),
                    new KeyValue(dust.centerYProperty(), y - riseHeight),
                    new KeyValue(dust.centerXProperty(),
                            dust.getCenterX() + drift),
                    new KeyValue(dust.radiusProperty(),
                            dust.getRadius() * 2.5));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 380),
                    new KeyValue(dust.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, rise, fade);
            registerCleanup(timeline, dust);
        }

        // Ground debris tossed upward
        int debris = (int) (4 + 3 * intensity);
        for (int i = 0; i < debris; i++) {
            Polygon chunk = buildRockChunk(4 + random.nextDouble() * 5,
                    i % 2 == 0 ? GROUND_BROWN : GROUND_DARK);
            chunk.setLayoutX(x + (random.nextDouble() - 0.5) * 20);
            chunk.setLayoutY(y);
            chunk.setOpacity(0);
            prepareTransientNode(chunk);
            battleField.getChildren().add(chunk);

            double angle = -Math.PI / 2
                    + (random.nextDouble() - 0.5) * Math.PI * 0.6;
            double dist = 20 + random.nextDouble() * 25 * intensity;
            int delay = 30 + i * 25;

            KeyFrame cAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(chunk.opacityProperty(), 0.9));
            KeyFrame cBurst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(chunk.layoutXProperty(),
                            chunk.getLayoutX() + Math.cos(angle) * dist),
                    new KeyValue(chunk.layoutYProperty(),
                            y + Math.sin(angle) * dist),
                    new KeyValue(chunk.rotateProperty(),
                            random.nextDouble() * 360));
            KeyFrame cFade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(chunk.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(cAppear, cBurst, cFade);
            registerCleanup(timeline, chunk);
        }

        addFlashCircle(x, y, 24 * intensity, GROUND_TAN, 0, 200, timeline);
    }

    // =================================================================
    // Default ground animation (fallback)
    // =================================================================

    private void addDefaultGround(double x, double y, double intensity,
                                  Timeline timeline) {
        int count = (int) (6 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon chunk = buildRockChunk(5 + random.nextDouble() * 7,
                    i % 2 == 0 ? GROUND_BROWN : GROUND_CLAY);
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 10;
            chunk.setLayoutX(x + Math.cos(angle) * radius);
            chunk.setLayoutY(y + Math.sin(angle) * radius);
            chunk.setRotate(random.nextDouble() * 360);
            chunk.setOpacity(0);
            chunk.setEffect(new DropShadow(4, GROUND_DARK));
            prepareTransientNode(chunk);
            battleField.getChildren().add(chunk);

            double burstR = 28 + 15 * intensity;
            int delay = i * 25;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(chunk.opacityProperty(), 0.9));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(chunk.layoutXProperty(),
                            x + Math.cos(angle) * burstR),
                    new KeyValue(chunk.layoutYProperty(),
                            y + Math.sin(angle) * burstR),
                    new KeyValue(chunk.rotateProperty(),
                            chunk.getRotate() + 120));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(chunk.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, chunk);
        }

        addDustCloud(x, y, intensity, 60, timeline);
        addFlashCircle(x, y, 16 * intensity, GROUND_TAN, 0, 150, timeline);
    }

    // =================================================================
    // Shared helpers
    // =================================================================

    /** Irregular polygon resembling a rough earth chunk. */
    private Polygon buildRockChunk(double size, Color fill) {
        Polygon p = new Polygon(
                -size * 0.5, -size * 0.2,
                -size * 0.3, -size * 0.55,
                 size * 0.15, -size * 0.5,
                 size * 0.5,  -size * 0.15,
                 size * 0.35,  size * 0.4,
                -size * 0.1,   size * 0.5,
                -size * 0.45,  size * 0.2);
        p.setFill(fill);
        p.setStroke(GROUND_DARK);
        p.setStrokeWidth(1);
        return p;
    }

    /** Tall tapered polygon used for earth pillars. */
    private Polygon buildPillarPolygon(double w, double h, Color fill) {
        Polygon p = new Polygon(
                -w * 0.35,  0.0,
                -w * 0.25, -h * 0.8,
                 0.0,       -h,
                 w * 0.25,  -h * 0.8,
                 w * 0.35,   0.0);
        p.setFill(fill);
        p.setStroke(GROUND_DARK);
        p.setStrokeWidth(1);
        return p;
    }

    /** Sharp upward blade polygon for precipice-blades. */
    private Polygon buildBladePolygon(double size, Color fill) {
        Polygon p = new Polygon(
                 0.0,          -size,
                 size * 0.2,   -size * 0.3,
                 size * 0.3,    0.0,
                -size * 0.3,    0.0,
                -size * 0.2,   -size * 0.3);
        p.setFill(fill);
        p.setStroke(GROUND_DARK);
        p.setStrokeWidth(1.2);
        return p;
    }

    /** Pointed polygon for drill-run spinning projectile. */
    private Polygon buildDrillPolygon(double size, Color fill) {
        Polygon p = new Polygon(
                 size * 0.7,    0.0,
                 size * 0.15,  -size * 0.3,
                -size * 0.5,   -size * 0.2,
                -size * 0.5,    size * 0.2,
                 size * 0.15,   size * 0.3);
        p.setFill(fill);
        p.setStroke(GROUND_DARK);
        p.setStrokeWidth(1);
        return p;
    }

    /** Soft mud blob circle. */
    private Circle buildMudBlob(double radius, Color fill) {
        Circle c = new Circle(radius, fill);
        c.setStroke(GROUND_DARK);
        c.setStrokeWidth(1);
        return c;
    }

    /** Crack lines radiating from an impact point. */
    private void addCrackLines(double x, double y, double intensity,
                               int startDelay, Timeline timeline) {
        int count = (int) (5 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI
                    + (random.nextDouble() - 0.5) * 0.4;
            double length = 18 + random.nextDouble() * 25 * intensity;

            Line crack = new Line(x, y,
                    x + Math.cos(angle) * 3,
                    y + Math.sin(angle) * 3);
            crack.setStroke(GROUND_DARK);
            crack.setStrokeWidth(1.5 + random.nextDouble() * 1.5);
            crack.setOpacity(0);
            prepareTransientNode(crack);
            battleField.getChildren().add(crack);

            int delay = startDelay + i * 18;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(crack.opacityProperty(), 0.9));
            KeyFrame extend = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(crack.endXProperty(),
                            x + Math.cos(angle) * length),
                    new KeyValue(crack.endYProperty(),
                            y + Math.sin(angle) * length));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 340),
                    new KeyValue(crack.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, extend, fade);
            registerCleanup(timeline, crack);
        }
    }

    /** Expanding flash circle at a point, fading out. */
    private void addFlashCircle(double x, double y, double radius, Color color,
                                int startDelay, int fadeDuration,
                                Timeline timeline) {
        Circle flash = new Circle(0, color.deriveColor(0, 1, 1, 0.85));
        flash.setCenterX(x);
        flash.setCenterY(y);
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

    /** Dust cloud — several soft circles expanding and fading. */
    private void addDustCloud(double x, double y, double intensity,
                              int startDelay, Timeline timeline) {
        int count = (int) (4 + 3 * intensity);
        for (int i = 0; i < count; i++) {
            double r = 6 + random.nextDouble() * 6;
            Circle dust = new Circle(r, GROUND_DUST);
            dust.setCenterX(x + (random.nextDouble() - 0.5) * 22);
            dust.setCenterY(y + (random.nextDouble() - 0.5) * 14);
            dust.setOpacity(0);
            dust.setEffect(new GaussianBlur(5));
            prepareTransientNode(dust);
            battleField.getChildren().add(dust);

            int delay = startDelay + i * 25;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(dust.opacityProperty(), 0.55));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(dust.radiusProperty(), r * 2.5),
                    new KeyValue(dust.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand);
            registerCleanup(timeline, dust);
        }
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
