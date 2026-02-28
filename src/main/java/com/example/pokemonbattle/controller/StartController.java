package com.example.pokemonbattle.controller;

import java.util.concurrent.atomic.AtomicBoolean;

import com.example.pokemonbattle.util.MediaCache;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class StartController {

    @FXML private StackPane rootPane;
    @FXML private MediaView bgVideo;
    @FXML private Region vignetteOverlay;
    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        if (vignetteOverlay != null && rootPane != null) {
            vignetteOverlay.prefWidthProperty().bind(rootPane.widthProperty());
            vignetteOverlay.prefHeightProperty().bind(rootPane.heightProperty());
        }

        Rectangle blackOverlay = new Rectangle();
        blackOverlay.setFill(Color.BLACK);
        blackOverlay.widthProperty().bind(rootPane.widthProperty());
        blackOverlay.heightProperty().bind(rootPane.heightProperty());
        blackOverlay.setOpacity(1.0);
        blackOverlay.setManaged(false);
        rootPane.getChildren().add(blackOverlay);

        PokeballOverlay pokeball = (PokeballOverlay) SceneManager.getData("pokeballOverlay");
        if (pokeball != null) {
            SceneManager.setData("pokeballOverlay", null);
            rootPane.getChildren().add(pokeball);
        }

        FadeTransition fadeOutBg = new FadeTransition(Duration.millis(500), blackOverlay);
        fadeOutBg.setFromValue(1.0);
        fadeOutBg.setToValue(0.0);
        fadeOutBg.setOnFinished(e -> {
            rootPane.getChildren().remove(blackOverlay);
            blackOverlay.widthProperty().unbind();
            blackOverlay.heightProperty().unbind();
        });
        FadeTransition fadeOutBall = pokeball != null
            ? new FadeTransition(Duration.millis(400), pokeball) : null;
        if (fadeOutBall != null) {
            final PokeballOverlay pb = pokeball;
            fadeOutBall.setFromValue(1.0);
            fadeOutBall.setToValue(0.0);
            fadeOutBall.setOnFinished(e -> {
                pb.stop();
                rootPane.getChildren().remove(pb);
            });
        }
        javafx.animation.PauseTransition timeout =
            new javafx.animation.PauseTransition(Duration.seconds(1.5));
        timeout.setOnFinished(e -> {
            if (blackOverlay.getOpacity() > 0) {
                fadeOutBg.play();
                if (fadeOutBall != null) fadeOutBall.play();
            }
        });
        MediaCache.buildVideoPlayer("start.mp4", player -> {
            mediaPlayer = player;     
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgVideo.setMediaPlayer(mediaPlayer);
            bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
            bgVideo.fitHeightProperty().bind(rootPane.heightProperty());
            bgVideo.setPreserveRatio(false);

            final FadeTransition ballFade = fadeOutBall;
            javafx.beans.value.ChangeListener<Duration> firstFrameListener =
                new javafx.beans.value.ChangeListener<>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Duration> obs,
                                       Duration old, Duration now) {
                        if (now != null && now.greaterThan(Duration.ZERO)) {
                            mediaPlayer.currentTimeProperty().removeListener(this);
                            timeout.stop();
                            fadeOutBg.play();
                            if (ballFade != null) ballFade.play();
                        }
                    }
                };
            mediaPlayer.currentTimeProperty().addListener(firstFrameListener);
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
            timeout.play();
        });
        timeout.play();
        MusicManager.getInstance().attachClickSounds(rootPane);
        Button startBtn = (Button) rootPane.lookup(".start-btn");
        if (startBtn != null) {
            startBtn.setRotate(-2);
            startBtn.setOnMouseEntered(e -> {
                RotateTransition rt = new RotateTransition(Duration.millis(150), startBtn);
                rt.setToAngle(-1);
                rt.play();
                startBtn.setScaleX(1.05);
                startBtn.setScaleY(1.05);
            });
            startBtn.setOnMouseExited(e -> {
                RotateTransition rt = new RotateTransition(Duration.millis(150), startBtn);
                rt.setToAngle(-2);
                rt.play();
                startBtn.setScaleX(1.0);
                startBtn.setScaleY(1.0);
            });
            startBtn.setOnMousePressed(e -> {
                RotateTransition rt = new RotateTransition(Duration.millis(80), startBtn);
                rt.setToAngle(0);
                rt.play();
                startBtn.setScaleX(0.98);
                startBtn.setScaleY(0.98);
            });
            startBtn.setOnMouseReleased(e -> {
                RotateTransition rt = new RotateTransition(Duration.millis(150), startBtn);
                rt.setToAngle(-1);
                rt.play();
                startBtn.setScaleX(1.05);
                startBtn.setScaleY(1.05);
            });
        }
    }

    @FXML
    protected void onStartButtonClick() {
        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.BLACK);
        overlay.widthProperty().bind(rootPane.widthProperty());
        overlay.heightProperty().bind(rootPane.heightProperty());
        overlay.setManaged(false);
        overlay.setOpacity(0);
        rootPane.getChildren().add(overlay);
        PokeballOverlay pokeball = PokeballOverlay.showOn(rootPane);
        AtomicBoolean switched = new AtomicBoolean(false);
        FadeTransition phase1 = new FadeTransition(Duration.millis(60), overlay);
        phase1.setFromValue(0.0);
        phase1.setToValue(0.7);
        phase1.setInterpolator(Interpolator.EASE_IN);
        FadeTransition phase2 = new FadeTransition(Duration.millis(40), overlay);
        phase2.setFromValue(0.7);
        phase2.setToValue(0.9);
        phase2.setInterpolator(Interpolator.EASE_OUT);
        phase2.setOnFinished(e -> {
            if (!switched.compareAndSet(false, true)) return;
            overlay.widthProperty().unbind();
            overlay.heightProperty().unbind();
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            SceneManager.setData("pokeballOverlay", pokeball);
            SceneManager.switchScene("wc.fxml", "Pokemon Battle", 1200, 700);
        });

        new SequentialTransition(phase1, phase2).play();
    }
}