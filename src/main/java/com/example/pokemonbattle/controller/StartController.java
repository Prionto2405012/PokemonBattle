package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Controller for the Start Screen.
 * Background is start.gif (animated). Start button transitions to the Loading screen.
 */
@SuppressWarnings("unused")
public class StartController {

    @FXML private StackPane rootPane;
    @FXML private ImageView bgImage;

    @FXML
    public void initialize() {
        // Bind background GIF to fill the container
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
    }

    /** Start button click handler — transitions to the loading screen. */
    @FXML
    protected void onStartButtonClick() {
        SceneManager.switchScene("loading_screen.fxml", "Pokemon Battle - Loading", 1200, 700);
    }
}
