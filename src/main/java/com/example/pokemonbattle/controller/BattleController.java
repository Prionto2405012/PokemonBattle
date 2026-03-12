package com.example.pokemonbattle.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.pokemonbattle.database.DatabaseManager;
import com.example.pokemonbattle.model.Battle;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class BattleController implements Battle.BattleListener {

    // FXML injections
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
    @FXML
    private StackPane battleSection;
    @FXML
    private AnchorPane battleField;
    @FXML
    private StackPane optionsSection;
    @FXML
    private ImageView playerSpriteImage;
    @FXML
    private ImageView opponentSpriteImage;
    @FXML
    private Label playerPokemonNameLabel;
    @FXML
    private Label playerPokemonHpLabel;
    @FXML
    private Label opponentPokemonNameLabel;
    @FXML
    private Label opponentPokemonHpLabel;
    @FXML
    private Rectangle playerHpBar;
    @FXML
    private Rectangle opponentHpBar;
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label playerTeamLabel;
    @FXML
    private VBox playerPokemonBox;
    @FXML
    private Label opponentNameLabel;
    @FXML
    private Label opponentTeamLabel;
    @FXML
    private VBox opponentPokemonBox;
    @FXML
    private Label battleStatusLabel;
    @FXML
    private Button startBattleButton;
    @FXML
    private Button backButton;
    @FXML
    private Button attackButton;
    @FXML
    private Button changePokemonMainButton;
    @FXML
    private Button itemsButton;
    @FXML
    private VBox moveSelectionBox;
    @FXML
    private VBox moveButtonsContainer; // replaces the 4 individual move buttons
    @FXML
    private VBox pokemonSelectionBox;
    @FXML
    private VBox pokemonButtonsBox;
    @FXML
    private VBox actionButtonsBox;
    @FXML
    private HBox mainBattleLayout;
    @FXML
    private StackPane battleResultOverlay;
    @FXML
    private VBox battleResultCard;
    @FXML
    private Label resultTitleLabel;
    @FXML
    private Label resultMessageLabel;
    @FXML
    private Button goBackResultButton;
    @FXML
    private Button battleAgainResultButton;
    @FXML
    private AnchorPane vsScreenPane;
    @FXML
    private ImageView vsBgImage;
    @FXML
    private ImageView vsPlayerSprite;
    @FXML
    private ImageView vsOpponentSprite;
    @FXML
    private Region rootBlackFade;

    // Move list constants (customise size / position of the "i" button here)
    private static final double MOVE_BTN_HEIGHT = 50.0; // px height of each move row
    private static final double INFO_BTN_WIDTH = 18.0; // px width of the "i" button
    private static final double INFO_BTN_HEIGHT = 18.0; // px height of the "i" button
    private static final double INFO_BTN_INSET_TOP = 4.0; // top margin inside move btn
    private static final double INFO_BTN_INSET_RIGHT = 4.0; // right margin inside move btn

    // Battle model / state
    private Player player;
    private Player opponent;
    private Battle battle;
    private boolean randomTeam;
    private static final double HP_BAR_MAX_WIDTH = 180.0;

    // Move-button tracking
    private final List<Button> activeMoveButtons = new ArrayList<>();

    // Info overlay (floating layer in rootPane)
    private Pane infoFloatingLayer;
    private VBox infoCard;
    private Label infoName, infoType, infoPower, infoAccuracy, infoPp, infoDescription;

    // Confetti
    private Canvas confettiCanvas;
    private AnimationTimer confettiTimer;

    // Battle log
    private final List<String> battleLog = new ArrayList<>();

    // Sprite scaling constants
    private static final double SPRITE_STANDARD_HEIGHT_M = 1.0;
        private static final double SPRITE_OPPONENT_BASE_PX = 180.0;
        private static final double SPRITE_PLAYER_BASE_PX = 250.0;
        private static final double SPRITE_OPPONENT_MIN_PX = 90.0;
        private static final double SPRITE_OPPONENT_MAX_PX = 340.0;
        private static final double SPRITE_PLAYER_MIN_PX = 120.0;
        private static final double SPRITE_PLAYER_MAX_PX = 420.0;
    private static final double SPRITE_SCALE_EXPONENT = 0.75;
    private static final java.util.Map<Integer, Double> POKEMON_HEIGHTS = loadPokemonHeights();

    // VS Intro constants
    private static final double VS_SLIDE_STOP_OFFSET = 25.0;
    private static final double VS_DRIFT_AMOUNT = 35.0;
    private static final double VS_SLIDE_IN_MS = 320.0;
    private static final double VS_DRIFT_HOLD_MS = 2700.0;
    private static final double VS_SLIDE_OUT_EARLY_MS = 500.0;
    private static final double VS_SLIDE_OUT_MS = 280.0;
    private static final double VS_FADE_TO_BLACK_MS = 420.0;
    private static final double VS_FADE_FROM_BLACK_MS = 380.0;
    private static final double VS_OFFSCREEN_OFFSET = 700.0;
    private static final int VS_NPC_COUNT = 7;

    // Lifecycle

    @FXML
    public void initialize() {
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        if (vsBgImage != null && rootPane != null) {
            vsBgImage.fitWidthProperty().bind(rootPane.widthProperty());
            vsBgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        startBattleButton.setOnAction(e -> onBack());
        backButton.setOnAction(e -> onBack());
        attackButton.setOnAction(e -> onAttackClicked());
        changePokemonMainButton.setOnAction(e -> onChangePokemonClicked());
        if (itemsButton != null)
            itemsButton.setOnAction(e -> onItemsClicked());

        loadBattleData();
        drawOptionsPanelPattern();
        setupInfoOverlay();
        SceneManager.enableCoordDebug(rootPane);
        MusicManager.getInstance().attachClickSounds(rootPane);

        Platform.runLater(this::playVSIntro);
    }

    // Info overlay setup

    private void setupInfoOverlay() {
        infoName = styledLabel("move-info-title");
        infoType = styledLabel("move-info-stat move-info-type");
        infoPower = styledLabel("move-info-stat move-info-power");
        infoAccuracy = styledLabel("move-info-stat move-info-accuracy");
        infoPp = styledLabel("move-info-stat move-info-pp");
        infoDescription = styledLabel("move-info-description");
        infoDescription.setWrapText(true);
        infoDescription.setMaxWidth(260);

        infoCard = new VBox(4, infoName, infoType, infoPower, infoAccuracy, infoPp, infoDescription);
        infoCard.getStyleClass().add("move-info-overlay");
        infoCard.setVisible(false);
        infoCard.setManaged(false);
        infoCard.setMouseTransparent(true);

        infoFloatingLayer = new Pane(infoCard);
        infoFloatingLayer.setMouseTransparent(true);
        infoFloatingLayer.prefWidthProperty().bind(rootPane.widthProperty());
        infoFloatingLayer.prefHeightProperty().bind(rootPane.heightProperty());
        rootPane.getChildren().add(infoFloatingLayer);
    }

    private Label styledLabel(String styleClasses) {
        Label l = new Label();
        l.getStyleClass().addAll(styleClasses.split("\\s+"));
        return l;
    }

    private void showInfoOverlay(Button iBtn, Move move, int currentPp) {
        infoName.setText(capitalize(move.getName()));
        String type = (move.getType() != null) ? move.getType() : "normal";
        infoType.setText("Type: " + capitalize(type));
        int pow = move.getPower();
        infoPower.setText("Power: " + (pow > 0 ? String.valueOf(pow) : "—"));
        int acc = move.getAccuracy();
        infoAccuracy.setText("Accuracy: " + (acc > 0 ? acc + "%" : "—"));
        int maxPp = move.getPp() > 0 ? move.getPp() : currentPp;
        infoPp.setText("PP: " + currentPp + " / " + maxPp);
        String desc = move.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "No description available.";
        }
        int splitAt = desc.indexOf("\n\n");
        if (splitAt >= 0 && splitAt + 2 < desc.length()) {
            desc = desc.substring(splitAt + 2).trim();
        }
        infoDescription.setText(desc);

        infoCard.setVisible(true);
        infoFloatingLayer.toFront();
        infoCard.toFront();

        Platform.runLater(() -> {
            infoCard.applyCss();
            infoCard.autosize();
            Bounds b = iBtn.localToScene(iBtn.getBoundsInLocal());
            Bounds r = rootPane.localToScene(rootPane.getBoundsInLocal());
            double cardW = infoCard.getWidth() > 10 ? infoCard.getWidth() : infoCard.prefWidth(-1);
            double cardH = infoCard.getHeight() > 10 ? infoCard.getHeight() : infoCard.prefHeight(-1);
            if (cardW < 160) cardW = 220;
            if (cardH < 120) cardH = 170;
            double x = (b.getMinX() - r.getMinX()) - cardW - 10;
            double y = (b.getMinY() - r.getMinY()) - cardH / 2.0 + iBtn.getHeight() / 2.0;
            x = Math.max(4, x);
            y = Math.max(4, Math.min(y, rootPane.getHeight() - cardH - 4));
            infoCard.setLayoutX(x);
            infoCard.setLayoutY(y);
        });
    }

    private void hideInfoOverlay() {
        if (infoCard != null)
            infoCard.setVisible(false);
    }

    // VS Intro

    private void playVSIntro() {
        int npcId = new Random().nextInt(VS_NPC_COUNT) + 1;
        String npcPath = "/com/example/pokemonbattle/sprites/trainer/npc/" + npcId + ".png";
        var npcUrl = getClass().getResource(npcPath);
        if (npcUrl != null)
            vsOpponentSprite.setImage(new Image(npcUrl.toExternalForm(), 0, 0, true, true));

        String avatarPath = PlayerSession.getInstance().getAvatarPath();
        if (avatarPath != null) {
            var avatarUrl = getClass().getResource(avatarPath);
            if (avatarUrl != null)
                vsPlayerSprite.setImage(new Image(avatarUrl.toExternalForm(), 0, 0, true, true));
        }

        vsPlayerSprite.setTranslateX(-VS_OFFSCREEN_OFFSET);
        vsOpponentSprite.setTranslateX(VS_OFFSCREEN_OFFSET);

        TranslateTransition playerSlideIn = new TranslateTransition(Duration.millis(VS_SLIDE_IN_MS), vsPlayerSprite);
        playerSlideIn.setToX(VS_SLIDE_STOP_OFFSET);
        playerSlideIn.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition opponentSlideIn = new TranslateTransition(Duration.millis(VS_SLIDE_IN_MS),
                vsOpponentSprite);
        opponentSlideIn.setToX(-VS_SLIDE_STOP_OFFSET);
        opponentSlideIn.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition slideIn = new ParallelTransition(playerSlideIn, opponentSlideIn);

        double driftMs = VS_DRIFT_HOLD_MS - VS_SLIDE_OUT_EARLY_MS;
        TranslateTransition playerDrift = new TranslateTransition(Duration.millis(driftMs), vsPlayerSprite);
        playerDrift.setByX(VS_DRIFT_AMOUNT);
        playerDrift.setInterpolator(Interpolator.LINEAR);
        TranslateTransition opponentDrift = new TranslateTransition(Duration.millis(driftMs), vsOpponentSprite);
        opponentDrift.setByX(-VS_DRIFT_AMOUNT);
        opponentDrift.setInterpolator(Interpolator.LINEAR);
        ParallelTransition drift = new ParallelTransition(playerDrift, opponentDrift);

        TranslateTransition playerSlideOut = new TranslateTransition(Duration.millis(VS_SLIDE_OUT_MS), vsPlayerSprite);
        playerSlideOut.setByX(VS_OFFSCREEN_OFFSET * 2.2);
        playerSlideOut.setInterpolator(Interpolator.EASE_IN);
        TranslateTransition opponentSlideOut = new TranslateTransition(Duration.millis(VS_SLIDE_OUT_MS),
                vsOpponentSprite);
        opponentSlideOut.setByX(-VS_OFFSCREEN_OFFSET * 2.2);
        opponentSlideOut.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition slideOut = new ParallelTransition(playerSlideOut, opponentSlideOut);

        SequentialTransition vsSequence = new SequentialTransition(slideIn, drift, slideOut);
        vsSequence.setOnFinished(e -> fadeToBlackAndRevealBattle());
        vsSequence.play();
    }

    private void fadeToBlackAndRevealBattle() {
        rootBlackFade.setOpacity(0.0);
        rootBlackFade.setVisible(true);
        FadeTransition fadeToBlack = new FadeTransition(Duration.millis(VS_FADE_TO_BLACK_MS), rootBlackFade);
        fadeToBlack.setToValue(1.0);
        fadeToBlack.setOnFinished(e -> {
            vsScreenPane.setVisible(false);
            vsScreenPane.setManaged(false);
            mainBattleLayout.setVisible(true);
            mainBattleLayout.setManaged(true);
            onStartBattle();
            FadeTransition fadeFromBlack = new FadeTransition(Duration.millis(VS_FADE_FROM_BLACK_MS), rootBlackFade);
            fadeFromBlack.setToValue(0.0);
            fadeFromBlack.setOnFinished(ev -> rootBlackFade.setVisible(false));
            fadeFromBlack.play();
        });
        fadeToBlack.play();
    }

    // Options panel background

    private void drawOptionsPanelPattern() {
        if (optionsSection == null)
            return;
        optionsSection.widthProperty().addListener((obs, o, n) -> repaintPattern());
        optionsSection.heightProperty().addListener((obs, o, n) -> repaintPattern());
        repaintPattern();
    }

    private void repaintPattern() {
        double w = optionsSection.getWidth(), h = optionsSection.getHeight();
        if (w <= 0 || h <= 0)
            return;
        optionsSection.getChildren().removeIf(n -> "patternCanvas".equals(n.getId()));
        Canvas canvas = new Canvas(w, h);
        canvas.setId("patternCanvas");
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(new javafx.scene.paint.LinearGradient(0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#5cbdb0")),
                new javafx.scene.paint.Stop(0.5, Color.web("#3a9e8f")),
                new javafx.scene.paint.Stop(1, Color.web("#2d8a7c"))));
        gc.fillRect(0, 0, w, h);
        gc.setStroke(Color.color(1, 1, 1, 0.08));
        gc.setLineWidth(12);
        for (double i = -h; i < w + h; i += 36)
            gc.strokeLine(i, 0, i + h, h);
        gc.setStroke(Color.color(1, 1, 1, 0.06));
        gc.setLineWidth(2);
        double sp = 80;
        for (double y = sp / 2; y < h; y += sp)
            for (double x = sp / 2; x < w; x += sp) {
                gc.strokeOval(x - 14, y - 14, 28, 28);
                gc.strokeLine(x - 14, y, x + 14, y);
            }
        optionsSection.getChildren().addFirst(canvas);
    }

    // Battle data loading

    private void loadBattleData() {
        player = (Player) SceneManager.getData("player");
        opponent = (Player) SceneManager.getData("opponent");
        Object rt = SceneManager.getData("randomTeam");
        randomTeam = (rt instanceof Boolean b) && b;

        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Battle data not found!");
            return;
        }

        playerNameLabel.setText(player.getName());
        displayPlayerTeam();
        opponentNameLabel.setText(opponent.getName());
        displayOpponentTeam();
        battleStatusLabel.setText("Teams loaded! Ready to battle.");
    }

    private void displayPlayerTeam() {
        playerPokemonBox.getChildren().clear();
        StringBuilder sb = new StringBuilder("Team Size: " + player.getTeam().size() + "\n");
        for (PokemonInstance p : player.getTeam())
            sb.append("• ").append(capitalize(p.getName())).append(" (Lv.").append(p.getLevel()).append(")\n");
        playerTeamLabel.setText(sb.toString().trim());
    }

    private void displayOpponentTeam() {
        opponentPokemonBox.getChildren().clear();
        StringBuilder sb = new StringBuilder("Team Size: " + opponent.getTeam().size() + "\n");
        for (PokemonInstance p : opponent.getTeam())
            sb.append("• ").append(capitalize(p.getName())).append(" (Lv.").append(p.getLevel()).append(")\n");
        opponentTeamLabel.setText(sb.toString().trim());
    }

    // Battle start & display

    private void onStartBattle() {
        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Teams not loaded!");
            return;
        }
        battle = new Battle(player, opponent);
        battle.addListener(this);
        updateBattleDisplay();
        showActionButtons();
        battleStatusLabel.setText("Battle Started! " +
                capitalize(player.getCurrentPokemon().getName()) + " vs " +
                capitalize(opponent.getCurrentPokemon().getName()));
    }

    private void updateBattleDisplay() {
        if (player == null || opponent == null)
            return;
        PokemonInstance pp = player.getCurrentPokemon(), op = opponent.getCurrentPokemon();

        double playerPx = getScaledSpritePx(pp.getId(), SPRITE_PLAYER_BASE_PX, SPRITE_PLAYER_MIN_PX, SPRITE_PLAYER_MAX_PX);
        double opponentPx = getScaledSpritePx(op.getId(), SPRITE_OPPONENT_BASE_PX, SPRITE_OPPONENT_MIN_PX,
            SPRITE_OPPONENT_MAX_PX);
        playerSpriteImage.setFitWidth(playerPx);
        playerSpriteImage.setFitHeight(playerPx);
        opponentSpriteImage.setFitWidth(opponentPx);
        opponentSpriteImage.setFitHeight(opponentPx);
        loadSpriteWithFallback(playerSpriteImage, pp.getId(), "back");
        loadSpriteWithFallback(opponentSpriteImage, op.getId(), "front");

        playerPokemonNameLabel.setText(capitalize(pp.getName()) + "  Lv." + pp.getLevel());
        playerPokemonHpLabel.setText(pp.getCurrentHp() + " / " + pp.getMaxHp());
        opponentPokemonNameLabel.setText(capitalize(op.getName()) + "  Lv." + op.getLevel());
        opponentPokemonHpLabel.setText(op.getCurrentHp() + " / " + op.getMaxHp());
        updateHpBar(playerHpBar, pp.getCurrentHp(), pp.getMaxHp());
        updateHpBar(opponentHpBar, op.getCurrentHp(), op.getMaxHp());
    }

    // Move buttons (list layout)

    private void updateMoveButtons() {
        activeMoveButtons.clear();
        moveButtonsContainer.getChildren().clear();

        PokemonInstance cur = player.getCurrentPokemon();
        var moves = cur.getBattleMoves();
        for (int i = 0; i < 4; i++) {
            if (i < moves.size()) {
                Move m = moves.get(i).getMove();
                int pp = moves.get(i).getCurrentPp();
                moveButtonsContainer.getChildren().add(createMoveRow(m, pp));
            }
        }
    }

    /**
     * Builds one move row: a coloured list button + type/PP overlay + "i" info
     * button.
     */
    private HBox createMoveRow(Move move, int currentPp) {
        String type = (move.getType() != null) ? move.getType() : "normal";
        int maxPp = move.getPp() > 0 ? move.getPp() : currentPp;
        String grad = getTypeGradient(type);
        String border = getTypeBorderColor(type);

        // Main move button (fills full row width)
        Button moveBtn = new Button(capitalize(move.getName()));
        moveBtn.setMaxWidth(Double.MAX_VALUE);
        moveBtn.setPrefHeight(MOVE_BTN_HEIGHT);
        moveBtn.setStyle(
                "-fx-background-color:" + grad + ";" +
                        "-fx-background-radius:10;-fx-border-color:" + border + ";" +
                        "-fx-border-radius:10;-fx-border-width:2;-fx-text-fill:white;" +
                        "-fx-font-family:'SPACE NOVA';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-cursor:hand;-fx-alignment:center-left;-fx-padding:0 0 0 14;");
        moveBtn.setOnAction(e -> onMoveSelected(move));
        activeMoveButtons.add(moveBtn);

        // Type + PP labels (right side, mouse-transparent overlay)
        Label typeLabel = new Label(capitalize(type));
        typeLabel.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.75);-fx-font-style:italic;");
        Label ppLabel = new Label("PP: " + currentPp + "/" + maxPp);
        ppLabel.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.90);");
        VBox rightInfo = new VBox(2, typeLabel, ppLabel);
        rightInfo.setAlignment(Pos.CENTER_RIGHT);
        rightInfo.setMouseTransparent(true);
        StackPane.setAlignment(rightInfo, Pos.CENTER_RIGHT);
        StackPane.setMargin(rightInfo, new Insets(0, INFO_BTN_WIDTH + INFO_BTN_INSET_RIGHT + 6, 0, 0));

        // "i" info button — light-blue gradient, top-right corner
        Button iBtn = new Button("i");
        iBtn.getStyleClass().add("move-info-btn");
        iBtn.setStyle(iBtn.getStyle() +
                "-fx-min-width:" + INFO_BTN_WIDTH + ";-fx-max-width:" + INFO_BTN_WIDTH + ";" +
                "-fx-min-height:" + INFO_BTN_HEIGHT + ";-fx-max-height:" + INFO_BTN_HEIGHT + ";");
        StackPane.setAlignment(iBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(iBtn, new Insets(INFO_BTN_INSET_TOP, INFO_BTN_INSET_RIGHT, 0, 0));
        final int finalPp = currentPp;
        iBtn.setOnMouseEntered(e -> showInfoOverlay(iBtn, move, finalPp));
        iBtn.setOnMousePressed(e -> showInfoOverlay(iBtn, move, finalPp));
        iBtn.setOnMouseExited(e -> hideInfoOverlay());
        iBtn.setOnAction(e -> {
        }); // consume click — don't trigger move

        StackPane wrapper = new StackPane(moveBtn, rightInfo, iBtn);
        HBox.setHgrow(wrapper, Priority.ALWAYS);

        HBox row = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return row;
    }

    private void disableMoveButtons() {
        for (Button b : activeMoveButtons)
            b.setDisable(true);
        hideInfoOverlay();
    }

    private void onMoveSelected(Move move) {
        if (battle.isFinished()) {
            battleStatusLabel.setText("Battle is already finished!");
            return;
        }
        battleStatusLabel.setText("Used " + capitalize(move.getName()) + "!");
        disableMoveButtons();

        Move aiMove = battle.getAIMove(opponent.getCurrentPokemon(), player.getCurrentPokemon());
        battle.executeRound(move, aiMove);
        updateBattleDisplay();
        updateMoveButtons();
        if (!battle.isFinished())
            showActionButtons();
    }

    // Type colour helpers

    private String getTypeGradient(String type) {
        if (type == null)
            return "linear-gradient(to bottom,#546e7a,#37474f)";
        return switch (type.toLowerCase()) {
            case "normal" -> "linear-gradient(to bottom,#b5c1d4,#bdd9e1)";
            case "fire" -> "linear-gradient(to bottom,#F08030,#A84820)";
            case "water" -> "linear-gradient(to bottom,#6890F0,#3860C0)";
            case "electric" -> "linear-gradient(to bottom,#C8A800,#906800)";
            case "grass" -> "linear-gradient(to bottom,#78C850,#489820)";
            case "ice" -> "linear-gradient(to bottom,#68B8B8,#3888A0)";
            case "fighting" -> "linear-gradient(to bottom,#C03028,#801010)";
            case "poison" -> "linear-gradient(to bottom,#A040A0,#702070)";
            case "ground" -> "linear-gradient(to bottom,#B89838,#806818)";
            case "flying" -> "linear-gradient(to bottom,#7868C0,#584890)";
            case "psychic" -> "linear-gradient(to bottom,#F85888,#A81040)";
            case "bug" -> "linear-gradient(to bottom,#788800,#506000)";
            case "rock" -> "linear-gradient(to bottom,#B8A038,#887010)";
            case "ghost" -> "linear-gradient(to bottom,#705898,#402870)";
            case "dragon" -> "linear-gradient(to bottom,#7038F8,#4008C8)";
            case "dark" -> "linear-gradient(to bottom,#705848,#402818)";
            case "steel" -> "linear-gradient(to bottom,#8898A8,#607080)";
            case "fairy" -> "linear-gradient(to bottom,#D87898,#A05070)";
            default -> "linear-gradient(to bottom,#546e7a,#37474f)";
        };
    }

    private String getTypeBorderColor(String type) {
        if (type == null)
            return "#263238";
        return switch (type.toLowerCase()) {
            case "normal" -> "#b5c1d4";
            case "fire" -> "#7A2800";
            case "water" -> "#183890";
            case "electric" -> "#604800";
            case "grass" -> "#286800";
            case "ice" -> "#186070";
            case "fighting" -> "#500000";
            case "poison" -> "#480048";
            case "ground" -> "#604808";
            case "flying" -> "#382880";
            case "psychic" -> "#780028";
            case "bug" -> "#304800";
            case "rock" -> "#584808";
            case "ghost" -> "#200048";
            case "dragon" -> "#200098";
            case "dark" -> "#201008";
            case "steel" -> "#384860";
            case "fairy" -> "#783048";
            default -> "#263238";
        };
    }

    // Sprite helpers

    private static java.util.Map<Integer, Double> loadPokemonHeights() {
        java.util.Map<Integer, Double> map = new java.util.HashMap<>();
        try (var stream = BattleController.class.getResourceAsStream(
                "/com/example/pokemonbattle/data/pokemon_heights.json")) {
            if (stream == null) {
                System.err.println("[BattleController] pokemon_heights.json not found");
                return map;
            }
            String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher idM = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
            java.util.regex.Matcher hM = java.util.regex.Pattern.compile("\"height\"\\s*:\\s*([\\d.]+)").matcher(json);
            java.util.List<long[]> ids = new java.util.ArrayList<>(), heights = new java.util.ArrayList<>();
            while (idM.find())
                ids.add(new long[] { idM.start(), Long.parseLong(idM.group(1)) });
            while (hM.find())
                heights.add(new long[] { hM.start(), Double.doubleToLongBits(Double.parseDouble(hM.group(1))) });
            int hi = 0;
            for (long[] idEntry : ids) {
                long idPos = idEntry[0];
                int id = (int) idEntry[1];
                while (hi < heights.size() && heights.get(hi)[0] < idPos)
                    hi++;
                if (hi < heights.size()) {
                    map.put(id, Double.longBitsToDouble(heights.get(hi)[1]));
                    hi++;
                }
            }
            System.out.println("[BattleController] Loaded heights for " + map.size() + " Pokemon");
        } catch (Exception e) {
            System.err.println("[BattleController] Failed to load pokemon_heights.json: " + e.getMessage());
        }
        return map;
    }

    private double getScaledSpritePx(int pokemonId, double basePx, double minPx, double maxPx) {
        Double h = POKEMON_HEIGHTS.get(pokemonId);
        if (h == null || h <= 0)
            return basePx;
        return Math.max(minPx,
                Math.min(maxPx, basePx * Math.pow(h / SPRITE_STANDARD_HEIGHT_M, SPRITE_SCALE_EXPONENT)));
    }

    private void loadSpriteWithFallback(ImageView target, int pokemonId, String direction) {
        String gifPath = String.format("/com/example/pokemonbattle/sprites/%s/gif/%d.gif", direction, pokemonId);
        var gifUrl = getClass().getResource(gifPath);
        if (gifUrl != null) {
            try {
                Image gif = new Image(gifUrl.toExternalForm(),
                        target.getFitWidth() > 0 ? target.getFitWidth() : 0,
                        target.getFitHeight() > 0 ? target.getFitHeight() : 0, true, true, true);
                if (!gif.isError()) {
                    target.setImage(gif);
                    return;
                }
            } catch (Exception e) {
                System.err.println("GIF load error (" + gifPath + "): " + e.getMessage());
            }
        }
        String pngPath = String.format("/com/example/pokemonbattle/sprites/%s/%d.png", direction, pokemonId);
        try {
            var pngStream = getClass().getResourceAsStream(pngPath);
            if (pngStream != null) {
                    Image png = new Image(pngStream,
                            target.getFitWidth() > 0 ? target.getFitWidth() : 0,
                            target.getFitHeight() > 0 ? target.getFitHeight() : 0, true, true);
                if (!png.isError())
                    target.setImage(png);
            }
        } catch (Exception e) {
            System.err.println("PNG fallback error (" + pngPath + "): " + e.getMessage());
        }
    }

    private void updateHpBar(Rectangle bar, int hp, int maxHp) {
        if (bar == null || maxHp <= 0)
            return;
        double ratio = Math.max(0, (double) hp / maxHp);
        bar.setWidth(HP_BAR_MAX_WIDTH * ratio);
        if (ratio > 0.5)
            bar.setFill(Color.web("#78C850"));
        else if (ratio > 0.2)
            bar.setFill(Color.web("#F8D030"));
        else
            bar.setFill(Color.web("#F85888"));
    }

    // Panel switching

    private void onBack() {
        SceneManager.clearData();
        SceneManager.switchSceneWithLoading("new_game.fxml", "Pokemon Battle - Setup", 1200, 700);
    }

    private void onAttackClicked() {
        if (battle == null || battle.isFinished()) {
            battleStatusLabel.setText("Start the battle first!");
            return;
        }
        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
        moveSelectionBox.setVisible(true);
        moveSelectionBox.setManaged(true);
        updateMoveButtons();
    }

    private void onChangePokemonClicked() {
        if (battle == null || battle.isFinished()) {
            battleStatusLabel.setText("Start the battle first!");
            return;
        }
        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        pokemonSelectionBox.setVisible(true);
        pokemonSelectionBox.setManaged(true);
        updatePokemonButtons();
    }

    private void onItemsClicked() {
        battleStatusLabel.setText("No items available in this battle.");
    }

    @FXML
    private void onBackToActions() {
        showActionButtons();
    }

    private void updatePokemonButtons() {
        pokemonButtonsBox.getChildren().clear();
        for (PokemonInstance pokemon : player.getTeam()) {
            Button btn = new Button(capitalize(pokemon.getName()) +
                    (pokemon.isFainted() ? " (Fainted)" : "  Lv." + pokemon.getLevel()));
            btn.setPrefWidth(260);
            btn.setPrefHeight(42);
            btn.getStyleClass().addAll("option-btn", "option-btn-green");
            btn.setStyle("-fx-font-size: 13px;");
            if (pokemon.isFainted()) {
                btn.setDisable(true);
                btn.setStyle("-fx-font-size: 13px; -fx-opacity: 0.5;");
            } else if (pokemon.getId() == player.getCurrentPokemon().getId()) {
                btn.setText(btn.getText() + " ✓");
                btn.setDisable(true);
            } else {
                btn.setOnAction(e -> onPokemonSelected(pokemon));
            }
            pokemonButtonsBox.getChildren().add(btn);
        }
    }

    private void onPokemonSelected(PokemonInstance pokemon) {
        player.setCurrentPokemon(pokemon);
        battleStatusLabel.setText("Go, " + capitalize(pokemon.getName()) + "!");
        updateBattleDisplay();
        showActionButtons();
    }

    private void showActionButtons() {
        actionButtonsBox.setVisible(true);
        actionButtonsBox.setManaged(true);
        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
    }

    // Battle listener callbacks

    @Override
    public void onDamageDealt(String attacker, String move, String defender, int damage) {
        String entry = capitalize(attacker) + " used " + capitalize(move) + "! " + damage + " dmg!";
        battleStatusLabel.setText(entry);
        battleLog.add(entry);
    }

    @Override
    public void onPokemonFainted(String pokemonName) {
        String entry = capitalize(pokemonName) + " fainted!";
        battleStatusLabel.setText(entry);
        battleLog.add(entry);
    }

    @Override
    public void onPokemonSwitched(String playerName, String pokemonName) {
        String entry = playerName + " sent out " + capitalize(pokemonName) + "!";
        battleStatusLabel.setText(entry);
        battleLog.add(entry);
        if (playerName.equals(player.getName()))
            displayPlayerTeam();
        else
            displayOpponentTeam();
        updateBattleDisplay();
        updateMoveButtons();
    }

    @Override
    public void onBattleEnd(String winnerName) {
        boolean playerWon = winnerName.equals(player.getName());
        saveBattleResult(playerWon);
        if (playerWon) {
            MusicManager.getInstance().stopBGM();
            MusicManager.getInstance().playVictorySFX();
        }
        Platform.runLater(() -> showResultOverlay(playerWon));
    }

    // Result overlay

    private void showResultOverlay(boolean playerWon) {
        if (playerWon)
            startConfetti();
        resultTitleLabel.setText(playerWon ? "Victory!" : "Defeat...");
        resultMessageLabel.setText(playerWon
                ? "Congratulations! You won against " + opponent.getName() + "!"
                : "You lost against " + opponent.getName() + ". Better luck next time!");

        battleResultCard.getStyleClass().removeAll("result-card-victory", "result-card-defeat");
        resultTitleLabel.getStyleClass().removeAll("result-title-victory", "result-title-defeat");
        resultMessageLabel.getStyleClass().removeAll("result-message-victory", "result-message-defeat");
        battleAgainResultButton.getStyleClass().removeAll("result-btn-again-victory", "result-btn-again-defeat");

        String v = playerWon ? "victory" : "defeat";
        battleResultCard.getStyleClass().add("result-card-" + v);
        resultTitleLabel.getStyleClass().add("result-title-" + v);
        resultMessageLabel.getStyleClass().add("result-message-" + v);
        battleAgainResultButton.getStyleClass().add("result-btn-again-" + v);

        battleResultOverlay.setOpacity(0);
        battleResultOverlay.setVisible(true);
        battleResultOverlay.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(450), battleResultOverlay);
        ft.setToValue(1.0);
        ft.play();
    }

    @FXML
    private void onGoBackClicked() {
        if (confettiTimer != null) {
            confettiTimer.stop();
            confettiTimer = null;
        }
        onBack();
    }

    @FXML
    private void onBattleAgainClicked() {
        if (confettiTimer != null) {
            confettiTimer.stop();
            confettiTimer = null;
        }
        if (confettiCanvas != null) {
            rootPane.getChildren().remove(confettiCanvas);
            confettiCanvas = null;
        }
        Player freshPlayer = new Player(player.getName());
        if (randomTeam) {
            freshPlayer.generateRandomTeam();
        } else {
            for (PokemonInstance p : player.getTeam())
                freshPlayer.addToTeam(new PokemonInstance(p.getId(), p.getLevel()));
        }
        Player freshOpponent = new Player("AI Trainer");
        freshOpponent.generateRandomTeam();
        SceneManager.switchSceneWithLoading("battle.fxml", "Pokemon Battle - Arena", 1200, 700,
                Map.of("player", freshPlayer, "opponent", freshOpponent, "randomTeam", randomTeam));
    }

    // Confetti
    private void startConfetti() {
        if (confettiCanvas != null)
            rootPane.getChildren().remove(confettiCanvas);
        confettiCanvas = new Canvas();
        confettiCanvas.widthProperty().bind(rootPane.widthProperty());
        confettiCanvas.heightProperty().bind(rootPane.heightProperty());
        confettiCanvas.setMouseTransparent(true);
        int insertIdx = Math.max(0, rootPane.getChildren().size() - 2);
        rootPane.getChildren().add(insertIdx, confettiCanvas);

        final int N = 140;
        double[] x = new double[N], y = new double[N], vx = new double[N], vy = new double[N];
        double[] ang = new double[N], av = new double[N], sz = new double[N];
        Color[] palette = {
                Color.web("#FFD700"), Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#45B7D1"),
                Color.web("#96CEB4"), Color.web("#FFEAA7"), Color.web("#DDA0DD"), Color.web("#98D8C8"),
                Color.web("#F7DC6F")
        };
        Color[] colors = new Color[N];
        Random rng = new Random();
        double sw = rootPane.getWidth() > 0 ? rootPane.getWidth() : 1200;
        for (int i = 0; i < N; i++) {
            x[i] = rng.nextDouble() * sw;
            y[i] = -rng.nextDouble() * 300;
            vx[i] = (rng.nextDouble() - 0.5) * 3.5;
            vy[i] = 2.5 + rng.nextDouble() * 3;
            ang[i] = rng.nextDouble() * Math.PI * 2;
            av[i] = (rng.nextDouble() - 0.5) * 0.14;
            sz[i] = 6 + rng.nextDouble() * 9;
            colors[i] = palette[rng.nextInt(palette.length)];
        }
        long[] t0 = { -1L };
        confettiTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (t0[0] < 0)
                    t0[0] = now;
                double elapsed = (now - t0[0]) / 1_000_000_000.0;
                double alpha = Math.max(0.0, 1.0 - elapsed / 4.0);
                GraphicsContext gc = confettiCanvas.getGraphicsContext2D();
                gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());
                for (int i = 0; i < N; i++) {
                    x[i] += vx[i];
                    y[i] += vy[i];
                    ang[i] += av[i];
                    if (y[i] > confettiCanvas.getHeight() + 20) {
                        y[i] = -12;
                        x[i] = rng.nextDouble() * confettiCanvas.getWidth();
                    }
                    gc.save();
                    gc.setGlobalAlpha(alpha);
                    gc.setFill(colors[i]);
                    gc.translate(x[i], y[i]);
                    gc.rotate(Math.toDegrees(ang[i]));
                    gc.fillRect(-sz[i] / 2, -sz[i] / 4, sz[i], sz[i] / 2);
                    gc.restore();
                }
                if (elapsed >= 4.0) {
                    stop();
                    gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());
                    rootPane.getChildren().remove(confettiCanvas);
                }
            }
        };
        confettiTimer.start();
    }

    // DB persistence
    private void saveBattleResult(boolean playerWon) {
        var user = PlayerSession.getInstance().getCurrentUser();
        if (user == null)
            return;
        String pokemonUsed = player.getTeam().stream().map(p -> p.getName().toLowerCase())
                .collect(Collectors.joining(","));
        String result = playerWon ? "WIN" : "LOSS";
        String logStr = String.join("\n", battleLog);
        Thread t = new Thread(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                String sql = "INSERT INTO battle_history (user_id,result,pokemon_used,opponent_type,opponent_name,battle_log) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, user.getId());
                    ps.setString(2, result);
                    ps.setString(3, pokemonUsed);
                    ps.setString(4, "AI");
                    ps.setString(5, opponent.getName());
                    ps.setString(6, logStr);
                    ps.executeUpdate();
                }
                String upsert = "INSERT INTO user_profiles (user_id,wins,losses,total_battles) VALUES (?,?,?,1) " +
                        "ON CONFLICT(user_id) DO UPDATE SET wins=wins+?,losses=losses+?,total_battles=total_battles+1";
                try (PreparedStatement ups = conn.prepareStatement(upsert)) {
                    int w = playerWon ? 1 : 0, l = playerWon ? 0 : 1;
                    ups.setInt(1, user.getId());
                    ups.setInt(2, w);
                    ups.setInt(3, l);
                    ups.setInt(4, w);
                    ups.setInt(5, l);
                    ups.executeUpdate();
                }
            } catch (Exception e) {
                System.err.println("[BattleController] Failed to save battle result: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // Utility
    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}