package com.example.pokemonbattle.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
@SuppressWarnings("unused")
public class SettingsController {
    @FXML
    private StackPane overlayRoot;
    @FXML
    private StackPane gameSoundToggle;
    @FXML
    private StackPane gameSoundKnob;
    @FXML
    private VBox gameSoundSubSection;
    @FXML
    private Button gameGen1Btn, gameGen2Btn, gameGen3Btn;
    @FXML
    private StackPane battleSoundToggle;
    @FXML
    private StackPane battleSoundKnob;
    @FXML
    private VBox battleSoundSubSection;
    @FXML
    private Button battleGen1Btn, battleGen2Btn, battleGen3Btn;
    @FXML
    private StackPane animationToggle;
    @FXML
    private StackPane animationKnob;
    @FXML
    private Button langEnBtn, langJpBtn;
    private final BooleanProperty gameSoundOn = new SimpleBooleanProperty(false);
    private final BooleanProperty battleSoundOn = new SimpleBooleanProperty(false);
    private final BooleanProperty animationOn = new SimpleBooleanProperty(false);
    private int selectedGameGen = 1;
    private int selectedBattleGen = 1;
    private String selectedLang = "en";
    private static final double KNOB_TRAVEL = 22.0;
    @FXML
    public void initialize() {
        bindSubSection(gameSoundOn, gameSoundSubSection, gameGen1Btn, gameGen2Btn, gameGen3Btn);
        bindSubSection(battleSoundOn, battleSoundSubSection, battleGen1Btn, battleGen2Btn, battleGen3Btn);
        markGenSelected(gameGen1Btn, gameGen2Btn, gameGen3Btn, selectedGameGen);
        markGenSelected(battleGen1Btn, battleGen2Btn, battleGen3Btn, selectedBattleGen);
    }
    @FXML
    void onGameSoundToggle(MouseEvent e) {
        gameSoundOn.set(!gameSoundOn.get());
        animateToggle(gameSoundToggle, gameSoundKnob, gameSoundOn.get());
    }
    @FXML
    void onBattleSoundToggle(MouseEvent e) {
        battleSoundOn.set(!battleSoundOn.get());
        animateToggle(battleSoundToggle, battleSoundKnob, battleSoundOn.get());
    }
    @FXML
    void onAnimationToggle(MouseEvent e) {
        animationOn.set(!animationOn.get());
        animateToggle(animationToggle, animationKnob, animationOn.get());
    }
    @FXML
    void onGameGenSelect(ActionEvent e) {
        selectedGameGen = parseGen((Button) e.getSource());
        markGenSelected(gameGen1Btn, gameGen2Btn, gameGen3Btn, selectedGameGen);
    }
    @FXML
    void onBattleGenSelect(ActionEvent e) {
        selectedBattleGen = parseGen((Button) e.getSource());
        markGenSelected(battleGen1Btn, battleGen2Btn, battleGen3Btn, selectedBattleGen);
    }
    @FXML
    void onLanguageSelect(ActionEvent e) {
        Button src = (Button) e.getSource();
        selectedLang = (String) src.getUserData();
        setSelected(langEnBtn, "en".equals(selectedLang));
        setSelected(langJpBtn, "jp".equals(selectedLang));
    }
    @FXML
    void onCloseButtonClick(ActionEvent e) {
        closeOverlay();
    }
    @FXML
    void onBackgroundClick(MouseEvent e) {
        if (e.getTarget() == overlayRoot)
            closeOverlay();
    }
    @FXML
    void onCardClick(MouseEvent e) {
        e.consume();
    }
    private void bindSubSection(BooleanProperty prop, VBox subSection, Button... genBtns) {
        for (Button btn : genBtns) {
            btn.disableProperty().bind(prop.not());
        }
        prop.addListener((obs, oldVal, on) -> {
            if (on) {
                subSection.getStyleClass().remove("section-disabled");
            } else {
                if (!subSection.getStyleClass().contains("section-disabled")) {
                    subSection.getStyleClass().add("section-disabled");
                }
            }
        });
        if (!prop.get()) {
            subSection.getStyleClass().add("section-disabled");
        }
    }
    private void animateToggle(StackPane track, StackPane knob, boolean on) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), knob);
        tt.setToX(on ? KNOB_TRAVEL : 0);
        tt.play();

        track.getStyleClass().removeAll("toggle-on", "toggle-off");
        track.getStyleClass().add(on ? "toggle-on" : "toggle-off");
    }
    private void markGenSelected(Button b1, Button b2, Button b3, int gen) {
        setSelected(b1, gen == 1);
        setSelected(b2, gen == 2);
        setSelected(b3, gen == 3);
    }
    private void setSelected(Button btn, boolean selected) {
        if (selected) {
            if (!btn.getStyleClass().contains("option-selected"))
                btn.getStyleClass().add("option-selected");
        } else {
            btn.getStyleClass().remove("option-selected");
        }
    }
    private int parseGen(Button btn) {
        try {
            return Integer.parseInt((String) btn.getUserData());
        } catch (Exception ex) {
            return 1;
        }
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
    public boolean isGameSoundOn() {
        return gameSoundOn.get();
    }
    public boolean isBattleSoundOn() {
        return battleSoundOn.get();
    }
    public boolean isAnimationOn() {
        return animationOn.get();
    }
    public int getSelectedGameGen() {
        return selectedGameGen;
    }
    public int getSelectedBattleGen() {
        return selectedBattleGen;
    }
    public String getSelectedLang() {
        return selectedLang;
    }
}