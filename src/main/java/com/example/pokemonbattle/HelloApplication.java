package com.example.pokemonbattle;

import com.example.pokemonbattle.util.SceneManager;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main JavaFX Application class for PokemonBattle.
 * This is the single entry point - do not create additional Application subclasses.
 */
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        // Initialize the SceneManager with the primary stage
        stage.setResizable(true);
        SceneManager.initialize(stage);
        
        // Load the start/splash screen as the initial view
        SceneManager.switchScene("start.fxml", "Pokemon Battle - Start", 800, 600);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
