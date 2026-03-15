// RockEffects.java
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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class RockEffects {

    private final Pane battleField;
    private final Random random = new Random();

    // Rock colour palette
    private static final Color ROCK_BROWN     = Color.web("#8D6E63");
    private static final Color ROCK_TAN       = Color.web("#A1887F");
    private static final Color ROCK_DARK      = Color.web("#4E342E");
    private static final Color ROCK_GREY      = Color.web("#9E9E9E");
    private static final Color ROCK_LIGHT     = Color.web("#D7CCC8");
    private static final Color ROCK_SAND      = Color.web("#BCAAA4");
    private static final Color ROCK_CRYSTAL   = Color.web("#B0BEC5");
    private static final Color ROCK_AMBER     = Color.web("#FFB300");
    private static final Color ROCK_DUST      = Color.web("#EFEBE9");

    public RockEffects(Pane battleField) {
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
            // Crystal / meteor / shard moves
            case "ancient-power" -> addShardBurst(endX, endY, intensity, timeline);
            case "meteor-beam"   -> addMeteorBeam(startX, startY, endX, endY, intensity, timeline);
            case "power-gem"     -> addGemSparkle(startX, startY, endX, endY, intensity, timeline);
            case "stone-edge"    -> addStoneEdge(endX, endY, intensity, timeline);

            // Rock projectile burst moves
            case "rock-blast"    -> addRockBurst(startX, startY, endX, endY, intensity, timeline);
            case "rock-slide"    -> addRockSlide(endX, endY, intensity, timeline);
            case "rock-throw"    -> addRockThrow(startX, startY, endX, endY, intensity, timeline);
            case "rock-tomb"     -> addRockTomb(endX, endY, intensity, timeline);
            case "rock-wrecker"  -> addRockWrecker(startX, startY, endX, endY, intensity, timeline);

            // Heavy body slam / debris moves
            case "head-smash"    -> addHeadSmash(endX, endY, intensity, timeline);
            case "rollout"       -> addRollout(startX, startY, endX, endY, intensity, timeline);
            case "smack-down"    -> addSmackDown(startX, startY, endX, endY, intensity, timeline);

            default              -> addDefaultRocks(endX, endY, intensity, timeline);
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
        int count = (int) (3 + 4 * intensity);

        for (int i = 0; i < count; i++) {
            Polygon rock = buildRockPolygon(8 + random.nextDouble() * 8 * intensity,
                    i % 3 == 0 ? ROCK_BROWN : i % 3 == 1 ? ROCK_GREY : ROCK_DARK);
            rock.setLayoutX(startX + (random.nextDouble() - 0.5) * 20);
            rock.setLayoutY(startY + (random.nextDouble() - 0.5) * 20);
            rock.setOpacity(0);
            rock.setRotate(random.nextDouble() * 360);
            rock.setEffect(new DropShadow(6, ROCK_DARK));
            prepareTransientNode(rock);
            battleField.getChildren().add(rock);

            int delay = i * 40;
            double tx = endX + (random.nextDouble() - 0.5) * 30;
            double ty = endY + (random.nextDouble() - 0.5) * 30;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(rock.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(rock.layoutXProperty(), tx),
                    new KeyValue(rock.layoutYProperty(), ty),
                    new KeyValue(rock.rotateProperty(), rock.getRotate() + 180));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(rock.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, rock);
        }
    }

    // =================================================================
    // Crystal / shard animations
    // =================================================================

    /** ancient-power – crystalline shards burst outward from impact. */
    private void addShardBurst(double x, double y, double intensity,
                               Timeline timeline) {
        int count = (int) (8 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon shard = buildShardPolygon(10 + random.nextDouble() * 8 * intensity,
                    i % 3 == 0 ? ROCK_CRYSTAL : i % 3 == 1 ? ROCK_AMBER : ROCK_LIGHT);
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 12 + random.nextDouble() * 8;
            shard.setLayoutX(x + Math.cos(angle) * radius);
            shard.setLayoutY(y + Math.sin(angle) * radius);
            shard.setRotate(Math.toDegrees(angle));
            shard.setOpacity(0);
            shard.setEffect(new GaussianBlur(2));
            prepareTransientNode(shard);
            battleField.getChildren().add(shard);

            double burstR = 35 + 20 * intensity;
            int delay = i * 22;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(shard.opacityProperty(), 1.0));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(shard.layoutXProperty(), x + Math.cos(angle) * burstR),
                    new KeyValue(shard.layoutYProperty(), y + Math.sin(angle) * burstR),
                    new KeyValue(shard.scaleXProperty(), 1.4),
                    new KeyValue(shard.scaleYProperty(), 1.4));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(shard.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, shard);
        }

        addFlashCircle(x, y, 22 * intensity, ROCK_AMBER, 0, 180, timeline);
    }

    /** meteor-beam – large meteor streaks from origin to target with trailing dust. */
    private void addMeteorBeam(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        // Core meteor
        Circle meteor = new Circle(12 * intensity, ROCK_AMBER);
        meteor.setCenterX(sx);
        meteor.setCenterY(sy);
        meteor.setOpacity(0);
        meteor.setEffect(new DropShadow(18, Color.ORANGERED));
        prepareTransientNode(meteor);
        battleField.getChildren().add(meteor);

        KeyFrame mAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(meteor.opacityProperty(), 1.0));
        KeyFrame mTravel = new KeyFrame(Duration.millis(280),
                new KeyValue(meteor.centerXProperty(), ex),
                new KeyValue(meteor.centerYProperty(), ey));
        KeyFrame mFade   = new KeyFrame(Duration.millis(340),
                new KeyValue(meteor.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(mAppear, mTravel, mFade);
        registerCleanup(timeline, meteor);

        // Trailing dust particles
        int trails = (int) (6 * intensity);
        for (int i = 0; i < trails; i++) {
            Circle dust = new Circle(3 + random.nextDouble() * 3, ROCK_DUST);
            dust.setCenterX(sx);
            dust.setCenterY(sy);
            dust.setOpacity(0);
            dust.setEffect(new GaussianBlur(3));
            prepareTransientNode(dust);
            battleField.getChildren().add(dust);

            int delay = 30 + i * 35;
            double frac = (i + 1.0) / (trails + 1);
            double mx = sx + (ex - sx) * frac + (random.nextDouble() - 0.5) * 20;
            double my = sy + (ey - sy) * frac + (random.nextDouble() - 0.5) * 20;

            KeyFrame dAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(dust.opacityProperty(), 0.7));
            KeyFrame dTravel = new KeyFrame(Duration.millis(delay + 120),
                    new KeyValue(dust.centerXProperty(), mx),
                    new KeyValue(dust.centerYProperty(), my));
            KeyFrame dFade   = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(dust.opacityProperty(), 0));
            timeline.getKeyFrames().addAll(dAppear, dTravel, dFade);
            registerCleanup(timeline, dust);
        }

        addFlashCircle(ex, ey, 28 * intensity, ROCK_AMBER, 260, 140, timeline);
    }

    /** power-gem – sparkling gem rays travel from attacker to target. */
    private void addGemSparkle(double sx, double sy, double ex, double ey,
                               double intensity, Timeline timeline) {
        int count = (int) (8 + 5 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon gem = buildShardPolygon(6 + random.nextDouble() * 6,
                    i % 4 == 0 ? ROCK_CRYSTAL : i % 4 == 1 ? ROCK_AMBER
                    : i % 4 == 2 ? Color.web("#E1BEE7") : ROCK_LIGHT);
            gem.setLayoutX(sx + (random.nextDouble() - 0.5) * 16);
            gem.setLayoutY(sy + (random.nextDouble() - 0.5) * 16);
            gem.setOpacity(0);
            gem.setRotate(random.nextDouble() * 360);
            gem.setEffect(new DropShadow(5, ROCK_AMBER));
            prepareTransientNode(gem);
            battleField.getChildren().add(gem);

            int delay = i * 25;
            double tx = ex + (random.nextDouble() - 0.5) * 24;
            double ty = ey + (random.nextDouble() - 0.5) * 24;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(gem.opacityProperty(), 1.0));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(gem.layoutXProperty(), tx),
                    new KeyValue(gem.layoutYProperty(), ty),
                    new KeyValue(gem.rotateProperty(), gem.getRotate() + 120));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(gem.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, gem);
        }

        addFlashCircle(ex, ey, 20 * intensity, ROCK_CRYSTAL, 180, 120, timeline);
    }

    /** stone-edge – jagged stone pillars shoot upward from below the target. */
    private void addStoneEdge(double x, double y, double intensity,
                              Timeline timeline) {
        int count = (int) (5 + 3 * intensity);
        for (int i = 0; i < count; i++) {
            double size = 18 + random.nextDouble() * 14 * intensity;
            Polygon spike = buildSpikePolygon(size,
                    i % 2 == 0 ? ROCK_DARK : ROCK_GREY);
            double offset = (i - count / 2.0) * 14;
            spike.setLayoutX(x + offset);
            spike.setLayoutY(y + 20);
            spike.setOpacity(0);
            spike.setScaleY(0.1);
            spike.setEffect(new DropShadow(6, ROCK_DARK));
            prepareTransientNode(spike);
            battleField.getChildren().add(spike);

            int delay = i * 35;
            KeyFrame emerge = new KeyFrame(Duration.millis(delay),
                    new KeyValue(spike.opacityProperty(), 1.0));
            KeyFrame full = new KeyFrame(Duration.millis(delay + 140),
                    new KeyValue(spike.scaleYProperty(), 1.2),
                    new KeyValue(spike.layoutYProperty(), y - size * 0.4));
            KeyFrame settle = new KeyFrame(Duration.millis(delay + 220),
                    new KeyValue(spike.scaleYProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 380),
                    new KeyValue(spike.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(emerge, full, settle, fade);
            registerCleanup(timeline, spike);
        }

        addFlashCircle(x, y, 18 * intensity, ROCK_BROWN, 0, 160, timeline);
    }

    // =================================================================
    // Rock projectile animations
    // =================================================================

    /** rock-blast – multiple boulders hurled in rapid succession. */
    private void addRockBurst(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        int count = (int) (4 + 3 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon rock = buildRockPolygon(10 + random.nextDouble() * 6,
                    i % 2 == 0 ? ROCK_BROWN : ROCK_GREY);
            rock.setLayoutX(sx + (random.nextDouble() - 0.5) * 18);
            rock.setLayoutY(sy + (random.nextDouble() - 0.5) * 18);
            rock.setOpacity(0);
            rock.setRotate(random.nextDouble() * 360);
            rock.setEffect(new DropShadow(5, ROCK_DARK));
            prepareTransientNode(rock);
            battleField.getChildren().add(rock);

            int delay = i * 60;
            double tx = ex + (random.nextDouble() - 0.5) * 30;
            double ty = ey + (random.nextDouble() - 0.5) * 30;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(rock.opacityProperty(), 0.95));
            KeyFrame travel = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(rock.layoutXProperty(), tx),
                    new KeyValue(rock.layoutYProperty(), ty),
                    new KeyValue(rock.rotateProperty(), rock.getRotate() + 200));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 250),
                    new KeyValue(rock.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, travel, fade);
            registerCleanup(timeline, rock);
        }

        addDustCloud(ex, ey, intensity, 140, timeline);
    }

    /** rock-slide – rocks cascade downward onto target. */
    private void addRockSlide(double x, double y, double intensity,
                              Timeline timeline) {
        int count = (int) (6 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            double size = 8 + random.nextDouble() * 10 * intensity;
            Polygon rock = buildRockPolygon(size,
                    i % 3 == 0 ? ROCK_BROWN : i % 3 == 1 ? ROCK_GREY : ROCK_TAN);
            double offsetX = (random.nextDouble() - 0.5) * 50;
            rock.setLayoutX(x + offsetX);
            rock.setLayoutY(y - 60 - random.nextDouble() * 30);
            rock.setOpacity(0);
            rock.setRotate(random.nextDouble() * 360);
            rock.setEffect(new DropShadow(4, ROCK_DARK));
            prepareTransientNode(rock);
            battleField.getChildren().add(rock);

            int delay = i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(rock.opacityProperty(), 0.95));
            KeyFrame fall = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(rock.layoutYProperty(), y + (random.nextDouble() - 0.5) * 16),
                    new KeyValue(rock.rotateProperty(), rock.getRotate() + 90 + random.nextDouble() * 90));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(rock.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, fall, fade);
            registerCleanup(timeline, rock);
        }

        addDustCloud(x, y, intensity, 120, timeline);
    }

    /** rock-throw – single rock projectile from attacker to target. */
    private void addRockThrow(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        Polygon rock = buildRockPolygon(14 * intensity, ROCK_BROWN);
        rock.setLayoutX(sx);
        rock.setLayoutY(sy);
        rock.setOpacity(0);
        rock.setEffect(new DropShadow(8, ROCK_DARK));
        prepareTransientNode(rock);
        battleField.getChildren().add(rock);

        KeyFrame appear = new KeyFrame(Duration.millis(0),
                new KeyValue(rock.opacityProperty(), 1.0));
        KeyFrame travel = new KeyFrame(Duration.millis(250),
                new KeyValue(rock.layoutXProperty(), ex),
                new KeyValue(rock.layoutYProperty(), ey),
                new KeyValue(rock.rotateProperty(), 360));
        KeyFrame fade = new KeyFrame(Duration.millis(320),
                new KeyValue(rock.opacityProperty(), 0));

        timeline.getKeyFrames().addAll(appear, travel, fade);
        registerCleanup(timeline, rock);

        addDustCloud(ex, ey, intensity, 220, timeline);
        addFlashCircle(ex, ey, 14 * intensity, ROCK_SAND, 230, 100, timeline);
    }

    /** rock-tomb – rocks fall from above and pile around the target. */
    private void addRockTomb(double x, double y, double intensity,
                             Timeline timeline) {
        int count = (int) (5 + 3 * intensity);
        for (int i = 0; i < count; i++) {
            double size = 10 + random.nextDouble() * 8 * intensity;
            Polygon rock = buildRockPolygon(size,
                    i % 2 == 0 ? ROCK_DARK : ROCK_BROWN);
            double angle = (i / (double) count) * 2 * Math.PI;
            // Rocks start from above the battlefield, spread horizontally around
            // the target but originate from the top of the screen
            double spreadX = (random.nextDouble() - 0.5) * 40;
            rock.setLayoutX(x + spreadX);
            rock.setLayoutY(-20 - random.nextDouble() * 40);   // above the viewport
            rock.setOpacity(0);
            rock.setEffect(new DropShadow(5, ROCK_DARK));
            prepareTransientNode(rock);
            battleField.getChildren().add(rock);

            double endR = 12 + random.nextDouble() * 10;
            int delay = i * 50;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(rock.opacityProperty(), 0.95));
            KeyFrame land = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(rock.layoutXProperty(), x + Math.cos(angle) * endR),
                    new KeyValue(rock.layoutYProperty(), y + Math.sin(angle) * endR * 0.5));
            KeyFrame settle = new KeyFrame(Duration.millis(delay + 500),
                    new KeyValue(rock.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, land, settle);
            registerCleanup(timeline, rock);
        }

        addDustCloud(x, y, intensity * 0.8, 100, timeline);
    }

    /** rock-wrecker – massive boulder hurled at target with explosive debris. */
    private void addRockWrecker(double sx, double sy, double ex, double ey,
                                double intensity, Timeline timeline) {
        // Massive boulder
        Polygon boulder = buildRockPolygon(22 * intensity, ROCK_DARK);
        boulder.setLayoutX(sx);
        boulder.setLayoutY(sy);
        boulder.setOpacity(0);
        boulder.setEffect(new DropShadow(14, ROCK_DARK));
        prepareTransientNode(boulder);
        battleField.getChildren().add(boulder);

        KeyFrame bAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(boulder.opacityProperty(), 1.0));
        KeyFrame bTravel = new KeyFrame(Duration.millis(300),
                new KeyValue(boulder.layoutXProperty(), ex),
                new KeyValue(boulder.layoutYProperty(), ey),
                new KeyValue(boulder.rotateProperty(), 540));
        KeyFrame bFade = new KeyFrame(Duration.millis(380),
                new KeyValue(boulder.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(bAppear, bTravel, bFade);
        registerCleanup(timeline, boulder);

        // Debris burst on impact
        int debris = (int) (8 + 5 * intensity);
        for (int i = 0; i < debris; i++) {
            Polygon frag = buildRockPolygon(4 + random.nextDouble() * 5,
                    i % 2 == 0 ? ROCK_BROWN : ROCK_GREY);
            frag.setLayoutX(ex);
            frag.setLayoutY(ey);
            frag.setOpacity(0);
            prepareTransientNode(frag);
            battleField.getChildren().add(frag);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 25 + random.nextDouble() * 30 * intensity;
            int delay = 280 + i * 12;

            KeyFrame fAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(frag.opacityProperty(), 0.9));
            KeyFrame fBurst = new KeyFrame(Duration.millis(delay + 160),
                    new KeyValue(frag.layoutXProperty(), ex + Math.cos(angle) * dist),
                    new KeyValue(frag.layoutYProperty(), ey + Math.sin(angle) * dist),
                    new KeyValue(frag.rotateProperty(), random.nextDouble() * 360));
            KeyFrame fFade = new KeyFrame(Duration.millis(delay + 260),
                    new KeyValue(frag.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(fAppear, fBurst, fFade);
            registerCleanup(timeline, frag);
        }

        addFlashCircle(ex, ey, 30 * intensity, ROCK_SAND, 270, 160, timeline);
    }

    // =================================================================
    // Heavy body-slam / debris animations
    // =================================================================

    /** head-smash – violent impact with debris and dust cloud. */
    private void addHeadSmash(double x, double y, double intensity,
                              Timeline timeline) {
        // Central impact flash
        addFlashCircle(x, y, 26 * intensity, ROCK_SAND, 0, 200, timeline);

        // Debris fragments
        int count = (int) (10 + 6 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon frag = buildRockPolygon(5 + random.nextDouble() * 7,
                    i % 3 == 0 ? ROCK_BROWN : i % 3 == 1 ? ROCK_GREY : ROCK_TAN);
            frag.setLayoutX(x);
            frag.setLayoutY(y);
            frag.setOpacity(0);
            prepareTransientNode(frag);
            battleField.getChildren().add(frag);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 20 + random.nextDouble() * 35 * intensity;
            int delay = i * 15;

            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(frag.opacityProperty(), 0.95));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 180),
                    new KeyValue(frag.layoutXProperty(), x + Math.cos(angle) * dist),
                    new KeyValue(frag.layoutYProperty(), y + Math.sin(angle) * dist),
                    new KeyValue(frag.rotateProperty(), random.nextDouble() * 360));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 300),
                    new KeyValue(frag.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, frag);
        }

        addDustCloud(x, y, intensity * 1.2, 60, timeline);
    }

    /** rollout – rolling boulder from attacker to target with dust trail. */
    private void addRollout(double sx, double sy, double ex, double ey,
                            double intensity, Timeline timeline) {
        Circle boulder = new Circle(14 * intensity, ROCK_BROWN);
        boulder.setStroke(ROCK_DARK);
        boulder.setStrokeWidth(2);
        boulder.setCenterX(sx);
        boulder.setCenterY(sy);
        boulder.setOpacity(0);
        boulder.setEffect(new DropShadow(8, ROCK_DARK));
        prepareTransientNode(boulder);
        battleField.getChildren().add(boulder);

        KeyFrame bAppear = new KeyFrame(Duration.millis(0),
                new KeyValue(boulder.opacityProperty(), 1.0));
        KeyFrame bTravel = new KeyFrame(Duration.millis(300),
                new KeyValue(boulder.centerXProperty(), ex),
                new KeyValue(boulder.centerYProperty(), ey));
        KeyFrame bFade   = new KeyFrame(Duration.millis(380),
                new KeyValue(boulder.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(bAppear, bTravel, bFade);
        registerCleanup(timeline, boulder);

        // Dust trail along path
        int trails = (int) (5 * intensity);
        for (int i = 0; i < trails; i++) {
            Circle dust = new Circle(4 + random.nextDouble() * 3, ROCK_DUST);
            double frac = (i + 1.0) / (trails + 1);
            double mx = sx + (ex - sx) * frac + (random.nextDouble() - 0.5) * 12;
            double my = sy + (ey - sy) * frac + (random.nextDouble() - 0.5) * 12;
            dust.setCenterX(mx);
            dust.setCenterY(my);
            dust.setOpacity(0);
            dust.setEffect(new GaussianBlur(3));
            prepareTransientNode(dust);
            battleField.getChildren().add(dust);

            int delay = 60 + i * 40;
            KeyFrame dAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(dust.opacityProperty(), 0.6));
            KeyFrame dFade   = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(dust.opacityProperty(), 0),
                    new KeyValue(dust.radiusProperty(), dust.getRadius() * 2));
            timeline.getKeyFrames().addAll(dAppear, dFade);
            registerCleanup(timeline, dust);
        }

        addFlashCircle(ex, ey, 16 * intensity, ROCK_SAND, 280, 120, timeline);
    }

    /** smack-down – stone projectile slams target downward with debris. */
    private void addSmackDown(double sx, double sy, double ex, double ey,
                              double intensity, Timeline timeline) {
        Polygon rock = buildRockPolygon(12 * intensity, ROCK_DARK);
        rock.setLayoutX(sx);
        rock.setLayoutY(sy);
        rock.setOpacity(0);
        rock.setEffect(new DropShadow(8, ROCK_DARK));
        prepareTransientNode(rock);
        battleField.getChildren().add(rock);

        KeyFrame appear = new KeyFrame(Duration.millis(0),
                new KeyValue(rock.opacityProperty(), 1.0));
        KeyFrame travel = new KeyFrame(Duration.millis(200),
                new KeyValue(rock.layoutXProperty(), ex),
                new KeyValue(rock.layoutYProperty(), ey),
                new KeyValue(rock.rotateProperty(), 270));
        KeyFrame fade = new KeyFrame(Duration.millis(280),
                new KeyValue(rock.opacityProperty(), 0));
        timeline.getKeyFrames().addAll(appear, travel, fade);
        registerCleanup(timeline, rock);

        // Small fragments on impact
        int frags = (int) (5 + 3 * intensity);
        for (int i = 0; i < frags; i++) {
            Polygon frag = buildRockPolygon(3 + random.nextDouble() * 4,
                    i % 2 == 0 ? ROCK_GREY : ROCK_TAN);
            frag.setLayoutX(ex);
            frag.setLayoutY(ey);
            frag.setOpacity(0);
            prepareTransientNode(frag);
            battleField.getChildren().add(frag);

            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 15 + random.nextDouble() * 18;
            int delay = 180 + i * 15;

            KeyFrame fAppear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(frag.opacityProperty(), 0.85));
            KeyFrame fBurst = new KeyFrame(Duration.millis(delay + 150),
                    new KeyValue(frag.layoutXProperty(), ex + Math.cos(angle) * dist),
                    new KeyValue(frag.layoutYProperty(), ey + Math.sin(angle) * dist));
            KeyFrame fFade = new KeyFrame(Duration.millis(delay + 240),
                    new KeyValue(frag.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(fAppear, fBurst, fFade);
            registerCleanup(timeline, frag);
        }

        addDustCloud(ex, ey, intensity, 170, timeline);
    }

    // =================================================================
    // Default rock animation (fallback)
    // =================================================================

    private void addDefaultRocks(double x, double y, double intensity,
                                 Timeline timeline) {
        int count = (int) (6 + 4 * intensity);
        for (int i = 0; i < count; i++) {
            Polygon rock = buildRockPolygon(6 + random.nextDouble() * 8,
                    i % 2 == 0 ? ROCK_BROWN : ROCK_GREY);
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 10;
            rock.setLayoutX(x + Math.cos(angle) * radius);
            rock.setLayoutY(y + Math.sin(angle) * radius);
            rock.setRotate(random.nextDouble() * 360);
            rock.setOpacity(0);
            rock.setEffect(new DropShadow(4, ROCK_DARK));
            prepareTransientNode(rock);
            battleField.getChildren().add(rock);

            double burstR = 30 + 15 * intensity;
            int delay = i * 25;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                    new KeyValue(rock.opacityProperty(), 0.9));
            KeyFrame burst = new KeyFrame(Duration.millis(delay + 200),
                    new KeyValue(rock.layoutXProperty(), x + Math.cos(angle) * burstR),
                    new KeyValue(rock.layoutYProperty(), y + Math.sin(angle) * burstR),
                    new KeyValue(rock.rotateProperty(), rock.getRotate() + 120));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 320),
                    new KeyValue(rock.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, burst, fade);
            registerCleanup(timeline, rock);
        }

        addFlashCircle(x, y, 16 * intensity, ROCK_SAND, 0, 150, timeline);
    }

    // =================================================================
    // Shared helpers
    // =================================================================

    /** Irregular polygon resembling a rough rock/boulder. */
    private Polygon buildRockPolygon(double size, Color fill) {
        Polygon p = new Polygon(
                -size * 0.5, -size * 0.2,
                -size * 0.3, -size * 0.55,
                 size * 0.15, -size * 0.5,
                 size * 0.5,  -size * 0.15,
                 size * 0.35,  size * 0.4,
                -size * 0.1,   size * 0.5,
                -size * 0.45,  size * 0.2);
        p.setFill(fill);
        p.setStroke(ROCK_DARK);
        p.setStrokeWidth(1);
        return p;
    }

    /** Diamond-shaped crystal shard. */
    private Polygon buildShardPolygon(double size, Color fill) {
        Polygon p = new Polygon(
                0.0,          -size * 0.7,
                size * 0.3,    0.0,
                0.0,           size * 0.7,
                -size * 0.3,   0.0);
        p.setFill(fill);
        p.setStroke(fill.darker());
        p.setStrokeWidth(1);
        return p;
    }

    /** Triangular spike pointing upward (for stone-edge pillars). */
    private Polygon buildSpikePolygon(double size, Color fill) {
        Polygon p = new Polygon(
                0.0,           -size,
                size * 0.35,    0.0,
                -size * 0.35,   0.0);
        p.setFill(fill);
        p.setStroke(ROCK_DARK);
        p.setStrokeWidth(1);
        return p;
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
            Circle dust = new Circle(r, ROCK_DUST);
            dust.setCenterX(x + (random.nextDouble() - 0.5) * 20);
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
