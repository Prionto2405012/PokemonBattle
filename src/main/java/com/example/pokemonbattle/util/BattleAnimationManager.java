package com.example.pokemonbattle.util;

import com.example.pokemonbattle.model.Move;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class BattleAnimationManager {
    
    private final ImageView playerSprite;
    private final ImageView opponentSprite;
    private final Pane battleField;
    
    // Animation constants
    private static final double ATTACK_DISTANCE = 120.0;
    private static final double ATTACK_DURATION_MS = 200.0;
    private static final double RETURN_DURATION_MS = 250.0;
    private static final double IMPACT_SHAKE_DISTANCE = 8.0;
    private static final double IMPACT_SCALE_REDUCTION = 0.88;
    private static final int IMPACT_FLASH_COUNT = 3;
    
    public BattleAnimationManager(ImageView playerSprite, ImageView opponentSprite, Pane battleField) {
        this.playerSprite = playerSprite;
        this.opponentSprite = opponentSprite;
        this.battleField = battleField;
    }
    
    /**
     * Play attack animation sequence
     */
    public void playAttackAnimation(ImageView attacker, ImageView defender, Move move, Runnable onComplete) {
        // Store original positions
        double originalX = attacker.getTranslateX();
        double originalY = attacker.getTranslateY();
        
        // Determine attack direction
        boolean attackingRight = (attacker == playerSprite);
        double attackDistance = attackingRight ? ATTACK_DISTANCE : -ATTACK_DISTANCE;
        
        // 1. Attacker rushes toward defender
        TranslateTransition rush = new TranslateTransition(Duration.millis(ATTACK_DURATION_MS), attacker);
        rush.setByX(attackDistance);
        rush.setInterpolator(Interpolator.EASE_IN);
        
        // 2. Impact effect on defender
        ParallelTransition impact = createImpactEffect(defender);
        
        // 3. Attacker returns to position
        TranslateTransition retreat = new TranslateTransition(Duration.millis(RETURN_DURATION_MS), attacker);
        retreat.setToX(originalX);
        retreat.setToY(originalY);
        retreat.setInterpolator(Interpolator.EASE_OUT);
        
        // 4. Defender recovers
        ParallelTransition recovery = createRecoveryEffect(defender);
        
        // Sequence: rush → impact → retreat & recovery
        SequentialTransition sequence = new SequentialTransition(
            rush,
            impact,
            new ParallelTransition(retreat, recovery)
        );
        
        sequence.setOnFinished(e -> {
            // Ensure sprites are reset
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
    
    /**
     * Create impact effect: shake, shrink, white flash
     */
    private ParallelTransition createImpactEffect(ImageView defender) {
        // Shake effect
        Timeline shake = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(defender.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(40), 
                new KeyValue(defender.translateXProperty(), IMPACT_SHAKE_DISTANCE)),
            new KeyFrame(Duration.millis(80), 
                new KeyValue(defender.translateXProperty(), -IMPACT_SHAKE_DISTANCE)),
            new KeyFrame(Duration.millis(120), 
                new KeyValue(defender.translateXProperty(), IMPACT_SHAKE_DISTANCE)),
            new KeyFrame(Duration.millis(160), 
                new KeyValue(defender.translateXProperty(), 0))
        );
        
        // Shrink effect
        ScaleTransition shrink = new ScaleTransition(Duration.millis(80), defender);
        shrink.setToX(IMPACT_SCALE_REDUCTION);
        shrink.setToY(IMPACT_SCALE_REDUCTION);
        shrink.setAutoReverse(true);
        shrink.setCycleCount(2);
        
        // White flash effect
        ColorAdjust whiteFlash = new ColorAdjust();
        defender.setEffect(whiteFlash);
        
        Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(whiteFlash.brightnessProperty(), 0)),
            new KeyFrame(Duration.millis(50), 
                new KeyValue(whiteFlash.brightnessProperty(), 0.9)),
            new KeyFrame(Duration.millis(100), 
                new KeyValue(whiteFlash.brightnessProperty(), 0)),
            new KeyFrame(Duration.millis(150), 
                new KeyValue(whiteFlash.brightnessProperty(), 0.7)),
            new KeyFrame(Duration.millis(200), 
                new KeyValue(whiteFlash.brightnessProperty(), 0))
        );
        
        // Impact particles (white circles)
        Timeline particles = createImpactParticles(defender);
        
        ParallelTransition impact = new ParallelTransition(shake, shrink, flash, particles);
        impact.setOnFinished(e -> defender.setEffect(null)); // Remove effect after
        
        return impact;
    }
    
    /**
     * Create white pop particles around defender
     */
    private Timeline createImpactParticles(ImageView defender) {
        Timeline particleTimeline = new Timeline();
        
        for (int i = 0; i < 6; i++) {
            Circle particle = new Circle(6, Color.WHITE);
            particle.setOpacity(0);
            
            // Position near defender
            double angle = (i / 6.0) * 2 * Math.PI;
            double radius = 40;
            double startX = defender.getLayoutX() + defender.getFitWidth() / 2;
            double startY = defender.getLayoutY() + defender.getFitHeight() / 2;
            
            particle.setCenterX(startX);
            particle.setCenterY(startY);
            
            battleField.getChildren().add(particle);
            
            // Animate particle
            Timeline particleAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(particle.opacityProperty(), 0),
                    new KeyValue(particle.radiusProperty(), 3)),
                new KeyFrame(Duration.millis(100),
                    new KeyValue(particle.opacityProperty(), 1.0),
                    new KeyValue(particle.radiusProperty(), 8),
                    new KeyValue(particle.centerXProperty(), startX + Math.cos(angle) * radius),
                    new KeyValue(particle.centerYProperty(), startY + Math.sin(angle) * radius)),
                new KeyFrame(Duration.millis(200),
                    new KeyValue(particle.opacityProperty(), 0),
                    new KeyValue(particle.radiusProperty(), 2))
            );
            
            particleAnim.setOnFinished(e -> battleField.getChildren().remove(particle));
            particleTimeline.getKeyFrames().addAll(particleAnim.getKeyFrames());
        }
        
        return particleTimeline;
    }
    
    /**
     * Recovery effect for defender
     */
    private ParallelTransition createRecoveryEffect(ImageView defender) {
        // Return to normal scale
        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(150), defender);
        scaleBack.setToX(1.0);
        scaleBack.setToY(1.0);
        
        // Return to normal position
        TranslateTransition slideBack = new TranslateTransition(Duration.millis(150), defender);
        slideBack.setToX(0);
        slideBack.setToY(0);
        
        return new ParallelTransition(scaleBack, slideBack);
    }
}
