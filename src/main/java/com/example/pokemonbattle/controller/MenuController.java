package com.example.pokemonbattle.controller;

import java.io.IOException;
import java.util.List;

import com.example.pokemonbattle.util.MusicManager;
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
import javafx.scene.Parent;
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

    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
    @FXML
    private Region menuOverlay;
    @FXML
    private VBox menuVBox;
    @FXML
    private VBox buttonContainer;
    @FXML
    private Button playGameButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button backButton;
    @FXML
    private Button exitButton;
    @FXML
    private StackPane selectBall;
    @FXML
    private StackPane exitOverlay;
    @FXML
    private Region exitBackdrop;
    @FXML
    private VBox exitDialog;
    @FXML
    private Button exitYesButton;
    @FXML
    private Button exitNoButton;
    private List<Button> menuButtons;
    private int selectedIndex = -1;
    private boolean keyboardMode = false;

    private void playCLickGlow(Button btn) {
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setRadius(0);
        glow.setSpread(0.4);
        glow.setColor(javafx.scene.paint.Color.web("#b4c69a"));
        btn.setEffect(glow);
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(120), new KeyValue(glow.radiusProperty(), 25, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(250), new KeyValue(glow.radiusProperty(), 0, Interpolator.EASE_IN)));
        pulse.setOnFinished(e -> btn.setEffect(null));
        pulse.play();
    }

    private void moveSelector(Button btn) {
        if (btn == null)
            return;
        selectBall.setVisible(true);
        javafx.geometry.Bounds b = btn.localToScene(btn.getBoundsInLocal());
        javafx.geometry.Bounds root = rootPane.localToScene(rootPane.getBoundsInLocal());
        double y = b.getMinY() - root.getMinY() + b.getHeight() / 2 - selectBall.getHeight() / 2;
        double x = btn.getLayoutX() - 25;
        if (!selectBall.isVisible()) {
            selectBall.setOpacity(0);
            selectBall.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), selectBall);
            fadeIn.setToValue(1);
            fadeIn.play();
        }
        TranslateTransition move = new TranslateTransition(Duration.millis(190), selectBall);
        move.setToX(x);
        move.setToY(y);
        move.setInterpolator(Interpolator.SPLINE(0.34, 0.97, 0.64, 1));
        Timeline scalePop = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(selectBall.scaleXProperty(), 1),
                        new KeyValue(selectBall.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(80), new KeyValue(selectBall.scaleXProperty(), 0.85),
                        new KeyValue(selectBall.scaleYProperty(), 0.85)),
                new KeyFrame(Duration.millis(190), new KeyValue(selectBall.scaleXProperty(), 1.05),
                        new KeyValue(selectBall.scaleYProperty(), 1.05)),
                new KeyFrame(Duration.millis(250), new KeyValue(selectBall.scaleXProperty(), 1),
                        new KeyValue(selectBall.scaleYProperty(), 1)));
        move.play();
        scalePop.play();
    }

    private void playButtonEntrance() {
        double initialDelay = 400, stagger = 90, slideDist = -50;
        Button[] ordered = { playGameButton, settingsButton, backButton, exitButton };
        for (int i = 0; i < ordered.length; i++) {
            Button btn = ordered[i];
            btn.setOpacity(0);
            btn.setTranslateX(slideDist);
            FadeTransition fade = new FadeTransition(Duration.millis(175), btn);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), btn);
            slide.setFromX(slideDist);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.2, 1));
            ParallelTransition entrance = new ParallelTransition(slide, fade);
            entrance.setDelay(Duration.millis(initialDelay + i * stagger));
            entrance.play();
        }
        if (menuOverlay != null) {
            menuOverlay.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), menuOverlay);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(300));
            ft.play();
        }
    }

    @FXML
    public void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/com/example/pokemonbattle/fonts/menu.ttf"), 24);
        Font loaded = Font
                .loadFont(getClass().getResourceAsStream("/com/example/pokemonbattle/fonts/SpaceNova-6Rpd1.otf"), 24);
        if (loaded != null)
            System.out.println("[MenuController] Font: " + loaded.getFamily());
        else
            System.err.println("[MenuController] Font failed to load");

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

        MusicManager mm = MusicManager.getInstance();

        // Start BGM only if nothing is already playing (avoids restarting on scene
        // switch back)
        if (mm.getCurrentTrack() == null) {
            mm.playRandomBGM();
        }

        // Wire click sound to all buttons in this scene
        mm.attachClickSounds(rootPane);
    }

    private void updateSelection() {
        for (int i = 0; i < menuButtons.size(); i++) {
            Button btn = menuButtons.get(i);
            if (i == selectedIndex) {
                btn.getStyleClass().remove("menu-hovered");
                btn.getStyleClass().add("menu-selected");
                moveSelector(btn);
            } else {
                btn.getStyleClass().remove("menu-selected");
            }
        }
        selectBall.setVisible(selectedIndex >= 0 && selectedIndex < menuButtons.size());
    }

    private void setupButtonHover() {
        for (int i = 0; i < menuButtons.size(); i++) {
            final int index = i;
            Button btn = menuButtons.get(i);
            btn.setOnMouseEntered(e -> {
                if (index != selectedIndex)
                    btn.getStyleClass().add("menu-hovered");
            });
            btn.setOnMouseExited(e -> btn.getStyleClass().remove("menu-hovered"));
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
        if (event.getCode() == KeyCode.ESCAPE && exitOverlay != null && exitOverlay.isVisible()) {
            onExitCancelled();
            event.consume();
        }
        else if (event.getCode() == KeyCode.UP) {
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
            // Wire click sounds to the overlay's buttons too
            MusicManager.getInstance().attachClickSounds((Parent) overlay);

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
        SceneManager.switchSceneWithLoading("wc.fxml", "Welcome", 1200, 700);
    }

    @FXML
    void onExitButtonClick() {
        showExitOverlay();
    }

    private void showExitOverlay() {
        exitOverlay.setVisible(true);
        exitOverlay.setManaged(true);
        exitOverlay.setMouseTransparent(false);

        // Start from invisible
        exitOverlay.setOpacity(0);
        exitDialog.setScaleX(0.85);
        exitDialog.setScaleY(0.85);
        exitDialog.setOpacity(0);

        // Backdrop fade
        FadeTransition backdropFade = new FadeTransition(Duration.millis(220), exitOverlay);
        backdropFade.setFromValue(0); backdropFade.setToValue(1);
        backdropFade.setInterpolator(Interpolator.EASE_OUT);

        // Dialog scale + fade in
        Timeline dialogPop = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(exitDialog.scaleXProperty(), 0.85),
                new KeyValue(exitDialog.scaleYProperty(), 0.85),
                new KeyValue(exitDialog.opacityProperty(), 0)
            ),
            new KeyFrame(Duration.millis(260),
                new KeyValue(exitDialog.scaleXProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                new KeyValue(exitDialog.scaleYProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                new KeyValue(exitDialog.opacityProperty(), 1.0, Interpolator.EASE_OUT)
            )
        );

        backdropFade.play();
        dialogPop.play();
    }

    private void hideExitOverlay(Runnable onFinished) {
        Timeline dialogDismiss = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(exitDialog.scaleXProperty(), 1.0),
                new KeyValue(exitDialog.scaleYProperty(), 1.0),
                new KeyValue(exitDialog.opacityProperty(), 1.0)
            ),
            new KeyFrame(Duration.millis(180),
                new KeyValue(exitDialog.scaleXProperty(), 0.88, Interpolator.EASE_IN),
                new KeyValue(exitDialog.scaleYProperty(), 0.88, Interpolator.EASE_IN),
                new KeyValue(exitDialog.opacityProperty(), 0.0, Interpolator.EASE_IN)
            )
        );
        FadeTransition backdropFade = new FadeTransition(Duration.millis(200), exitOverlay);
        backdropFade.setFromValue(1); backdropFade.setToValue(0);
        backdropFade.setInterpolator(Interpolator.EASE_IN);
        backdropFade.setOnFinished(e -> {
            exitOverlay.setVisible(false);
            exitOverlay.setManaged(false);
            if (onFinished != null) onFinished.run();
        });
        dialogDismiss.play();
        backdropFade.play();
    }

    @FXML
    void onExitConfirmed() {
        hideExitOverlay(() -> System.exit(0));
    }

    @FXML
    void onExitCancelled() {
        hideExitOverlay(null);
    }
}