// ElectricEffects.java
package com.example.pokemonbattle.util.effects;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
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
            double angle = random.nextDouble() * 2 * Math.PI;
            double length = 80 + random.nextDouble() * 40;
            
            double offsetX = movingRight ? i * 15 : -i * 15;
            double offsetY = (random.nextDouble() - 0.5) * 28;
            
            Polyline spark = createBolt(
                startX + offsetX,
                startY + offsetY,
                startX + offsetX + Math.cos(angle) * length,
                startY + offsetY + Math.sin(angle) * length,
                4,
                14);
            
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
            
            registerCleanup(timeline, spark);
        }
    }

    /**
     * Create ranged effect for electric special moves.
     */
    public Timeline createRangedEffect(double startX, double startY, double endX, double endY,
            String moveName, int movePower) {
        Timeline timeline = new Timeline();

        addSourceBurst(startX, startY, movePower, timeline);

        int boltCount = moveName.equals("discharge") ? 5 : 3;
        for (int i = 0; i < boltCount; i++) {
            double laneOffset = (random.nextDouble() - 0.5) * 90;
            Polyline bolt = createBolt(
                startX,
                startY,
                endX + laneOffset * 0.22,
                endY + laneOffset,
                6,
                20 + movePower / 14.0);

            battleField.getChildren().add(bolt);

            int delay = i * 55;
            KeyFrame flash1 = new KeyFrame(Duration.millis(delay),
                new KeyValue(bolt.opacityProperty(), 0));
            KeyFrame flash2 = new KeyFrame(Duration.millis(delay + 35),
                new KeyValue(bolt.opacityProperty(), 1.0));
            KeyFrame flash3 = new KeyFrame(Duration.millis(delay + 110),
                new KeyValue(bolt.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(flash1, flash2, flash3);
            registerCleanup(timeline, bolt);
        }

        return timeline;
    }
    
    /**
     * Create impact effect for electric moves
     */
    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        boolean isFangMove = moveName.contains("fang");
        
        if (isFangMove) {
            addFangVisual(x, y, timeline);
        }
        
        // Electric zaps - bigger and scaled to power
        int zapCount = Math.min(14 + movePower / 20, 20);
        
        for (int i = 0; i < zapCount; i++) {
            double angle = (i / (double)zapCount) * 2 * Math.PI + random.nextDouble() * 0.5;
            double length = 80 + random.nextDouble() * 50 + (movePower / 3.0);
            
            Polyline zap = createBolt(
                x,
                y,
                x + Math.cos(angle) * length,
                y + Math.sin(angle) * length,
                5,
                18 + movePower / 16.0);
            
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
            
            registerCleanup(timeline, zap);
        }
    }

    private void addSourceBurst(double x, double y, int movePower, Timeline timeline) {
        Circle flare = new Circle(18 + movePower / 16.0, Color.rgb(255, 245, 160, 0.85));
        flare.setCenterX(x);
        flare.setCenterY(y);
        flare.setOpacity(0);
        flare.setEffect(new DropShadow(24, Color.GOLD));
        prepareTransientNode(flare);

        battleField.getChildren().add(flare);

        KeyFrame appear = new KeyFrame(Duration.millis(30),
            new KeyValue(flare.opacityProperty(), 1.0),
            new KeyValue(flare.radiusProperty(), flare.getRadius() * 1.15));
        KeyFrame fade = new KeyFrame(Duration.millis(140),
            new KeyValue(flare.opacityProperty(), 0),
            new KeyValue(flare.radiusProperty(), flare.getRadius() * 1.8));

        timeline.getKeyFrames().addAll(appear, fade);
        registerCleanup(timeline, flare);
    }
    
    private void addFangVisual(double x, double y, Timeline timeline) {
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
            prepareTransientNode(fang);
            
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
            
            registerCleanup(timeline, fang);
        }
    }

    private Polyline createBolt(double startX, double startY, double endX, double endY,
            int segmentCount, double maxOffset) {
        Polyline bolt = new Polyline();
        bolt.setStroke(Color.web("#FFE55C"));
        bolt.setStrokeWidth(7);
        bolt.setStrokeLineCap(StrokeLineCap.ROUND);
        bolt.setEffect(new DropShadow(24, Color.web("#FFB300")));
        bolt.setOpacity(0);
        bolt.setFill(null);

        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double px = -(dy / distance);
        double py = dx / distance;

        bolt.getPoints().addAll(startX, startY);
        for (int i = 1; i < segmentCount; i++) {
            double progress = i / (double) segmentCount;
            double offset = (random.nextDouble() - 0.5) * maxOffset;
            double pointX = startX + dx * progress + px * offset;
            double pointY = startY + dy * progress + py * offset;
            bolt.getPoints().addAll(pointX, pointY);
        }
        bolt.getPoints().addAll(endX, endY);

        prepareTransientNode(bolt);
        return bolt;
    }

    private void prepareTransientNode(Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private void registerCleanup(Timeline timeline, Node node) {
        EventHandler<ActionEvent> previousOnFinished = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (previousOnFinished != null) {
                previousOnFinished.handle(e);
            }
        });
    }
}