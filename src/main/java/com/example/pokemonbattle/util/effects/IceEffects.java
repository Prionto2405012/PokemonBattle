// IceEffects.java
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class IceEffects {
    private final Pane battleField;
    private final Random random = new Random();
    
    public IceEffects(Pane battleField) {
        this.battleField = battleField;
    }
    
    /**
     * Create beam effect for ice beam moves
     */
    public Timeline createBeamEffect(double startX, double startY, double endX, double endY, 
                                    String moveName, int movePower) {
        Timeline beamTimeline = new Timeline();
        
        // Calculate beam width based on power
        double beamWidth = Math.min(20 + movePower / 10.0, 35);
        
        // Create beam rectangle
        Rectangle beam = new Rectangle();
        beam.setFill(Color.CYAN);
        beam.setOpacity(0);
        beam.setEffect(new DropShadow(35, Color.LIGHTBLUE));
        
        double angle = Math.atan2(endY - startY, endX - startX);
        double distance = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        
        beam.setWidth(0);
        beam.setHeight(beamWidth);
        beam.setX(startX);
        beam.setY(startY - beamWidth / 2);
        beam.setRotate(Math.toDegrees(angle));
        prepareTransientNode(beam);
        
        battleField.getChildren().add(beam);
        
        // Beam grows
        KeyFrame grow = new KeyFrame(Duration.millis(150),
            new KeyValue(beam.widthProperty(), distance),
            new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame hold = new KeyFrame(Duration.millis(300),
            new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame fade = new KeyFrame(Duration.millis(400),
            new KeyValue(beam.opacityProperty(), 0));
        
        beamTimeline.getKeyFrames().addAll(grow, hold, fade);
        registerCleanup(beamTimeline, beam);
        
        // Add ice particles along the beam
        addBeamParticles(startX, startY, endX, endY, beamTimeline);
        
        return beamTimeline;
    }
    
    private void addBeamParticles(double startX, double startY, double endX, double endY, Timeline timeline) {
        int particleCount = 30;
        
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double)particleCount;
            double px = startX + (endX - startX) * t;
            double py = startY + (endY - startY) * t;
            
            Circle particle = new Circle(12 + random.nextDouble() * 4, Color.WHITE);
            particle.setCenterX(px);
            particle.setCenterY(py);
            particle.setOpacity(0);
            particle.setEffect(new DropShadow(8, Color.CYAN));
            prepareTransientNode(particle);
            
            battleField.getChildren().add(particle);
            
            int delay = i * 15;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(particle.opacityProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 300),
                new KeyValue(particle.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, fade);
            
            registerCleanup(timeline, particle);
        }
    }
    
    /**
     * Create impact effect for ice moves
     */
    public void createImpactEffect(double startX, double startY, double endX, double endY,
            String moveName, int movePower, Timeline timeline) {
        boolean isFangMove = moveName.contains("fang");
        boolean isWindMove = moveName.contains("wind") || moveName.contains("blizzard") || moveName.contains("avalanche");
        boolean isBreathMove = moveName.contains("breath") || moveName.contains("reception") || 
                              moveName.contains("freeze") || moveName.contains("powder");
        boolean isCrushMove = moveName.contains("crush");
        
        if (isFangMove) {
            addFangVisual(endX, endY, timeline);
        } else if (isWindMove) {
            addWindEffect(startX, startY, endX, endY, movePower, timeline);
        } else if (isBreathMove) {
            addBreathEffect(startX, startY, endX, endY, movePower, timeline);
        } else if (isCrushMove) {
            addCrushEffect(startX, startY, endX, endY, movePower, timeline);
        } else {
            // Default ice impact
            addIceShardsAndSnowflakes(startX, startY, endX, endY, movePower, timeline);
        }
    }
    
    private void addFangVisual(double x, double y, Timeline timeline) {
        for (int i = 0; i < 2; i++) {
            Polygon fang = new Polygon();
            fang.getPoints().addAll(
                0.0, 0.0,
                -18.0, -25.0,
                0.0, -55.0,
                18.0, -25.0
            );
            
            fang.setFill(Color.CYAN);
            fang.setStroke(Color.LIGHTBLUE);
            fang.setStrokeWidth(12);
            fang.setEffect(new DropShadow(25, Color.DEEPSKYBLUE));
            
            double xOffset = i == 0 ? -15 : 15;
            fang.setLayoutX(x + xOffset);
            fang.setLayoutY(y);
            fang.setOpacity(0);
            fang.setRotate(i == 0 ? -20 : 20);
            prepareTransientNode(fang);
            
            battleField.getChildren().add(fang);
            
            KeyFrame appear = new KeyFrame(Duration.millis(50),
                new KeyValue(fang.opacityProperty(), 1.0));
            KeyFrame bite = new KeyFrame(Duration.millis(100),
                new KeyValue(fang.scaleXProperty(), 1.4),
                new KeyValue(fang.scaleYProperty(), 1.4));
            KeyFrame disappear = new KeyFrame(Duration.millis(200),
                new KeyValue(fang.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, bite, disappear);
            
            registerCleanup(timeline, fang);
        }
        
        addIceShardsAndSnowflakes(x - 90, y, x, y, 65, timeline);
    }
    
    private void addWindEffect(double startX, double startY, double endX, double endY, int movePower,
            Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.hypot(dx, dy);
        if (distance < 1) {
            distance = 1;
        }
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        double corridor = Math.max(160.0, safeBattleHeight() * 0.9);
        double segmentLength = Math.min(Math.max(160.0, distance * 0.5), distance + 120.0);

        // Wind lines
        int windLineCount = Math.min(18 + movePower / 12, 26);
        
        for (int i = 0; i < windLineCount; i++) {
            Line windLine = new Line();
            windLine.setStroke(Color.LIGHTBLUE);
            windLine.setStrokeWidth(12);
            windLine.setOpacity(0);
            windLine.setEffect(new GaussianBlur(2));

            double lane = (i + random.nextDouble()) / windLineCount;
            double progressStart = Math.max(0.0, lane * 0.82 - 0.20);
            double progressEnd = Math.min(1.0, progressStart + (segmentLength / distance));

            double spreadOffset = (random.nextDouble() - 0.5) * corridor;
            double jitter = (random.nextDouble() - 0.5) * 22.0;

            double sx = startX + (ux * distance * progressStart) + (px * spreadOffset);
            double sy = startY + (uy * distance * progressStart) + (py * spreadOffset);
            double ex = sx + (ux * segmentLength) + (px * jitter);
            double ey = sy + (uy * segmentLength) + (py * jitter);

            double targetSpreadOffset = (random.nextDouble() - 0.5) * corridor;
            double tx = startX + (ux * distance * progressEnd) + (px * targetSpreadOffset);
            double ty = startY + (uy * distance * progressEnd) + (py * targetSpreadOffset);

            windLine.setStartX(sx);
            windLine.setStartY(sy);
            windLine.setEndX(ex);
            windLine.setEndY(ey);
            prepareTransientNode(windLine);
            
            battleField.getChildren().add(windLine);
            
            int delay = i * 20;
            KeyFrame start = new KeyFrame(Duration.millis(delay));
            KeyFrame appear = new KeyFrame(Duration.millis(delay + 50),
                new KeyValue(windLine.opacityProperty(), 0.7));
            KeyFrame move = new KeyFrame(Duration.millis(delay + 280),
                new KeyValue(windLine.startXProperty(), tx),
                new KeyValue(windLine.startYProperty(), ty),
                new KeyValue(windLine.endXProperty(), tx + (ux * segmentLength) + (px * jitter)),
                new KeyValue(windLine.endYProperty(), ty + (uy * segmentLength) + (py * jitter)),
                new KeyValue(windLine.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(start, appear, move);
            
            registerCleanup(timeline, windLine);
        }
        
        // Snowflakes in wind
        int snowCount = Math.min(20 + movePower / 10, 32);
        addSnowflakes(startX, startY, endX, endY, snowCount, true, timeline);
    }
    
    private void addBreathEffect(double startX, double startY, double endX, double endY, int movePower,
            Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        double cloudSpan = Math.max(120.0, safeBattleHeight() * 0.55);

        // Mist clouds
        int mistCount = Math.min(15 + movePower / 25, 20);
        
        for (int i = 0; i < mistCount; i++) {
            Circle mist = new Circle(20 + random.nextDouble() * 20, Color.LIGHTCYAN);
            mist.setOpacity(0);
            mist.setEffect(new GaussianBlur(15));

            double progress = Math.max(0.0, (i + random.nextDouble()) / mistCount * 0.75);
            double laneOffset = (random.nextDouble() - 0.5) * cloudSpan;
            double travel = Math.min(distance * 0.35, 180 + random.nextDouble() * 100);

            mist.setCenterX(startX + (ux * distance * progress) + (px * laneOffset));
            mist.setCenterY(startY + (uy * distance * progress) + (py * laneOffset));
            prepareTransientNode(mist);
            
            battleField.getChildren().add(mist);
            
            int delay = i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(mist.opacityProperty(), 0.6));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 200),
                new KeyValue(mist.radiusProperty(), mist.getRadius() * 2),
                new KeyValue(mist.centerXProperty(), mist.getCenterX() + (ux * travel)),
                new KeyValue(mist.centerYProperty(), mist.getCenterY() + (uy * travel)),
                new KeyValue(mist.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, expand);
            
            registerCleanup(timeline, mist);
        }
        
        // Light snowflakes
        addSnowflakes(startX, startY, endX, endY, 30, false, timeline);
    }
    
    private void addCrushEffect(double startX, double startY, double endX, double endY, int movePower,
            Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        int cubeCount = Math.min(15 + movePower / 20, 20);
        
        for (int i = 0; i < cubeCount; i++) {
            Rectangle cube = new Rectangle(15 + random.nextDouble() * 10, 12 + random.nextDouble() * 10);
            cube.setFill(Color.CYAN);
            cube.setStroke(Color.LIGHTBLUE);
            cube.setStrokeWidth(3);
            cube.setOpacity(0);
            cube.setEffect(new DropShadow(8, Color.DEEPSKYBLUE));

            double progress = (i + random.nextDouble()) / cubeCount;
            double lateral = (random.nextDouble() - 0.5) * Math.max(120.0, safeBattleHeight() * 0.55);

            cube.setX(startX + (ux * distance * progress) + (px * lateral));
            cube.setY(startY + (uy * distance * progress) + (py * lateral) - 120);
            cube.setRotate(random.nextDouble() * 360);
            prepareTransientNode(cube);
            
            battleField.getChildren().add(cube);
            
            int delay = i * 40;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(cube.opacityProperty(), 1.0));
            KeyFrame fall = new KeyFrame(Duration.millis(delay + 300),
                new KeyValue(cube.yProperty(), cube.getY() + 140),
                new KeyValue(cube.rotateProperty(), cube.getRotate() + 180));
            KeyFrame shatter = new KeyFrame(Duration.millis(delay + 350),
                new KeyValue(cube.opacityProperty(), 0),
                new KeyValue(cube.scaleXProperty(), 0.3),
                new KeyValue(cube.scaleYProperty(), 0.3));
            
            timeline.getKeyFrames().addAll(appear, fall, shatter);
            
            registerCleanup(timeline, cube);
        }
    }
    
    private void addIceShardsAndSnowflakes(double startX, double startY, double endX, double endY, int movePower,
            Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        // Ice shards
        int shardCount = Math.min(10 + movePower / 20, 15);
        
        for (int i = 0; i < shardCount; i++) {
            Polygon shard = new Polygon();
            shard.getPoints().addAll(
                0.0, 0.0,
                -10.0, -20.0,
                5.0, -40.0,
                10.0, -20.0
            );
            shard.setFill(Color.CYAN);
            shard.setStroke(Color.LIGHTBLUE);
            shard.setStrokeWidth(1.5);
            shard.setEffect(new DropShadow(10, Color.DEEPSKYBLUE));

            double progress = (i + random.nextDouble()) / shardCount;
            double lateral = (random.nextDouble() - 0.5) * Math.max(120.0, safeBattleHeight() * 0.5);

            shard.setLayoutX(startX + (ux * distance * progress) + (px * lateral));
            shard.setLayoutY(startY + (uy * distance * progress) + (py * lateral));
            shard.setOpacity(0);
            shard.setRotate(Math.toDegrees(Math.atan2(uy, ux)) + random.nextDouble() * 40);
            prepareTransientNode(shard);
            
            battleField.getChildren().add(shard);
            
            int delay = i * 35;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(shard.opacityProperty(), 1.0));
            KeyFrame fly = new KeyFrame(Duration.millis(delay + 250),
                new KeyValue(shard.translateXProperty(), (ux * 90) + (px * (random.nextDouble() - 0.5) * 28)),
                new KeyValue(shard.translateYProperty(), (uy * 90) + (py * (random.nextDouble() - 0.5) * 28)),
                new KeyValue(shard.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, fly);
            
            registerCleanup(timeline, shard);
        }
        
        // Snowflakes
        int snowCount = Math.min(15 + movePower / 20, 20);
        addSnowflakes(startX, startY, endX, endY, snowCount, false, timeline);
    }
    
    private void addSnowflakes(double startX, double startY, double endX, double endY, int count,
            boolean windBlown, Timeline timeline) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.max(1.0, Math.hypot(dx, dy));
        double ux = dx / distance;
        double uy = dy / distance;
        double px = -uy;
        double py = ux;

        for (int i = 0; i < count; i++) {
            Circle snowflake = new Circle(5 + random.nextDouble() * 4, Color.WHITE);
            snowflake.setEffect(new DropShadow(6, Color.LIGHTBLUE));

            double progress = (i + random.nextDouble()) / count;
            double lateral = (random.nextDouble() - 0.5) * Math.max(120.0, safeBattleHeight() * 0.7);

            snowflake.setCenterX(startX + (ux * distance * progress) + (px * lateral));
            snowflake.setCenterY(startY + (uy * distance * progress) + (py * lateral));
            snowflake.setOpacity(0);
            prepareTransientNode(snowflake);
            
            battleField.getChildren().add(snowflake);
            
            int delay = i * 25;
            double travel = windBlown ? Math.min(distance * 0.55, 240.0) : Math.min(distance * 0.35, 140.0);
            double lateralDrift = windBlown ? (random.nextDouble() - 0.5) * 60.0 : (random.nextDouble() - 0.5) * 34.0;
            double snowEndX = snowflake.getCenterX() + (ux * travel) + (px * lateralDrift);
            double snowEndY = snowflake.getCenterY() + (uy * travel) + (py * lateralDrift) + (windBlown ? 0.0 : 34.0);
            
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(snowflake.opacityProperty(), 1.0));
            KeyFrame driftFrame = new KeyFrame(Duration.millis(delay + 350),
                new KeyValue(snowflake.centerXProperty(), snowEndX),
                new KeyValue(snowflake.centerYProperty(), snowEndY),
                new KeyValue(snowflake.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, driftFrame);
            
            registerCleanup(timeline, snowflake);
        }
    }

    private double safeBattleHeight() {
        double h = battleField.getHeight();
        return h > 0 ? h : 700.0;
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