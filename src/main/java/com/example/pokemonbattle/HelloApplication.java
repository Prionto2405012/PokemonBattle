package com.example.pokemonbattle;

import com.example.pokemonbattle.util.MediaCache;
import com.example.pokemonbattle.util.SceneManager;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        stage.setResizable(true);
        SceneManager.initialize(stage);
        Font menuFont = Font.loadFont(
            HelloApplication.class.getResourceAsStream("/com/example/pokemonbattle/fonts/menu.ttf"), 18);
        if (menuFont != null) System.out.println("[App] menu.ttf family: " + menuFont.getFamily());
        else System.err.println("[App] Failed to load menu.ttf");

        Font spaceNova = Font.loadFont(
            HelloApplication.class.getResourceAsStream("/com/example/pokemonbattle/fonts/SpaceNova-6Rpd1.otf"), 18);
        if (spaceNova != null) System.out.println("[App] SpaceNova family: " + spaceNova.getFamily());
        else System.err.println("[App] Failed to load SpaceNova");
        MediaCache.preload();
        SceneManager.switchScene("start.fxml", "Pokemon Battle", 1200, 700);
    }
    public static void main(String[] args) {
        launch(args);
    }
}
