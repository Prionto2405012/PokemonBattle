package com.example.pokemonbattle.util.effects;

import com.example.pokemonbattle.util.GifCanvas;
import com.example.pokemonbattle.util.MediaCache;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class ContactOverlayEffects {

    private static final String PUNCH_ASSET = "punch.png";
    private static final String FEET_ASSET = "feet.png";
    private static final String FANG_ASSET = "fang.gif";

    private final Pane battleField;

    public ContactOverlayEffects(Pane battleField) {
        this.battleField = battleField;
    }

    public void addPunchImage(double x, double y, Timeline timeline) {
        addStaticImpactImage(PUNCH_ASSET, x, y, 160, 160, timeline);
    }

    public void addFeetImage(double x, double y, Timeline timeline) {
        addStaticImpactImage(FEET_ASSET, x, y, 170, 170, timeline);
    }

    public void addFangAnimation(double x, double y, Timeline timeline) {
        try {
            MediaCache.GifFrameData data = MediaCache.getGifFrames(FANG_ASSET);
            if (data == null || data.isEmpty()) {
                return;
            }

            GifCanvas canvas = new GifCanvas(data, 190, 190);
            canvas.setLayoutX(x - 95);
            canvas.setLayoutY(y - 108);
            canvas.setOpacity(0);
            canvas.setScaleX(0.72);
            canvas.setScaleY(0.72);
            prepareTransientNode(canvas);
            battleField.getChildren().add(canvas);
            canvas.play();

            KeyFrame appear = new KeyFrame(Duration.millis(45),
                new KeyValue(canvas.opacityProperty(), 1.0),
                new KeyValue(canvas.scaleXProperty(), 1.08),
                new KeyValue(canvas.scaleYProperty(), 1.08));
            KeyFrame settle = new KeyFrame(Duration.millis(150),
                new KeyValue(canvas.scaleXProperty(), 1.0),
                new KeyValue(canvas.scaleYProperty(), 1.0));
            KeyFrame fade = new KeyFrame(Duration.millis(360),
                new KeyValue(canvas.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(appear, settle, fade);
            registerCleanup(timeline, canvas, canvas::stop);
        } catch (Exception ignored) {
            // Overlay is optional; the main type effect should still play.
        }
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
            registerCleanup(timeline, imageView, null);
        } catch (Exception ignored) {
            // Overlay is optional; the main type effect should still play.
        }
    }

    private void prepareTransientNode(Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private void registerCleanup(Timeline timeline, Node node, Runnable extraCleanup) {
        EventHandler<ActionEvent> previousOnFinished = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (extraCleanup != null) {
                extraCleanup.run();
            }
            if (previousOnFinished != null) {
                previousOnFinished.handle(e);
            }
        });
    }
}