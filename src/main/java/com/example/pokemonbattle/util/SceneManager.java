package com.example.pokemonbattle.util;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Manages scene transitions for the application.
 * Reuses the same Stage instance to avoid creating multiple windows.
 */
public class SceneManager {
    private static Stage primaryStage;
    private static final String RESOURCE_PATH = "/com/example/pokemonbattle/";
    private static final String CSS_PATH = RESOURCE_PATH + "css/style.css";

    /**
     * Initialize the SceneManager with the primary stage.
     * Call this once from HelloApplication.start()
     */
    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Load and switch to a new scene.
     * 
     * @param fxmlFile the FXML file name (e.g., "start.fxml")
     * @param title the window title
     * @param width scene width
     * @param height scene height
     */
    public static void switchScene(String fxmlFile, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource(RESOURCE_PATH + "view/" + fxmlFile)
            );
            Scene scene = new Scene(loader.load(), width, height);
            
            // Apply global stylesheet
            scene.getStylesheets().add(
                SceneManager.class.getResource(CSS_PATH).toExternalForm()
            );
            
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + fxmlFile);
            e.printStackTrace();
        }
    }

    /**
     * Get the primary stage instance.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
