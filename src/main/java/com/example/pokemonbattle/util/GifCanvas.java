package com.example.pokemonbattle.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.util.Duration;
public class GifCanvas extends Canvas {

    private final Timeline timeline;
    public GifCanvas(MediaCache.GifFrameData data, double width, double height) {
        super(width, height);

        GraphicsContext gc = getGraphicsContext2D();

        if (data == null || data.isEmpty()) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, width, height);
            timeline = null;
            return;
        }
        WritableImage[] frames  = data.frames();
        long[] delays  = data.delaysMs();

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        double t = 0;
        for (int i = 0; i < frames.length; i++) {
            final WritableImage frame = frames[i];
            timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(t),
                    e -> gc.drawImage(frame, 0, 0, getWidth(), getHeight()))
            );
            t += delays[i];
        }
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(t)));
    }

    public void play()  { if (timeline != null) timeline.play();  }
    public void stop()  { if (timeline != null) timeline.stop();  }
    public void pause() { if (timeline != null) timeline.pause(); }
}
