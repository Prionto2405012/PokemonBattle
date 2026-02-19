package com.example.pokemonbattle;

import com.example.pokemonbattle.util.MediaCache;
import com.example.pokemonbattle.util.SceneManager;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main JavaFX Application class for PokemonBattle.
 *
 * Startup flow:
 *   intro.fxml  →  start.fxml  →  loading_screen.fxml  →  menu.fxml
 */
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        stage.setResizable(true);
        SceneManager.initialize(stage);

        // Kick off background pre-loading of all media assets immediately.
        // By the time the intro video finishes the GIFs / PNGs / MP4 URLs
        // are already warm in memory — no per-scene I/O stutter.
        MediaCache.preload();

        // Launch the cinematic intro video first
        SceneManager.switchScene("intro.fxml", "Pokemon Battle", 1200, 700);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
