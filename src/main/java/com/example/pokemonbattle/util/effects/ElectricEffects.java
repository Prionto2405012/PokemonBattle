// ElectricEffects.java
package com.example.pokemonbattle.util.effects;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

public class ElectricEffects {
    private final Pane battleField;
    private final Random random = new Random();
    
    public ElectricEffects(Pane battleField) {
        this.battleField = battleField;
    }
    
    /**
     * Add sparks during movement for electric moves
     */
    public void addMovementSparks(double startX, double startY, boolean movingRight, Timeline timeline) {
        for (int i = 0; i < 8; i++) {
            Line spark = new Line();
            spark.setStroke(Color.YELLOW);
            spark.setStrokeWidth(10);
            spark.setEffect(new DropShadow(25, Color.GOLD));
            
            double angle = random.nextDouble() * 2 * Math.PI;
            double length = 80 + random.nextDouble() * 40;
            
            double offsetX = movingRight ? i * 15 : -i * 15;
            
            spark.setStartX(startX + offsetX);
            spark.setStartY(startY);
            spark.setEndX(startX + offsetX + Math.cos(angle) * length);
            spark.setEndY(startY + Math.sin(angle) * length);
            spark.setOpacity(0);
            
            battleField.getChildren().add(spark);
            
            int delay = i * 25;
            KeyFrame flash1 = new KeyFrame(Duration.millis(delay),
                new KeyValue(spark.opacityProperty(), 0));
            KeyFrame flash2 = new KeyFrame(Duration.millis(delay + 30),
                new KeyValue(spark.opacityProperty(), 1.0));
            KeyFrame flash3 = new KeyFrame(Duration.millis(delay + 60),
                new KeyValue(spark.opacityProperty(), 0));
            KeyFrame flash4 = new KeyFrame(Duration.millis(delay + 100),
                new KeyValue(spark.opacityProperty(), 0.8));
            KeyFrame flash5 = new KeyFrame(Duration.millis(delay + 130),
                new KeyValue(spark.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(flash1, flash2, flash3, flash4, flash5);
            
            final Line s = spark;
            timeline.setOnFinished(e -> battleField.getChildren().remove(s));
        }
    }
    
    /**
     * Create impact effect for electric moves
     */
    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        boolean isFangMove = moveName.contains("fang");
        
        if (isFangMove) {
            addFangVisual(x, y, "electric", timeline);
        }
        
        // Electric zaps - bigger and scaled to power
        int zapCount = Math.min(4 + movePower / 25, 12);
        
        for (int i = 0; i < zapCount; i++) {
            Line zap = new Line();
            zap.setStroke(Color.YELLOW);
            zap.setStrokeWidth(15);
            zap.setEffect(new DropShadow(30, Color.GOLD));
            
            double angle = (i / (double)zapCount) * 2 * Math.PI + random.nextDouble() * 0.5;
            double length = 80 + random.nextDouble() * 50 + (movePower / 3.0);
            
            zap.setStartX(x);
            zap.setStartY(y);
            zap.setEndX(x + Math.cos(angle) * length);
            zap.setEndY(y + Math.sin(angle) * length);
            zap.setOpacity(0);
            
            battleField.getChildren().add(zap);
            
            int delay = i * 20;
            KeyFrame flash1 = new KeyFrame(Duration.millis(delay),
                new KeyValue(zap.opacityProperty(), 0));
            KeyFrame flash2 = new KeyFrame(Duration.millis(delay + 25),
                new KeyValue(zap.opacityProperty(), 1.0));
            KeyFrame flash3 = new KeyFrame(Duration.millis(delay + 50),
                new KeyValue(zap.opacityProperty(), 0));
            KeyFrame flash4 = new KeyFrame(Duration.millis(delay + 90),
                new KeyValue(zap.opacityProperty(), 0.9));
            KeyFrame flash5 = new KeyFrame(Duration.millis(delay + 115),
                new KeyValue(zap.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(flash1, flash2, flash3, flash4, flash5);
            
            final Line z = zap;
            timeline.setOnFinished(e -> battleField.getChildren().remove(z));
        }
    }
    
    private void addFangVisual(double x, double y, String type, Timeline timeline) {
        // Create two fangs
        for (int i = 0; i < 2; i++) {
            Polygon fang = new Polygon();
            fang.getPoints().addAll(
                0.0, 0.0,
                -18.0, -25.0,
                0.0, -55.0,
                18.0, -25.0
            );
            
            fang.setFill(Color.YELLOW);
            fang.setStroke(Color.GOLD);
            fang.setStrokeWidth(5);
            fang.setEffect(new DropShadow(20, Color.GOLD));
            
            double xOffset = i == 0 ? -15 : 15;
            fang.setLayoutX(x + xOffset);
            fang.setLayoutY(y);
            fang.setOpacity(0);
            fang.setRotate(i == 0 ? -20 : 20);
            
            battleField.getChildren().add(fang);
            
            KeyFrame appear = new KeyFrame(Duration.millis(50),
                new KeyValue(fang.opacityProperty(), 1.0),
                new KeyValue(fang.scaleXProperty(), 1.0),
                new KeyValue(fang.scaleYProperty(), 1.0));
            KeyFrame bite = new KeyFrame(Duration.millis(100),
                new KeyValue(fang.scaleXProperty(), 1.4),
                new KeyValue(fang.scaleYProperty(), 1.4));
            KeyFrame disappear = new KeyFrame(Duration.millis(200),
                new KeyValue(fang.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, bite, disappear);
            
            final Polygon f = fang;
            timeline.setOnFinished(e -> battleField.getChildren().remove(f));
        }
    }
}