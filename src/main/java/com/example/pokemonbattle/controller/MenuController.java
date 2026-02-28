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
import javafx.scene.text.Font;
import javafx.util.Duration;
@SuppressWarnings("unused") 
public class MenuController {
    @FXML private StackPane rootPane; 
    @FXML private ImageView bgImage;
    @FXML private Region menuOverlay;
    @FXML private VBox menuVBox;
    @FXML private VBox buttonContainer;
    @FXML private Button playGameButton;
    @FXML private Button settingsButton;
    @FXML private Button backButton;
    @FXML private Button exitButton;
    @FXML private StackPane selectBall;
    private List<Button> menuButtons;
    private int selectedIndex = -1;
    private boolean keyboardMode = false;  

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
    private void moveSelector(Button btn){
        if(btn==null) return;
        selectBall.setVisible(true);
        javafx.geometry.Bounds bounds=btn.localToScene(btn.getBoundsInLocal());
        javafx.geometry.Bounds rootBounds=rootPane.localToScene(rootPane.getBoundsInLocal());
        double y=bounds.getMinY() - rootBounds.getMinY() + bounds.getHeight()/2 - selectBall.getHeight()/2;
        double x=btn.getLayoutX() -25;
        if(!selectBall.isVisible()){
            selectBall.setOpacity(0);
            selectBall.setVisible(true);
            FadeTransition fade=new FadeTransition(Duration.millis(150), selectBall);
            fade.setToValue(1);
            fade.play();
        }
        TranslateTransition move=new TranslateTransition(Duration.millis(190), selectBall);
        move.setToX(x);
        move.setToY(y);
        move.setInterpolator(Interpolator.SPLINE(0.34,0.97,0.64,1));
        Timeline scalePop= new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(selectBall.scaleXProperty(), 1), new KeyValue(selectBall.scaleYProperty(), 1)),
            new KeyFrame(Duration.millis(80), new KeyValue(selectBall.scaleXProperty(), 0.85), new KeyValue(selectBall.scaleYProperty(), 0.85)),
            new KeyFrame(Duration.millis(190), new KeyValue(selectBall.scaleXProperty(), 1.05), new KeyValue(selectBall.scaleYProperty(), 1.05)),
            new KeyFrame(Duration.millis(250), new KeyValue(selectBall.scaleXProperty(), 1), new KeyValue(selectBall.scaleYProperty(), 1))
        );
        move.play();
        scalePop.play();
    }
    private void playButtonEntrance() {
        double initialDelay=400;
        double stagger=90;
        double slideDist=-50;
        Button[] ordered={playGameButton, settingsButton, backButton, exitButton};
        for(int i=0;i<ordered.length;i++){
            Button btn=ordered[i];
            btn.setOpacity(0);
            btn.setTranslateX(slideDist);
            FadeTransition fade=new FadeTransition(Duration.millis(175),btn);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide=new TranslateTransition(Duration.millis(300),btn);
            slide.setFromX(slideDist);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.SPLINE(0.2,0.8,0.2,1));
            ParallelTransition entrance=new ParallelTransition(slide, fade);
            entrance.setDelay(Duration.millis(initialDelay + i * stagger));
            entrance.play();
        }
        if(menuOverlay!=null){
            menuOverlay.setOpacity(0);
            FadeTransition overlayFade= new FadeTransition(Duration.millis(300), menuOverlay);
            overlayFade.setFromValue(0);
            overlayFade.setToValue(1);
            overlayFade.setDelay(Duration.millis(300));
            overlayFade.play();
        }
    }
    @FXML
    public void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/com/example/pokemonbattle/fonts/menu.ttf"), 24);
        Font loaded = Font.loadFont(getClass().getResourceAsStream("/com/example/pokemonbattle/fonts/SpaceNova-6Rpd1.otf"), 24);
        if (loaded != null) System.out.println("[MenuController] Loaded font family: " + loaded.getFamily());
        else System.err.println("[MenuController] Font failed to load — stream was null or corrupt");
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        menuButtons = List.of(playGameButton, settingsButton, backButton, exitButton);
        selectedIndex = -1;
        selectBall.setVisible(false);
        selectBall.setManaged(false);
        setupButtonHover();
        playButtonEntrance();
    }
    private void updateSelection() {
        for(int i = 0; i < menuButtons.size(); i++){
            Button btn = menuButtons.get(i);
            if(i == selectedIndex){
                btn.getStyleClass().remove("menu-hovered");
                btn.getStyleClass().add("menu-selected");
                moveSelector(btn);
            }
            else{
                btn.getStyleClass().remove("menu-selected");
            }
        }
        if(selectedIndex >= 0 && selectedIndex < menuButtons.size()) selectBall.setVisible(true);
        else selectBall.setVisible(false);
            
    }
    private void setupButtonHover() {
        for (int i = 0; i < menuButtons.size(); i++) {
            final int index = i;
            Button btn = menuButtons.get(i);

            btn.setOnMouseEntered(e -> {
                if (index != selectedIndex) {    
                    btn.getStyleClass().add("menu-hovered");
                }
            });
            btn.setOnMouseExited(e -> {
                btn.getStyleClass().remove("menu-hovered"); 
            });
            btn.setOnMouseClicked(e -> {
                btn.getStyleClass().remove("menu-hovered"); 
                selectedIndex = index;
                keyboardMode = false;
                updateSelection();          
            });
        }
    }
    void onKeyPressed(KeyEvent event) {
        keyboardMode = true;         
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
    void onplayGameButtonClick() {
        System.out.println("New Game clicked!");
        SceneManager.switchSceneWithLoading("new_game.fxml", "New Game", 1200, 700);
    }
    @FXML
    void onClick() {
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
