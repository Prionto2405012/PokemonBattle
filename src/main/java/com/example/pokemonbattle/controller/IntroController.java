package com.example.pokemonbattle.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
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
    private Rectangle startupMask;
    private static final double TRIGGER_BEFORE_END_MS = 1000.0;
    private static final Duration INTRO_RETRY_DELAY = Duration.millis(900);
    private volatile boolean transitionTriggered = false;
    private final AtomicBoolean startupRevealTriggered = new AtomicBoolean(false);
    private PauseTransition retryTimer;

    @FXML
    public void initialize() {
        startupMask = new Rectangle();
        startupMask.setFill(Color.BLACK);
        startupMask.widthProperty().bind(rootPane.widthProperty());
        startupMask.heightProperty().bind(rootPane.heightProperty());
        startupMask.setManaged(false);
        startupMask.setOpacity(1.0);
        rootPane.getChildren().add(startupMask);

        attemptIntroPlayback();
    }

    private void attemptIntroPlayback() {
        if (mediaPlayer != null) {
            return;
        }
        MediaPlayer candidate = com.example.pokemonbattle.util.MediaCache.claimMediaPlayer("intro.mp4");
        if (candidate == null) {
            com.example.pokemonbattle.util.MediaCache.buildVideoPlayer("intro.mp4", this::attachAndStart);
            scheduleIntroRetry("IntroController: intro.mp4 player unavailable, retrying live build.");
            return;
        }
        attachAndStart(candidate);
    }

    private void attachAndStart(MediaPlayer player) {
        if (retryTimer != null) {
            retryTimer.stop();
            retryTimer = null;
        }

        mediaPlayer = player;
        final MediaPlayer activePlayer = player;

        mediaView.setMediaPlayer(activePlayer);
        mediaView.fitWidthProperty().bind(rootPane.widthProperty());
        mediaView.fitHeightProperty().bind(rootPane.heightProperty());

        activePlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (transitionTriggered || mediaPlayer != activePlayer) return;
            Duration total = activePlayer.getTotalDuration();
            if (total == null || total.isUnknown() || total.isIndefinite()) return;
            double remainingMs = total.subtract(newTime).toMillis();
            if (remainingMs <= TRIGGER_BEFORE_END_MS) {
                transitionTriggered = true;
                startFlashTransition();
            }
        });

        activePlayer.setOnEndOfMedia(() -> {
            if (mediaPlayer == activePlayer) {
                handleEndOfMedia();
            }
        });

        activePlayer.setOnError(() -> {
            if (mediaPlayer != activePlayer) {
                return;
            }
            Throwable error = activePlayer.getError();
            System.err.println("IntroController: MediaPlayer error — "
                    + (error != null ? error.getMessage() : "Unknown error")
                    + ". Retrying intro render.");
            releaseActivePlayer();
            scheduleIntroRetry(null);
        });

        activePlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (newTime == null || !newTime.greaterThan(Duration.ZERO)) {
                return;
            }
            if (!startupRevealTriggered.compareAndSet(false, true)) {
                return;
            }
            Rectangle mask = startupMask;
            if (mask == null) {
                return;
            }

            FadeTransition reveal = new FadeTransition(Duration.millis(250), mask);
            reveal.setFromValue(mask.getOpacity());
            reveal.setToValue(0.0);
            reveal.setOnFinished(e -> {
                mask.widthProperty().unbind();
                mask.heightProperty().unbind();
                rootPane.getChildren().remove(mask);
                if (startupMask == mask) {
                    startupMask = null;
                }
            });
            reveal.play();
        });

        activePlayer.setOnReady(() -> {
            if (mediaPlayer != activePlayer) {
                return;
            }
            activePlayer.seek(Duration.ZERO);
            activePlayer.play();
        });
        if (activePlayer.getStatus() == MediaPlayer.Status.READY) {
            activePlayer.seek(Duration.ZERO);
            activePlayer.play();
        }
    }

    private void scheduleIntroRetry(String reason) {
        if (transitionTriggered) {
            return;
        }
        if (reason != null && !reason.isBlank()) {
            System.err.println(reason);
        }
        if (retryTimer != null) {
            retryTimer.stop();
        }
        retryTimer = new PauseTransition(INTRO_RETRY_DELAY);
        retryTimer.setOnFinished(e -> {
            if (!transitionTriggered) {
                attemptIntroPlayback();
            }
        });
        retryTimer.play();
    }

    private void releaseActivePlayer() {
        MediaPlayer active = mediaPlayer;
        mediaPlayer = null;
        if (active == null) {
            mediaView.setMediaPlayer(null);
            return;
        }
        try {
            active.stop();
            active.dispose();
        } catch (Exception e) {
            System.err.println("IntroController: error disposing MediaPlayer — " + e.getMessage());
        }
        mediaView.setMediaPlayer(null);
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
            // Pass pokeball reference in switch payload so it survives sceneData.clear().
            Map<String, Object> transitionData = new HashMap<>();
            transitionData.put("pokeballOverlay", pokeball);
            SceneManager.switchSceneWithData("start.fxml", "Pokemon Battle", 1200, 700, transitionData);
        });

        fadeBg.play();
        fadeBall.play();
        MusicManager.getInstance().playRandomBGM();
    }

    private void disposeMediaPlayer() {
        if (retryTimer != null) {
            retryTimer.stop();
            retryTimer = null;
        }
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
}