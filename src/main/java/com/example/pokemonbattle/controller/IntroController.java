package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class IntroController {
    @FXML private StackPane rootPane;
    @FXML private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private static final double TRIGGER_BEFORE_END_MS = 1000.0;
    private volatile boolean transitionTriggered = false;

    @FXML
    public void initialize() {
        mediaPlayer = com.example.pokemonbattle.util.MediaCache.claimMediaPlayer("intro.mp4");
        if (mediaPlayer == null) {
            System.err.println("IntroController: intro.mp4 player unavailable, skipping intro.");
            goToStartScreen();
            return;
        }

        mediaView.setMediaPlayer(mediaPlayer);
        mediaView.fitWidthProperty().bind(rootPane.widthProperty());
        mediaView.fitHeightProperty().bind(rootPane.heightProperty());

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (transitionTriggered) return;
            Duration total = mediaPlayer.getTotalDuration();
            if (total == null || total.isUnknown() || total.isIndefinite()) return;
            double remainingMs = total.subtract(newTime).toMillis();
            if (remainingMs <= TRIGGER_BEFORE_END_MS) {
                transitionTriggered = true;
                startFlashTransition();
            }
        });
        mediaPlayer.setOnEndOfMedia(this::handleEndOfMedia);
        mediaPlayer.setOnError(() -> {
            Throwable error = mediaPlayer.getError();
            System.err.println("IntroController: MediaPlayer error — " + (error != null ? error.getMessage() : "Unknown error"));
            goToStartScreen();
        });
        mediaPlayer.setOnReady(mediaPlayer::play);
    }

    private void handleEndOfMedia() {
        if (!transitionTriggered) {
            transitionTriggered = true;
            startFlashTransition();
        }
    }

    private void startFlashTransition() {
        // 1. Black overlay behind the pokeball
        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.BLACK);
        overlay.widthProperty().bind(rootPane.widthProperty());
        overlay.heightProperty().bind(rootPane.heightProperty());
        overlay.setOpacity(0);
        overlay.setManaged(false);

        // 2. Pokeball on top
        PokeballOverlay pokeball = new PokeballOverlay();
        pokeball.setOpacity(0);
        // do NOT setManaged(false) — layout must be active for centering to work

        rootPane.getChildren().addAll(overlay, pokeball);
        pokeball.play();

        // 3. Fade in black + pokeball together
        FadeTransition fadeBg = new FadeTransition(Duration.millis(700), overlay);
        fadeBg.setFromValue(0.0);
        fadeBg.setToValue(1.0);

        FadeTransition fadeBall = new FadeTransition(Duration.millis(400), pokeball);
        fadeBall.setFromValue(0.0);
        fadeBall.setToValue(1.0);

        // 4. Volume fade
        if (mediaPlayer != null) {
            new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(mediaPlayer.volumeProperty(), mediaPlayer.getVolume())),
                new KeyFrame(Duration.millis(700),  new KeyValue(mediaPlayer.volumeProperty(), 0.0))
            ).play();
        }

        fadeBg.setOnFinished(e -> {
            disposeMediaPlayer();
            // Keep pokeball spinning while start scene loads and reveals
            // Pass pokeball reference to StartController via SceneManager data
            SceneManager.setData("pokeballOverlay", pokeball);
            goToStartScreen();
        });

        fadeBg.play();
        fadeBall.play();
    }

    private void disposeMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception e) {
                System.err.println("IntroController: error disposing MediaPlayer — " + e.getMessage());
            } finally {
                mediaPlayer = null;
            }
        }
    }

    private void goToStartScreen() {
        SceneManager.switchScene("start.fxml", "Pokemon Battle", 1200, 700);
    }
}