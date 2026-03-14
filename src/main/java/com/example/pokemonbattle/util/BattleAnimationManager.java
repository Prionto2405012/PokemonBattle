// BattleAnimationManager.java
package com.example.pokemonbattle.util;

import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.util.effects.*;
import javafx.animation.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
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
    
    public BattleAnimationManager(ImageView playerSprite, ImageView opponentSprite, Pane battleField) {
        this.playerSprite = playerSprite;
        this.opponentSprite = opponentSprite;
        this.battleField = battleField;
        
        this.electricEffects = new ElectricEffects(battleField);
        this.iceEffects = new IceEffects(battleField);
        this.fireEffects = new FireEffects(battleField);
    }
    
    public void playAttackAnimation(ImageView attacker, ImageView defender, Move move, Runnable onComplete) {
        double originalX = attacker.getTranslateX();
        double originalY = attacker.getTranslateY();
        
        boolean attackingRight = (attacker == playerSprite);
        
        String moveName = move.getName().toLowerCase();
        String moveType = move.getType() != null ? move.getType().toLowerCase() : "normal";
        int movePower = move.getPower() != null ? move.getPower() : 50;
        
        // Determine attack distance based on move type
        double attackDistance = getAttackDistance(moveType, moveName);
        attackDistance = attackingRight ? attackDistance : -attackDistance;
        
        // Check if we need special handling (ranged attacks)
        if (isRangedMove(moveType, moveName)) {
            playRangedAnimation(attacker, defender, move, moveName, moveType, movePower, onComplete);
            return;
        }
        
        // Create movement transition
        TranslateTransition rush = new TranslateTransition(Duration.millis(ATTACK_DURATION_MS), attacker);
        rush.setByX(attackDistance);
        rush.setInterpolator(Interpolator.EASE_IN);
        
        // Create ongoing effect during movement (for electric types)
        Timeline movementEffect = createMovementEffect(attacker, defender, moveName, moveType, attackingRight);
        
        // Create impact effect
        ParallelTransition impact = createImpactEffect(defender, move, moveName, moveType, movePower);
        
        // Return transition
        TranslateTransition retreat = new TranslateTransition(Duration.millis(RETURN_DURATION_MS), attacker);
        retreat.setToX(originalX);
        retreat.setToY(originalY);
        retreat.setInterpolator(Interpolator.EASE_OUT);
        
        // Recovery
        ParallelTransition recovery = createRecoveryEffect(defender);
        
        // Combine: movement + movement effect → impact → retreat & recovery
        SequentialTransition sequence = new SequentialTransition(
            new ParallelTransition(rush, movementEffect),
            impact,
            new ParallelTransition(retreat, recovery)
        );
        
        sequence.setOnFinished(e -> {
            attacker.setTranslateX(originalX);
            attacker.setTranslateY(originalY);
            defender.setScaleX(1.0);
            defender.setScaleY(1.0);
            defender.setTranslateX(0);
            defender.setTranslateY(0);
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        sequence.play();
    }
    
    private double getAttackDistance(String moveType, String moveName) {
        // Ice moves only move slightly
        if (moveType.equals("ice")) {
            return ATTACK_DISTANCE_SLIGHT;
        }
        // Ranged moves don't move at all (handled separately)
        if (isRangedMove(moveType, moveName)) {
            return 0;
        }
        // Normal melee moves
        return ATTACK_DISTANCE_FULL;
    }
    
    private boolean isRangedMove(String moveType, String moveName) {
        // Ice beam-type moves
        if (moveName.contains("beam") || moveName.contains("spear")) {
            return true;
        }
        return false;
    }
    
    private void playRangedAnimation(ImageView attacker, ImageView defender, Move move, 
                                    String moveName, String moveType, int movePower, Runnable onComplete) {
        double attackerX = attacker.getLayoutX() + attacker.getFitWidth() / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        double defenderX = defender.getLayoutX() + defender.getFitWidth() / 2;
        double defenderY = defender.getLayoutY() + defender.getFitHeight() / 2;
        
        Timeline beamEffect = null;
        
        if (moveType.equals("ice")) {
            beamEffect = iceEffects.createBeamEffect(attackerX, attackerY, defenderX, defenderY, moveName, movePower);
        }
        
        if (beamEffect != null) {
            beamEffect.setOnFinished(e -> {
                ParallelTransition impact = createImpactEffect(defender, move, moveName, moveType, movePower);
                ParallelTransition recovery = createRecoveryEffect(defender);
                
                SequentialTransition impactSeq = new SequentialTransition(impact, recovery);
                impactSeq.setOnFinished(ev -> {
                    defender.setScaleX(1.0);
                    defender.setScaleY(1.0);
                    defender.setTranslateX(0);
                    defender.setTranslateY(0);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
                impactSeq.play();
            });
            beamEffect.play();
        } else {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
    
    private Timeline createMovementEffect(ImageView attacker, ImageView defender, 
                                         String moveName, String moveType, boolean attackingRight) {
        Timeline effect = new Timeline();
        
        double attackerX = attacker.getLayoutX() + attacker.getFitWidth() / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        
        // Electric moves get sparks during movement
        if (moveType.equals("electric")) {
            electricEffects.addMovementSparks(attackerX, attackerY, attackingRight, effect);
        }
        
        return effect;
    }
    
    private ParallelTransition createImpactEffect(ImageView defender, Move move, 
                                                 String moveName, String moveType, int movePower) {
        // Base shake
        Timeline shake = createShakeEffect(defender, movePower);
        
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
        
        // Type-specific impact effect
        double defenderX = defender.getLayoutX() + defender.getFitWidth() / 2;
        double defenderY = defender.getLayoutY() + defender.getFitHeight() / 2;
        
        Timeline typeEffect = createTypeSpecificImpact(defenderX, defenderY, moveName, moveType, movePower);
        
        ParallelTransition impact = new ParallelTransition(shake, shrink, flashTimeline, typeEffect);
        impact.setOnFinished(e -> defender.setEffect(null));
        
        return impact;
    }
    
    private Timeline createShakeEffect(ImageView defender, int movePower) {
        double shakeIntensity = Math.min(IMPACT_SHAKE_DISTANCE * (movePower / 80.0), 15.0);
        
        return new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(defender.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(40), 
                new KeyValue(defender.translateXProperty(), shakeIntensity)),
            new KeyFrame(Duration.millis(80), 
                new KeyValue(defender.translateXProperty(), -shakeIntensity)),
            new KeyFrame(Duration.millis(120), 
                new KeyValue(defender.translateXProperty(), shakeIntensity)),
            new KeyFrame(Duration.millis(160), 
                new KeyValue(defender.translateXProperty(), 0))
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
    
    private Timeline createTypeSpecificImpact(double x, double y, String moveName, 
                                             String moveType, int movePower) {
        Timeline effect = new Timeline();
        
        switch (moveType) {
            case "electric":
                electricEffects.createImpactEffect(x, y, moveName, movePower, effect);
                break;
            case "ice":
                iceEffects.createImpactEffect(x, y, moveName, movePower, effect);
                break;
            case "fire":
                fireEffects.createImpactEffect(x, y, moveName, movePower, effect);
                break;
            default:
                createDefaultImpact(x, y, movePower, effect);
                break;
        }
        
        return effect;
    }
    
    private void createDefaultImpact(double x, double y, int movePower, Timeline timeline) {
        int particleCount = Math.min(3 + movePower / 20, 10);
        
        for (int i = 0; i < particleCount; i++) {
            javafx.scene.shape.Circle particle = new javafx.scene.shape.Circle(6, javafx.scene.paint.Color.WHITE);
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
            
            final javafx.scene.shape.Circle p = particle;
            timeline.setOnFinished(e -> battleField.getChildren().remove(p));
        }
    }
    
    private ParallelTransition createRecoveryEffect(ImageView defender) {
        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(150), defender);
        scaleBack.setToX(1.0);
        scaleBack.setToY(1.0);
        
        TranslateTransition slideBack = new TranslateTransition(Duration.millis(150), defender);
        slideBack.setToX(0);
        slideBack.setToY(0);
        
        return new ParallelTransition(scaleBack, slideBack);
    }
}