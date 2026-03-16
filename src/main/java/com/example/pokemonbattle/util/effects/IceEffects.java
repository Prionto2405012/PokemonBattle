// IceEffects.java
package com.example.pokemonbattle.util.effects;

import com.example.pokemonbattle.util.MediaCache;
import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private static final String PUNCH_ASSET = "punch.png";
    private static final String FANG_ASSET = "fang.gif";
    
    public IceEffects(Pane battleField) {
        this.battleField = battleField;
    }
    
    /**
     * Create beam effect for ice beam moves
     */
    public Timeline createBeamEffect(double startX, double startY, double endX, double endY, String moveName, int movePower) {
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
        KeyFrame grow = new KeyFrame(Duration.millis(200),
            new KeyValue(beam.widthProperty(), distance),
            new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame hold = new KeyFrame(Duration.millis(400),
            new KeyValue(beam.opacityProperty(), 0.9));
        KeyFrame fade = new KeyFrame(Duration.millis(540),
            new KeyValue(beam.opacityProperty(), 0));
        
        beamTimeline.getKeyFrames().addAll(grow, hold, fade);
        registerCleanup(beamTimeline, beam);
        
        // Add ice particles along the beam
        addBeamParticles(startX, startY, endX, endY, beamTimeline);
        
        return beamTimeline;
    }
    
    private void addBeamParticles(double startX, double startY, double endX, double endY, Timeline timeline) {
        int particleCount = 35;
        
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double)particleCount;
            double px = startX + (endX - startX) * t;
            double py = startY + (endY - startY) * t;
            
            Circle particle = new Circle(10 + random.nextDouble() * 5, Color.WHITE);
            particle.setCenterX(px + (random.nextDouble() - 0.5) * 10);
            particle.setCenterY(py + (random.nextDouble() - 0.5) * 10);
            particle.setOpacity(0);
            particle.setEffect(new DropShadow(12, Color.CYAN));
            prepareTransientNode(particle);
            
            battleField.getChildren().add(particle);
            
            int delay = i * 24;
            double baseRadius = particle.getRadius();
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(particle.opacityProperty(), 0.95));
            KeyFrame shimmer = new KeyFrame(Duration.millis(delay + 240),
                new KeyValue(particle.opacityProperty(), 0.7),
                new KeyValue(particle.radiusProperty(), baseRadius * 1.3));
            KeyFrame fade = new KeyFrame(Duration.millis(delay + 540),
                new KeyValue(particle.opacityProperty(), 0),
                new KeyValue(particle.radiusProperty(), baseRadius * 0.65));
            
            timeline.getKeyFrames().addAll(appear, shimmer, fade);
            
            registerCleanup(timeline, particle);
        }
    }
    
    /**
     * Create impact effect for ice moves
     */
    public void createImpactEffect(double startX, double startY, double endX, double endY,
            String moveName, int movePower, Timeline timeline) {
        boolean isFangMove = moveName.contains("fang");
        boolean isPunchMove = moveName.contains("punch");
        boolean isBallMove = moveName.contains("ball");
        boolean isWindMove = moveName.contains("wind") || moveName.contains("blizzard") || moveName.contains("avalanche");
        boolean isBreathMove = moveName.contains("breath") || moveName.contains("reception") || 
                              moveName.contains("freeze") || moveName.contains("powder");
        boolean isCrushMove = moveName.contains("crush");
        
        if (isFangMove) {
            addFangImage(endX, endY, timeline);
            addIceShardsAndSnowflakes(startX, startY, endX, endY, movePower, timeline);
        } else if (isPunchMove) {
            addPunchImage(endX, endY, timeline);
            addIceShardsAndSnowflakes(startX, startY, endX, endY, movePower, timeline);
        } else if (isBallMove) {
            addIceBallEffect(startX, startY, endX, endY, movePower, timeline);
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
    
    private void addIceBallEffect(double startX, double startY, double endX, double endY, int movePower,
            Timeline timeline) {
        double orbRadius = 20 + Math.min(movePower / 10.0, 10);

        Circle orb = new Circle(orbRadius, Color.web("#CFF6FF"));
        orb.setStroke(Color.WHITE);
        orb.setStrokeWidth(4);
        orb.setEffect(new DropShadow(22, Color.web("#7FDBFF")));
        orb.setCenterX(startX);
        orb.setCenterY(startY);
        orb.setOpacity(0);
        prepareTransientNode(orb);
        battleField.getChildren().add(orb);

        Circle halo = new Circle(orbRadius * 1.45, Color.color(0.8, 0.95, 1.0, 0.22));
        halo.setStroke(Color.web("#DDFBFF"));
        halo.setStrokeWidth(2.5);
        halo.setCenterX(startX);
        halo.setCenterY(startY);
        halo.setOpacity(0);
        halo.setEffect(new GaussianBlur(8));
        prepareTransientNode(halo);
        battleField.getChildren().add(halo);

        double midX = (startX + endX) / 2.0;
        double midY = Math.min(startY, endY) - 70;

        KeyFrame appear = new KeyFrame(Duration.millis(55),
            new KeyValue(orb.opacityProperty(), 1.0),
            new KeyValue(halo.opacityProperty(), 0.95));
        KeyFrame arcMid = new KeyFrame(Duration.millis(200),
            new KeyValue(orb.centerXProperty(), midX),
            new KeyValue(orb.centerYProperty(), midY),
            new KeyValue(halo.centerXProperty(), midX),
            new KeyValue(halo.centerYProperty(), midY));
        KeyFrame impact = new KeyFrame(Duration.millis(380),
            new KeyValue(orb.centerXProperty(), endX),
            new KeyValue(orb.centerYProperty(), endY),
            new KeyValue(halo.centerXProperty(), endX),
            new KeyValue(halo.centerYProperty(), endY),
            new KeyValue(halo.radiusProperty(), halo.getRadius() * 1.7));
        KeyFrame fade = new KeyFrame(Duration.millis(480),
            new KeyValue(orb.opacityProperty(), 0),
            new KeyValue(halo.opacityProperty(), 0),
            new KeyValue(orb.radiusProperty(), orb.getRadius() * 0.7));

        timeline.getKeyFrames().addAll(appear, arcMid, impact, fade);
        registerCleanup(timeline, orb);
        registerCleanup(timeline, halo);

        addIceShardsAndSnowflakes(startX, startY, endX, endY, movePower + 15, timeline);
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
        int windLineCount = Math.min(20 + movePower / 10, 30);
        
        for (int i = 0; i < windLineCount; i++) {
            Line windLine = new Line();
            windLine.setStroke(Color.LIGHTBLUE);
            windLine.setStrokeWidth(3 + random.nextDouble() * 3);
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
            
            int delay = i * 27;
            KeyFrame start = new KeyFrame(Duration.millis(delay));
            KeyFrame appear = new KeyFrame(Duration.millis(delay + 70),
                new KeyValue(windLine.opacityProperty(), 0.7));
            KeyFrame move = new KeyFrame(Duration.millis(delay + 380),
                new KeyValue(windLine.startXProperty(), tx),
                new KeyValue(windLine.startYProperty(), ty),
                new KeyValue(windLine.endXProperty(), tx + (ux * segmentLength) + (px * jitter)),
                new KeyValue(windLine.endYProperty(), ty + (uy * segmentLength) + (py * jitter)),
                new KeyValue(windLine.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(start, appear, move);
            
            registerCleanup(timeline, windLine);
        }
        
        // Snowflakes in wind
        int snowCount = Math.min(24 + movePower / 8, 36);
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
        int mistCount = Math.min(18 + movePower / 20, 25);
        
        for (int i = 0; i < mistCount; i++) {
            Circle mist = new Circle(22 + random.nextDouble() * 22, Color.LIGHTCYAN);
            mist.setOpacity(0);
            mist.setEffect(new GaussianBlur(15));

            double progress = Math.max(0.0, (i + random.nextDouble()) / mistCount * 0.75);
            double laneOffset = (random.nextDouble() - 0.5) * cloudSpan;
            double travel = Math.min(distance * 0.35, 180 + random.nextDouble() * 100);

            mist.setCenterX(startX + (ux * distance * progress) + (px * laneOffset));
            mist.setCenterY(startY + (uy * distance * progress) + (py * laneOffset));
            prepareTransientNode(mist);
            
            battleField.getChildren().add(mist);
            
            int delay = i * 40;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(mist.opacityProperty(), 0.6));
            KeyFrame expand = new KeyFrame(Duration.millis(delay + 270),
                new KeyValue(mist.radiusProperty(), mist.getRadius() * 2),
                new KeyValue(mist.centerXProperty(), mist.getCenterX() + (ux * travel)),
                new KeyValue(mist.centerYProperty(), mist.getCenterY() + (uy * travel)),
                new KeyValue(mist.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, expand);
            
            registerCleanup(timeline, mist);
        }
        
        // Light snowflakes
        addSnowflakes(startX, startY, endX, endY, 32, false, timeline);
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

        int cubeCount = Math.min(18 + movePower / 18, 24);
        
        for (int i = 0; i < cubeCount; i++) {
            Rectangle cube = new Rectangle(16 + random.nextDouble() * 12, 16 + random.nextDouble() * 12);
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
            
            int delay = i * 55;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(cube.opacityProperty(), 1.0));
            KeyFrame fall = new KeyFrame(Duration.millis(delay + 400),
                new KeyValue(cube.yProperty(), cube.getY() + 140),
                new KeyValue(cube.rotateProperty(), cube.getRotate() + 180));
            KeyFrame shatter = new KeyFrame(Duration.millis(delay + 470),
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
        int shardCount = Math.min(16 + movePower / 15, 22);
        
        for (int i = 0; i < shardCount; i++) {
            // Multi-faceted ice shard with >15 polygon sides
            Polygon shard = new Polygon();
            int sides = 16;
            double shardLen = 18 + random.nextDouble() * 10;
            for (int s = 0; s < sides; s++) {
                double angle = (s / (double) sides) * 2 * Math.PI - Math.PI / 2;
                double rx = shardLen * 0.3 * (0.7 + 0.3 * Math.abs(Math.cos(angle * 2)));
                double ry = shardLen * (0.6 + 0.4 * Math.abs(Math.cos(angle)));
                shard.getPoints().addAll(Math.cos(angle) * rx, Math.sin(angle) * ry);
            }
            shard.setFill(Color.CYAN);
            shard.setStroke(Color.LIGHTBLUE);
            shard.setStrokeWidth(1.5);
            shard.setEffect(new DropShadow(12, Color.DEEPSKYBLUE));

            double progress = (i + random.nextDouble()) / shardCount;
            double lateral = (random.nextDouble() - 0.5) * Math.max(120.0, safeBattleHeight() * 0.5);

            shard.setLayoutX(startX + (ux * distance * progress) + (px * lateral));
            shard.setLayoutY(startY + (uy * distance * progress) + (py * lateral));
            shard.setOpacity(0);
            shard.setRotate(Math.toDegrees(Math.atan2(uy, ux)) + random.nextDouble() * 40);
            prepareTransientNode(shard);
            
            battleField.getChildren().add(shard);
            
            int delay = i * 47;
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(shard.opacityProperty(), 1.0));
            KeyFrame fly = new KeyFrame(Duration.millis(delay + 340),
                new KeyValue(shard.translateXProperty(), (ux * 90) + (px * (random.nextDouble() - 0.5) * 28)),
                new KeyValue(shard.translateYProperty(), (uy * 90) + (py * (random.nextDouble() - 0.5) * 28)),
                new KeyValue(shard.opacityProperty(), 0));
            
            timeline.getKeyFrames().addAll(appear, fly);
            
            registerCleanup(timeline, shard);
        }
        
        // Snowflakes
        int snowCount = Math.min(18 + movePower / 15, 28);
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

        int actualCount = Math.max(count, 16);
        for (int i = 0; i < actualCount; i++) {
            Circle snowflake = new Circle(9 + random.nextDouble() * 5, Color.WHITE);
            snowflake.setEffect(new DropShadow(8, Color.LIGHTBLUE));

            double progress = (i + random.nextDouble()) / actualCount;
            double lateral = (random.nextDouble() - 0.5) * Math.max(120.0, safeBattleHeight() * 0.7);

            snowflake.setCenterX(startX + (ux * distance * progress) + (px * lateral));
            snowflake.setCenterY(startY + (uy * distance * progress) + (py * lateral));
            snowflake.setOpacity(0);
            prepareTransientNode(snowflake);
            
            battleField.getChildren().add(snowflake);
            
            int delay = i * 34;
            double travel = windBlown ? Math.min(distance * 0.55, 240.0) : Math.min(distance * 0.35, 140.0);
            double lateralDrift = windBlown ? (random.nextDouble() - 0.5) * 60.0 : (random.nextDouble() - 0.5) * 34.0;
            double snowEndX = snowflake.getCenterX() + (ux * travel) + (px * lateralDrift);
            double snowEndY = snowflake.getCenterY() + (uy * travel) + (py * lateralDrift) + (windBlown ? 0.0 : 34.0);
            
            KeyFrame appear = new KeyFrame(Duration.millis(delay),
                new KeyValue(snowflake.opacityProperty(), 1.0));
            KeyFrame driftFrame = new KeyFrame(Duration.millis(delay + 470),
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

    private void addPunchImage(double x, double y, Timeline timeline) {
        addStaticImpactImage(PUNCH_ASSET, x, y, 160, 160, timeline);
    }

    private void addFangImage(double x, double y, Timeline timeline) {
        addStaticImpactImage(FANG_ASSET, x, y, 190, 190, timeline);
    }

    private void addStaticImpactImage(String assetName, double x, double y,
                                      double width, double height,
                                      Timeline timeline) {
        try {
            Image image = MediaCache.getImage(assetName);
            if (image == null) {
                return;
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(true);
            imageView.setLayoutX(x - width / 2.0);
            imageView.setLayoutY(y - height / 2.0);
            imageView.setOpacity(0);
            imageView.setScaleX(0.55);
            imageView.setScaleY(0.55);
            prepareTransientNode(imageView);
            battleField.getChildren().add(imageView);

            KeyFrame appear = new KeyFrame(Duration.millis(35),
                new KeyValue(imageView.opacityProperty(), 1.0),
                new KeyValue(imageView.scaleXProperty(), 1.25),
                new KeyValue(imageView.scaleYProperty(), 1.25));
            KeyFrame settle = new KeyFrame(Duration.millis(115),
                new KeyValue(imageView.scaleXProperty(), 1.0),
                new KeyValue(imageView.scaleYProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(330),
                new KeyValue(imageView.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(appear, settle, fade);
            registerCleanup(timeline, imageView);
        } catch (Exception ignored) {
            // Overlay is optional; the core move effect should still play.
        }
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