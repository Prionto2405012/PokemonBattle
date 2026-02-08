package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Controller for the Welcome Screen.
 * Collects player name/info before proceeding to main menu.
 */
@SuppressWarnings("unused") // Methods are called by FXML
public class WcController {
    @FXML
    private StackPane rootPane; // Root container for background image
    @FXML
    private ImageView bgImage; // Background image
    @FXML
    private TextField playerNameField;

    /**
     * Initialize the controller - bind background image to fill the container.
     */
    @FXML
    public void initialize() {
        // Bind background image to fill the container (with null check)
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        
        // Focus on the text field for immediate typing
        if (playerNameField != null) {
            playerNameField.requestFocus();
        }
    }
    @FXML
    protected void onContinueButtonClick() {
        String playerName = playerNameField.getText().trim();
        
        if (playerName.isEmpty()) {
            playerNameField.setPromptText("Please enter your name!");
            return;
        }
        
        // TODO: Store player name in a model/session manager
        System.out.println("Player name: " + playerName);
        
        // Navigate to main menu
        SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 800, 600);
    }
    
    /**
     * Handle "Back" button click.
     */
    @FXML
    protected void onBackButtonClick() {
        SceneManager.switchScene("start.fxml", "Pokemon Battle - Start", 800, 600);
    }
}
