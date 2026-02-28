package com.example.pokemonbattle.util;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public final class CurtainTransitionManager {

    private static final Duration FALL_DURATION = Duration.millis(300);
    private static final Duration RISE_DURATION = Duration.millis(300);
    private static final Interpolator FALL_INTERPOLATOR = Interpolator.EASE_IN;
    private static final Interpolator RISE_INTERPOLATOR = Interpolator.EASE_OUT;

    private CurtainTransitionManager() {}
    public static void riseOn(Pane rootPane) {
        riseOn(rootPane, RISE_DURATION, RISE_INTERPOLATOR);
    }

    public static void riseOn(Pane rootPane, Duration riseDuration, Interpolator riseInterpolator) {
        double h = rootPane.getHeight();
        Rectangle curtain = new Rectangle();
        curtain.setFill(Color.BLACK);
        curtain.widthProperty().bind(rootPane.widthProperty());
        curtain.heightProperty().bind(rootPane.heightProperty());
        curtain.setTranslateY(0);   // starts fully covering the pane
        curtain.setManaged(false);
        rootPane.getChildren().add(curtain);

        TranslateTransition rise = new TranslateTransition(riseDuration, curtain);
        rise.setFromY(0);
        rise.setToY(h);
        rise.setInterpolator(riseInterpolator);
        rise.setOnFinished(re -> {
            rootPane.getChildren().remove(curtain);
            curtain.widthProperty().unbind();
            curtain.heightProperty().unbind();
        });
        rise.play();
    }

    public static void executeCurtainTransition(Pane rootPane, Runnable sceneSwitchAction) {
        executeCurtainTransition(rootPane, sceneSwitchAction,
                FALL_DURATION, RISE_DURATION,
                FALL_INTERPOLATOR, RISE_INTERPOLATOR);
    }
    public static void executeCurtainTransition(Pane rootPane, Runnable sceneSwitchAction, Duration fallDuration, Duration riseDuration) {
        executeCurtainTransition(rootPane, sceneSwitchAction, fallDuration, riseDuration, FALL_INTERPOLATOR, RISE_INTERPOLATOR);
    }
    public static void executeCurtainTransition(Pane rootPane, Runnable sceneSwitchAction, Duration fallDuration, Duration riseDuration, Interpolator fallInterpolator, Interpolator riseInterpolator) {
        double h = rootPane.getHeight();
        Rectangle curtain = new Rectangle();
        curtain.setFill(Color.BLACK);
        curtain.widthProperty().bind(rootPane.widthProperty());
        curtain.heightProperty().bind(rootPane.heightProperty());
        curtain.setTranslateY(-h); 
        rootPane.getChildren().add(curtain);
        TranslateTransition fall = new TranslateTransition(fallDuration, curtain);
        fall.setFromY(-h);
        fall.setToY(0);
        fall.setInterpolator(fallInterpolator);
        fall.setOnFinished(e -> {
            if (sceneSwitchAction != null) {
                sceneSwitchAction.run();
            }
            TranslateTransition rise = new TranslateTransition(riseDuration, curtain);
            rise.setFromY(0);
            rise.setToY(rootPane.getHeight());
            rise.setInterpolator(riseInterpolator);
            rise.setOnFinished(re -> {
                rootPane.getChildren().remove(curtain);
                curtain.widthProperty().unbind();
                curtain.heightProperty().unbind();
            });

            rise.play();
        });

        fall.play();
    }
}
