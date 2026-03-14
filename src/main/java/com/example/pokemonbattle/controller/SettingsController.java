package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.util.MusicManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
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
        installPokeballKnob(gameSoundKnob);
        installPokeballKnob(battleSoundKnob);
        installPokeballKnob(animationKnob);
        bindSubSection(gameSoundOn, gameSoundSubSection, gameGen1Btn, gameGen2Btn, gameGen3Btn);
        bindSubSection(battleSoundOn, battleSoundSubSection, battleGen1Btn, battleGen2Btn, battleGen3Btn);
        applyToggleStyle(gameSoundToggle, gameSoundKnob, gameSoundOn.get());
        applyToggleStyle(battleSoundToggle, battleSoundKnob, battleSoundOn.get());
        applyToggleStyle(animationToggle, animationKnob, animationOn.get());
        markGenSelected(gameGen1Btn, gameGen2Btn, gameGen3Btn, selectedGameGen);
        markGenSelected(battleGen1Btn, battleGen2Btn, battleGen3Btn, selectedBattleGen);
        if (volumeSlider != null) {
            installPokeballSliderThumb(volumeSlider);
            installVolumeTrackFill(volumeSlider);
            volumeSlider.setValue(mm.getMasterVolume());
            updateVolumeLabel(mm.getMasterVolume());
            volumeSlider.valueProperty().addListener((obs, o, val) -> {
                mm.setMasterVolume(val.doubleValue());
                updateVolumeLabel(val.doubleValue());
                updateVolumeTrackFill(volumeSlider);
            });
            volumeSlider.disableProperty().bind(gameSoundOn.not());
        }
        if (mm.getCurrentTrack() == null && gameSoundOn.get()) {
            mm.playRandomBGM();
        }

        mm.attachClickSounds(overlayRoot);
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

        double tiltAngle = on ? 10.0 : -10.0;
        Timeline tilt = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(knob.rotateProperty(), 0.0, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(90),
                new KeyValue(knob.rotateProperty(), tiltAngle, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(180),
                new KeyValue(knob.rotateProperty(), 0.0, Interpolator.EASE_IN)));

        ParallelTransition toggleMotion = new ParallelTransition(tt, tilt);
        toggleMotion.play();
        applyToggleStyle(track, knob, on);
    }

    private void applyToggleStyle(StackPane track, StackPane knob, boolean on) {
        track.getStyleClass().removeAll("toggle-on", "toggle-off");
        track.getStyleClass().add(on ? "toggle-on" : "toggle-off");
        knob.setTranslateX(on ? KNOB_TRAVEL : 0);
        knob.setRotate(0);
    }

    private void installPokeballKnob(StackPane knob) {
        if (knob == null) {
            return;
        }
        knob.getChildren().setAll(createPokeballGraphic(18));
    }

    private void installPokeballSliderThumb(Slider slider) {
        if (slider == null) {
            return;
        }

        Runnable installThumbGraphic = () -> {
            Node thumb = slider.lookup(".thumb");
            if (thumb instanceof StackPane thumbPane) {
                if (!Boolean.TRUE.equals(thumbPane.getProperties().get("pokeballThumbInstalled"))) {
                    thumbPane.getChildren().setAll(createPokeballGraphic(16));
                    thumbPane.getProperties().put("pokeballThumbInstalled", Boolean.TRUE);
                }
            }
            updateVolumeTrackFill(slider);
        };

        Platform.runLater(installThumbGraphic);
        slider.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(installThumbGraphic));
        slider.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(installThumbGraphic);
            }
        });
    }

    private void installVolumeTrackFill(Slider slider) {
        if (slider == null) {
            return;
        }

        Runnable applyTrackFill = () -> updateVolumeTrackFill(slider);
        Platform.runLater(applyTrackFill);
        slider.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(applyTrackFill));
        slider.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(applyTrackFill);
            }
        });
    }

    private void updateVolumeTrackFill(Slider slider) {
        if (slider == null) {
            return;
        }

        Node track = slider.lookup(".track");
        if (track == null) {
            return;
        }

        double range = slider.getMax() - slider.getMin();
        double progress = range <= 0 ? 0 : (slider.getValue() - slider.getMin()) / range;
        double normalized = Math.max(0.0, Math.min(1.0, progress));
        double pct = normalized * 100.0;
        String stop = String.format(java.util.Locale.US, "%.2f%%", pct);

        Color baseFill = getInterpolatedVolumeColor(normalized);
        Color brightFill = baseFill.interpolate(Color.WHITE, 0.18);
        String baseFillCss = toRgbaCss(baseFill, 0.96);
        String brightFillCss = toRgbaCss(brightFill, 0.96);

        track.setStyle(
                "-fx-background-color: linear-gradient(to right, " +
                        brightFillCss + " 0%, " + baseFillCss + " " + stop + ", " +
                        "rgba(30,70,60,0.9) " + stop + ", rgba(30,70,60,0.9) 100%);" +
                        "-fx-background-radius: 4;" +
                        "-fx-pref-height: 5px;" +
                        "-fx-border-color: rgba(80,160,140,0.3);" +
                        "-fx-border-radius: 4;" +
                        "-fx-border-width: 1;");
    }

    private Color getInterpolatedVolumeColor(double normalized) {
        // Smoothly blend low -> mid -> high colors as slider value changes.
        Color low = Color.web("#5e8bff");
        Color mid = Color.web("#42f5ef");
        Color high = Color.web("#56e37b");

        if (normalized <= 0.5) {
            return low.interpolate(mid, normalized / 0.5);
        }
        return mid.interpolate(high, (normalized - 0.5) / 0.5);
    }

    private String toRgbaCss(Color color, double alpha) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format(java.util.Locale.US, "rgba(%d,%d,%d,%.3f)", r, g, b, alpha);
    }

    private Pane createPokeballGraphic(double size) {
        double r = size / 2.0;

        Arc top = new Arc(r, r, r, r, 0, 180);
        top.setType(ArcType.CHORD);
        top.setFill(Color.web("#f93318"));
        top.setStroke(Color.TRANSPARENT);

        Arc bottom = new Arc(r, r, r, r, 180, 180);
        bottom.setType(ArcType.CHORD);
        bottom.setFill(Color.WHITE);
        bottom.setStroke(Color.TRANSPARENT);

        Circle ring = new Circle(r, r, r);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.BLACK);
        ring.setStrokeWidth(1.8);

        Line divider = new Line(0, r, size, r);
        divider.setStroke(Color.BLACK);
        divider.setStrokeWidth(1.8);

        Circle btnOuter = new Circle(r, r, size * 0.18);
        btnOuter.setFill(Color.BLACK);

        Circle btnBorder = new Circle(r, r, size * 0.13);
        btnBorder.setFill(Color.WHITE);

        Circle btnInner = new Circle(r, r, size * 0.09);
        btnInner.setFill(Color.web("#7f8c8d"));

        Pane ball = new Pane();
        ball.setPrefSize(size, size);
        ball.setMinSize(size, size);
        ball.setMaxSize(size, size);
        ball.setMouseTransparent(true);
        ball.getChildren().addAll(top, bottom, ring, divider, btnOuter, btnBorder, btnInner);
        return ball;
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