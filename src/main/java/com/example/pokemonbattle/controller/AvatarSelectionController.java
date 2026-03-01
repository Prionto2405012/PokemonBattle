package com.example.pokemonbattle.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Controller for the Avatar Selection overlay.
 * Implements a simulated 3D carousel with smooth animations.
 * Follows the overlay pattern established by SettingsController.
 */
public class AvatarSelectionController {

    @FXML
    private StackPane overlayRoot;
    @FXML
    private StackPane carouselPane;
    @FXML
    private Button filterAllBtn;
    @FXML
    private Button filterMaleBtn;
    @FXML
    private Button filterFemaleBtn;
    @FXML
    private Button leftArrowBtn;
    @FXML
    private Button rightArrowBtn;
    @FXML
    private Button selectButton;
    @FXML
    private Label avatarInfoLabel;

    // ── Avatar data ─────────────────────────────────────────────

    private record AvatarInfo(String resourcePath, String gender, int index) {
        String displayName() {
            return gender.substring(0, 1).toUpperCase() + gender.substring(1)
                    + " Trainer " + index;
        }
    }

    private final List<AvatarInfo> allAvatars = new ArrayList<>();
    private List<AvatarInfo> filteredAvatars = new ArrayList<>();
    private int currentIndex = 0;
    private String activeFilter = "ALL";
    private boolean animating = false;

    // Callback invoked after avatar selection; receives the resource path
    private Consumer<String[]> onAvatarSelected;

    // ── Carousel layout constants ───────────────────────────────

    private static final int VISIBLE_SLOTS = 5; // center ± 2
    private static final double CENTER_SCALE = 1.0;
    private static final double SIDE1_SCALE = 0.72;
    private static final double SIDE2_SCALE = 0.50;
    private static final double CENTER_OPACITY = 1.0;
    private static final double SIDE1_OPACITY = 0.70;
    private static final double SIDE2_OPACITY = 0.35;
    private static final double SIDE1_TX = 175; // translateX for ±1
    private static final double SIDE2_TX = 300; // translateX for ±2
    private static final Duration ANIM_DURATION = Duration.millis(320);

    // ── Initialisation ──────────────────────────────────────────

    @FXML
    public void initialize() {
        buildAvatarList();
        filteredAvatars = new ArrayList<>(allAvatars);
        currentIndex = 0;
        renderCarousel(false);
        updateInfoLabel();
        MusicManager.getInstance().attachClickSounds(overlayRoot);
    }

    /**
     * Set callback for when the user confirms an avatar.
     * Called from the parent controller that shows this overlay.
     * args[0] = resourcePath, args[1] = gender
     */
    public void setOnAvatarSelected(Consumer<String[]> callback) {
        this.onAvatarSelected = callback;
    }

    private void buildAvatarList() {
        allAvatars.clear();
        String base = "/com/example/pokemonbattle/sprites/trainer/";
        for (int i = 1; i <= 4; i++) {
            allAvatars.add(new AvatarInfo(base + "male/" + i + ".jpg", "male", i));
        }
        for (int i = 1; i <= 4; i++) {
            allAvatars.add(new AvatarInfo(base + "female/" + i + ".jpg", "female", i));
        }
    }

    @FXML
    void onFilterAll(ActionEvent e) {
        applyFilter("ALL");
    }

    @FXML
    void onFilterMale(ActionEvent e) {
        applyFilter("MALE");
    }

    @FXML
    void onFilterFemale(ActionEvent e) {
        applyFilter("FEMALE");
    }

    private void applyFilter(String filter) {
        if (filter.equals(activeFilter))
            return;
        activeFilter = filter;

        // Update filter button styles
        setFilterActive(filterAllBtn, "ALL".equals(filter));
        setFilterActive(filterMaleBtn, "MALE".equals(filter));
        setFilterActive(filterFemaleBtn, "FEMALE".equals(filter));

        // Rebuild filtered list
        filteredAvatars = allAvatars.stream()
                .filter(a -> switch (activeFilter) {
                    case "MALE" -> "male".equals(a.gender());
                    case "FEMALE" -> "female".equals(a.gender());
                    default -> true;
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        currentIndex = 0;

        // Smooth crossfade when switching filter
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), carouselPane);
        fadeOut.setToValue(0.3);
        fadeOut.setOnFinished(ev -> {
            renderCarousel(false);
            updateInfoLabel();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), carouselPane);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void setFilterActive(Button btn, boolean active) {
        btn.getStyleClass().remove("filter-active");
        if (active)
            btn.getStyleClass().add("filter-active");
    }

    // ── Carousel navigation ─────────────────────────────────────

    @FXML
    void onCarouselLeft(ActionEvent e) {
        navigate(-1);
    }

    @FXML
    void onCarouselRight(ActionEvent e) {
        navigate(+1);
    }

    private void navigate(int direction) {
        if (animating || filteredAvatars.size() <= 1)
            return;
        animating = true;

        int total = filteredAvatars.size();
        currentIndex = (currentIndex + direction + total) % total;

        animateCarouselTransition(direction, () -> {
            animating = false;
            updateInfoLabel();
        });
    }

    // ── Carousel rendering ──────────────────────────────────────

    /**
     * Render the carousel from scratch (no animation).
     */
    private void renderCarousel(boolean animated) {
        // Remove only avatar cards, keep arrows
        carouselPane.getChildren().removeIf(n -> n instanceof VBox || (n instanceof StackPane && n != overlayRoot));

        if (filteredAvatars.isEmpty())
            return;

        int total = filteredAvatars.size();
        int[] offsets = { -2, -1, 0, 1, 2 };

        for (int offset : offsets) {
            if (total < VISIBLE_SLOTS && Math.abs(offset) >= total)
                continue;

            int idx = ((currentIndex + offset) % total + total) % total;
            AvatarInfo avatar = filteredAvatars.get(idx);
            VBox card = createAvatarCard(avatar, offset);

            // Position properties
            double[] props = getSlotProperties(offset);
            card.setScaleX(props[0]);
            card.setScaleY(props[0]);
            card.setOpacity(props[1]);
            card.setTranslateX(props[2]);
            card.setViewOrder(Math.abs(offset)); // center on top

            carouselPane.getChildren().add(
                    carouselPane.getChildren().size() - 2, card); // before arrows

            if (animated) {
                card.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(250), card);
                ft.setToValue(props[1]);
                ft.play();
            }
        }

        // Ensure arrows are on top
        leftArrowBtn.toFront();
        rightArrowBtn.toFront();
    }

    /**
     * Animate a carousel step (direction = -1 or +1).
     * Moves existing cards to their new slot positions.
     */
    private void animateCarouselTransition(int direction, Runnable onDone) {
        // Rebuild the carousel with a smooth crossfade approach
        // Fade out old state
        List<javafx.scene.Node> oldCards = carouselPane.getChildren().stream()
                .filter(n -> n instanceof VBox)
                .toList();

        FadeTransition fadeOutAll = new FadeTransition(Duration.millis(140), carouselPane);
        fadeOutAll.setFromValue(1.0);
        fadeOutAll.setToValue(0.5);

        fadeOutAll.setOnFinished(e -> {
            renderCarousel(false);
            FadeTransition fadeInAll = new FadeTransition(Duration.millis(180), carouselPane);
            fadeInAll.setToValue(1.0);
            fadeInAll.setOnFinished(ev -> {
                if (onDone != null)
                    onDone.run();
            });
            fadeInAll.play();
        });
        fadeOutAll.play();
    }

    /**
     * Create a single avatar card (VBox with image + border).
     */
    private VBox createAvatarCard(AvatarInfo avatar, int offset) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("avatar-card");
        if (offset == 0)
            card.getStyleClass().add("avatar-card-center");

        // Calculate size based on slot
        double size = offset == 0 ? 180 : (Math.abs(offset) == 1 ? 140 : 100);

        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        try {
            var url = getClass().getResource(avatar.resourcePath());
            if (url != null) {
                iv.setImage(new Image(url.toExternalForm(), size, size, true, true, true));
            }
        } catch (Exception e) {
            System.err.println("[AvatarSelection] Failed to load: " + avatar.resourcePath());
        }

        // Subtle glow on center card
        if (offset == 0) {
            DropShadow glow = new DropShadow();
            glow.setRadius(18);
            glow.setColor(Color.web("#42f5c2", 0.45));
            glow.setSpread(0.1);
            iv.setEffect(glow);
        }

        card.getChildren().add(iv);
        card.setMouseTransparent(true); // let StackPane handle clicks
        card.setPrefWidth(size + 24);
        card.setPrefHeight(size + 24);
        card.setMaxWidth(size + 24);
        card.setMaxHeight(size + 24);

        return card;
    }

    /**
     * Returns [scale, opacity, translateX] for a given slot offset.
     */
    private double[] getSlotProperties(int offset) {
        return switch (Math.abs(offset)) {
            case 0 -> new double[] { CENTER_SCALE, CENTER_OPACITY, 0 };
            case 1 -> new double[] { SIDE1_SCALE, SIDE1_OPACITY, offset * SIDE1_TX };
            default -> new double[] { SIDE2_SCALE, SIDE2_OPACITY, offset * SIDE2_TX };
        };
    }

    private void updateInfoLabel() {
        if (filteredAvatars.isEmpty()) {
            avatarInfoLabel.setText("No avatars available");
            return;
        }
        AvatarInfo center = filteredAvatars.get(currentIndex);
        avatarInfoLabel.setText(center.displayName());
    }

    // ── Selection ───────────────────────────────────────────────

    @FXML
    void onSelectAvatar(ActionEvent e) {
        if (filteredAvatars.isEmpty())
            return;
        AvatarInfo selected = filteredAvatars.get(currentIndex);

        // Save to PlayerSession
        PlayerSession session = PlayerSession.getInstance();
        session.saveAvatar(selected.resourcePath(), selected.gender());

        System.out.println("[AvatarSelection] Selected: " + selected.displayName()
                + " → " + selected.resourcePath());

        // Notify parent
        if (onAvatarSelected != null) {
            onAvatarSelected.accept(new String[] { selected.resourcePath(), selected.gender() });
        }

        // Close overlay
        closeOverlay();
    }

    // ── Overlay lifecycle (same pattern as SettingsController) ──

    @FXML
    void onCardClick(MouseEvent e) {
        e.consume();
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

    private void closeOverlay() {
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
