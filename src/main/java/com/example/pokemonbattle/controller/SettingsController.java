package com.example.pokemonbattle.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Controller for the Settings Overlay.
 * Handles close actions from the X button (top-right corner)
 * and the Close button (bottom of card), as well as background-click dismiss.
 */
@SuppressWarnings("unused") // Methods called via FXML
public class SettingsController {

    @FXML private StackPane overlayRoot;
    @FXML private Button closeButton;
    @FXML private Button closeXButton;

    // ── FXML handlers ────────────────────────────────────────────────────────

    /** Called by the bottom "Close" button AND the top-right X button. */
    @FXML
    protected void onCloseButtonClick() {
        closeOverlay();
    }

    /**
     * Consumes mouse events on the card itself so they don't bubble up
     * to onBackgroundClick and accidentally close the overlay.
     */
    @FXML
    protected void onCardClick(MouseEvent event) {
        event.consume();
    }

    /**
     * Closes the overlay when the user clicks the dark backdrop
     * (outside the card).  Clicks on the card are consumed by onCardClick
     * before they can reach this handler.
     */
    @FXML
    protected void onBackgroundClick(MouseEvent event) {
        closeOverlay();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Fade the overlay out then remove it from its parent pane.
     */
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
