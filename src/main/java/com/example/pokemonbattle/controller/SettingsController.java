package com.example.pokemonbattle.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
@SuppressWarnings("unused")
public class SettingsController {

    @FXML private StackPane overlayRoot;
    @FXML private Button closeButton;
    @FXML private Button closeXButton;
    @FXML
    protected void onCloseButtonClick() {
        closeOverlay();
    }
    @FXML
    protected void onCardClick(MouseEvent event) {
        event.consume();
    }
    @FXML
    protected void onBackgroundClick(MouseEvent event) {
        closeOverlay();
    }
    private void closeOverlay() {
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlayRoot);
        ft.setFromValue(overlayRoot.getOpacity());
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            if (overlayRoot.getParent() instanceof Pane parent) {
                parent.getChildren().remove(overlayRoot);
            }
        });
        ft.play();
    }
}
