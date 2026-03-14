// BattleAnimationManager.java
package com.example.pokemonbattle.util;

import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.util.effects.ElectricEffects;
import com.example.pokemonbattle.util.effects.FightingEffects;
import com.example.pokemonbattle.util.effects.FireEffects;
import com.example.pokemonbattle.util.effects.IceEffects;
import com.example.pokemonbattle.util.effects.WaterEffects;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class BattleAnimationManager {

    private final ImageView playerSprite;
    private final ImageView opponentSprite;
    private final Pane battleField;

    // Animation constants
    private static final double ATTACK_DISTANCE_FULL   = 120.0;
    private static final double ATTACK_DISTANCE_SLIGHT = 30.0;
    private static final double ATTACK_DURATION_MS     = 200.0;
    private static final double RETURN_DURATION_MS     = 250.0;
    private static final double IMPACT_SHAKE_DISTANCE  = 8.0;
    private static final double IMPACT_SCALE_REDUCTION = 0.88;

    // Type effect handlers
    private final ElectricEffects electricEffects;
    private final IceEffects      iceEffects;
    private final FireEffects     fireEffects;
    private final FightingEffects fightingEffects;
    private final WaterEffects    waterEffects;

    public BattleAnimationManager(ImageView playerSprite, ImageView opponentSprite, Pane battleField) {
        this.playerSprite   = playerSprite;
        this.opponentSprite = opponentSprite;
        this.battleField    = battleField;

        this.electricEffects = new ElectricEffects(battleField);
        this.iceEffects      = new IceEffects(battleField);
        this.fireEffects     = new FireEffects(battleField);
        this.fightingEffects = new FightingEffects(battleField);
        this.waterEffects    = new WaterEffects(battleField);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    public void playAttackAnimation(ImageView attacker, ImageView defender, Move move, Runnable onComplete) {
        double attackerOriginalX      = attacker.getTranslateX();
        double attackerOriginalY      = attacker.getTranslateY();
        double attackerOriginalScaleX = attacker.getScaleX();
        double attackerOriginalScaleY = attacker.getScaleY();
        double defenderOriginalX      = defender.getTranslateX();
        double defenderOriginalY      = defender.getTranslateY();
        double defenderOriginalScaleX = defender.getScaleX();
        double defenderOriginalScaleY = defender.getScaleY();

        boolean attackingRight = (attacker == playerSprite);

        String  moveName    = (move != null && move.getName() != null) ? move.getName().toLowerCase() : "tackle";
        String  moveType    = (move != null && move.getType() != null) ? move.getType().toLowerCase() : "normal";
        String  damageClass = (move != null && move.getDamage_class() != null)
            ? move.getDamage_class().toLowerCase()
            : "physical";
        Integer powerVal    = (move != null) ? move.getPower() : null;
        int     movePower   = (powerVal != null) ? powerVal : 50;

        double attackDistance = getAttackDistance(moveType, moveName, damageClass);
        attackDistance = attackingRight ? attackDistance : -attackDistance;

        // Ranged / special-movement moves bypass the standard melee sequence
        if (isRangedMove(moveName, moveType, damageClass)) {
            playRangedAnimation(attacker, defender, moveName, moveType, movePower,
                    defenderOriginalX, defenderOriginalY,
                    defenderOriginalScaleX, defenderOriginalScaleY, onComplete);
            return;
        }

        // ── Standard melee sequence ───────────────────────────────────────────
        TranslateTransition rush = new TranslateTransition(Duration.millis(ATTACK_DURATION_MS), attacker);
        rush.setByX(attackDistance);
        rush.setInterpolator(Interpolator.EASE_IN);

        Timeline movementEffect = createMovementEffect(attacker, moveType, attackingRight, moveName);

        double attackerX = attacker.getLayoutX() + attacker.getFitWidth()  / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        ParallelTransition impact = createImpactEffect(defender, attackerX, attackerY,
                moveName, moveType, movePower, defenderOriginalX);

        TranslateTransition retreat = new TranslateTransition(Duration.millis(RETURN_DURATION_MS), attacker);
        retreat.setToX(attackerOriginalX);
        retreat.setToY(attackerOriginalY);
        retreat.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition recovery = createRecoveryEffect(
                defender, defenderOriginalX, defenderOriginalY,
                defenderOriginalScaleX, defenderOriginalScaleY);

        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(rush, movementEffect),
                impact,
                new ParallelTransition(retreat, recovery));

        sequence.setOnFinished(e -> {
            attacker.setTranslateX(attackerOriginalX);
            attacker.setTranslateY(attackerOriginalY);
            attacker.setScaleX(attackerOriginalScaleX);
            attacker.setScaleY(attackerOriginalScaleY);
            defender.setScaleX(defenderOriginalScaleX);
            defender.setScaleY(defenderOriginalScaleY);
            defender.setTranslateX(defenderOriginalX);
            defender.setTranslateY(defenderOriginalY);
            if (onComplete != null) onComplete.run();
        });

        sequence.play();
    }

    // =========================================================================
    // DISTANCE / RANGED ROUTING
    // =========================================================================

    private double getAttackDistance(String moveType, String moveName, String damageClass) {
        // Ice: melee punch closes gap fully, everything else lunges slightly
        if (moveType.equals("ice")) {
            return moveName.contains("punch") ? ATTACK_DISTANCE_FULL : ATTACK_DISTANCE_SLIGHT;
        }

        // Fire: named melee moves close gap fully, all others lunge slightly
        if (moveType.equals("fire")) {
            return switch (moveName) {
                case "fire-punch", "fire-fang", "blaze-kick", "flare-blitz",
                     "flame-charge", "flame-wheel", "raging-fury",
                     "flame-burst", "temper-flare" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Water: crabhammer is the only true melee
        if (moveType.equals("water")) {
            return moveName.equals("crabhammer") ? ATTACK_DISTANCE_FULL : ATTACK_DISTANCE_SLIGHT;
        }

        // Ranged moves don't move the attacker at all (handled via playRangedAnimation)
        if (isRangedMove(moveName, moveType, damageClass)) return 0;

        return ATTACK_DISTANCE_FULL;
    }

    /**
     * Returns true for moves whose primary visual travels from attacker to defender
     * as a projectile/beam/wave — the attacker sprite does NOT rush across.
     */
    private boolean isRangedMove(String moveName, String moveType, String damageClass) {
        // Ice beam-type moves
        if (moveName.contains("beam") || moveName.contains("spear")) return true;

        return switch (moveType) {
            // Fire ranged / area moves — attacker only lunges slightly
            case "fire" -> switch (moveName) {
                case "flamethrower", "overheat", "heat-wave", "blast-burn",
                     "mystical-fire", "ember", "fire-spin", "magma-storm",
                     "lava-plume", "eruption", "inferno", "fire-pledge",
                     "fire-blast", "incinerate", "burning-jealousy",
                     "sacred-fire", "burn-up" -> true;
                default -> false;
            };
            case "water" -> !moveName.equals("crabhammer");
            case "electric" -> damageClass.equals("special");
            default -> false;
        };
    }

    // =========================================================================
    // RANGED ANIMATION PATH
    // =========================================================================

    private void playRangedAnimation(ImageView attacker, ImageView defender,
            String moveName, String moveType, int movePower,
            double defenderOriginalX, double defenderOriginalY,
            double defenderOriginalScaleX, double defenderOriginalScaleY,
            Runnable onComplete) {

        double attackerX = attacker.getLayoutX() + attacker.getFitWidth()  / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        double defenderX = defender.getLayoutX() + defender.getFitWidth()  / 2;
        double defenderY = defender.getLayoutY() + defender.getFitHeight() / 2;

        Timeline leadEffect = null;

        switch (moveType) {
            case "ice" -> leadEffect = iceEffects.createBeamEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower);
            case "electric" -> leadEffect = electricEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower);
            case "water" -> {
            Timeline wt = new Timeline();
            waterEffects.createImpactEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, wt);
            leadEffect = wt;
            }
            case "fire" -> {
            Timeline ft = new Timeline();
            fireEffects.createImpactEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, ft);
            leadEffect = ft;
            }
            default -> {
            }
        }

        if (leadEffect != null) {
            EventHandler<ActionEvent> previous = leadEffect.getOnFinished();
            leadEffect.setOnFinished(e -> {
                if (previous != null) previous.handle(e);

                ParallelTransition impact = createImpactEffect(
                        defender, attackerX, attackerY,
                        moveName, moveType, movePower, defenderOriginalX);
                ParallelTransition recovery = createRecoveryEffect(
                        defender, defenderOriginalX, defenderOriginalY,
                        defenderOriginalScaleX, defenderOriginalScaleY);

                SequentialTransition seq = new SequentialTransition(impact, recovery);
                seq.setOnFinished(ev -> {
                    defender.setScaleX(defenderOriginalScaleX);
                    defender.setScaleY(defenderOriginalScaleY);
                    defender.setTranslateX(defenderOriginalX);
                    defender.setTranslateY(defenderOriginalY);
                    if (onComplete != null) onComplete.run();
                });
                seq.play();
            });
            leadEffect.play();
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    // =========================================================================
    // MOVEMENT EFFECT (during attacker rush)
    // =========================================================================

    private Timeline createMovementEffect(ImageView attacker, String moveType,
            boolean attackingRight, String moveName) {
        Timeline effect = new Timeline();

        double attackerX = attacker.getLayoutX() + attacker.getFitWidth()  / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;

        if (moveType.equals("electric")) {
            electricEffects.addMovementSparks(attackerX, attackerY, attackingRight, effect);
        } else if (moveName.equals("flare-blitz") || moveName.equals("flame-wheel")
                || moveName.equals("flame-charge")) {
            fireEffects.addChargeTrailForMove(moveName, attackerX, attackerY, attackingRight, effect);
        }

        return effect;
    }

    // =========================================================================
    // IMPACT EFFECT
    // =========================================================================

    private ParallelTransition createImpactEffect(ImageView defender,
            double attackerX, double attackerY,
            String moveName, String moveType, int movePower,
            double defenderBaseTranslateX) {

        Timeline shake = createShakeEffect(defender, movePower, defenderBaseTranslateX);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(80), defender);
        shrink.setToX(IMPACT_SCALE_REDUCTION);
        shrink.setToY(IMPACT_SCALE_REDUCTION);
        shrink.setAutoReverse(true);
        shrink.setCycleCount(2);

        ColorAdjust flash = new ColorAdjust();
        defender.setEffect(flash);
        Timeline flashTimeline = createFlashEffect(flash, movePower);

        double pushDirection = (defender == playerSprite) ? -1.0
                : (defender == opponentSprite ? 1.0 : 0.0);
        double pushDistance = Math.min(10 + movePower / 18.0, 22.0);
        TranslateTransition knockback = new TranslateTransition(Duration.millis(120), defender);
        knockback.setToX(defenderBaseTranslateX + pushDirection * pushDistance);
        knockback.setInterpolator(Interpolator.EASE_OUT);

        double defenderX = defender.getLayoutX() + defender.getFitWidth()  / 2;
        double defenderY = defender.getLayoutY() + defender.getFitHeight() / 2;

        Timeline typeEffect = createTypeSpecificImpact(
                attackerX, attackerY, defenderX, defenderY,
                moveName, moveType, movePower);

        ParallelTransition impact = new ParallelTransition(
                shake, shrink, flashTimeline, typeEffect, knockback);
        impact.setOnFinished(e -> defender.setEffect(null));

        return impact;
    }

    // =========================================================================
    // SHAKE / FLASH
    // =========================================================================

    private Timeline createShakeEffect(ImageView defender, int movePower, double defenderBaseTranslateX) {
        double shakeIntensity = Math.min(IMPACT_SHAKE_DISTANCE * (movePower / 80.0), 15.0);
        return new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX)),
                new KeyFrame(Duration.millis(40),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX + shakeIntensity)),
                new KeyFrame(Duration.millis(80),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX - shakeIntensity)),
                new KeyFrame(Duration.millis(120),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX + shakeIntensity)),
                new KeyFrame(Duration.millis(160),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX)));
    }

    private Timeline createFlashEffect(ColorAdjust flash, int movePower) {
        double flashIntensity = Math.min(0.9, 0.6 + movePower / 200.0);
        return new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(flash.brightnessProperty(), 0)),
                new KeyFrame(Duration.millis(50),
                        new KeyValue(flash.brightnessProperty(), flashIntensity)),
                new KeyFrame(Duration.millis(100),
                        new KeyValue(flash.brightnessProperty(), 0)),
                new KeyFrame(Duration.millis(150),
                        new KeyValue(flash.brightnessProperty(), flashIntensity * 0.7)),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(flash.brightnessProperty(), 0)));
    }

    // =========================================================================
    // TYPE-SPECIFIC IMPACT DISPATCH
    // =========================================================================

    private Timeline createTypeSpecificImpact(double startX, double startY,
            double endX, double endY,
            String moveName, String moveType, int movePower) {

        Timeline effect = new Timeline();

        switch (moveType) {
            case "electric" -> electricEffects.createImpactEffect(endX, endY, moveName, movePower, effect);
            case "ice"      -> iceEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "fire"     -> fireEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "fighting" -> fightingEffects.createImpactEffect(endX, endY, moveName, movePower, effect);
            case "water"    -> waterEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            default         -> createDefaultImpact(endX, endY, movePower, effect);
        }

        // For non-fighting punch moves (fire-punch, ice-punch, thunder-punch),
        // overlay the punch image + elemental particles on top of the type effect
        if (!moveType.equals("fighting") && moveName.contains("punch")) {
            fightingEffects.addPunchImageAndOverlay(endX, endY, moveName, moveType, effect);
        }

        return effect;
    }

    // =========================================================================
    // DEFAULT IMPACT (white particles)
    // =========================================================================

    private void createDefaultImpact(double x, double y, int movePower, Timeline timeline) {
        int particleCount = Math.min(3 + movePower / 20, 10);

        for (int i = 0; i < particleCount; i++) {
            Circle particle = new Circle(6, javafx.scene.paint.Color.WHITE);
            prepareTransientNode(particle);
            particle.setOpacity(0);

            double angle  = (i / (double) particleCount) * 2 * Math.PI;
            double radius = 30 + movePower / 4.0;

            particle.setCenterX(x);
            particle.setCenterY(y);
            battleField.getChildren().add(particle);

            KeyFrame appear = new KeyFrame(Duration.millis(50),
                    new KeyValue(particle.opacityProperty(), 1.0),
                    new KeyValue(particle.radiusProperty(), 8));
            KeyFrame expand = new KeyFrame(Duration.millis(150),
                    new KeyValue(particle.centerXProperty(), x + Math.cos(angle) * radius),
                    new KeyValue(particle.centerYProperty(), y + Math.sin(angle) * radius),
                    new KeyValue(particle.radiusProperty(), 3));
            KeyFrame fade = new KeyFrame(Duration.millis(200),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand, fade);
            registerCleanup(timeline, particle);
        }
    }

    // =========================================================================
    // RECOVERY
    // =========================================================================

    private ParallelTransition createRecoveryEffect(ImageView defender,
            double baseTranslateX, double baseTranslateY,
            double baseScaleX, double baseScaleY) {

        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(150), defender);
        scaleBack.setToX(baseScaleX);
        scaleBack.setToY(baseScaleY);

        TranslateTransition slideBack = new TranslateTransition(Duration.millis(150), defender);
        slideBack.setToX(baseTranslateX);
        slideBack.setToY(baseTranslateY);

        return new ParallelTransition(scaleBack, slideBack);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void prepareTransientNode(javafx.scene.Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private void registerCleanup(Timeline timeline, javafx.scene.Node node) {
        EventHandler<ActionEvent> previous = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (previous != null) previous.handle(e);
        });
    }
}