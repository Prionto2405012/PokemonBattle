package com.example.pokemonbattle.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public final class CurtainTransitionManager {
    private static final double CURTAIN_MIN_OPACITY = 0.15;
    private static final double CURTAIN_MAX_OPACITY = 1.0;

    private static final Duration FALL_DURATION = Duration.millis(300);
    private static final Duration RISE_DURATION = Duration.millis(300);
    private static final Interpolator FALL_INTERPOLATOR = Interpolator.EASE_IN;
    private static final Interpolator RISE_INTERPOLATOR = Interpolator.EASE_OUT;

    private CurtainTransitionManager() {}
    public static void riseOn(Pane rootPane) {
        riseOn(rootPane, RISE_DURATION, RISE_INTERPOLATOR);
    }

    public static void riseOn(Pane rootPane, Duration riseDuration, Interpolator riseInterpolator) {
        double h = rootPane.getHeight() > 0 ? rootPane.getHeight() : rootPane.getLayoutBounds().getHeight();

        Rectangle curtain = new Rectangle();
        curtain.setFill(Color.BLACK);
        curtain.widthProperty().bind(rootPane.widthProperty());
        curtain.heightProperty().bind(rootPane.heightProperty());
        curtain.setManaged(false);
        curtain.setTranslateY(0);
        curtain.setOpacity(CURTAIN_MAX_OPACITY);
        rootPane.getChildren().add(curtain);

        TranslateTransition rise = new TranslateTransition(riseDuration, curtain);
        rise.setFromY(0);
        rise.setToY(h);
        rise.setInterpolator(riseInterpolator);

        FadeTransition fade = new FadeTransition(riseDuration, curtain);
        fade.setFromValue(CURTAIN_MAX_OPACITY);
        fade.setToValue(CURTAIN_MIN_OPACITY);

        ParallelTransition parallelRise = new ParallelTransition(rise, fade);
        parallelRise.setOnFinished(e -> {
            rootPane.getChildren().remove(curtain);
            curtain.widthProperty().unbind();
            curtain.heightProperty().unbind();
        });
        parallelRise.play();
    }

    public static void executeCurtainTransition(Pane rootPane, Runnable sceneSwitchAction) {
        executeCurtainTransition(
                rootPane, sceneSwitchAction,
                FALL_DURATION, RISE_DURATION,
                FALL_INTERPOLATOR, RISE_INTERPOLATOR
        );
    }

    public static void executeCurtainTransition(
            Pane rootPane,
            Runnable sceneSwitchAction,
            Duration fallDuration,
            Duration riseDuration
    ) {
        executeCurtainTransition(
                rootPane, sceneSwitchAction,
                fallDuration, riseDuration,
                FALL_INTERPOLATOR, RISE_INTERPOLATOR
        );
    }

    public static void executeCurtainTransition(
            Pane rootPane,
            Runnable sceneSwitchAction,
            Duration fallDuration,
            Duration riseDuration,
            Interpolator fallInterpolator,
            Interpolator riseInterpolator
    ) {
        double h = rootPane.getHeight() > 0 ? rootPane.getHeight() : rootPane.getLayoutBounds().getHeight();

        Rectangle curtain = new Rectangle();
        curtain.setFill(Color.BLACK);
        curtain.widthProperty().bind(rootPane.widthProperty());
        curtain.heightProperty().bind(rootPane.heightProperty());
        curtain.setManaged(false);
        curtain.setTranslateY(-h);
        curtain.setOpacity(CURTAIN_MIN_OPACITY);
        rootPane.getChildren().add(curtain);

        TranslateTransition fall = new TranslateTransition(fallDuration, curtain);
        fall.setFromY(-h);
        fall.setToY(0);
        fall.setInterpolator(fallInterpolator);

        FadeTransition darken = new FadeTransition(fallDuration, curtain);
        darken.setFromValue(CURTAIN_MIN_OPACITY);
        darken.setToValue(CURTAIN_MAX_OPACITY);

        ParallelTransition cover = new ParallelTransition(fall, darken);
        cover.setOnFinished(e -> {
            if (sceneSwitchAction != null) {
                sceneSwitchAction.run();
            }

            TranslateTransition rise = new TranslateTransition(riseDuration, curtain);
            rise.setFromY(0);
            rise.setToY(rootPane.getHeight());
            rise.setInterpolator(riseInterpolator);

            FadeTransition lighten = new FadeTransition(riseDuration, curtain);
            lighten.setFromValue(CURTAIN_MAX_OPACITY);
            lighten.setToValue(CURTAIN_MIN_OPACITY);

            ParallelTransition uncover = new ParallelTransition(rise, lighten);
            uncover.setOnFinished(re -> {
                rootPane.getChildren().remove(curtain);
                curtain.widthProperty().unbind();
                curtain.heightProperty().unbind();
            });
            uncover.play();
        });
        cover.play();
    }

    public static void fallOn(Pane rootPane, Runnable onCovered) {
        fallOn(rootPane, onCovered, FALL_DURATION, FALL_INTERPOLATOR);
    }

    public static void fallOn(
            Pane rootPane,
            Runnable onCovered,
            Duration fallDuration,
            Interpolator fallInterpolator
    ) {
        double h = rootPane.getHeight() > 0 ? rootPane.getHeight() : rootPane.getLayoutBounds().getHeight();

        Rectangle curtain = new Rectangle();
        curtain.setFill(Color.BLACK);
        curtain.widthProperty().bind(rootPane.widthProperty());
        curtain.heightProperty().bind(rootPane.heightProperty());
        curtain.setManaged(false);
        curtain.setTranslateY(-h);
        curtain.setOpacity(CURTAIN_MIN_OPACITY);
        rootPane.getChildren().add(curtain);

        TranslateTransition fall = new TranslateTransition(fallDuration, curtain);
        fall.setFromY(-h);
        fall.setToY(0);
        fall.setInterpolator(fallInterpolator);

        FadeTransition darken = new FadeTransition(fallDuration, curtain);
        darken.setFromValue(CURTAIN_MIN_OPACITY);
        darken.setToValue(CURTAIN_MAX_OPACITY);

        ParallelTransition cover = new ParallelTransition(fall, darken);
        cover.setOnFinished(e -> {
            if (onCovered != null) {
                onCovered.run();
            }
            curtain.widthProperty().unbind();
            curtain.heightProperty().unbind();
            // Keep the fully-covered curtain on screen.
            // Next scene should call riseOn on its own root.
        });
        cover.play();
    }
}
