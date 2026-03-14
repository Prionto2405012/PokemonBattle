// IceEffects.java
package com.example.pokemonbattle.util.effects;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
        double beamWidth = Math.min(8 + movePower / 15.0, 25);
        
        // Create beam rectangle
        Rectangle beam = new Rectangle();
        beam.setFill(Color.CYAN);
        beam.setOpacity(0);
        beam.setEffect(new DropShadow(15, Color.LIGHTBLUE));
        
        double angle = Math.atan2(endY - startY, endX - startX);
        double distance = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        
        beam.setWidth(0);
        beam.setHeight(beamWidth);
        beam.setX(startX);
        beam.setY(startY - beamWidth / 2);
        beam.setRotate(Math.toDegrees(angle));
        
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
        beamTimeline.setOnFinished(e -> battleField.getChildren().remove(beam));
        
        // Add ice particles along the beam
        addBeamParticles(startX, startY, endX, endY, beamTimeline);
        
        return beamTimeline;
    }
    
    private void addBeamParticles(double startX, double startY, double endX, double endY, Timeline timeline) {
        int particleCount = 10;
        
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double)particleCount;
            double px = startX + (endX - startX) * t;
            double py = startY + (endY - startY) * t;
            
            Circle particle = new Circle(3 + random.nextDouble() * 4, Color.WHITE);
            particle.setCenterX(px);
            particle.setCenterY(py);
            particle.setOpacity(0);
            particle.setEffect(new DropShadow(8, Color.CYAN));
            
            battleField.getChildren().add(particle);
            
            int delay = i * 15;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(particle.opacityProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 300),
                new KeyValue(particle.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, fade);
            
            final Circle p = particle;
            timeline.setOnFinished(e -> battleField.getChildren().remove(p));
        }
    }
    
    /**
     * Create impact effect for ice moves
     */
    public void createImpactEffect(double x, double y, String moveName, int movePower, Timeline timeline) {
        boolean isFangMove = moveName.contains("fang");
        boolean isWindMove = moveName.contains("wind") || moveName.contains("blizzard") || moveName.contains("avalanche");
        boolean isBreathMove = moveName.contains("breath") || moveName.contains("reception") || 
                              moveName.contains("freeze") || moveName.contains("powder");
        boolean isCrushMove = moveName.contains("crush");
        
        if (isFangMove) {
            addFangVisual(x, y, timeline);
        } else if (isWindMove) {
            addWindEffect(x, y, movePower, timeline);
        } else if (isBreathMove) {
            addBreathEffect(x, y, movePower, timeline);
        } else if (isCrushMove) {
            addCrushEffect(x, y, movePower, timeline);
        } else {
            // Default ice impact
            addIceShardsAndSnowflakes(x, y, movePower, timeline);
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
            
            fang.setFill(Color.CYAN);
            fang.setStroke(Color.LIGHTBLUE);
            fang.setStrokeWidth(2);
            fang.setEffect(new DropShadow(10, Color.DEEPSKYBLUE));
            
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
        
        addIceShardsAndSnowflakes(x, y, 65, timeline);
    }
    
    private void addWindEffect(double x, double y, int movePower, Timeline timeline) {
        // Wind lines
        int windLineCount = Math.min(5 + movePower / 20, 12);
        
        for (int i = 0; i < windLineCount; i++) {
            Line windLine = new Line();
            windLine.setStroke(Color.LIGHTBLUE);
            windLine.setStrokeWidth(3);
            windLine.setOpacity(0);
            windLine.setEffect(new GaussianBlur(2));
            
            double startAngle = -0.3 + random.nextDouble() * 0.6;
            double length = 40 + random.nextDouble() * 40;
            double yOffset = random.nextDouble() * 80 - 40;
            
            windLine.setStartX(x - 60);
            windLine.setStartY(y + yOffset);
            windLine.setEndX(x - 60 + Math.cos(startAngle) * length);
            windLine.setEndY(y + yOffset + Math.sin(startAngle) * length);
            
            battleField.getChildren().add(windLine);
            
            int delay = i * 20;
            KeyFrame start = new KeyFrame(Duration.millis(delay));
            KeyFrame appear = new KeyFrame(Duration.millis(delay + 50),
                new KeyValue(windLine.opacityProperty(), 0.7));
            KeyFrame move = new KeyFrame(Duration.millis(delay + 250),
                new KeyValue(windLine.startXProperty(), x + 80),
                new KeyValue(windLine.endXProperty(), x + 80 + Math.cos(startAngle) * length),
                new KeyValue(windLine.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(start, appear, move);
            
            final Line wl = windLine;
            timeline.setOnFinished(e -> battleField.getChildren().remove(wl));
        }
        
        // Snowflakes in wind
        int snowCount = Math.min(8 + movePower / 15, 20);
        addSnowflakes(x, y, snowCount, true, timeline);
    }
    
    private void addBreathEffect(double x, double y, int movePower, Timeline timeline) {
        // Mist clouds
        int mistCount = Math.min(6 + movePower / 25, 12);
        
        for (int i = 0; i < mistCount; i++) {
            Circle mist = new Circle(15 + random.nextDouble() * 20, Color.LIGHTCYAN);
            mist.setOpacity(0);
            mist.setEffect(new GaussianBlur(15));
            
            double angle = random.nextDouble() * Math.PI / 2 - Math.PI / 4;
            double distance = 30 + random.nextDouble() * 40;
            
            mist.setCenterX(x - 40);
            mist.setCenterY(y + random.nextDouble() * 40 - 20);
            
            battleField.getChildren().add(mist);
            
            int delay = i * 30;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(mist.opacityProperty(), 0.6));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 200),
                new KeyValue(mist.radiusProperty(), mist.getRadius() * 2),
                new KeyValue(mist.centerXProperty(), x + Math.cos(angle) * distance),
                new KeyValue(mist.centerYProperty(), mist.getCenterY() + Math.sin(angle) * distance),
                new KeyValue(mist.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, expand);
            
            final Circle m = mist;
            timeline.setOnFinished(e -> battleField.getChildren().remove(m));
        }
        
        // Light snowflakes
        addSnowflakes(x, y, 10, false, timeline);
    }
    
    private void addCrushEffect(double x, double y, int movePower, Timeline timeline) {
        int cubeCount = Math.min(6 + movePower / 20, 12);
        
        for (int i = 0; i < cubeCount; i++) {
            Rectangle cube = new Rectangle(12 + random.nextDouble() * 10, 12 + random.nextDouble() * 10);
            cube.setFill(Color.CYAN);
            cube.setStroke(Color.LIGHTBLUE);
            cube.setStrokeWidth(2);
            cube.setOpacity(0);
            cube.setEffect(new DropShadow(8, Color.DEEPSKYBLUE));
            
            double xOffset = random.nextDouble() * 60 - 30;
            cube.setX(x + xOffset);
            cube.setY(y - 80);
            cube.setRotate(random.nextDouble() * 360);
            
            battleField.getChildren().add(cube);
            
            int delay = i * 40;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(cube.opacityProperty(), 1.0));
            KeyFrame fall = new KeyFrame(Duration.millis(delay + 300),
                new KeyValue(cube.yProperty(), y + 20),
                new KeyValue(cube.rotateProperty(), cube.getRotate() + 180));
            KeyFrame shatter = new KeyFrame(Duration.millis(delay + 350),
                new KeyValue(cube.opacityProperty(), 0),
                new KeyValue(cube.scaleXProperty(), 0.3),
                new KeyValue(cube.scaleYProperty(), 0.3));
            
            timeline.getKeyFrames().addAll(appear, fall, shatter);
            
            final Rectangle c = cube;
            timeline.setOnFinished(e -> battleField.getChildren().remove(c));
        }
    }
    
    private void addIceShardsAndSnowflakes(double x, double y, int movePower, Timeline timeline) {
        // Ice shards
        int shardCount = Math.min(4 + movePower / 30, 8);
        
        for (int i = 0; i < shardCount; i++) {
            Polygon shard = new Polygon();
            shard.getPoints().addAll(
                0.0, 0.0,
                -4.0, -20.0,
                0.0, -30.0,
                4.0, -20.0
            );
            shard.setFill(Color.CYAN);
            shard.setStroke(Color.LIGHTBLUE);
            shard.setStrokeWidth(1.5);
            shard.setEffect(new DropShadow(10, Color.DEEPSKYBLUE));
            
            double angle = (i / (double)shardCount) * 2 * Math.PI;
            double radius = 35;
            
            shard.setLayoutX(x + Math.cos(angle) * radius);
            shard.setLayoutY(y + Math.sin(angle) * radius);
            shard.setOpacity(0);
            shard.setRotate(Math.toDegrees(angle) + random.nextDouble() * 40);
            
            battleField.getChildren().add(shard);
            
            int delay = i * 35;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(shard.opacityProperty(), 1.0));
            KeyFrame fly = new KeyFrame(Duration.millis(delay + 250),
                new KeyValue(shard.translateXProperty(), Math.cos(angle) * 50),
                new KeyValue(shard.translateYProperty(), Math.sin(angle) * 50),
                new KeyValue(shard.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, fly);
            
            final Polygon s = shard;
            timeline.setOnFinished(e -> battleField.getChildren().remove(s));
        }
        
        // Snowflakes
        int snowCount = Math.min(8 + movePower / 20, 15);
        addSnowflakes(x, y, snowCount, false, timeline);
    }
    
    private void addSnowflakes(double x, double y, int count, boolean windBlown, Timeline timeline) {
        for (int i = 0; i < count; i++) {
            Circle snowflake = new Circle(2 + random.nextDouble() * 4, Color.WHITE);
            snowflake.setEffect(new DropShadow(6, Color.LIGHTBLUE));
            
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * 60;
            
            snowflake.setCenterX(x);
            snowflake.setCenterY(y);
            snowflake.setOpacity(0);
            
            battleField.getChildren().add(snowflake);
            
            int delay = i * 25;
            double endX = windBlown ? x + 70 + random.nextDouble() * 40 : x + Math.cos(angle) * distance;
            double endY = windBlown ? y + random.nextDouble() * 40 - 20 : y + Math.sin(angle) * distance + 40;
            
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(snowflake.opacityProperty(), 1.0));
            KeyFrame drift = new KeyFrame(Duration.millis(delay + 350),
                new KeyValue(snowflake.centerXProperty(), endX),
                new KeyValue(snowflake.centerYProperty(), endY),
                new KeyValue(snowflake.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, drift);
            
            final Circle s = snowflake;
            timeline.setOnFinished(e -> battleField.getChildren().remove(s));
        }
    }
}