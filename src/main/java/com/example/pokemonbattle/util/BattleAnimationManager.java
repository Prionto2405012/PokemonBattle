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
    private static final double ATTACK_DISTANCE_FULL = 120.0;
    private static final double ATTACK_DISTANCE_SLIGHT = 30.0;
    private static final double ATTACK_DURATION_MS = 200.0;
    private static final double RETURN_DURATION_MS = 250.0;
    private static final double IMPACT_SHAKE_DISTANCE = 8.0;
    private static final double IMPACT_SCALE_REDUCTION = 0.88;
    
    // Type effect handlers
    private final ElectricEffects electricEffects;
    private final IceEffects iceEffects;
    private final FireEffects fireEffects;
    private final FightingEffects fightingEffects;
    private final WaterEffects waterEffects;
    public BattleAnimationManager(ImageView playerSprite, ImageView opponentSprite, Pane battleField) {
        this.playerSprite = playerSprite;
        this.opponentSprite = opponentSprite;
        this.battleField = battleField;
        
        this.electricEffects = new ElectricEffects(battleField);
        this.iceEffects = new IceEffects(battleField);
        this.fireEffects = new FireEffects(battleField);
        this.fightingEffects = new FightingEffects(battleField);
        this.waterEffects = new WaterEffects(battleField);
    }
    
    public void playAttackAnimation(ImageView attacker, ImageView defender, Move move, Runnable onComplete) {
        double attackerOriginalX = attacker.getTranslateX();
        double attackerOriginalY = attacker.getTranslateY();
        double attackerOriginalScaleX = attacker.getScaleX();
        double attackerOriginalScaleY = attacker.getScaleY();
        double defenderOriginalX = defender.getTranslateX();
        double defenderOriginalY = defender.getTranslateY();
        double defenderOriginalScaleX = defender.getScaleX();
        double defenderOriginalScaleY = defender.getScaleY();
        
        boolean attackingRight = (attacker == playerSprite);
        
        String moveName = (move != null && move.getName() != null) ? move.getName().toLowerCase() : "tackle";
        String moveType = (move != null && move.getType() != null) ? move.getType().toLowerCase() : "normal";
        Integer powerValue = (move != null) ? move.getPower() : null;
        int movePower = (powerValue != null) ? powerValue : 50;
        
        // Determine attack distance based on move type
        double attackDistance = getAttackDistance(moveType, moveName);
        attackDistance = attackingRight ? attackDistance : -attackDistance;
        
        // Check if we need special handling (ranged attacks)
        if (isRangedMove(moveName, moveType)) {
            playRangedAnimation(attacker, defender, moveName, moveType, movePower, defenderOriginalX, defenderOriginalY, defenderOriginalScaleX, defenderOriginalScaleY, onComplete);
            return;
        }
        
        // Create movement transition
        TranslateTransition rush = new TranslateTransition(Duration.millis(ATTACK_DURATION_MS), attacker);
        rush.setByX(attackDistance);
        rush.setInterpolator(Interpolator.EASE_IN);
        
        // Create ongoing effect during movement (for electric types)
        Timeline movementEffect = createMovementEffect(attacker, moveType, attackingRight);
        
        // Create impact effect
        double attackerX = attacker.getLayoutX() + attacker.getFitWidth() / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        ParallelTransition impact = createImpactEffect(defender, attackerX, attackerY,
            moveName, moveType, movePower, defenderOriginalX);
        
        // Return transition
        TranslateTransition retreat = new TranslateTransition(Duration.millis(RETURN_DURATION_MS), attacker);
        retreat.setToX(attackerOriginalX);
        retreat.setToY(attackerOriginalY);
        retreat.setInterpolator(Interpolator.EASE_OUT);
        
        // Recovery
        ParallelTransition recovery = createRecoveryEffect(defender, defenderOriginalX, defenderOriginalY, defenderOriginalScaleX, defenderOriginalScaleY);
        
        // Combine: movement + movement effect → impact → retreat & recovery
        SequentialTransition sequence = new SequentialTransition(
            new ParallelTransition(rush, movementEffect),impact, new ParallelTransition(retreat, recovery));
        
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
    
    private double getAttackDistance(String moveType, String moveName) {
        // Ice moves only move slightly, but ice-punch is melee so it uses full distance
        if (moveType.equals("ice") && !moveName.contains("punch")) {
            return ATTACK_DISTANCE_SLIGHT;
        }
        if(moveType.equals("water")) {
            if(moveName.equals("crabhammer")) return ATTACK_DISTANCE_FULL;
            return ATTACK_DISTANCE_SLIGHT;
        }
        // Ranged moves don't move at all (handled separately)
        if (isRangedMove(moveName, moveType)) return 0;
        // Normal melee moves
        return ATTACK_DISTANCE_FULL;
    }
    
    private boolean isRangedMove(String moveName, String moveType) {
        // Ice beam-type moves
        if(moveName.contains("beam") || moveName.contains("spear")) return true;
        if(moveType.equals("water") && !moveName.equals("crabhammer")) return true;
        return false;
    }
    private void playRangedAnimation(ImageView attacker, ImageView defender,
            String moveName, String moveType, int movePower,
            double defenderOriginalX, double defenderOriginalY,
            double defenderOriginalScaleX, double defenderOriginalScaleY,
            Runnable onComplete) {
        double attackerX = attacker.getLayoutX() + attacker.getFitWidth() / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        double defenderX = defender.getLayoutX() + defender.getFitWidth() / 2;
        double defenderY = defender.getLayoutY() + defender.getFitHeight() / 2;
        
        Timeline beamEffect = null;
        
        if (moveType.equals("ice")) {
            beamEffect = iceEffects.createBeamEffect(attackerX, attackerY, defenderX, defenderY, moveName, movePower);
        }
        else if (moveType.equals("water")) {
            // Water ranged moves: build a standalone timeline, fire impact inline
            Timeline waterTimeline = new Timeline();
            waterEffects.createImpactEffect(
                    attackerX, attackerY, defenderX, defenderY,
                    moveName, movePower, waterTimeline);
            beamEffect = waterTimeline;
        }
        if (beamEffect != null) {
            EventHandler<ActionEvent> previousOnFinished = beamEffect.getOnFinished();
            beamEffect.setOnFinished(e -> {
                if (previousOnFinished != null) {
                    previousOnFinished.handle(e);
                }

                ParallelTransition impact = createImpactEffect(defender, attackerX, attackerY,
                    moveName, moveType, movePower, defenderOriginalX);
                ParallelTransition recovery = createRecoveryEffect( defender, defenderOriginalX, defenderOriginalY, defenderOriginalScaleX, defenderOriginalScaleY);
                SequentialTransition impactSeq = new SequentialTransition(impact, recovery);
                impactSeq.setOnFinished(ev -> {
                    defender.setScaleX(defenderOriginalScaleX);
                    defender.setScaleY(defenderOriginalScaleY);
                    defender.setTranslateX(defenderOriginalX);
                    defender.setTranslateY(defenderOriginalY);
                    if(onComplete != null) onComplete.run();
                });
                impactSeq.play();
            });
            beamEffect.play();
        } else {
            if(onComplete != null) onComplete.run();
        }
    }
    
    private Timeline createMovementEffect(ImageView attacker, String moveType, boolean attackingRight) {
        Timeline effect = new Timeline();
        
        double attackerX = attacker.getLayoutX() + attacker.getFitWidth() / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        
        // Electric moves get sparks during movement
        if (moveType.equals("electric")) {
            electricEffects.addMovementSparks(attackerX, attackerY, attackingRight, effect);
        }
        
        return effect;
    }
    
    private ParallelTransition createImpactEffect(ImageView defender, double attackerX, double attackerY, String moveName, String moveType, int movePower, double defenderBaseTranslateX) {
        // Base shake
        Timeline shake = createShakeEffect(defender, movePower, defenderBaseTranslateX);
        
        // Shrink
        ScaleTransition shrink = new ScaleTransition(Duration.millis(80), defender);
        shrink.setToX(IMPACT_SCALE_REDUCTION);
        shrink.setToY(IMPACT_SCALE_REDUCTION);
        shrink.setAutoReverse(true);
        shrink.setCycleCount(2);
        
        // Flash
        ColorAdjust flash = new ColorAdjust();
        defender.setEffect(flash);
        Timeline flashTimeline = createFlashEffect(flash, movePower);

        // Push defender slightly away from attacker side: player gets pushed left, opponent right.
        double pushDirection = (defender == playerSprite) ? -1.0 : (defender == opponentSprite ? 1.0 : 0.0);
        double pushDistance = Math.min(10 + movePower / 18.0, 22.0);
        TranslateTransition knockback = new TranslateTransition(Duration.millis(120), defender);
        knockback.setToX(defenderBaseTranslateX + (pushDirection * pushDistance));
        knockback.setInterpolator(Interpolator.EASE_OUT);
        
        // Type-specific impact effect
        double defenderX = defender.getLayoutX() + defender.getFitWidth() / 2;
        double defenderY = defender.getLayoutY() + defender.getFitHeight() / 2;
        
        Timeline typeEffect = createTypeSpecificImpact(attackerX, attackerY, defenderX, defenderY,
            moveName, moveType, movePower);
        
        ParallelTransition impact = new ParallelTransition(shake, shrink, flashTimeline, typeEffect, knockback);
        impact.setOnFinished(e -> defender.setEffect(null));
        
        return impact;
    }
    
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
                new KeyValue(defender.translateXProperty(), defenderBaseTranslateX))
        );
    }
    
    private Timeline createFlashEffect(ColorAdjust flash, int movePower) {
        double flashIntensity = Math.min(0.9, 0.6 + (movePower / 200.0));
        
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
                new KeyValue(flash.brightnessProperty(), 0))
        );
    }
    
    private Timeline createTypeSpecificImpact(double startX, double startY, double endX, double endY,
            String moveName, String moveType, int movePower) {
        Timeline effect = new Timeline();
        
        switch (moveType) {
            case "electric" -> electricEffects.createImpactEffect(endX, endY, moveName, movePower, effect);
            case "ice"      -> iceEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "fire"     -> fireEffects.createImpactEffect(endX, endY, moveName, movePower, effect);
            case "fighting" -> fightingEffects.createImpactEffect(endX, endY, moveName, movePower, effect);
            case "water"    -> waterEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            default         -> createDefaultImpact(endX, endY, movePower, effect);
        }

        // For non-fighting punch moves overlay punch image + elemental particles
        // on top of the type effect already applied above
        if (!moveType.equals("fighting") && moveName.contains("punch")) {
            fightingEffects.addPunchImageAndOverlay(endX, endY, moveName, moveType, effect);
        }

        return effect;
    }
    
    private void createDefaultImpact(double x, double y, int movePower, Timeline timeline) {
        int particleCount = Math.min(10 + movePower / 20, 10);
        
        for (int i = 0; i < particleCount; i++) {
            Circle particle = new Circle(6, javafx.scene.paint.Color.WHITE);
            prepareTransientNode(particle);
            particle.setOpacity(0);
            
            double angle = (i / (double)particleCount) * 2 * Math.PI;
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
    
    private ParallelTransition createRecoveryEffect(ImageView defender, double baseTranslateX, double baseTranslateY,
            double baseScaleX, double baseScaleY) {
        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(150), defender);
        scaleBack.setToX(baseScaleX);
        scaleBack.setToY(baseScaleY);
        
        TranslateTransition slideBack = new TranslateTransition(Duration.millis(150), defender);
        slideBack.setToX(baseTranslateX);
        slideBack.setToY(baseTranslateY);
        
        return new ParallelTransition(scaleBack, slideBack);
    }
    //helpers
    private void prepareTransientNode(javafx.scene.Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private void registerCleanup(Timeline timeline, javafx.scene.Node node) {
        EventHandler<ActionEvent> previousOnFinished = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (previousOnFinished != null) {
                previousOnFinished.handle(e);
            }
        });
    }
}