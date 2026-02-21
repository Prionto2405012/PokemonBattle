package com.example.pokemonbattle.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class PokeballOverlay extends StackPane {

    private final RotateTransition spin;
    private final Timeline glow;

    // ---------------------------------------------------------------
    // Static helpers — use these from controllers
    // ---------------------------------------------------------------

    /**
     * Creates a PokeballOverlay, adds it to the given rootPane, and starts
     * animating. Returns the instance so you can pass it to hideFrom() later.
     */
    public static PokeballOverlay showOn(StackPane rootPane) {
        PokeballOverlay pb = new PokeballOverlay();
        pb.setOpacity(0);
        rootPane.getChildren().add(pb);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), pb);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        pb.play();
        return pb;
    }

    /**
     * Fades the pokeball out and removes it from the rootPane, then calls onDone.
     * Safe to call even if pb is null.
     */
    public static void hideFrom(StackPane rootPane, PokeballOverlay pb, Runnable onDone) {
        if (pb == null) {
            if (onDone != null)
                onDone.run();
            return;
        }
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), pb);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            pb.stop();
            rootPane.getChildren().remove(pb);
            if (onDone != null)
                onDone.run();
        });
        fadeOut.play();
    }

    // ---------------------------------------------------------------
    // Instance
    // ---------------------------------------------------------------

    public PokeballOverlay() {
        double size = 120;
        double r = size / 2;

        Arc top = new Arc(r, r, r, r, 0, 180);
        top.setType(ArcType.CHORD);
        top.setFill(Color.web("#f93318"));
        top.setStroke(Color.TRANSPARENT);

        Arc bottom = new Arc(r, r, r, r, 180, 180);
        bottom.setType(ArcType.CHORD);
        bottom.setFill(Color.WHITE);
        bottom.setStroke(Color.TRANSPARENT);

        Circle outerRing = new Circle(r, r, r);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.BLACK);
        outerRing.setStrokeWidth(6);

        Line divider = new Line(0, r, size, r);
        divider.setStroke(Color.BLACK);
        divider.setStrokeWidth(6);

        Circle btnOuter = new Circle(r, r, 18);
        btnOuter.setFill(Color.BLACK);
        Circle btnBorder = new Circle(r, r, 14);
        btnBorder.setFill(Color.WHITE);
        Circle btnInner = new Circle(r, r, 10);
        btnInner.setFill(Color.web("#7f8c8d"));

        Pane ball = new Pane();
        ball.setPrefSize(size, size);
        ball.setMaxSize(size, size);
        ball.getChildren().addAll(top, bottom, outerRing, divider, btnOuter, btnBorder, btnInner);

        getChildren().add(ball);
        StackPane.setAlignment(ball, Pos.CENTER);
        StackPane.setAlignment(this, Pos.CENTER);
        setAlignment(Pos.CENTER);
        setPickOnBounds(false);
        setMouseTransparent(true);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        spin = new RotateTransition(Duration.seconds(3), ball);
        spin.setByAngle(360);
        spin.setCycleCount(RotateTransition.INDEFINITE);
        spin.setInterpolator(Interpolator.EASE_BOTH);

        glow = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(btnInner.fillProperty(), Color.web("#7f8c8d"))),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(btnInner.fillProperty(), Color.RED)),
                new KeyFrame(Duration.seconds(3), new KeyValue(btnInner.fillProperty(), Color.web("#7f8c8d"))));
        glow.setCycleCount(Timeline.INDEFINITE);
    }

    public void play() {
        spin.play();
        glow.play();
    }

    public void stop() {
        spin.stop();
        glow.stop();
    }
}