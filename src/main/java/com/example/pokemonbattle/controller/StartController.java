package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * Controller for the Start Screen.
 * Background is start.mp4 (looping video). Start button transitions to the Loading screen.
 */
@SuppressWarnings("unused")
public class StartController {

    @FXML private StackPane rootPane;
    @FXML private MediaView bgVideo;

    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        // Start invisible — will fade in from the black of the intro transition
        if (rootPane != null) rootPane.setOpacity(0.0);

        java.net.URL videoUrl = getClass().getResource(
                "/com/example/pokemonbattle/assets/start.mp4");

        if (videoUrl != null && bgVideo != null && rootPane != null) {
            Media media = new Media(videoUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop forever
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setMute(true);
            bgVideo.setMediaPlayer(mediaPlayer);

            // Bind video to full width; subtract 8px (4px top + 4px bottom ≈ 1mm each)
            // leaving a thin black bar via the StackPane's black background.
            bgVideo.fitWidthProperty().bind(rootPane.widthProperty());
            bgVideo.fitHeightProperty().bind(rootPane.heightProperty().subtract(8));
            bgVideo.setPreserveRatio(false);
        }

        // Fade the scene in from black (matches the fade-to-black from IntroController)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /** Start button click handler — stops video and transitions to the loading screen. */
    @FXML
    protected void onStartButtonClick() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        SceneManager.switchScene("loading_screen.fxml", "Pokemon Battle - Loading", 1200, 700);
    }
}
