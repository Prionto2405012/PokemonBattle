package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;

/**
 * Controller for the Main Menu Screen.
 * Provides navigation to game features.
 */
public class MenuController {
    
    /**
     * Handle "New Game" button click.
     */
    @FXML
    protected void onNewGameButtonClick() {
        System.out.println("New Game clicked - implement game logic here");
        // TODO: Load game screen
        // SceneManager.switchScene("game.fxml", "Pokemon Battle - Game", 1024, 768);
    }
    
    /**
     * Handle "Load Game" button click.
     */
    @FXML
    protected void onLoadGameButtonClick() {
        System.out.println("Load Game clicked - implement save/load logic here");
        // TODO: Show load game dialog
    }
    
    /**
     * Handle "Settings" button click.
     */
    @FXML
    protected void onSettingsButtonClick() {
        System.out.println("Settings clicked - implement settings screen here");
        // TODO: Load settings screen
    }
    
    /**
     * Handle "Back" button click - return to welcome screen.
     */
    @FXML
    protected void onBackButtonClick() {
        SceneManager.switchScene("wc.fxml", "Pokemon Battle - Welcome", 800, 600);
    }
    
    /**
     * Handle "Exit" button click - close application.
     */
    @FXML
    protected void onExitButtonClick() {
        Platform.exit();
    }
}
