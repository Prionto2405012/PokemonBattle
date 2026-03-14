// FireEffects.java
package com.example.pokemonbattle.util.effects;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

public class FireEffects {
    private final Pane battleField;
    private final Random random = new Random();
    
    public FireEffects(Pane battleField) {
        this.battleField = battleField;
    }
    
    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        boolean isFangMove = moveName.contains("fang");
        
        if (isFangMove) {
            addFangVisual(x, y, timeline);
        }
        
        // Fire particles
        int flameCount = Math.min(6 + movePower / 20, 12);
        
        for (int i = 0; i < flameCount; i++) {
            Polygon flame = new Polygon();
            flame.getPoints().addAll(
                0.0, 0.0,
                -6.0, -18.0,
                0.0, -30.0,
                6.0, -18.0
            );
            
            Color flameColor = i % 2 == 0 ? Color.ORANGERED : Color.ORANGE;
            flame.setFill(flameColor);
            flame.setEffect(new GaussianBlur(4));
            
            double angle = (i / (double)flameCount) * 2 * Math.PI;
            double radius = 30;
            
            flame.setLayoutX(x + Math.cos(angle) * radius);
            flame.setLayoutY(y + Math.sin(angle) * radius);
            flame.setOpacity(0);
            flame.setRotate(Math.toDegrees(angle));
            
            battleField.getChildren().add(flame);
            
            int delay = i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(flame.opacityProperty(), 1.0));
            KeyFrame grow = new KeyFrame(Duration.millis(delay + 60),
                new KeyValue(flame.scaleXProperty(), 1.6),
                new KeyValue(flame.scaleYProperty(), 1.6));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 180),
                new KeyValue(flame.opacityProperty(), 0),
                new KeyValue(flame.scaleYProperty(), 2.2));
            
            timeline.getKeyFrames().addAll(appear, grow, fade);
            
            final Polygon f = flame;
            timeline.setOnFinished(e -> battleField.getChildren().remove(f));
        }
    }
    
    private void addFangVisual(double x, double y, Timeline timeline) {
        for (int i = 0; i < 2; i++) {
            Polygon fang = new Polygon();
            fang.getPoints().addAll(
                0.0, 0.0,
                -8.0, -25.0,
                0.0, -35.0,
                8.0, -25.0
            );
            
            fang.setFill(Color.ORANGERED);
            fang.setStroke(Color.ORANGE);
            fang.setStrokeWidth(2);
            fang.setEffect(new DropShadow(10, Color.DARKORANGE));
            
            double xOffset = i == 0 ? -15 : 15;
            fang.setLayoutX(x + xOffset);
            fang.setLayoutY(y);
            fang.setOpacity(0);
            fang.setRotate(i == 0 ? -20 : 20);
            
            battleField.getChildren().add(fang);
            
            KeyFrame appear = new KeyFrame(Duration.millis(50),
                new KeyValue(fang.opacityProperty(), 1.0));
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