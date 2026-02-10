package com.example.pokemonbattle.controller;

import java.util.List;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for Main Menu screen with keyboard navigation.
 */
@SuppressWarnings("unused") // Methods are called by FXML
public class MenuController {

    @FXML
    private StackPane rootPane; // Root container for background image
    @FXML
    private ImageView bgImage; // Background image
    @FXML
    private VBox menuVBox;

    @FXML
    private Button newGameButton;
    @FXML
    private Button loadGameButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button backButton;
    @FXML
    private Button exitButton;

    private List<Button> menuButtons;
    private int selectedIndex = 0;

    @FXML
    public void initialize() {
        // Bind background image to fill the container (with null check)
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        
        // List of buttons in menu order
        menuButtons = List.of(newGameButton, loadGameButton, settingsButton, backButton, exitButton);

        // Make VBox focusable to receive keyboard events
        menuVBox.setFocusTraversable(true);
        menuVBox.requestFocus();

        // Set initial selection highlight
        updateSelection();
    }

    /**
     * Keyboard navigation handler.
     * Arrow keys / WASD to move, Enter/Space to select.
     */
    @FXML
    void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        switch (code) {
            case UP, W -> {
                selectedIndex = (selectedIndex - 1 + menuButtons.size()) % menuButtons.size();
                updateSelection();
            }
            case DOWN, S -> {
                selectedIndex = (selectedIndex + 1) % menuButtons.size();
                updateSelection();
            }
            case ENTER, SPACE -> {
                // Fire the selected button
                menuButtons.get(selectedIndex).fire();
            }
            default -> {
                // Optional: add left/right navigation if needed
            }
        }
    }

    /**
     * Updates visual selection for the currently focused button.
     */
    private void updateSelection() {
        // Reset all buttons: remove style and reset scale
        menuButtons.forEach(b -> {
            b.getStyleClass().remove("button-selected");
            b.setScaleX(1);
            b.setScaleY(1);
        });

        // Apply 'selected' style to currently selected button
        Button selectedButton = menuButtons.get(selectedIndex);
        if (!selectedButton.getStyleClass().contains("button-selected")) {
            selectedButton.getStyleClass().add("button-selected");
        }

        // Smooth scale-up animation
        ScaleTransition st = new ScaleTransition(Duration.millis(100), selectedButton);
        st.setToX(1.1);
        st.setToY(1.1);
        st.play();

        // Request focus so Enter key works
        selectedButton.requestFocus();
    }

    // ===== Button action handlers =====

    @FXML
    void onNewGameButtonClick() {
        System.out.println("New Game clicked!");
        // SceneManager.switchScene("newgame.fxml", "New Game", 800, 600);
    }

    @FXML
    void onLoadGameButtonClick() {
        System.out.println("Load Game clicked!");
        // SceneManager.switchScene("loadgame.fxml", "Load Game", 800, 600);
    }

    @FXML
    void onSettingsButtonClick() {
        System.out.println("Settings clicked!");
        // SceneManager.switchScene("settings.fxml", "Settings", 800, 600);
    }

    @FXML
    void onBackButtonClick() {
        System.out.println("Back clicked!");
        SceneManager.switchScene("wc.fxml", "Welcome", 800, 600);
    }

    @FXML
    void onExitButtonClick() {
        System.out.println("Exit clicked!");
        System.exit(0);
    }
}
