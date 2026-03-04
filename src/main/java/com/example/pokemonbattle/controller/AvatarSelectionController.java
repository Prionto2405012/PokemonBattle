package com.example.pokemonbattle.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

/**
 * Controller for the Avatar Selection overlay.
 * Implements a simulated 3D rotating carousel (ring of cards)
 * with smooth animations, glassmorphic overlay, and animated action buttons.
 */
public class AvatarSelectionController {

    @FXML private StackPane overlayRoot;
    @FXML private StackPane carouselPane;
    @FXML private Button filterAllBtn;
    @FXML private Button filterMaleBtn;
    @FXML private Button filterFemaleBtn;
    @FXML private Button leftArrowBtn;
    @FXML private Button rightArrowBtn;
    @FXML private Label avatarInfoLabel;
    @FXML private HBox actionButtonsBox;

    // -- Avatar data ------------------------------------------------------

    private record AvatarInfo(String resourcePath, String gender, int index) {
        String displayName() {
            return gender.substring(0, 1).toUpperCase() + gender.substring(1)
                    + " Trainer " + index;
        }
    }

    private final List<AvatarInfo> allAvatars = new ArrayList<>();
    private List<AvatarInfo> filteredAvatars = new ArrayList<>();
    private final List<VBox> carouselCards = new ArrayList<>();
    private int currentIndex = 0;
    private String activeFilter = "ALL";
    private boolean animating = false;

    // Callback invoked after avatar selection; receives [resourcePath, gender]
    private Consumer<String[]> onAvatarSelected;

    // -- 3D Carousel constants --------------------------------------------

    private static final double RADIUS       = 310;
    private static final double CARD_SIZE    = 210;
    private static final double ANIM_MS      = 900;

    // Animated angle property drives the carousel ring orientation
    private final DoubleProperty carouselAngle = new SimpleDoubleProperty(0);

    // -- Initialisation ---------------------------------------------------

    @FXML
    public void initialize() {
        buildAvatarList();
        filteredAvatars = new ArrayList<>(allAvatars);
        currentIndex = 0;

        // Whenever angle changes, reposition all cards
        carouselAngle.addListener((obs, oldVal, newVal) ->
                updateCardPositions(newVal.doubleValue()));

        buildCarouselCards();
        updateCardPositions(0);
        updateInfoLabel();
        buildActionButtons();
        MusicManager.getInstance().attachClickSounds(overlayRoot);
    }

    /**
     * Set callback for when the user confirms an avatar.
     * args[0] = resourcePath, args[1] = gender
     */
    public void setOnAvatarSelected(Consumer<String[]> callback) {
        this.onAvatarSelected = callback;
    }

    private void buildAvatarList() {
        allAvatars.clear();
        String base = "/com/example/pokemonbattle/sprites/trainer/";
        for (int i = 1; i <= 4; i++) {
            allAvatars.add(new AvatarInfo(base + "male/" + i + ".png", "male", i));
        }
        for (int i = 1; i <= 4; i++) {
            allAvatars.add(new AvatarInfo(base + "female/" + i + ".png", "female", i));
        }
    }

    // -- Filters ----------------------------------------------------------

    @FXML void onFilterAll(ActionEvent e)    { applyFilter("ALL"); }
    @FXML void onFilterMale(ActionEvent e)   { applyFilter("MALE"); }
    @FXML void onFilterFemale(ActionEvent e) { applyFilter("FEMALE"); }

    private void applyFilter(String filter) {
        if (filter.equals(activeFilter)) return;
        activeFilter = filter;

        setFilterActive(filterAllBtn,    "ALL".equals(filter));
        setFilterActive(filterMaleBtn,   "MALE".equals(filter));
        setFilterActive(filterFemaleBtn, "FEMALE".equals(filter));

        filteredAvatars = allAvatars.stream()
                .filter(a -> switch (activeFilter) {
                    case "MALE"   -> "male".equals(a.gender());
                    case "FEMALE" -> "female".equals(a.gender());
                    default       -> true;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        currentIndex = 0;
        carouselAngle.set(0);

        // Smooth crossfade on filter change
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), carouselPane);
        fadeOut.setToValue(0.3);
        fadeOut.setOnFinished(ev -> {
            buildCarouselCards();
            updateCardPositions(0);
            updateInfoLabel();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), carouselPane);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void setFilterActive(Button btn, boolean active) {
        btn.getStyleClass().remove("filter-active");
        if (active) btn.getStyleClass().add("filter-active");
    }

    // -- 3D Carousel: build cards -----------------------------------------

    private void buildCarouselCards() {
        carouselPane.getChildren().removeIf(n -> n instanceof VBox);
        carouselCards.clear();

        for (AvatarInfo avatar : filteredAvatars) {
            VBox card = createAvatarCard(avatar);
            carouselCards.add(card);
            // Insert before the arrow buttons (arrows are last two children)
            int insertIdx = Math.max(0, carouselPane.getChildren().size() - 2);
            carouselPane.getChildren().add(insertIdx, card);
        }

        leftArrowBtn.toFront();
        rightArrowBtn.toFront();
    }

    private VBox createAvatarCard(AvatarInfo avatar) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("avatar-card-3d");
        card.setPrefWidth(CARD_SIZE + 20);
        card.setPrefHeight(CARD_SIZE + 30);
        card.setMaxWidth(CARD_SIZE + 20);
        card.setMaxHeight(CARD_SIZE + 30);

        ImageView iv = new ImageView();
        iv.setFitWidth(CARD_SIZE);
        iv.setFitHeight(CARD_SIZE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        try {
            var url = getClass().getResource(avatar.resourcePath());
            if (url != null) {
                iv.setImage(new Image(url.toExternalForm(),
                        CARD_SIZE, CARD_SIZE, true, true, true));
            }
        } catch (Exception e) {
            System.err.println("[AvatarSelection] Failed to load: " + avatar.resourcePath());
        }

        card.getChildren().add(iv);
        card.setMouseTransparent(true);
        return card;
    }

    // -- 3D Carousel: position cards around the ring ----------------------

    private void updateCardPositions(double angleOffset) {
        int n = carouselCards.size();
        if (n == 0) return;

        double angleStep = 360.0 / n;

        for (int i = 0; i < n; i++) {
            double angleDeg = i * angleStep + angleOffset;
            double angleRad = Math.toRadians(angleDeg);

            double x = RADIUS * Math.sin(angleRad);
            double z = RADIUS * Math.cos(angleRad);   // +R = front, -R = back

            // depthFactor: 0 = furthest back, 1 = closest front
            double depthFactor = Math.max(0, Math.min(1,
                    (z + RADIUS) / (2 * RADIUS)));

            VBox card = carouselCards.get(i);
            card.setTranslateX(x * 0.80);

            double scale = 0.32 + 0.68 * depthFactor;
            card.setScaleX(scale);
            card.setScaleY(scale);

            card.setOpacity(0.12 + 0.88 * Math.pow(depthFactor, 1.6));
            card.setViewOrder(1.0 - depthFactor);  // lower = on top

            // Glow on the front-most card
            if (depthFactor > 0.94) {
                DropShadow glow = new DropShadow();
                glow.setRadius(22);
                glow.setColor(Color.web("#42f5c2", 0.45));
                glow.setSpread(0.12);
                card.setEffect(glow);
            } else {
                card.setEffect(null);
            }
        }
    }

    // -- 3D Carousel: navigation ------------------------------------------

    @FXML void onCarouselLeft(ActionEvent e)  { navigate(-1); }
    @FXML void onCarouselRight(ActionEvent e) { navigate(+1); }

    private void navigate(int direction) {
        if (animating || filteredAvatars.size() <= 1) return;
        animating = true;

        int n = filteredAvatars.size();
        double angleStep = 360.0 / n;

        currentIndex = (currentIndex + direction + n) % n;
        double targetAngle = -currentIndex * angleStep;

        Timeline anim = new Timeline(
                new KeyFrame(Duration.millis(ANIM_MS),
                        new KeyValue(carouselAngle, targetAngle,
                                Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)))
        );
        anim.setOnFinished(e -> {
            animating = false;
            updateInfoLabel();
        });
        anim.play();
    }

    private void updateInfoLabel() {
        if (filteredAvatars.isEmpty()) {
            avatarInfoLabel.setText("No avatars available");
            return;
        }
        AvatarInfo center = filteredAvatars.get(currentIndex);
        avatarInfoLabel.setText(center.displayName());
    }

    // -- Action buttons (Cancel / Select) with underline animation --------

    private void buildActionButtons() {
        if (actionButtonsBox == null) return;
        actionButtonsBox.getChildren().clear();

        VBox cancelBtn = createAnimatedButton(
                "Cancel", Color.web("#e05555"), true, this::closeOverlay);
        VBox selectBtn = createAnimatedButton(
                "Choose", Color.web("#42f5ad"), false, this::doSelectAvatar);

        actionButtonsBox.getChildren().addAll(cancelBtn, selectBtn);
    }

    /**
     * Build an animated button matching the subscribe-style reference:
     * text + SVG arrow, with an underline that grows on hover and
     * text/arrow colour shift.
     *
     * @param text       button label
     * @param hoverColor colour for hover state (underline + text)
     * @param arrowLeft  true = arrow points left (cancel), false = right (select)
     * @param action     click handler
     */
    private VBox createAnimatedButton(String text, Color hoverColor,
                                       boolean arrowLeft, Runnable action) {
        // -- label --
        Label label = new Label(text);
        String baseLabelStyle = "-fx-font-family: 'SPACE NOVA'; -fx-font-size: 18px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d0e0e8;";
        label.setStyle(baseLabelStyle);

        // -- SVG arrow (right-pointing by default) --
        SVGPath arrow = new SVGPath();
        arrow.setContent("M14 5 L21 12 L14 19 M21 12 H3");
        arrow.setFill(Color.TRANSPARENT);
        arrow.setStroke(Color.web("#d0e0e8"));
        arrow.setStrokeWidth(2.8);
        arrow.setStrokeLineCap(StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(StrokeLineJoin.ROUND);

        double arrowScale = 0.62;
        arrow.setScaleX(arrowLeft ? -arrowScale : arrowScale);
        arrow.setScaleY(arrowScale);

        // -- content row --
        HBox content;
        if (arrowLeft) {
            content = new HBox(6, arrow, label);
        } else {
            content = new HBox(6, label, arrow);
        }
        content.setAlignment(Pos.CENTER);

        // -- underline --
        Rectangle underline = new Rectangle(0, 2.5);
        underline.setFill(Color.TRANSPARENT);
        underline.setArcWidth(2);
        underline.setArcHeight(2);

        VBox wrapper = new VBox(5, content, underline);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setCursor(Cursor.HAND);

        // Hex string of hover colour for inline CSS
        String hoverHex = String.format("#%02x%02x%02x",
                (int) (hoverColor.getRed() * 255),
                (int) (hoverColor.getGreen() * 255),
                (int) (hoverColor.getBlue() * 255));
        String hoverLabelStyle = "-fx-font-family: 'SPACE NOVA'; -fx-font-size: 18px; "
                + "-fx-font-weight: bold; -fx-text-fill: " + hoverHex + ";";

        // -- hover enter --
        wrapper.setOnMouseEntered(e -> {
            underline.setFill(hoverColor);
            label.setStyle(hoverLabelStyle);
            arrow.setStroke(hoverColor);

            Timeline tl = new Timeline(new KeyFrame(Duration.millis(300),
                    new KeyValue(underline.widthProperty(),
                            content.getBoundsInLocal().getWidth(),
                            Interpolator.EASE_OUT)));
            tl.play();

            TranslateTransition arrowAnim =
                    new TranslateTransition(Duration.millis(200), arrow);
            arrowAnim.setToX(arrowLeft ? -4 : 4);
            arrowAnim.play();
        });

        // -- hover exit --
        wrapper.setOnMouseExited(e -> {
            label.setStyle(baseLabelStyle);
            arrow.setStroke(Color.web("#d0e0e8"));

            Timeline tl = new Timeline(new KeyFrame(Duration.millis(300),
                    new KeyValue(underline.widthProperty(), 0,
                            Interpolator.EASE_OUT)));
            tl.setOnFinished(ev -> underline.setFill(Color.TRANSPARENT));
            tl.play();

            TranslateTransition arrowAnim =
                    new TranslateTransition(Duration.millis(200), arrow);
            arrowAnim.setToX(0);
            arrowAnim.play();
        });

        wrapper.setOnMouseClicked(e -> action.run());
        return wrapper;
    }

    // -- Selection --------------------------------------------------------

    private void doSelectAvatar() {
        if (filteredAvatars.isEmpty()) return;
        AvatarInfo selected = filteredAvatars.get(currentIndex);

        // Save to PlayerSession
        PlayerSession session = PlayerSession.getInstance();
        session.saveAvatar(selected.resourcePath(), selected.gender());

        System.out.println("[AvatarSelection] Selected: " + selected.displayName()
                + " -> " + selected.resourcePath());

        // Notify parent
        if (onAvatarSelected != null) {
            onAvatarSelected.accept(
                    new String[]{ selected.resourcePath(), selected.gender() });
        }

        closeOverlay();
    }

    @FXML
    void onSelectAvatar(ActionEvent e) {
        doSelectAvatar();
    }

    // -- Overlay lifecycle ------------------------------------------------

    @FXML void onCardClick(MouseEvent e)  { e.consume(); }

    @FXML void onCloseButtonClick(ActionEvent e) { closeOverlay(); }

    @FXML
    void onBackgroundClick(MouseEvent e) {
        if (e.getTarget() == overlayRoot) closeOverlay();
    }

    private void closeOverlay() {
        // Remove blur from all background siblings
        if (overlayRoot.getParent() instanceof Pane parent) {
            for (Node n : parent.getChildren()) {
                if (n != overlayRoot) n.setEffect(null);
            }
        }
        FadeTransition ft = new FadeTransition(Duration.millis(200), overlayRoot);
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
