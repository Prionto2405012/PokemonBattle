package com.example.pokemonbattle.example;

import com.example.pokemonbattle.controller.LoadingScreenController;
import com.example.pokemonbattle.util.SceneManager;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Example integration of the AAA Loading Screen
 * 
 * QUICK TEST:
 * 1. Run this class as JavaFX Application
 * 2. Watch the Pokeball rotate and load
 * 3. See the opening animation at 100%
 * 4. Transitions to menu after completion
 * 
 * TO INTEGRATE INTO YOUR GAME:
 * - Copy the showLoadingScreen() method to your HelloApplication.java
 * - Call it before loading heavy resources
 * - Replace simulateLoading() with your real data loading Task
 */
public class LoadingScreenExample extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Set window properties
            primaryStage.setTitle("Pokemon Battle - Loading");
            primaryStage.setResizable(false);
            
            // Show loading screen with simulated loading
            showLoadingScreenWithSimulation(primaryStage);
            
            // Alternative: Show with real task
            // showLoadingScreenWithRealTask(primaryStage);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * METHOD 1: Simulated Loading (for testing)
     */
    private void showLoadingScreenWithSimulation(Stage stage) {
        try {
            // Load loading screen FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pokemonbattle/view/loading_screen.fxml")
            );
            StackPane loadingRoot = loader.load();
            LoadingScreenController controller = loader.getController();
            
            // Use simulated loading (auto-completes in ~3 seconds)
            controller.simulateLoading();
            
            // Show the scene
            Scene scene = new Scene(loadingRoot, 1200, 700);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * METHOD 2: Real Task Loading (production-ready)
     */
    private void showLoadingScreenWithRealTask(Stage stage) {
        try {
            // Load loading screen FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pokemonbattle/view/loading_screen.fxml")
            );
            StackPane loadingRoot = loader.load();
            LoadingScreenController controller = loader.getController();
            
            // Create your real loading task
            Task<Void> loadingTask = createGameLoadingTask();
            
            // Bind task to loading screen
            controller.bindToTask(loadingTask, () -> {
                // Cleanup resources
                controller.cleanup();
                
                // Transition to menu after loading completes
                SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
            });
            
            // Show the scene
            Scene scene = new Scene(loadingRoot, 1200, 700);
            stage.setScene(scene);
            stage.show();
            
            // Start loading task on background thread
            new Thread(loadingTask).start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a realistic game loading task.
     * Replace this with your actual game initialization.
     */
    private Task<Void> createGameLoadingTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Stage 1: Initialize database (0-20%)
                updateMessage("Initializing database...");
                Thread.sleep(800);
                // DatabaseManager.getInstance().getConnection();
                updateProgress(20, 100);
                
                // Stage 2: Load Pokemon data (20-40%)
                updateMessage("Loading Pokemon database...");
                Thread.sleep(700);
                // loadPokemonData();
                updateProgress(40, 100);
                
                // Stage 3: Load moves (40-60%)
                updateMessage("Loading battle moves...");
                Thread.sleep(600);
                // loadMoves();
                updateProgress(60, 100);
                
                // Stage 4: Load sprites (60-80%)
                updateMessage("Loading sprites and assets...");
                Thread.sleep(900);
                // loadSprites();
                updateProgress(80, 100);
                
                // Stage 5: Initialize battle engine (80-100%)
                updateMessage("Initializing battle engine...");
                Thread.sleep(500);
                // initializeBattleEngine();
                updateProgress(100, 100);
                
                return null;
            }
        };
    }

    /**
     * INTEGRATION EXAMPLE FOR HelloApplication.java
     * 
     * Add this to your existing HelloApplication:
     */
    public static class IntegrationExample {
        
        public void integrateIntoHelloApplication(Stage primaryStage) throws Exception {
            // Initialize SceneManager
            SceneManager.initialize(primaryStage);
            
            // Show loading screen instead of directly showing menu
            showLoadingScreen(primaryStage);
        }
        
        private void showLoadingScreen(Stage stage) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pokemonbattle/view/loading_screen.fxml")
                );
                StackPane loadingRoot = loader.load();
                LoadingScreenController controller = loader.getController();
                
                // Create loading task
                Task<Void> loadTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        // Load your game resources here
                        updateMessage("Loading game data...");
                        loadGameResources();
                        updateProgress(50, 100);
                        
                        updateMessage("Preparing battle system...");
                        initializeBattleSystem();
                        updateProgress(100, 100);
                        
                        return null;
                    }
                };
                
                // Bind and show
                controller.bindToTask(loadTask, () -> {
                    controller.cleanup();
                    SceneManager.switchScene("start.fxml", "Pokemon Battle", 1200, 700);
                });
                
                Scene scene = new Scene(loadingRoot, 1200, 700);
                stage.setScene(scene);
                stage.show();
                
                new Thread(loadTask).start();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private void loadGameResources() throws Exception {
            // Your resource loading code
            Thread.sleep(1000);
        }
        
        private void initializeBattleSystem() throws Exception {
            // Your initialization code
            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
