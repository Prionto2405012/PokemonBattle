package com.example.pokemonbattle.controller;

import java.io.IOException;
import java.util.List;

import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXMLLoader;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
    private StackPane leftSidebar;
    @FXML
    private Pane sidebarBlurLayer;
    @FXML
    private Rectangle dimOverlay;

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

        // Make sidebar focusable
        leftSidebar.setFocusTraversable(true);
        leftSidebar.requestFocus();

        // Attach key handler at scene level so it fires regardless of focus owner
        Platform.runLater(() -> {
            if (leftSidebar.getScene() != null) {
                leftSidebar.getScene().addEventFilter(
                    KeyEvent.KEY_PRESSED, this::onKeyPressed);
            }
        });

        // Build glass blur backdrop
        setupGlassSidebar();

        // Cinematic sidebar entrance
        playSidebarIntro();
    }

    /**
     * Builds the glassmorphic blurred backdrop inside the sidebar.
     */
    private void setupGlassSidebar() {
        Platform.runLater(() -> {
            if (sidebarBlurLayer == null || bgImage == null || rootPane == null) return;

            // Blurred copy of the background — full root size so it shows the correct portion
            ImageView blurBg = new ImageView(bgImage.getImage());
            blurBg.setPreserveRatio(false);
            blurBg.setEffect(new GaussianBlur(40));
            blurBg.fitWidthProperty().bind(rootPane.widthProperty());
            blurBg.fitHeightProperty().bind(rootPane.heightProperty());

            // Frosted dark-teal tint
            Rectangle tint = new Rectangle();
            tint.setFill(Color.web("#0a1e1c", 0.50));
            tint.setWidth(leftSidebar.getWidth());
            tint.setHeight(leftSidebar.getHeight());

            // Clip blurLayer to sidebar bounds so blur doesn't bleed out
            Rectangle clip = new Rectangle(leftSidebar.getWidth(), leftSidebar.getHeight());
            sidebarBlurLayer.setClip(clip);

            // Keep tint and clip sized to sidebar without binding (binding causes
            // "bound value cannot be set" when layout touches these during animation)
            leftSidebar.widthProperty().addListener((obs, o, n) -> {
                tint.setWidth(n.doubleValue());
                clip.setWidth(n.doubleValue());
            });
            leftSidebar.heightProperty().addListener((obs, o, n) -> {
                tint.setHeight(n.doubleValue());
                clip.setHeight(n.doubleValue());
            });

            sidebarBlurLayer.getChildren().addAll(blurBg, tint);
        });
    }

    /**
     * Plays the cinematic left-sidebar entrance animation.
     */
    private void playSidebarIntro() {
        Platform.runLater(() -> {
            if (leftSidebar == null || dimOverlay == null || rootPane == null) return;

            // Bind sidebar width to 1/3.2 of root
            leftSidebar.prefWidthProperty().bind(rootPane.widthProperty().divide(3.2));
            leftSidebar.maxWidthProperty().bind(rootPane.widthProperty().divide(3.2));
            leftSidebar.minWidthProperty().bind(rootPane.widthProperty().divide(3.2));

            // Size dim overlay to fill root without binding (binding causes
            // "bound value cannot be set" errors during animation/layout passes)
            dimOverlay.setWidth(rootPane.getWidth());
            dimOverlay.setHeight(rootPane.getHeight());
            rootPane.widthProperty().addListener((obs, o, n) -> dimOverlay.setWidth(n.doubleValue()));
            rootPane.heightProperty().addListener((obs, o, n) -> dimOverlay.setHeight(n.doubleValue()));

            leftSidebar.applyCss();
            leftSidebar.layout();

            // Initial states
            leftSidebar.setTranslateX(-400);
            dimOverlay.setOpacity(0);
            List.of(newGameButton, loadGameButton, settingsButton, backButton, exitButton)
                .forEach(b -> { b.setOpacity(0); b.setTranslateX(-20); });

            //Gaussian blur 
            GaussianBlur blur = new GaussianBlur(12);
            leftSidebar.setEffect(blur);
            DoubleProperty blurValue = new SimpleDoubleProperty(18);
            blurValue.addListener((obs, o, n) -> blur.setRadius(n.doubleValue()));
            Timeline blurTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,      new KeyValue(blurValue, 18, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(450), new KeyValue(blurValue, 0,  Interpolator.EASE_OUT))
            );

            //Sidebar slide 
            TranslateTransition slide = new TranslateTransition(Duration.millis(450), leftSidebar);
            slide.setFromX(-400);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            //Dim fade
            FadeTransition dimFade = new FadeTransition(Duration.millis(450), dimOverlay);
            dimFade.setFromValue(0);
            dimFade.setToValue(0.35);
            dimFade.setInterpolator(Interpolator.EASE_OUT);

            //Main transition 
            ParallelTransition intro = new ParallelTransition(slide, dimFade, blurTimeline);
            intro.setOnFinished(e -> {
                // Restore drop shadow (replaces the temp blur effect)
                DropShadow shadow = new DropShadow(20, 5, 0, Color.web("#073622bf"));
                shadow.setSpread(0.4);
                leftSidebar.setEffect(shadow);
            });
            intro.play();

            //Staggered button entrances
            Button[] ordered = { newGameButton, loadGameButton, settingsButton, backButton, exitButton };
            for (int i = 0; i < ordered.length; i++) {
                Button btn = ordered[i];
                FadeTransition fade = new FadeTransition(Duration.millis(180), btn);
                fade.setFromValue(0);
                fade.setToValue(1);
                TranslateTransition move = new TranslateTransition(Duration.millis(180), btn);
                move.setFromX(-20);
                move.setToX(0);
                move.setInterpolator(Interpolator.EASE_OUT);
                ParallelTransition entrance = new ParallelTransition(fade, move);
                entrance.setDelay(Duration.millis(120 + i * 80L));
                entrance.play();
            }
        });
    }

    /**
     * Keyboard navigation handler (scene-level, not FXML-bound).
     * Arrow keys / WASD to navigate, Enter/Space to activate.
     */
    void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        switch (code) {
            case UP, W -> {
                selectedIndex = (selectedIndex - 1 + menuButtons.size()) % menuButtons.size();
                updateSelection();
                event.consume();
            }
            case DOWN, S -> {
                selectedIndex = (selectedIndex + 1) % menuButtons.size();
                updateSelection();
                event.consume();
            }
            case ENTER, SPACE -> {
                menuButtons.get(selectedIndex).fire();
                event.consume();
            }
            default -> {}
        }
    }

    /**
     * Updates visual selection for the currently focused button.
     */
    private void updateSelection() {
        menuButtons.forEach(b -> {
            b.getStyleClass().remove("button-selected");
            b.setScaleX(1);
            b.setScaleY(1);
        });

        Button selectedButton = menuButtons.get(selectedIndex);
        if (!selectedButton.getStyleClass().contains("button-selected")) {
            selectedButton.getStyleClass().add("button-selected");
        }
        // Keep focus on leftSidebar so scene-level key handler stays active
        leftSidebar.requestFocus();
    }

    // ===== Button action handlers =====

    @FXML
    void onNewGameButtonClick() {
        System.out.println("New Game clicked!");
        SceneManager.switchSceneWithLoading("new_game.fxml", "New Game", 1200, 700);
    }

    @FXML
    void onLoadGameButtonClick() {
        System.out.println("Load Game clicked!");
        // SceneManager.switchScene("loadgame.fxml", "Load Game", 1200, 700);
    }

    @FXML
    void onSettingsButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pokemonbattle/view/settings.fxml"));
            javafx.scene.Node overlay = loader.load();

            // Start transparent, then fade in
            overlay.setOpacity(0.0);
            rootPane.getChildren().add(overlay);

            FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        } catch (IOException e) {
            System.err.println("Error loading settings overlay: " + e.getMessage());
        }
    }

    @FXML
    void onBackButtonClick() {
        System.out.println("Back clicked!");
        SceneManager.switchSceneWithLoading("wc.fxml", "Welcome", 1200, 700);
    }

    @FXML
    void onExitButtonClick() {
        System.out.println("Exit clicked!");
        System.exit(0);
    }
}
