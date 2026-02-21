package com.example.pokemonbattle.controller;
import java.io.IOException;
import java.util.List;

import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
@SuppressWarnings("unused") 
public class MenuController {
    @FXML private StackPane rootPane; 
    @FXML private ImageView bgImage;
    @FXML private Region menuOverlay;
    @FXML private VBox menuVBox;
    @FXML private VBox buttonContainer;
    @FXML private Button newGameButton;
    @FXML private Button loadGameButton;
    @FXML private Button settingsButton;
    @FXML private Button backButton;
    @FXML private Button exitButton;
    @FXML private StackPane pokeballNewGame;
    @FXML private StackPane pokeballLoadGame;
    @FXML private StackPane pokeballSettings;
    @FXML private StackPane pokeballBack;
    @FXML private StackPane pokeballExit;
    private List<Button> menuButtons;
    private int selectedIndex = -1;
    private boolean keyboardMode = false;  // tracks which input mode is active
    private void playCLickGlow(Button btn){
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setRadius(0);
        glow.setSpread(0.4);
        glow.setColor(javafx.scene.paint.Color.web("#b4c69a"));
        btn.setEffect(glow);
        Timeline pulse=new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 0)),
            new KeyFrame(Duration.millis(120), new KeyValue(glow.radiusProperty(), 25, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(250), new KeyValue(glow.radiusProperty(), 0, Interpolator.EASE_IN))
        );
        pulse.setOnFinished(e->btn.setEffect(null));
        pulse.play();
    }
    private void setupPokeballVisibility() {
        List<StackPane> balls = List.of(pokeballNewGame, pokeballLoadGame, pokeballSettings, pokeballBack, pokeballExit);
        for(int i=0;i<menuButtons.size();i++) {
            Button btn = menuButtons.get(i);
            StackPane ball = balls.get(i);
            ball.setOpacity(0);
            FadeTransition fadeIn=new FadeTransition(Duration.millis(100), ball);
            fadeIn.setToValue(1);
            FadeTransition fadeOut=new FadeTransition(Duration.millis(100), ball);
            fadeOut.setToValue(0);
            Runnable updateVisibility=()->{
                boolean hovered=btn.isHover();
                boolean selected=btn.getStyleClass().contains("button-selected");
                if(hovered || selected) {
                    fadeOut.stop();
                    fadeIn.playFromStart();
                } else {
                    fadeIn.stop();
                    fadeOut.playFromStart();
                }
            };
            btn.hoverProperty().addListener((obs, oldVal, newVal) -> updateVisibility.run());
            btn.getStyleClass().addListener((javafx.collections.ListChangeListener<String>) change ->{
                updateVisibility.run();
            });
            updateVisibility.run();
        }
    }
    private void playButtonEntrance() {
        // Fade in the left-quarter overlay together with the first button
        if (menuOverlay != null && rootPane != null) {
            menuOverlay.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.25));
            menuOverlay.prefHeightProperty().bind(rootPane.heightProperty());
            FadeTransition overlayFade = new FadeTransition(Duration.millis(500), menuOverlay);
            overlayFade.setFromValue(0);
            overlayFade.setToValue(1);
            overlayFade.setDelay(Duration.millis(150));
            overlayFade.play();
        }

        Button[] ordered = {newGameButton, loadGameButton, settingsButton, backButton, exitButton};
        for (int i = 0; i < ordered.length; i++) {
            Button btn = ordered[i];
            btn.setOpacity(0);
            btn.setTranslateX(-200);

            FadeTransition fade = new FadeTransition(Duration.millis(300), btn);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(400), btn);
            slide.setFromX(-200);
            slide.setToX(0);  // rests at natural position inside already-positioned container
            slide.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition entrance = new ParallelTransition(slide, fade);
            entrance.setDelay(Duration.millis(150 + i * 120));
            entrance.play();
        }
    }
    @FXML
    public void initialize() {
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        menuButtons = List.of(newGameButton, loadGameButton, settingsButton, backButton, exitButton);
        selectedIndex = -1;      // no button selected at start — do NOT call updateSelection() here
        setupPokeballVisibility();
        setupButtonHover();
        playButtonEntrance();
    }
    private void updateSelection() {
        for (int i = 0; i < menuButtons.size(); i++) {
            if (i == selectedIndex) {
                menuButtons.get(i).getStyleClass().add("menu-selected");
            } else {
                menuButtons.get(i).getStyleClass().remove("menu-selected");
            }
        }
    }
    private void setupButtonHover() {
        for (int i = 0; i < menuButtons.size(); i++) {
            final int index = i;
            Button btn = menuButtons.get(i);

            btn.setOnMouseEntered(e -> {
                keyboardMode = false;           // mouse takes over
                selectedIndex = index;
                updateSelection();
            });

            btn.setOnMouseExited(e -> {
                if (!keyboardMode) {            // only clear if still in mouse mode
                    selectedIndex = -1;
                    updateSelection();
                }
            });
        }
    }
    void onKeyPressed(KeyEvent event) {
        keyboardMode = true;                    // keyboard takes over
        // clear any mouse hover visual first
        if (event.getCode() == KeyCode.UP) {
            selectedIndex = (selectedIndex <= 0) ? menuButtons.size() - 1 : selectedIndex - 1;
            updateSelection();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            selectedIndex = (selectedIndex + 1) % menuButtons.size();
            updateSelection();
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER && selectedIndex >= 0) {
            menuButtons.get(selectedIndex).fire();
            event.consume();
        }
    }
    @FXML
    void onNewGameButtonClick() {
        System.out.println("New Game clicked!");
        SceneManager.switchSceneWithLoading("new_game.fxml", "New Game", 1200, 700);
    }
    @FXML
    void onLoadGameButtonClick() {
        System.out.println("Load Game clicked!");
    }
    @FXML
    void onSettingsButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pokemonbattle/view/settings.fxml"));
            javafx.scene.Node overlay = loader.load();
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
