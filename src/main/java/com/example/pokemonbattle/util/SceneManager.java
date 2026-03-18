package com.example.pokemonbattle.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class SceneManager {
    private static Stage primaryStage;
    private static final String RESOURCE_PATH = "/com/example/pokemonbattle/";
    private static final long DEFAULT_LOADING_MIN_MS = 900L;
    private static final Map<String, Object> sceneData = new HashMap<>();
    public static void initialize(Stage stage) {
        primaryStage = stage;
    }
    public static void enableCoordDebug(javafx.scene.Parent root) {
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            String id         = target.getId() != null ? target.getId() : "-";
            String styleClass = target.getStyleClass().isEmpty() ? "-" : target.getStyleClass().get(0);
            System.out.printf(
                "[Click] Scene(%.1f, %.1f)  Local(%.1f, %.1f)  >> %s  id=%s  class=%s%n",
                e.getSceneX(), e.getSceneY(),
                e.getX(), e.getY(),
                target.getClass().getSimpleName(), id, styleClass
            );
        });
        System.out.println("[CoordDebug] Click anywhere on the scene to print coordinates.");
    }
    public static void switchScene(String fxmlFile, String title, int width, int height) {
        switchSceneWithData(fxmlFile, title, width, height, null);
    }
    public static void switchSceneWithData(String fxmlFile, String title, int width, int height, Map<String, Object> data) {
        try {
            sceneData.clear();
            if (data != null) sceneData.putAll(data);
            var fxmlUrl = SceneManager.class.getResource(RESOURCE_PATH + "view/" + fxmlFile);
            if (fxmlUrl == null) throw new RuntimeException("FXML file not found: " + RESOURCE_PATH + "view/" + fxmlFile);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load(), width, height);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } 
        catch (IOException e) {
            throw new RuntimeException("Error loading FXML file: " + fxmlFile, e);
        }
    }
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height) {
        switchSceneWithLoading(fxmlFile, title, width, height, null, DEFAULT_LOADING_MIN_MS);
    }
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height, long minimumLoadingMs) {
        switchSceneWithLoading(fxmlFile, title, width, height, null, minimumLoadingMs);
    }
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height, Map<String, Object> data) {
        switchSceneWithLoading(fxmlFile, title, width, height, data, DEFAULT_LOADING_MIN_MS);
    }
    public static void switchSceneWithLoading(String fxmlFile, String title, int width, int height, Map<String, Object> data, long minimumLoadingMs) {
        try {
            var loadingUrl = SceneManager.class.getResource(RESOURCE_PATH + "view/loading_screen.fxml");
            if (loadingUrl == null) throw new RuntimeException("loading_screen.fxml not found");
            FXMLLoader loadingLoader = new FXMLLoader(loadingUrl);
            Scene loadingScene = new Scene(loadingLoader.load(), 1200, 700);
            com.example.pokemonbattle.controller.LoadingScreenController lsc = loadingLoader.getController();
            primaryStage.setScene(loadingScene);
            primaryStage.show();
            var fxmlUrl = SceneManager.class.getResource(RESOURCE_PATH + "view/" + fxmlFile);
            if (fxmlUrl == null)
                throw new RuntimeException("FXML not found: " + RESOURCE_PATH + "view/" + fxmlFile);
            javafx.concurrent.Task<Void> loadingTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    long minDurationMs = Math.max(300L, minimumLoadingMs);
                    long prepMs = 120L;
                    updateMessage("Loading...");
                    updateProgress(0, 100);
                    Thread.sleep(prepMs);

                    sceneData.clear();
                    if (data != null) sceneData.putAll(data);
                    updateProgress(20, 100);

                    updateMessage("Almost ready...");
                    updateProgress(80, 100);
                    long holdMs = Math.max(0L, minDurationMs - prepMs);
                    if (holdMs > 0) {
                        Thread.sleep(holdMs);
                    }
                    updateProgress(100, 100);
                    updateMessage("Done!");
                    return null;
                }
            };
            lsc.bindToTask(loadingTask, () -> {
                try {
                    FXMLLoader loader = new FXMLLoader(fxmlUrl);
                    Scene scene = new Scene(loader.load(), width, height);
                    primaryStage.setTitle(title);
                    primaryStage.setScene(scene);
                } catch (IOException e) {
                    throw new RuntimeException("Error loading FXML: " + fxmlFile, e);
                }
            });

            new Thread(loadingTask).start();

        } catch (IOException e) {
            throw new RuntimeException("Error loading scene with loading screen", e);
        }
    }
    public static Object getData(String key) {
        return sceneData.get(key);
    }
    public static void setData(String key, Object value) {
        sceneData.put(key, value);
    }
    public static void clearData() {
        sceneData.clear();
    }
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
