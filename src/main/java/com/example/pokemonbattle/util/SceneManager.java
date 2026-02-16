package com.example.pokemonbattle.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.example.pokemonbattle.controller.LoadingScreenController;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Manages scene transitions for the application.
 * Reuses the same Stage instance to avoid creating multiple windows.
 * Supports passing data between scenes.
 */
public class SceneManager {
    private static Stage primaryStage;
    private static final String RESOURCE_PATH = "/com/example/pokemonbattle/";
    private static final Map<String, Object> sceneData = new HashMap<>();
    
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
        switchSceneWithData(fxmlFile, title, width, height, null);
    }

    /**
     * Load and switch to a new scene with data.
     * Data can be retrieved in the controller using SceneManager.getData(key)
     * 
     * @param fxmlFile the FXML file name (e.g., "battle.fxml")
     * @param title the window title
     * @param width scene width
     * @param height scene height
     * @param data map of data to pass to the new scene
     */
    public static void switchSceneWithData(String fxmlFile, String title, int width, int height, Map<String, Object> data) {
        try {
            // Clear old data and store new data
            sceneData.clear();
            if (data != null) {
                sceneData.putAll(data);
            }
            
            // Verify FXML file exists
            var fxmlUrl = SceneManager.class.getResource(RESOURCE_PATH + "view/" + fxmlFile);
            if (fxmlUrl == null) {
                throw new RuntimeException("FXML file not found: " + RESOURCE_PATH + "view/" + fxmlFile);
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load(), width, height);
            
            // CSS stylesheets are now loaded directly from FXML files
            
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } 
        catch (IOException e) {
            throw new RuntimeException("Error loading FXML file: " + fxmlFile, e);
        }
    }

    /**
     * Switch to a new scene with loading screen animation.
     * Shows the loading screen briefly while the target scene loads in the background.
     * 
     * @param fxmlFile the FXML file name (e.g., "battle.fxml")
     * @param title the window title for the target scene
     * @param width scene width
     * @param height scene height
     */
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height) {
        switchSceneWithLoading(fxmlFile, title, width, height, null);
    }

    /**
     * Switch to a new scene with loading screen animation and data.
     * Shows the loading screen briefly while the target scene loads in the background.
     * 
     * @param fxmlFile the FXML file name (e.g., "battle.fxml")
     * @param title the window title for the target scene
     * @param width scene width
     * @param height scene height
     * @param data map of data to pass to the new scene
     */
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height, Map<String, Object> data) {
        try {
            // Load loading screen
            FXMLLoader loadingLoader = new FXMLLoader(
                SceneManager.class.getResource(RESOURCE_PATH + "view/loading_screen.fxml")
            );
            StackPane loadingRoot = loadingLoader.load();
            LoadingScreenController loadingController = loadingLoader.getController();
            
            // Show loading screen immediately
            Scene loadingScene = new Scene(loadingRoot, 1200, 700);
            primaryStage.setScene(loadingScene);
            primaryStage.show();
            
            // Create background task to load target scene
            Task<LoadedSceneData> loadingTask = new Task<>() {
                @Override
                protected LoadedSceneData call() throws Exception {
                    // Update progress stages
                    updateMessage("Loading scene...");
                    updateProgress(0, 100);
                    
                    // Small delay to show the loading animation
                    Thread.sleep(300);
                    updateProgress(30, 100);
                    
                    // Clear old data and store new data
                    sceneData.clear();
                    if (data != null) {
                        sceneData.putAll(data);
                    }
                    updateProgress(50, 100);
                    
                    // Verify FXML file exists
                    var fxmlUrl = SceneManager.class.getResource(RESOURCE_PATH + "view/" + fxmlFile);
                    if (fxmlUrl == null) {
                        throw new RuntimeException("FXML file not found: " + RESOURCE_PATH + "view/" + fxmlFile);
                    }
                    updateProgress(70, 100);
                    
                    // Load the target FXML
                    updateMessage("Preparing scene...");
                    FXMLLoader loader = new FXMLLoader(fxmlUrl);
                    Scene scene = new Scene(loader.load(), width, height);
                    updateProgress(100, 100);
                    
                    return new LoadedSceneData(scene, title);
                }
            };
            
            // Bind task to loading controller
            loadingController.bindToTask(loadingTask, () -> {
                // Get the loaded scene
                LoadedSceneData loadedScene = loadingTask.getValue();
                
                // Cleanup loading resources
                loadingController.cleanup();
                
                // Switch to target scene
                primaryStage.setTitle(loadedScene.title);
                primaryStage.setScene(loadedScene.scene);
            });
            
            // Start loading on background thread
            new Thread(loadingTask).start();
            
        } catch (IOException e) {
            throw new RuntimeException("Error loading scene with loading screen", e);
        }
    }

    /**
     * Internal class to hold loaded scene data
     */
    private static class LoadedSceneData {
        final Scene scene;
        final String title;
        
        LoadedSceneData(Scene scene, String title) {
            this.scene = scene;
            this.title = title;
        }
    }

    /**
     * Get data passed to the current scene
     * 
     * @param key the data key
     * @return the data value, or null if not found
     */
    public static Object getData(String key) {
        return sceneData.get(key);
    }

    /**
     * Store data for use in current or future scenes
     * 
     * @param key the data key
     * @param value the data value
     */
    public static void setData(String key, Object value) {
        sceneData.put(key, value);
    }

    /**
     * Clear all stored scene data
     */
    public static void clearData() {
        sceneData.clear();
    }

    /**
     * Get the primary stage instance.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
