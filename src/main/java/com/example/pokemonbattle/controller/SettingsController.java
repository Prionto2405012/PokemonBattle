package com.example.pokemonbattle.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class SettingsController implements Initializable {

    // ── Overlay root ─────────────────────────────────────────────────────────
    @FXML private StackPane overlayRoot;
    @FXML private Button    closeButton;
    @FXML private Button    closeXButton;

    // ── Game Sound ────────────────────────────────────────────────────────────
    @FXML private StackPane gameSoundToggle;
    @FXML private Region    gameSoundTrack;
    @FXML private Region    gameSoundKnob;
    @FXML private VBox      gameSoundSubSection;
    @FXML private Label     gameSoundGen1;
    @FXML private Label     gameSoundGen2;
    @FXML private Label     gameSoundGen3;

    // ── Battle Sound ──────────────────────────────────────────────────────────
    @FXML private StackPane battleSoundToggle;
    @FXML private Region    battleSoundTrack;
    @FXML private Region    battleSoundKnob;
    @FXML private VBox      battleSoundSubSection;
    @FXML private Label     battleSoundGen1;
    @FXML private Label     battleSoundGen2;
    @FXML private Label     battleSoundGen3;

    // ── Show Move Animation ───────────────────────────────────────────────────
    @FXML private StackPane animationToggle;
    @FXML private Region    animationTrack;
    @FXML private Region    animationKnob;

    // ── Language ──────────────────────────────────────────────────────────────
    @FXML private Label langEnglish;
    @FXML private Label langJapanese;

    // ── State properties ──────────────────────────────────────────────────────
    private final BooleanProperty gameSoundEnabled    = new SimpleBooleanProperty(false);
    private final BooleanProperty battleSoundEnabled  = new SimpleBooleanProperty(false);
    private final BooleanProperty showAnimation       = new SimpleBooleanProperty(true);

    // ── Toggle knob travel ────────────────────────────────────────────────────
    /** translateX of the knob in OFF position (left gap). */
    private static final double KNOB_OFF_X = 3.0;
    /** translateX of the knob in ON  position (track width - knob width - gap). */
    private static final double KNOB_ON_X  = 25.0;   // 48 - 20 - 3

    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set initial toggle and sub-section states
        applyToggleVisual(gameSoundToggle,   gameSoundKnob,   gameSoundEnabled.get());
        applyToggleVisual(battleSoundToggle, battleSoundKnob, battleSoundEnabled.get());
        applyToggleVisual(animationToggle,   animationKnob,   showAnimation.get());

        applySubSectionState(gameSoundSubSection,   gameSoundEnabled.get());
        applySubSectionState(battleSoundSubSection, battleSoundEnabled.get());

        // Default selections
        selectOption(gameSoundGen1,   gameSoundGen2,   gameSoundGen3);
        selectOption(battleSoundGen1, battleSoundGen2, battleSoundGen3);
        selectOption(langEnglish,     langJapanese);
    }

    // ═════════════════════════════════════ FXML handlers ═════════════════════

    // ── Close ─────────────────────────────────────────────────────────────────
    @FXML protected void onCloseButtonClick()            { closeOverlay(); }
    @FXML protected void onCardClick(MouseEvent e)       { e.consume(); }
    @FXML protected void onBackgroundClick(MouseEvent e) { closeOverlay(); }

    // ── Game Sound toggle ─────────────────────────────────────────────────────
    @FXML
    protected void onGameSoundToggle() {
        boolean next = !gameSoundEnabled.get();
        gameSoundEnabled.set(next);
        animateToggle(gameSoundToggle, gameSoundKnob, next);
        fadeSubSection(gameSoundSubSection, next);
    }

    // ── Battle Sound toggle ───────────────────────────────────────────────────
    @FXML
    protected void onBattleSoundToggle() {
        boolean next = !battleSoundEnabled.get();
        battleSoundEnabled.set(next);
        animateToggle(battleSoundToggle, battleSoundKnob, next);
        fadeSubSection(battleSoundSubSection, next);
    }

    // ── Show Move Animation toggle ────────────────────────────────────────────
    @FXML
    protected void onAnimationToggle() {
        boolean next = !showAnimation.get();
        showAnimation.set(next);
        animateToggle(animationToggle, animationKnob, next);
    }

    // ── Game Sound option buttons ─────────────────────────────────────────────
    @FXML protected void onGameSoundGen1() { if (gameSoundEnabled.get()) selectOption(gameSoundGen1, gameSoundGen2, gameSoundGen3); }
    @FXML protected void onGameSoundGen2() { if (gameSoundEnabled.get()) selectOption(gameSoundGen2, gameSoundGen1, gameSoundGen3); }
    @FXML protected void onGameSoundGen3() { if (gameSoundEnabled.get()) selectOption(gameSoundGen3, gameSoundGen1, gameSoundGen2); }

    // ── Battle Sound option buttons ───────────────────────────────────────────
    @FXML protected void onBattleSoundGen1() { if (battleSoundEnabled.get()) selectOption(battleSoundGen1, battleSoundGen2, battleSoundGen3); }
    @FXML protected void onBattleSoundGen2() { if (battleSoundEnabled.get()) selectOption(battleSoundGen2, battleSoundGen1, battleSoundGen3); }
    @FXML protected void onBattleSoundGen3() { if (battleSoundEnabled.get()) selectOption(battleSoundGen3, battleSoundGen1, battleSoundGen2); }

    // ── Language ──────────────────────────────────────────────────────────────
    @FXML protected void onLangEnglish()  { selectOption(langEnglish,  langJapanese); }
    @FXML protected void onLangJapanese() { selectOption(langJapanese, langEnglish); }

    // ═══════════════════════════════════ Reusable helpers ════════════════════

    /**
     * Immediately applies CSS visual state to a toggle (no animation).
     * Used on initialization.
     */
    private void applyToggleVisual(StackPane toggle, Region knob, boolean on) {
        knob.setTranslateX(on ? KNOB_ON_X : KNOB_OFF_X);
        setStyleClass(toggle, "toggle-on", on);
    }

    /**
     * Animates the toggle knob from its current position to the target side,
     * and swaps the .toggle-on style class on the parent StackPane.
     */
    private void animateToggle(StackPane toggle, Region knob, boolean on) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(160), knob);
        tt.setToX(on ? KNOB_ON_X : KNOB_OFF_X);
        tt.play();
        setStyleClass(toggle, "toggle-on", on);
    }
    private void applySubSectionState(VBox subSection, boolean enabled) {
        subSection.setDisable(!enabled);
        setStyleClass(subSection, "section-disabled", !enabled);
    }
    private void fadeSubSection(VBox subSection, boolean nowEnabled) {
        if (!nowEnabled) {
            subSection.setDisable(true);
        } else {
            subSection.setDisable(false);
        }

        double targetOpacity = nowEnabled ? 1.0 : 0.35;
        FadeTransition ft = new FadeTransition(Duration.millis(220), subSection);
        ft.setFromValue(subSection.getOpacity());
        ft.setToValue(targetOpacity);
        ft.setOnFinished(e -> setStyleClass(subSection, "section-disabled", !nowEnabled));
        ft.play();
    }
    private void selectOption(Label selected, Label... others) {
        selected.getStyleClass().add("option-selected");
        for (Label other : others) {
            other.getStyleClass().remove("option-selected");
        }
    }
    private void setStyleClass(javafx.scene.Node node, String cls, boolean apply) {
        if (apply) {
            if (!node.getStyleClass().contains(cls)) {
                node.getStyleClass().add(cls);
            }
        } else {
            node.getStyleClass().remove(cls);
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
    public BooleanProperty gameSoundEnabledProperty()   { return gameSoundEnabled; }
    public BooleanProperty battleSoundEnabledProperty() { return battleSoundEnabled; }
    public BooleanProperty showAnimationProperty()       { return showAnimation; }
}
