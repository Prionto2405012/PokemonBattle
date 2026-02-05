package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
// import javafx.util.Duration;

/**
 * Controller for the Start/Splash Screen.
 * Shows initial loading or splash content, then transitions to Welcome screen.
 */
public class StartController {

    /**
     * Called automatically by JavaFX after FXML is loaded.
     */
    @FXML
    public void initialize() {
        // Auto-transition to welcome screen after 3 seconds (optional)
        // PauseTransition delay = new PauseTransition(Duration.seconds(3));
        // delay.setOnFinished(event -> goToWelcomeScreen());
        // delay.play();
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
