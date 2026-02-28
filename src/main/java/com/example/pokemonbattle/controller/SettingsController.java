package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.MusicManager;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
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
    private StackPane gameSoundToggle, gameSoundKnob;
    @FXML
    private VBox gameSoundSubSection;
    @FXML
    private Button gameGen1Btn, gameGen2Btn, gameGen3Btn;
    @FXML
    private Slider volumeSlider;
    @FXML
    private Label volumePercentLabel;

    @FXML
    private StackPane battleSoundToggle, battleSoundKnob;
    @FXML
    private VBox battleSoundSubSection;
    @FXML
    private Button battleGen1Btn, battleGen2Btn, battleGen3Btn;

    @FXML
    private StackPane animationToggle, animationKnob;

    @FXML
    private Button langEnBtn, langJpBtn;
    private final BooleanProperty gameSoundOn = new SimpleBooleanProperty(true);
    private final BooleanProperty battleSoundOn = new SimpleBooleanProperty(false);
    private final BooleanProperty animationOn = new SimpleBooleanProperty(false);

    private int selectedGameGen = 1;
    private int selectedBattleGen = 1;
    private String selectedLang = "en";
    private static final double KNOB_TRAVEL = 22.0;
    @FXML
    public void initialize() {
        MusicManager mm = MusicManager.getInstance();
        bindSubSection(gameSoundOn, gameSoundSubSection, gameGen1Btn, gameGen2Btn, gameGen3Btn);
        bindSubSection(battleSoundOn, battleSoundSubSection, battleGen1Btn, battleGen2Btn, battleGen3Btn);
        applyToggleStyle(gameSoundToggle, gameSoundKnob, gameSoundOn.get());
        applyToggleStyle(battleSoundToggle, battleSoundKnob, battleSoundOn.get());
        applyToggleStyle(animationToggle, animationKnob, animationOn.get());
        markGenSelected(gameGen1Btn, gameGen2Btn, gameGen3Btn, selectedGameGen);
        markGenSelected(battleGen1Btn, battleGen2Btn, battleGen3Btn, selectedBattleGen);
        if (volumeSlider != null) {
            volumeSlider.setValue(mm.getMasterVolume());
            updateVolumeLabel(mm.getMasterVolume());
            volumeSlider.valueProperty().addListener((obs, o, val) -> {
                mm.setMasterVolume(val.doubleValue());
                updateVolumeLabel(val.doubleValue());
            });
            volumeSlider.disableProperty().bind(gameSoundOn.not());
        }
        if (mm.getCurrentTrack() == null && gameSoundOn.get()) {
            mm.playRandomBGM();
        }
    }
    @FXML
    void onGameSoundToggle(MouseEvent e) {
        boolean nowOn = !gameSoundOn.get();
        gameSoundOn.set(nowOn);
        animateToggle(gameSoundToggle, gameSoundKnob, nowOn);
        MusicManager.getInstance().setSoundEnabled(nowOn);
    }

    @FXML
    void onBattleSoundToggle(MouseEvent e) {
        boolean nowOn = !battleSoundOn.get();
        battleSoundOn.set(nowOn);
        animateToggle(battleSoundToggle, battleSoundKnob, nowOn);
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
        if (gameSoundOn.get()) {
            MusicManager.getInstance().switchBGM(genPath(selectedGameGen));
        }
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
    void onCardClick(MouseEvent e) {
        e.consume();
    }

    @FXML
    void onBackgroundClick(MouseEvent e) {
        if (e.getTarget() == overlayRoot)
            closeOverlay();
    }

    private void closeOverlay() {
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlayRoot);
        ft.setFromValue(overlayRoot.getOpacity());
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            if (overlayRoot.getParent() instanceof Pane parent)
                parent.getChildren().remove(overlayRoot);
        });
        ft.play();
    }
    private void bindSubSection(BooleanProperty prop, VBox subSection, Button... btns) {
        for (Button btn : btns)
            btn.disableProperty().bind(prop.not());
        prop.addListener((obs, o, on) -> {
            if (on)
                subSection.getStyleClass().remove("section-disabled");
            else if (!subSection.getStyleClass().contains("section-disabled"))
                subSection.getStyleClass().add("section-disabled");
        });
        if (!prop.get())
            subSection.getStyleClass().add("section-disabled");
    }

    private void animateToggle(StackPane track, StackPane knob, boolean on) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), knob);
        tt.setToX(on ? KNOB_TRAVEL : 0);
        tt.play();
        applyToggleStyle(track, knob, on);
    }

    private void applyToggleStyle(StackPane track, StackPane knob, boolean on) {
        track.getStyleClass().removeAll("toggle-on", "toggle-off");
        track.getStyleClass().add(on ? "toggle-on" : "toggle-off");
        knob.setTranslateX(on ? KNOB_TRAVEL : 0);
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
        } else
            btn.getStyleClass().remove("option-selected");
    }
    private int parseGen(Button btn) {
        try {
            return Integer.parseInt((String) btn.getUserData());
        } catch (Exception ex) {
            return 1;
        }
    }
    private String genPath(int gen) {
        return switch (gen) {
            case 2 -> "/com/example/pokemonbattle/audio/gen2.mp3";
            case 3 -> "/com/example/pokemonbattle/audio/gen3.mp3";
            default -> "/com/example/pokemonbattle/audio/gen1.mp3";
        };
    }

    private void updateVolumeLabel(double v) {
        if (volumePercentLabel != null)
            volumePercentLabel.setText(Math.round(v * 100) + "%");
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