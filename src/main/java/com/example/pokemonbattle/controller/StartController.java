package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

// import javafx.util.Duration;

/**
 * Controller for the Start/Splash Screen.
 * Shows initial loading or splash content, then transitions to Welcome screen.
 */
@SuppressWarnings("unused") // FXML fields and methods
public class StartController {

    /**
     * Called automatically by JavaFX after FXML is loaded.
     */
    @FXML
    private StackPane rootPane; // Root container for potential background image
    @FXML
    private ImageView bgImage; // ImageView for background image (optional)
    @FXML
    public void initialize() {
        // Auto-transition to welcome screen after 3 seconds (optional)
        // PauseTransition delay = new PauseTransition(Duration.seconds(3));
        // delay.setOnFinished(event -> goToWelcomeScreen());
        // delay.play();
        
        // Bind background image to fill the container (with null check)
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
    }

    /**
     * Manual button click to skip splash screen.
     */
    @FXML
    protected void onStartButtonClick() {
        goToWelcomeScreen();
    }

    /**
     * Navigate to the welcome screen.
     */
    private void goToWelcomeScreen() {
        SceneManager.switchScene("wc.fxml", "Pokemon Battle - Welcome", 800, 600);
    }
}
