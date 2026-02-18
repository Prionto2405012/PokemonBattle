package com.example.pokemonbattle.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
     * Switch to the loading screen, then on to a target scene.
     * The loading screen controller is obtained after load and bound to a
     * background Task that loads the actual target scene.
     *
     * @param fxmlFile the FXML file name (e.g., "battle.fxml")
     * @param title    the window title for the target scene
     * @param width    scene width
     * @param height   scene height
     */
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height) {
        switchSceneWithLoading(fxmlFile, title, width, height, null);
    }

    /**
     * Switch to the loading screen, then on to a target scene with data.
     *
     * @param fxmlFile the FXML file name (e.g., "battle.fxml")
     * @param title    the window title for the target scene
     * @param width    scene width
     * @param height   scene height
     * @param data     map of data to pass to the new scene
     */
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height, Map<String, Object> data) {
        try {
            // Load the loading screen FXML
            var loadingUrl = SceneManager.class.getResource(
                    RESOURCE_PATH + "view/loading_screen.fxml");
            if (loadingUrl == null) {
                throw new RuntimeException("loading_screen.fxml not found");
            }

            FXMLLoader loadingLoader = new FXMLLoader(loadingUrl);
            Scene loadingScene = new Scene(loadingLoader.load(), 1200, 700);
            com.example.pokemonbattle.controller.LoadingScreenController lsc =
                    loadingLoader.getController();

            primaryStage.setScene(loadingScene);
            primaryStage.show();

            // Background task: load target scene
            javafx.concurrent.Task<LoadedSceneData> loadingTask =
                    new javafx.concurrent.Task<>() {
                @Override
                protected LoadedSceneData call() throws Exception {
                    updateMessage("Loading scene...");
                    updateProgress(0, 100);
                    Thread.sleep(200);

                    sceneData.clear();
                    if (data != null) sceneData.putAll(data);
                    updateProgress(40, 100);

                    var fxmlUrl = SceneManager.class.getResource(
                            RESOURCE_PATH + "view/" + fxmlFile);
                    if (fxmlUrl == null)
                        throw new RuntimeException("FXML not found: " + fxmlFile);

                    updateMessage("Preparing scene...");
                    FXMLLoader loader = new FXMLLoader(fxmlUrl);
                    Scene scene = new Scene(loader.load(), width, height);
                    updateProgress(100, 100);

                    return new LoadedSceneData(scene, title);
                }
            };

            lsc.bindToTask(loadingTask, () -> {
                LoadedSceneData d = loadingTask.getValue();
                lsc.cleanup();
                primaryStage.setTitle(d.title);
                primaryStage.setScene(d.scene);
            });

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
