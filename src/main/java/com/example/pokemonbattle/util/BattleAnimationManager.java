package com.example.pokemonbattle.util;

import java.util.Random;

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
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class BattleAnimationManager {
    
    private final ImageView playerSprite;
    private final ImageView opponentSprite;
    private final Pane battleField;
    private final Random random = new Random();
    
    // Animation constants
    private static final double ATTACK_DISTANCE = 120.0;
    private static final double ATTACK_DURATION_MS = 200.0;
    private static final double RETURN_DURATION_MS = 250.0;
    private static final double IMPACT_SHAKE_DISTANCE = 8.0;
    private static final double IMPACT_SCALE_REDUCTION = 0.88;
    
    public BattleAnimationManager(ImageView playerSprite, ImageView opponentSprite, Pane battleField) {
        this.playerSprite = playerSprite;
        this.opponentSprite = opponentSprite;
        this.battleField = battleField;
    }
    
    /**
     * Play attack animation sequence with move-specific effects
     */
    public void playAttackAnimation(ImageView attacker, ImageView defender, Move move, Runnable onComplete) {
        // Store original positions
        double originalX = attacker.getTranslateX();
        double originalY = attacker.getTranslateY();
        
        // Determine attack direction
        boolean attackingRight = (attacker == playerSprite);
        double attackDistance = attackingRight ? ATTACK_DISTANCE : -ATTACK_DISTANCE;
        
        String moveName = move.getName().toLowerCase();
        String moveType = move.getType() != null ? move.getType().toLowerCase() : "";
        
        // Check if this is a punch move
        boolean isPunchMove = moveName.contains("punch");
        
        // Check for special projectile moves
        boolean isElectroBall = moveName.contains("electro") && moveName.contains("ball");
        boolean isShadowBall = moveName.contains("shadow") && moveName.contains("ball");
        boolean isEnergyBall = moveName.contains("energy") && moveName.contains("ball");
        boolean isProjectile = isElectroBall || isShadowBall || isEnergyBall || 
                               moveName.contains("ball") && !isPunchMove;
        
        // For projectile moves, don't rush - just shoot projectile
        if (isProjectile) {
            playProjectileAnimation(attacker, defender, move, moveName, moveType, onComplete);
            return;
        }
        
        // 1. Attacker rushes toward defender
        TranslateTransition rush = new TranslateTransition(Duration.millis(ATTACK_DURATION_MS), attacker);
        rush.setByX(attackDistance);
        rush.setInterpolator(Interpolator.EASE_IN);
        
        // 2. Impact effect on defender with move-specific visuals
        ParallelTransition impact = createImpactEffect(defender, move, moveName, moveType, isPunchMove);
        
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
     * Play projectile animation (for ball moves)
     */
    private void playProjectileAnimation(ImageView attacker, ImageView defender, Move move, 
                                        String moveName, String moveType, Runnable onComplete) {
        double startX = attacker.getLayoutX() + attacker.getFitWidth() / 2;
        double startY = attacker.getLayoutY() + attacker.getFitHeight() / 2;
        double endX = defender.getLayoutX() + defender.getFitWidth() / 2;
        double endY = defender.getLayoutY() + defender.getFitHeight() / 2;
        
        // Create projectile based on move type
        Circle projectile;
        
        if (moveName.contains("electro") && moveName.contains("ball")) {
            // Electric ball with lightning effect
            projectile = new Circle(15, Color.YELLOW);
            projectile.setEffect(new DropShadow(20, Color.GOLD));
        } else if (moveName.contains("shadow") && moveName.contains("ball")) {
            // Shadow ball
            projectile = new Circle(18, Color.web("#705898"));
            projectile.setEffect(new DropShadow(25, Color.web("#402870")));
        } else if (moveName.contains("energy") && moveName.contains("ball")) {
            // Energy ball (grass)
            projectile = new Circle(16, Color.web("#78C850"));
            projectile.setEffect(new DropShadow(20, Color.web("#489820")));
        } else {
            // Generic ball
            projectile = new Circle(14, Color.WHITE);
            projectile.setEffect(new DropShadow(15, Color.LIGHTGRAY));
        }
        
        projectile.setCenterX(startX);
        projectile.setCenterY(startY);
        battleField.getChildren().add(projectile);
        
        // Add spinning effect for electro ball
        if (moveName.contains("electro") && moveName.contains("ball")) {
            addElectricSparksToBall(projectile, startX, startY, endX, endY);
        }
        
        // Projectile travel animation
        TranslateTransition projectileTravel = new TranslateTransition(Duration.millis(400), projectile);
        projectileTravel.setToX(endX - startX);
        projectileTravel.setToY(endY - startY);
        projectileTravel.setInterpolator(Interpolator.EASE_IN);
        
        projectileTravel.setOnFinished(e -> {
            battleField.getChildren().remove(projectile);
            
            // Impact effect on defender
            ParallelTransition impact = createImpactEffect(defender, move, moveName, moveType, false);
            ParallelTransition recovery = createRecoveryEffect(defender);
            
            SequentialTransition impactSequence = new SequentialTransition(impact, recovery);
            impactSequence.setOnFinished(ev -> {
                defender.setScaleX(1.0);
                defender.setScaleY(1.0);
                defender.setTranslateX(0);
                defender.setTranslateY(0);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
            impactSequence.play();
        });
        
        projectileTravel.play();
    }
    
    /**
     * Add electric sparks to electro ball
     */
    private void addElectricSparksToBall(Circle ball, double startX, double startY, double endX, double endY) {
        Timeline sparkAnimation = new Timeline();
        
        for (int i = 0; i < 8; i++) {
            Line spark = new Line();
            spark.setStroke(Color.YELLOW);
            spark.setStrokeWidth(2);
            
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 20;
            
            spark.setStartX(startX);
            spark.setStartY(startY);
            spark.setEndX(startX + Math.cos(angle) * radius);
            spark.setEndY(startY + Math.sin(angle) * radius);
            spark.setOpacity(0);
            
            battleField.getChildren().add(spark);
            
            final int delay = i * 50;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(spark.opacityProperty(), 1.0));
            KeyFrame disappear = new KeyFrame(Duration.millis(delay + 100),
                new KeyValue(spark.opacityProperty(), 0));
            
            sparkAnimation.getKeyFrames().addAll(appear, disappear);
            sparkAnimation.setOnFinished(e -> battleField.getChildren().remove(spark));
        }
        
        sparkAnimation.play();
    }
    
    /**
     * Create impact effect with move-specific visuals
     */
    private ParallelTransition createImpactEffect(ImageView defender, Move move, 
                                                 String moveName, String moveType, boolean isPunchMove) {
        // Base shake effect
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
        
        // Base flash effect
        ColorAdjust flash = new ColorAdjust();
        defender.setEffect(flash);
        
        Timeline flashTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(flash.brightnessProperty(), 0)),
            new KeyFrame(Duration.millis(50), 
                new KeyValue(flash.brightnessProperty(), 0.9)),
            new KeyFrame(Duration.millis(100), 
                new KeyValue(flash.brightnessProperty(), 0)),
            new KeyFrame(Duration.millis(150), 
                new KeyValue(flash.brightnessProperty(), 0.7)),
            new KeyFrame(Duration.millis(200), 
                new KeyValue(flash.brightnessProperty(), 0))
        );
        
        // Move-specific particle effects
        Timeline moveEffect = createMoveSpecificEffect(defender, moveName, moveType, isPunchMove);
        
        ParallelTransition impact = new ParallelTransition(shake, shrink, flashTimeline, moveEffect);
        impact.setOnFinished(e -> defender.setEffect(null));
        
        return impact;
    }
    
    /**
     * Create move-specific visual effects
     */
    private Timeline createMoveSpecificEffect(ImageView defender, String moveName, 
                                             String moveType, boolean isPunchMove) {
        Timeline effectTimeline = new Timeline();
        
        double centerX = defender.getLayoutX() + defender.getFitWidth() / 2;
        double centerY = defender.getLayoutY() + defender.getFitHeight() / 2;
        
        // PUNCH MOVES - Show fist + element effect
        if (isPunchMove) {
            // Add fist visual
            addFistVisual(centerX, centerY, effectTimeline);
            
            if (moveName.contains("thunder") || moveType.equals("electric")) {
                addElectricZapEffect(centerX, centerY, effectTimeline);
            } else if (moveName.contains("fire") || moveType.equals("fire")) {
                addFirePunchEffect(centerX, centerY, effectTimeline);
            } else if (moveName.contains("ice") || moveType.equals("ice")) {
                addIcePunchEffect(centerX, centerY, effectTimeline);
            }
        } 
        // ELECTRIC MOVES (non-punch, non-ball) - Electric zap
        else if (moveType.equals("electric") && !moveName.contains("ball")) {
            addElectricZapEffect(centerX, centerY, effectTimeline);
        }
        // ELECTRO BALL - Already handled in projectile
        else if (moveName.contains("electro") && moveName.contains("ball")) {
            addElectricBurstEffect(centerX, centerY, effectTimeline);
        }
        // Default impact particles
        else {
            addDefaultImpactParticles(centerX, centerY, effectTimeline);
        }
        
        return effectTimeline;
    }
    
    /**
     * Add fist visual for punch moves
     */
    private void addFistVisual(double x, double y, Timeline timeline) {
        // Create a simple fist shape using rectangles
        Rectangle palm = new Rectangle(x - 15, y - 10, 30, 25);
        palm.setFill(Color.web("#FFE0BD"));
        palm.setStroke(Color.web("#8B6342"));
        palm.setStrokeWidth(2);
        palm.setArcWidth(8);
        palm.setArcHeight(8);
        palm.setOpacity(0);
        
        battleField.getChildren().add(palm);
        
        KeyFrame appear = new KeyFrame(Duration.millis(50),
            new KeyValue(palm.opacityProperty(), 1.0),
            new KeyValue(palm.scaleXProperty(), 1.0),
            new KeyValue(palm.scaleYProperty(), 1.0));
        KeyFrame grow = new KeyFrame(Duration.millis(100),
            new KeyValue(palm.scaleXProperty(), 1.3),
            new KeyValue(palm.scaleYProperty(), 1.3));
        KeyFrame disappear = new KeyFrame(Duration.millis(200),
            new KeyValue(palm.opacityProperty(), 0));
        
        timeline.getKeyFrames().addAll(appear, grow, disappear);
        timeline.setOnFinished(e -> battleField.getChildren().remove(palm));
    }
    
    /**
     * Electric zap effect - lightning bolts
     */
    private void addElectricZapEffect(double x, double y, Timeline timeline) {
        for (int i = 0; i < 6; i++) {
            Line zap = new Line();
            zap.setStroke(Color.YELLOW);
            zap.setStrokeWidth(3);
            zap.setEffect(new DropShadow(10, Color.GOLD));
            
            double angle = (i / 6.0) * 2 * Math.PI + random.nextDouble() * 0.3;
            double length = 40 + random.nextDouble() * 30;
            
            zap.setStartX(x);
            zap.setStartY(y);
            zap.setEndX(x + Math.cos(angle) * length);
            zap.setEndY(y + Math.sin(angle) * length);
            zap.setOpacity(0);
            
            battleField.getChildren().add(zap);
            
            int delay = i * 30;
            KeyFrame flash1 = new KeyFrame(Duration.millis(delay),
                new KeyValue(zap.opacityProperty(), 0));
            KeyFrame flash2 = new KeyFrame(Duration.millis(delay + 20),
                new KeyValue(zap.opacityProperty(), 1.0));
            KeyFrame flash3 = new KeyFrame(Duration.millis(delay + 40),
                new KeyValue(zap.opacityProperty(), 0));
            KeyFrame flash4 = new KeyFrame(Duration.millis(delay + 80),
                new KeyValue(zap.opacityProperty(), 0.8));
            KeyFrame flash5 = new KeyFrame(Duration.millis(delay + 100),
                new KeyValue(zap.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(flash1, flash2, flash3, flash4, flash5);
            timeline.setOnFinished(e -> battleField.getChildren().remove(zap));
        }
    }
    
    /**
     * Fire punch effect - red/orange flames
     */
    private void addFirePunchEffect(double x, double y, Timeline timeline) {
        for (int i = 0; i < 8; i++) {
            Polygon flame = new Polygon();
            flame.getPoints().addAll(
                0.0, 0.0,
                -5.0, -15.0,
                0.0, -25.0,
                5.0, -15.0
            );
            
            Color flameColor = i % 2 == 0 ? Color.ORANGERED : Color.ORANGE;
            flame.setFill(flameColor);
            flame.setEffect(new GaussianBlur(3));
            
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 25;
            
            flame.setLayoutX(x + Math.cos(angle) * radius);
            flame.setLayoutY(y + Math.sin(angle) * radius);
            flame.setOpacity(0);
            flame.setRotate(Math.toDegrees(angle));
            
            battleField.getChildren().add(flame);
            
            int delay = i * 25;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(flame.opacityProperty(), 0));
            KeyFrame grow = new KeyFrame(Duration.millis(delay + 50),
                new KeyValue(flame.opacityProperty(), 1.0),
                new KeyValue(flame.scaleXProperty(), 1.5),
                new KeyValue(flame.scaleYProperty(), 1.5));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 150),
                new KeyValue(flame.opacityProperty(), 0),
                new KeyValue(flame.scaleYProperty(), 2.0));
            
            timeline.getKeyFrames().addAll(appear, grow, fade);
            timeline.setOnFinished(e -> battleField.getChildren().remove(flame));
        }
    }
    
    /**
     * Ice punch effect - icy shards and snowflakes
     */
    private void addIcePunchEffect(double x, double y, Timeline timeline) {
        // Ice shards
        for (int i = 0; i < 6; i++) {
            Polygon shard = new Polygon();
            shard.getPoints().addAll(
                0.0, 0.0,
                -3.0, -20.0,
                0.0, -25.0,
                3.0, -20.0
            );
            shard.setFill(Color.CYAN);
            shard.setStroke(Color.LIGHTBLUE);
            shard.setStrokeWidth(1);
            shard.setEffect(new DropShadow(8, Color.DEEPSKYBLUE));
            
            double angle = (i / 6.0) * 2 * Math.PI;
            double radius = 30;
            
            shard.setLayoutX(x + Math.cos(angle) * radius);
            shard.setLayoutY(y + Math.sin(angle) * radius);
            shard.setOpacity(0);
            shard.setRotate(Math.toDegrees(angle) + random.nextDouble() * 30);
            
            battleField.getChildren().add(shard);
            
            int delay = i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(shard.opacityProperty(), 1.0));
            KeyFrame fly = new KeyFrame(Duration.millis(delay + 200),
                new KeyValue(shard.translateXProperty(), Math.cos(angle) * 40),
                new KeyValue(shard.translateYProperty(), Math.sin(angle) * 40),
                new KeyValue(shard.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, fly);
            timeline.setOnFinished(e -> battleField.getChildren().remove(shard));
        }
        
        // Snowflakes
        for (int i = 0; i < 10; i++) {
            Circle snowflake = new Circle(2 + random.nextDouble() * 3, Color.WHITE);
            snowflake.setEffect(new DropShadow(5, Color.LIGHTBLUE));
            
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * 50;
            
            snowflake.setCenterX(x);
            snowflake.setCenterY(y);
            snowflake.setOpacity(0);
            
            battleField.getChildren().add(snowflake);
            
            int delay = i * 20;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(snowflake.opacityProperty(), 1.0));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 300),
                new KeyValue(snowflake.centerXProperty(), x + Math.cos(angle) * distance),
                new KeyValue(snowflake.centerYProperty(), y + Math.sin(angle) * distance + 30),
                new KeyValue(snowflake.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, drift);
            timeline.setOnFinished(e -> battleField.getChildren().remove(snowflake));
        }
    }
    
    /**
     * Electric burst for electro ball impact
     */
    private void addElectricBurstEffect(double x, double y, Timeline timeline) {
        // Electric ring expansion
        Circle ring = new Circle(x, y, 5);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.YELLOW);
        ring.setStrokeWidth(4);
        ring.setEffect(new DropShadow(15, Color.GOLD));
        ring.setOpacity(0);
        
        battleField.getChildren().add(ring);
        
        KeyFrame expand = new KeyFrame(Duration.millis(200),
            new KeyValue(ring.radiusProperty(), 60),
            new KeyValue(ring.opacityProperty(), 1.0),
            new KeyValue(ring.strokeWidthProperty(), 1));
        KeyFrame fade = new KeyFrame(Duration.millis(300),
            new KeyValue(ring.opacityProperty(), 0));
        
        timeline.getKeyFrames().addAll(expand, fade);
        
        // Add lightning bolts
        addElectricZapEffect(x, y, timeline);
        
        timeline.setOnFinished(e -> battleField.getChildren().remove(ring));
    }
    
    /**
     * Default impact particles (white pops)
     */
    private void addDefaultImpactParticles(double x, double y, Timeline timeline) {
        for (int i = 0; i < 6; i++) {
            Circle particle = new Circle(6, Color.WHITE);
            particle.setOpacity(0);
            
            double angle = (i / 6.0) * 2 * Math.PI;
            double radius = 40;
            
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
            timeline.setOnFinished(e -> battleField.getChildren().remove(particle));
        }
    }
    
    /**
     * Recovery effect for defender
     */
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