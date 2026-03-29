package com.example.pokemonbattle.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.pokemonbattle.database.DatabaseManager;
import com.example.pokemonbattle.model.Battle;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.server.ActionMessage;
import com.example.pokemonbattle.server.BattleChatMessage;
import com.example.pokemonbattle.server.BattleEndMessage;
import com.example.pokemonbattle.server.BattleUpdateMessage;
import com.example.pokemonbattle.server.DamageMessage;
import com.example.pokemonbattle.server.ForfeitMessage;
import com.example.pokemonbattle.server.GameMessage;
import com.example.pokemonbattle.server.ServerConnection;
import com.example.pokemonbattle.server.SwitchNotifyMessage;
import com.example.pokemonbattle.server.TurnReadyMessage;
import com.example.pokemonbattle.util.BattleAnimationManager;
import com.example.pokemonbattle.util.ChatManager;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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

/**
 * Controller for ONLINE battle screen.
 * Server is the single source of truth for damage; no local calculation.
 */
@SuppressWarnings("unused")
public class OnlineBattleController {

    // FXML injections
    @FXML private StackPane rootPane;
    @FXML private ImageView bgImage;
    @FXML private StackPane battleSection;
    @FXML private AnchorPane battleField;
    @FXML private StackPane optionsSection;
    @FXML private HBox mainBattleLayout;

    @FXML private ImageView playerSpriteImage;
    @FXML private ImageView opponentSpriteImage;
    @FXML private Label playerPokemonNameLabel;
    @FXML private Label playerPokemonHpLabel;
    @FXML private Label opponentPokemonNameLabel;
    @FXML private Label opponentPokemonHpLabel;
    @FXML private Rectangle playerHpBar;
    @FXML private Rectangle opponentHpBar;
    @FXML private HBox playerTypesBox;
    @FXML private HBox opponentTypesBox;

    @FXML private Label playerNameLabel;
    @FXML private Label opponentNameLabel;
    @FXML private VBox playerPokemonBox;
    @FXML private VBox opponentPokemonBox;
    @FXML private Label playerTeamLabel;
    @FXML private Label opponentTeamLabel;
    @FXML private Label battleStatusLabel;

    @FXML private VBox actionButtonsBox;
    @FXML private Button startBattleButton;
    @FXML private Button attackButton;
    @FXML private Button changePokemonMainButton;
    @FXML private Button itemsButton;
    @FXML private Button backButton;
    @FXML private Label waitingLabel;

    @FXML private VBox moveSelectionBox;
    @FXML private VBox moveButtonsContainer;
    @FXML private VBox pokemonSelectionBox;
    @FXML private VBox pokemonButtonsBox;

    @FXML private AnchorPane vsScreenPane;
    @FXML private ImageView vsBgImage;
    @FXML private ImageView vsPlayerSprite;
    @FXML private ImageView vsOpponentSprite;
    @FXML private Region rootBlackFade;

    @FXML private StackPane battleResultOverlay;
    @FXML private VBox battleResultCard;
    @FXML private Label resultTitleLabel;
    @FXML private Label resultMessageLabel;
    @FXML private Button goBackResultButton;
    @FXML private Button battleAgainResultButton;

    @FXML private StackPane forfeitOverlay;
    @FXML private Region forfeitBackdrop;
    @FXML private VBox forfeitDialog;
    @FXML private Button forfeitYesButton;
    @FXML private Button forfeitNoButton;

    @FXML private Button battleSettingsButton;
    @FXML private VBox battleQuickMenu;
    @FXML private StackPane exitOverlay;
    @FXML private Region exitBackdrop;
    @FXML private VBox exitDialog;
    @FXML private Button exitYesButton;
    @FXML private Button exitNoButton;

    // Forced switch overlay
    @FXML private StackPane forcedSwitchOverlay;
    @FXML private Label forcedSwitchTitleLabel;
    @FXML private VBox forcedSwitchPokemonList;

    // Chat section
    @FXML private VBox chatSection;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private Button chatToggleButton;
    @FXML private VBox quickMessagesBox;
    @FXML private Button quickMsg1;
    @FXML private Button quickMsg2;
    @FXML private Button quickMsg3;
    @FXML private Button quickMsg4;
    @FXML private Button quickMsg5;
    @FXML private Button quickMsg6;
    @FXML private TextField chatInputField;
    @FXML private Button chatSendButton;

    // Move list constants
    private static final double MOVE_BTN_HEIGHT = 50.0;
    private static final double INFO_BTN_WIDTH = 18.0;
    private static final double INFO_BTN_HEIGHT = 18.0;
    private static final double INFO_BTN_INSET_TOP = 4.0;
    private static final double INFO_BTN_INSET_RIGHT = 4.0;
    private static final double EFF_ICON_INSET_RIGHT = 8.0;
    private static final double EFF_ICON_INSET_BOTTOM = 6.0;
    private static final double CHAT_SECTION_EXPANDED_HEIGHT = 260.0;
    private static final double CHAT_SECTION_COLLAPSED_HEIGHT = 46.0;

    // Pokemon selection constants
    private static final double POKEMON_BTN_HEIGHT = 70.0;
    private static final double POKEMON_SPRITE_SIZE = 60.0;
    private static final double POKEMON_INFO_BTN_SIZE = 16.0;

    // Battle state
    private Player player;
    private Player opponent;
    private ServerConnection serverConnection;
    private Integer battleId;
    private String opponentAvatarPath;
    private int turnCount = 0;
    private boolean battleEnded = false;
    private boolean moveSent = false;

    private final List<String> battleLog = new ArrayList<>();

    // Move-button tracking
    private final List<Button> activeMoveButtons = new ArrayList<>();

    // Info overlay
    private Pane infoFloatingLayer;
    private VBox infoCard;
    private Label infoName, infoType, infoPower, infoAccuracy, infoPp, infoDescription;

    // Pokemon info overlay
    private VBox pokemonInfoCard;
    private Label pokemonInfoName, pokemonInfoLevel;
    private HBox pokemonInfoTypes;
    private VBox pokemonInfoStats, pokemonInfoMoves;

    // Confetti
    private Canvas confettiCanvas;
    private AnimationTimer confettiTimer;

    private static final double HP_BAR_MAX_WIDTH = 180.0;

    // Sprite scaling
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

    private BattleAnimationManager animationManager;
    private ChatManager chatManager;
    private boolean chatExpanded = true;
    private final Queue<DamageMessage> pendingDamageMessages = new ArrayDeque<>();
    private final Queue<SwitchNotifyMessage> pendingSwitchNotifies = new ArrayDeque<>();
    private boolean damageAnimationInProgress = false;

    //  Lifecycle 
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

        player = (Player) SceneManager.getData("player");
        opponent = (Player) SceneManager.getData("opponent");
        serverConnection = (ServerConnection) SceneManager.getData("serverConnection");
        battleId = (Integer) SceneManager.getData("battleId");
        opponentAvatarPath = (String) SceneManager.getData("opponentAvatarPath");

        if (player == null || opponent == null || serverConnection == null) {
            battleStatusLabel.setText("Error: missing battle data!");
            disableAllButtons();
            return;
        }

        serverConnection.setMessageListener(this::handleServerMessage);
        serverConnection.setOnDisconnect(() -> Platform.runLater(this::handleDisconnect));

        if (startBattleButton != null)
            startBattleButton.setOnAction(e -> onRunClicked());
        attackButton.setOnAction(e -> onFightClicked());
        changePokemonMainButton.setOnAction(e -> onChangePokemonClicked());
        backButton.setOnAction(e -> onRunClicked());
        if (itemsButton != null)
            itemsButton.setOnAction(e -> onItemsClicked());
        if (battleSettingsButton != null)
            battleSettingsButton.setOnAction(e -> onBattleMenuToggleClicked());

        if (optionsSection != null)
            optionsSection.setPickOnBounds(false);
        if (actionButtonsBox != null)
            actionButtonsBox.setPickOnBounds(false);

        if (exitOverlay != null) {
            exitOverlay.setVisible(false);
            exitOverlay.setManaged(false);
            exitOverlay.setMouseTransparent(true);
        }
        if (forfeitOverlay != null) {
            forfeitOverlay.setVisible(false);
            forfeitOverlay.setManaged(false);
            forfeitOverlay.setMouseTransparent(true);
        }
        if (forcedSwitchOverlay != null) {
            forcedSwitchOverlay.setVisible(false);
            forcedSwitchOverlay.setManaged(false);
            forcedSwitchOverlay.setMouseTransparent(true);
        }
        if (battleResultOverlay != null) {
            battleResultOverlay.setMouseTransparent(true);
        }
        if (vsScreenPane != null) {
            vsScreenPane.setMouseTransparent(false);
        }
        if (rootBlackFade != null) {
            rootBlackFade.setMouseTransparent(true);
        }

        setVisible(waitingLabel, false);
        drawOptionsPanelPattern();
        setupInfoOverlay();
        setupPokemonInfoOverlay();
        SceneManager.enableCoordDebug(rootPane);

        battleStatusLabel.setText("Online Battle! " + cap(player.getCurrentPokemon().getName()) +
                " vs " + cap(opponent.getCurrentPokemon().getName()));

        MusicManager.getInstance().attachClickSounds(rootPane);
        Platform.runLater(this::playVSIntro);
        animationManager = new BattleAnimationManager(playerSpriteImage, opponentSpriteImage, battleField);
        animationManager.setAnimationEnabled(PlayerSession.getInstance().isMoveAnimationEnabled());

        // Initialize chat manager
        initializeChat();
    }

    //  Info overlay 

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

    private void setupPokemonInfoOverlay() {
        pokemonInfoName = styledLabel("pokemon-info-name");
        pokemonInfoLevel = styledLabel("pokemon-info-level");
        pokemonInfoTypes = new HBox(4);
        pokemonInfoTypes.setAlignment(Pos.CENTER_LEFT);
        pokemonInfoStats = new VBox(3);
        pokemonInfoMoves = new VBox(3);

        VBox header = new VBox(2, pokemonInfoName, pokemonInfoLevel, pokemonInfoTypes);
        header.setAlignment(Pos.CENTER_LEFT);

        Label statsTitle = styledLabel("pokemon-info-section-title");
        statsTitle.setText("Stats:");
        Label movesTitle = styledLabel("pokemon-info-section-title");
        movesTitle.setText("Moves:");

        pokemonInfoCard = new VBox(8, header, statsTitle, pokemonInfoStats, movesTitle, pokemonInfoMoves);
        pokemonInfoCard.getStyleClass().add("pokemon-info-overlay");
        pokemonInfoCard.setVisible(false);
        pokemonInfoCard.setManaged(false);
        pokemonInfoCard.setMouseTransparent(true);
        pokemonInfoCard.setMaxWidth(300);

        if (infoFloatingLayer != null) {
            infoFloatingLayer.getChildren().add(pokemonInfoCard);
        }
    }

    private Label styledLabel(String styleClasses) {
        Label l = new Label();
        l.getStyleClass().addAll(styleClasses.split("\\s+"));
        return l;
    }

    private void showInfoOverlay(Button iBtn, Move move, int currentPp) {
        infoName.setText(cap(move.getName()));
        String type = (move.getType() != null) ? move.getType() : "normal";
        infoType.setText("Type: " + cap(type));
        Integer pow = move.getPower();
        infoPower.setText("Power: " + (pow != null && pow > 0 ? String.valueOf(pow) : "—"));
        Integer acc = move.getAccuracy();
        infoAccuracy.setText("Accuracy: " + (acc != null && acc > 0 ? acc + "%" : "—"));
        int maxPp = move.getPp() > 0 ? move.getPp() : currentPp;
        infoPp.setText("PP: " + currentPp + " / " + maxPp);
        String desc = move.getDescription();
        if (desc == null || desc.isBlank()) desc = "No description available.";
        int splitAt = desc.indexOf("\n\n");
        if (splitAt >= 0 && splitAt + 2 < desc.length()) desc = desc.substring(splitAt + 2).trim();
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

    private void showPokemonInfoOverlay(Button iBtn, PokemonInstance pokemon) {
        pokemonInfoName.setText(cap(pokemon.getName()));
        pokemonInfoLevel.setText("Lv. " + pokemon.getLevel());
        pokemonInfoTypes.getChildren().clear();
        for (String type : pokemon.getTypes()) {
            Label typeLabel = new Label(type.toUpperCase());
            typeLabel.setStyle("-fx-font-size: 9px; -fx-padding: 3 6; -fx-background-color: " +
                    getTypeColor(type) + "; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-weight: bold;");
            pokemonInfoTypes.getChildren().add(typeLabel);
        }
        pokemonInfoStats.getChildren().clear();
        pokemonInfoStats.getChildren().add(createStatLabel("HP: " + pokemon.getCurrentHp() + " / " + pokemon.getMaxHp()));
        pokemonInfoStats.getChildren().add(createStatLabel("Attack: " + pokemon.getAttack()));
        pokemonInfoStats.getChildren().add(createStatLabel("Defense: " + pokemon.getDefense()));
        pokemonInfoStats.getChildren().add(createStatLabel("Sp.Atk: " + pokemon.getSpAttack()));
        pokemonInfoStats.getChildren().add(createStatLabel("Sp.Def: " + pokemon.getSpDefense()));
        pokemonInfoStats.getChildren().add(createStatLabel("Speed: " + pokemon.getSpeed()));
        pokemonInfoMoves.getChildren().clear();
        for (var battleMove : pokemon.getBattleMoves()) {
            Move move = battleMove.getMove();
            Label moveLabel = new Label("• " + cap(move.getName()) + " (" + cap(move.getType() != null ? move.getType() : "Normal") + ")");
            moveLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e0e0e0;");
            pokemonInfoMoves.getChildren().add(moveLabel);
        }
        pokemonInfoCard.setVisible(true);
        infoFloatingLayer.toFront();
        pokemonInfoCard.toFront();
        Platform.runLater(() -> {
            pokemonInfoCard.applyCss();
            pokemonInfoCard.autosize();
            Bounds b = iBtn.localToScene(iBtn.getBoundsInLocal());
            Bounds r = rootPane.localToScene(rootPane.getBoundsInLocal());
            double cardW = pokemonInfoCard.getWidth() > 10 ? pokemonInfoCard.getWidth() : pokemonInfoCard.prefWidth(-1);
            double cardH = pokemonInfoCard.getHeight() > 10 ? pokemonInfoCard.getHeight() : pokemonInfoCard.prefHeight(-1);
            if (cardW < 200) cardW = 280;
            if (cardH < 150) cardH = 200;
            double x = (b.getMinX() - r.getMinX()) - cardW - 10;
            double y = (b.getMinY() - r.getMinY()) - cardH / 2.0 + iBtn.getHeight() / 2.0;
            x = Math.max(4, x);
            y = Math.max(4, Math.min(y, rootPane.getHeight() - cardH - 4));
            pokemonInfoCard.setLayoutX(x);
            pokemonInfoCard.setLayoutY(y);
        });
    }

    private Label createStatLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #d0d0d0;");
        return label;
    }

    private void hideInfoOverlay() {
        if (infoCard != null) infoCard.setVisible(false);
        if (pokemonInfoCard != null) pokemonInfoCard.setVisible(false);
    }

    @FXML
    private void onBattleMenuToggleClicked() {
        if (battleQuickMenu == null)
            return;
        boolean isVisible = !battleQuickMenu.isVisible();
        battleQuickMenu.setVisible(isVisible);
        battleQuickMenu.setManaged(isVisible);
    }

    @FXML
    private void onBattleGuideClicked() {
        hideBattleQuickMenu();
        SceneManager.switchSceneWithLoading("wc.fxml", "Welcome", 1200, 700);
    }

    @FXML
    private void onQuitToHomeClicked() {
        hideBattleQuickMenu();
        doDisconnectAndLeave();
    }

    @FXML
    private void onQuitToDesktopClicked() {
        hideBattleQuickMenu();
        showExitOverlay();
    }

    private void showExitOverlay() {
        if (exitOverlay == null || exitDialog == null)
            return;
        exitOverlay.setVisible(true);
        exitOverlay.setManaged(true);
        exitOverlay.setMouseTransparent(false);

        exitOverlay.setOpacity(0);
        exitDialog.setScaleX(0.85);
        exitDialog.setScaleY(0.85);
        exitDialog.setOpacity(0);

        FadeTransition backdropFade = new FadeTransition(Duration.millis(220), exitOverlay);
        backdropFade.setFromValue(0);
        backdropFade.setToValue(1);
        backdropFade.setInterpolator(Interpolator.EASE_OUT);

        Timeline dialogPop = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(exitDialog.scaleXProperty(), 0.85),
                        new KeyValue(exitDialog.scaleYProperty(), 0.85),
                        new KeyValue(exitDialog.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(exitDialog.scaleXProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                        new KeyValue(exitDialog.scaleYProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                        new KeyValue(exitDialog.opacityProperty(), 1.0, Interpolator.EASE_OUT)));

        backdropFade.play();
        dialogPop.play();
    }

    private void hideExitOverlay(Runnable onFinished) {
        if (exitOverlay == null || exitDialog == null) {
            if (onFinished != null)
                onFinished.run();
            return;
        }
        Timeline dismiss = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(exitDialog.scaleXProperty(), 1.0),
                        new KeyValue(exitDialog.scaleYProperty(), 1.0),
                        new KeyValue(exitDialog.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(exitDialog.scaleXProperty(), 0.88, Interpolator.EASE_IN),
                        new KeyValue(exitDialog.scaleYProperty(), 0.88, Interpolator.EASE_IN),
                        new KeyValue(exitDialog.opacityProperty(), 0.0, Interpolator.EASE_IN)));
        FadeTransition backdropFade = new FadeTransition(Duration.millis(200), exitOverlay);
        backdropFade.setFromValue(1);
        backdropFade.setToValue(0);
        backdropFade.setInterpolator(Interpolator.EASE_IN);
        backdropFade.setOnFinished(e -> {
            exitOverlay.setVisible(false);
            exitOverlay.setManaged(false);
            exitOverlay.setMouseTransparent(true);
            if (onFinished != null)
                onFinished.run();
        });
        dismiss.play();
        backdropFade.play();
    }

    @FXML
    private void onExitConfirmed() {
        hideExitOverlay(() -> System.exit(0));
    }

    @FXML
    private void onExitCancelled() {
        hideExitOverlay(null);
    }

    @FXML
    private void onBackClicked() {
        hideBattleQuickMenu();
    }

    private void hideBattleQuickMenu() {
        if (battleQuickMenu == null)
            return;
        battleQuickMenu.setVisible(false);
        battleQuickMenu.setManaged(false);
    }

    @FXML
    private void onBattleSettingsClicked() {
        if (rootPane == null) {
            return;
        }

        boolean alreadyOpen = rootPane.getChildren().stream()
                .anyMatch(node -> node.getStyleClass().contains("overlay-root"));
        if (alreadyOpen) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pokemonbattle/view/settings.fxml"));
            javafx.scene.Node overlay = loader.load();
            overlay.setOpacity(0.0);
            rootPane.getChildren().add(overlay);
            MusicManager.getInstance().attachClickSounds((Parent) overlay);

            FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        } catch (IOException e) {
            System.err.println("Error loading settings overlay: " + e.getMessage());
        }
    }

    //  Forced switch overlay 
    /**
     * Shows a modal overlay forcing the player to pick a replacement when their
     * active Pokémon faints during an online battle.
     */
    private void showForcedSwitchOverlay() {
        forcedSwitchPokemonList.getChildren().clear();

        List<PokemonInstance> available = player.getTeam().stream()
                .filter(p -> !p.isFainted())
                .toList();

        if (available.isEmpty()) {
            // No pokemon left — battle end will arrive from server
            return;
        }

        forcedSwitchTitleLabel.setText(cap(player.getCurrentPokemon().getName()) + " fainted!");

        for (PokemonInstance pokemon : available) {
            forcedSwitchPokemonList.getChildren().add(createForcedSwitchRow(pokemon));
        }

        forcedSwitchOverlay.setOpacity(0);
        forcedSwitchOverlay.setVisible(true);
        forcedSwitchOverlay.setManaged(true);
        forcedSwitchOverlay.setMouseTransparent(false);
        FadeTransition ft = new FadeTransition(Duration.millis(280), forcedSwitchOverlay);
        ft.setToValue(1.0);
        ft.play();
    }

    private HBox createForcedSwitchRow(PokemonInstance pokemon) {
        // Sprite
        ImageView sprite = new ImageView();
        sprite.setFitWidth(64);
        sprite.setFitHeight(64);
        sprite.setPreserveRatio(true);
        String gifPath = "/com/example/pokemonbattle/sprites/front/gif/" + pokemon.getId() + ".gif";
        var gifUrl = getClass().getResource(gifPath);
        if (gifUrl != null) {
            try {
                Image gif = new Image(gifUrl.toExternalForm(), 64, 64, true, true, true);
                if (!gif.isError()) sprite.setImage(gif);
                else loadPokemonPngSprite(sprite, pokemon.getId(), 64);
            } catch (Exception e) { loadPokemonPngSprite(sprite, pokemon.getId(), 64); }
        } else {
            loadPokemonPngSprite(sprite, pokemon.getId(), 64);
        }

        // Name + level
        Label nameLabel = new Label(cap(pokemon.getName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: white;");

        Label levelLabel = new Label("Lv. " + pokemon.getLevel());
        levelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.75);");

        // HP bar (mini)
        double hpRatio = Math.max(0, (double) pokemon.getCurrentHp() / pokemon.getMaxHp());
        String hpColor = hpRatio > 0.5 ? "#78C850" : hpRatio > 0.2 ? "#F8D030" : "#F85888";
        Label hpLabel = new Label("HP: " + pokemon.getCurrentHp() + " / " + pokemon.getMaxHp());
        hpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85);");

        Rectangle hpTrack = new Rectangle(130, 6);
        hpTrack.setFill(Color.web("#333"));
        hpTrack.setArcWidth(6); hpTrack.setArcHeight(6);
        Rectangle hpFill = new Rectangle(130 * hpRatio, 6);
        hpFill.setFill(Color.web(hpColor));
        hpFill.setArcWidth(6); hpFill.setArcHeight(6);
        StackPane hpBarPane = new StackPane(hpTrack, hpFill);
        hpBarPane.setAlignment(Pos.CENTER_LEFT);

        // Type badges
        HBox typesBox = new HBox(4);
        for (String type : pokemon.getTypes()) {
            Label tl = new Label(type.substring(0, Math.min(3, type.length())).toUpperCase());
            tl.setStyle("-fx-font-size: 9px; -fx-padding: 2 5; -fx-background-color: " +
                    getTypeColor(type) + "; -fx-text-fill: white; -fx-background-radius: 3; -fx-font-weight: bold;");
            typesBox.getChildren().add(tl);
        }

        // Moves summary
        VBox movesBox = new VBox(2);
        for (var ms : pokemon.getBattleMoves()) {
            Move m = ms.getMove();
            String typeColor = getTypeColor(m.getType() != null ? m.getType() : "normal");
            Label ml = new Label("• " + cap(m.getName()));
            ml.setStyle("-fx-font-size: 11px; -fx-text-fill: " + typeColor + ";");
            movesBox.getChildren().add(ml);
        }

        // Stats (compact)
        Label statsLabel = new Label(
            "ATK " + pokemon.getAttack() + "  DEF " + pokemon.getDefense() +
            "  SP.A " + pokemon.getSpAttack() + "  SPD " + pokemon.getSpeed());
        statsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.65);");

        VBox leftInfo = new VBox(4, nameLabel, levelLabel, typesBox, hpLabel, hpBarPane, statsLabel);
        leftInfo.setAlignment(Pos.CENTER_LEFT);

        Label movesTitle = new Label("Moves:");
        movesTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.55); -fx-font-weight: bold;");
        VBox rightInfo = new VBox(3, movesTitle);
        rightInfo.getChildren().addAll(movesBox.getChildren());
        rightInfo.setAlignment(Pos.TOP_LEFT);
        rightInfo.setMinWidth(120);

        HBox content = new HBox(12, sprite, leftInfo, rightInfo);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);

        StackPane rowPane = new StackPane(content);
        rowPane.setPadding(new Insets(10, 14, 10, 14));
        rowPane.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(106,173,140,0.35), rgba(60,100,90,0.25));" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;" +
            "-fx-cursor: hand;");

        rowPane.setOnMouseEntered(e -> rowPane.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(120,200,80,0.40), rgba(80,160,120,0.35));" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(120,200,80,0.7);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 2;" +
            "-fx-cursor: hand;"));
        rowPane.setOnMouseExited(e -> rowPane.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(106,173,140,0.35), rgba(60,100,90,0.25));" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;" +
            "-fx-cursor: hand;"));

        rowPane.setOnMouseClicked(e -> onForcedSwitchSelected(pokemon));

        HBox row = new HBox(rowPane);
        HBox.setHgrow(rowPane, Priority.ALWAYS);
        return row;
    }

    private void onForcedSwitchSelected(PokemonInstance pokemon) {
        // Hide overlay
        FadeTransition ft = new FadeTransition(Duration.millis(200), forcedSwitchOverlay);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            forcedSwitchOverlay.setVisible(false);
            forcedSwitchOverlay.setManaged(false);
            forcedSwitchOverlay.setMouseTransparent(true);
        });
        ft.play();

        // Send switch action to server
        onPokemonSelected(pokemon);
    }

    //  VS Intro 

    private void playVSIntro() {
        MusicManager.getInstance().startBattleMusicForEncounter();

        boolean oppLoaded = false;
        if (opponentAvatarPath != null && !opponentAvatarPath.isEmpty()) {
            var oppUrl = getClass().getResource(opponentAvatarPath);
            if (oppUrl != null) {
                vsOpponentSprite.setImage(new Image(oppUrl.toExternalForm(), 0, 0, true, true));
                oppLoaded = true;
            }
        }
        if (!oppLoaded) {
            int npcId = new Random().nextInt(7) + 1;
            var npcUrl = getClass().getResource("/com/example/pokemonbattle/sprites/trainer/npc/" + npcId + ".png");
            if (npcUrl != null)
                vsOpponentSprite.setImage(new Image(npcUrl.toExternalForm(), 0, 0, true, true));
        }
        String avatarPath = PlayerSession.getInstance().getAvatarPath();
        if (avatarPath != null) {
            var url = getClass().getResource(avatarPath);
            if (url != null)
                vsPlayerSprite.setImage(new Image(url.toExternalForm(), 0, 0, true, true));
        }

        vsPlayerSprite.setTranslateX(-VS_OFFSCREEN_OFFSET);
        vsOpponentSprite.setTranslateX(VS_OFFSCREEN_OFFSET);

        TranslateTransition psi = new TranslateTransition(Duration.millis(VS_SLIDE_IN_MS), vsPlayerSprite);
        psi.setToX(VS_SLIDE_STOP_OFFSET);
        psi.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition osi = new TranslateTransition(Duration.millis(VS_SLIDE_IN_MS), vsOpponentSprite);
        osi.setToX(-VS_SLIDE_STOP_OFFSET);
        osi.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition slideIn = new ParallelTransition(psi, osi);

        double driftMs = VS_DRIFT_HOLD_MS - VS_SLIDE_OUT_EARLY_MS;
        TranslateTransition pd = new TranslateTransition(Duration.millis(driftMs), vsPlayerSprite);
        pd.setByX(VS_DRIFT_AMOUNT);
        pd.setInterpolator(Interpolator.LINEAR);
        TranslateTransition od = new TranslateTransition(Duration.millis(driftMs), vsOpponentSprite);
        od.setByX(-VS_DRIFT_AMOUNT);
        od.setInterpolator(Interpolator.LINEAR);
        ParallelTransition drift = new ParallelTransition(pd, od);

        TranslateTransition pso = new TranslateTransition(Duration.millis(VS_SLIDE_OUT_MS), vsPlayerSprite);
        pso.setByX(VS_OFFSCREEN_OFFSET * 2.2);
        pso.setInterpolator(Interpolator.EASE_IN);
        TranslateTransition oso = new TranslateTransition(Duration.millis(VS_SLIDE_OUT_MS), vsOpponentSprite);
        oso.setByX(-VS_OFFSCREEN_OFFSET * 2.2);
        oso.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition slideOut = new ParallelTransition(pso, oso);

        SequentialTransition vsSeq = new SequentialTransition(slideIn, drift, slideOut);
        vsSeq.setOnFinished(e -> fadeToBlackAndRevealBattle());
        vsSeq.play();
    }

    private void fadeToBlackAndRevealBattle() {
        rootBlackFade.setOpacity(0.0);
        rootBlackFade.setVisible(true);
        FadeTransition ftb = new FadeTransition(Duration.millis(VS_FADE_TO_BLACK_MS), rootBlackFade);
        ftb.setToValue(1.0);
        ftb.setOnFinished(e -> {
            vsScreenPane.setVisible(false);
            vsScreenPane.setManaged(false);
            mainBattleLayout.setVisible(true);
            mainBattleLayout.setManaged(true);
            updateBattleDisplay();
            FadeTransition ffb = new FadeTransition(Duration.millis(VS_FADE_FROM_BLACK_MS), rootBlackFade);
            ffb.setToValue(0.0);
            ffb.setOnFinished(ev -> rootBlackFade.setVisible(false));
            ffb.play();
        });
        ftb.play();
    }

    //  Server message handler 

    private void handleServerMessage(GameMessage msg) {
        Platform.runLater(() -> {
            switch (msg.getMessageType()) {
                case "DAMAGE"       -> applyDamage((DamageMessage) msg);
                case "SWITCH_NOTIFY"-> applySwitchNotify((SwitchNotifyMessage) msg);
                case "BATTLE_UPDATE"-> applyBattleUpdate((BattleUpdateMessage) msg);
                case "TURN_READY"   -> onTurnReady((TurnReadyMessage) msg);
                case "BATTLE_END"   -> applyBattleEnd((BattleEndMessage) msg);
                case "BATTLE_CHAT"  -> applyBattleChat((BattleChatMessage) msg);
                default             -> System.out.println("[OnlineBattle] Unknown msg: " + msg.getMessageType());
            }
        });
    }

    private void applyBattleChat(BattleChatMessage msg) {
        if (chatManager == null || msg == null || msg.getMessageText() == null) return;

        String sender = msg.getSenderName();
        if (sender != null && player != null && sender.equals(player.getName())) {
            return;
        }

        chatManager.receiveOpponentMessage(sender, msg.getMessageText());
    }

    private void applyDamage(DamageMessage msg) {
        pendingDamageMessages.offer(msg);
        processNextDamageMessage();
    }

    private void processNextDamageMessage() {
        if (damageAnimationInProgress) return;

        DamageMessage msg = pendingDamageMessages.poll();
        if (msg == null) {
            flushPendingSwitchNotifies();
            return;
        }

        damageAnimationInProgress = true;
        boolean targetIsMe = player.getName().equals(msg.getTargetName());
        Player target = targetIsMe ? player : opponent;
        PokemonInstance targetPok = target.getCurrentPokemon();
        if (targetPok == null) {
            damageAnimationInProgress = false;
            processNextDamageMessage();
            return;
        }

        ImageView attackerSprite = player.getName().equals(msg.getAttackerName())
                ? playerSpriteImage : opponentSpriteImage;
        ImageView defenderSprite = attackerSprite == playerSpriteImage
                ? opponentSpriteImage : playerSpriteImage;

        Runnable afterAnimation = () -> {
            targetPok.setCurrentHp(msg.getTargetCurrentHp());
            PokemonInstance attackerPok = null;
            if (player.getName().equals(msg.getAttackerName())) {
                attackerPok = player.getCurrentPokemon();
            } else if (opponent.getName().equals(msg.getAttackerName())) {
                attackerPok = opponent.getCurrentPokemon();
            }
            if (attackerPok != null && msg.getAttackerCurrentHp() != null) {
                attackerPok.setCurrentHp(msg.getAttackerCurrentHp());
            }
            updateBattleDisplay();

            if (animationManager != null) {
                if (msg.getDamageDealt() != null && msg.getDamageDealt() > 0) {
                    animationManager.showDamageNumber(defenderSprite, msg.getDamageDealt());
                }
                if (msg.getHealAmount() != null && msg.getHealAmount() > 0) {
                    animationManager.showHealNumber(attackerSprite, msg.getHealAmount());
                }
            }

            String effText = "";
            if (msg.getEffectiveness() != null && msg.getEffectiveness() > 1.0f)
                effText = " (Super effective!)";
            else if (msg.getEffectiveness() != null && msg.getEffectiveness() < 1.0f && msg.getEffectiveness() > 0)
                effText = " (Not very effective...)";
            else if (msg.getEffectiveness() != null && msg.getEffectiveness() == 0f)
                effText = " (No effect)";

            String logEntry = cap(msg.getAttackerName()) + " used " + cap(msg.getMoveUsed()) + "! "
                    + msg.getDamageDealt() + " dmg" + effText;
            battleStatusLabel.setText(logEntry);
            battleLog.add(logEntry);

            if (msg.isTargetFainted()) {
                targetPok.setFainted(true);
                String faintEntry = cap(targetPok.getName()) + " fainted!";
                battleStatusLabel.setText(faintEntry);
                battleLog.add(faintEntry);
                updateBattleDisplay();
            }

            damageAnimationInProgress = false;
            // Small gap keeps sequential move execution readable in online mode.
            PauseTransition delay = new PauseTransition(Duration.millis(140));
            delay.setOnFinished(e -> processNextDamageMessage());
            delay.play();
        };

        if (animationManager != null) {
            Move moveForAnimation = resolveMoveForAnimation(msg.getMoveUsed());
            animationManager.playAttackAnimation(attackerSprite, defenderSprite, moveForAnimation, afterAnimation);
        } else {
            afterAnimation.run();
        }
    }

    private Move resolveMoveForAnimation(String moveName) {
        if (moveName == null || moveName.isBlank()) return null;
        String needle = moveName.trim().toLowerCase();
        if (player != null) {
            Move found = findMoveInTeam(player, needle);
            if (found != null) return found;
        }
        if (opponent != null) {
            Move found = findMoveInTeam(opponent, needle);
            if (found != null) return found;
        }
        return null;
    }

    private Move findMoveInTeam(Player side, String moveNameLower) {
        for (PokemonInstance p : side.getTeam()) {
            for (PokemonInstance.MoveSlot slot : p.getBattleMoves()) {
                Move move = slot.getMove();
                if (move != null && move.getName() != null
                        && move.getName().equalsIgnoreCase(moveNameLower)) {
                    return move;
                }
            }
        }
        return null;
    }

    private void applyBattleUpdate(BattleUpdateMessage msg) {
        battleStatusLabel.setText(msg.getMessage());
        battleLog.add(msg.getMessage());
        updateBattleDisplay();
    }

    private void applySwitchNotify(SwitchNotifyMessage msg) {
        if (damageAnimationInProgress || !pendingDamageMessages.isEmpty()) {
            pendingSwitchNotifies.offer(msg);
            return;
        }

        applySwitchNotifyNow(msg);
    }

    private void flushPendingSwitchNotifies() {
        while (!pendingSwitchNotifies.isEmpty()) {
            SwitchNotifyMessage switchMsg = pendingSwitchNotifies.poll();
            if (switchMsg != null) {
                applySwitchNotifyNow(switchMsg);
            }
        }
    }

    private void applySwitchNotifyNow(SwitchNotifyMessage msg) {
        boolean isMe = player.getName().equals(msg.getPlayerName());
        Player side = isMe ? player : opponent;
        for (PokemonInstance p : side.getTeam()) {
            if (p.getId() == msg.getNewPokemonId()) {
                side.setCurrentPokemon(p);
                p.setCurrentHp(msg.getNewPokemonHp());
                break;
            }
        }
        String who = isMe ? "You" : cap(msg.getPlayerName());
        String switchEntry = who + " switched to " + cap(msg.getNewPokemonName()) + "!";
        battleStatusLabel.setText(switchEntry);
        battleLog.add(switchEntry);
        updateBattleDisplay();
    }

    private void onTurnReady(TurnReadyMessage msg) {
        flushPendingSwitchNotifies();
        turnCount = msg.getTurnNumber();
        moveSent = false;
        setVisible(waitingLabel, false);
        showActionButtons();
        attackButton.setDisable(false);
        changePokemonMainButton.setDisable(false);
    }

    private void applyBattleEnd(BattleEndMessage msg) {
        if (battleEnded) return;
        pendingDamageMessages.clear();
        pendingSwitchNotifies.clear();
        damageAnimationInProgress = false;
        battleEnded = true;
        boolean playerWon = player.getName().equals(msg.getWinnerName());
        saveBattleResult(playerWon, msg.getWinnerName());
        showResultOverlay(playerWon);
    }

    private void handleDisconnect() {
        if (battleEnded) return;
        pendingDamageMessages.clear();
        pendingSwitchNotifies.clear();
        damageAnimationInProgress = false;
        battleEnded = true;
        battleStatusLabel.setText("Connection lost!");
        battleLog.add("Connection to server lost.");
        disableAllButtons();
        setVisible(waitingLabel, false);
        resultTitleLabel.setText("Disconnected");
        resultMessageLabel.setText("Connection to the server was lost.\nThe battle could not be completed.");
        battleResultCard.getStyleClass().removeAll("result-card-victory", "result-card-defeat");
        resultTitleLabel.getStyleClass().removeAll("result-title-victory", "result-title-defeat");
        resultMessageLabel.getStyleClass().removeAll("result-message-victory", "result-message-defeat");
        battleResultCard.getStyleClass().add("result-card-defeat");
        resultTitleLabel.getStyleClass().add("result-title-defeat");
        resultMessageLabel.getStyleClass().add("result-message-defeat");
        battleResultOverlay.setOpacity(0);
        battleResultOverlay.setVisible(true);
        battleResultOverlay.setManaged(true);
        battleResultOverlay.setMouseTransparent(false);
        FadeTransition ft = new FadeTransition(Duration.millis(450), battleResultOverlay);
        ft.setToValue(1.0);
        ft.play();
    }

    //  Result overlay 

    private void showResultOverlay(boolean playerWon) {
        MusicManager.getInstance().stopBGM();
        if (playerWon) {
            MusicManager.getInstance().playVictorySFX();
            startConfetti();
        }
        resultTitleLabel.setText(playerWon ? "Victory!" : "Defeat...");
        resultMessageLabel.setText(playerWon
                ? "Congratulations! You defeated " + cap(opponent.getName()) + "!"
                : "You lost against " + cap(opponent.getName()) + ". Better luck next time!");

        battleResultCard.getStyleClass().removeAll("result-card-victory", "result-card-defeat");
        resultTitleLabel.getStyleClass().removeAll("result-title-victory", "result-title-defeat");
        resultMessageLabel.getStyleClass().removeAll("result-message-victory", "result-message-defeat");
        if (battleAgainResultButton != null)
            battleAgainResultButton.getStyleClass().removeAll("result-btn-again-victory", "result-btn-again-defeat");

        String v = playerWon ? "victory" : "defeat";
        battleResultCard.getStyleClass().add("result-card-" + v);
        resultTitleLabel.getStyleClass().add("result-title-" + v);
        resultMessageLabel.getStyleClass().add("result-message-" + v);
        if (battleAgainResultButton != null)
            battleAgainResultButton.getStyleClass().add("result-btn-again-" + v);

        battleResultOverlay.setOpacity(0);
        battleResultOverlay.setVisible(true);
        battleResultOverlay.setManaged(true);
        battleResultOverlay.setMouseTransparent(false);
        FadeTransition ft = new FadeTransition(Duration.millis(450), battleResultOverlay);
        ft.setToValue(1.0);
        ft.play();
        disableAllButtons();
    }

    @FXML
    private void onGoBackClicked() {
        if (confettiTimer != null) { confettiTimer.stop(); confettiTimer = null; }
        if (chatManager != null) { chatManager.shutdown(); }
        doDisconnectAndLeave();
    }

    @FXML
    private void onBattleAgainClicked() {
        if (confettiTimer != null) { confettiTimer.stop(); confettiTimer = null; }
        if (chatManager != null) { chatManager.shutdown(); }
        doDisconnectAndLeave();
    }

    //  Forfeit overlay 
    private void showForfeitOverlay() {
        forfeitOverlay.setVisible(true);
        forfeitOverlay.setManaged(true);
        forfeitOverlay.setMouseTransparent(false);
        forfeitOverlay.setOpacity(0);
        forfeitDialog.setScaleX(0.85);
        forfeitDialog.setScaleY(0.85);
        forfeitDialog.setOpacity(0);
        FadeTransition backdropFade = new FadeTransition(Duration.millis(220), forfeitOverlay);
        backdropFade.setFromValue(0);
        backdropFade.setToValue(1);
        backdropFade.setInterpolator(Interpolator.EASE_OUT);
        Timeline dialogPop = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(forfeitDialog.scaleXProperty(), 0.85),
                        new KeyValue(forfeitDialog.scaleYProperty(), 0.85),
                        new KeyValue(forfeitDialog.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(forfeitDialog.scaleXProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                        new KeyValue(forfeitDialog.scaleYProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                        new KeyValue(forfeitDialog.opacityProperty(), 1.0, Interpolator.EASE_OUT)));
        backdropFade.play();
        dialogPop.play();
    }

    private void hideForfeitOverlay(Runnable onFinished) {
        Timeline dismiss = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(forfeitDialog.scaleXProperty(), 1.0),
                        new KeyValue(forfeitDialog.scaleYProperty(), 1.0),
                        new KeyValue(forfeitDialog.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(forfeitDialog.scaleXProperty(), 0.88, Interpolator.EASE_IN),
                        new KeyValue(forfeitDialog.scaleYProperty(), 0.88, Interpolator.EASE_IN),
                        new KeyValue(forfeitDialog.opacityProperty(), 0.0, Interpolator.EASE_IN)));
        FadeTransition backdropFade = new FadeTransition(Duration.millis(200), forfeitOverlay);
        backdropFade.setFromValue(1);
        backdropFade.setToValue(0);
        backdropFade.setInterpolator(Interpolator.EASE_IN);
        backdropFade.setOnFinished(e -> {
            forfeitOverlay.setVisible(false);
            forfeitOverlay.setManaged(false);
            forfeitOverlay.setMouseTransparent(true);
            if (onFinished != null) onFinished.run();
        });
        dismiss.play();
        backdropFade.play();
    }

    @FXML
    private void onForfeitConfirmed() {
        hideForfeitOverlay(() -> {
            if (serverConnection != null && battleId != null) {
                try {
                    serverConnection.sendMessage(new ForfeitMessage(battleId));
                } catch (Exception e) {
                    System.err.println("[OnlineBattle] Failed to send forfeit: " + e.getMessage());
                }
            }
            battleEnded = true;
            battleLog.add("Player forfeited the battle.");
            saveBattleResult(false, opponent.getName());
            showResultOverlay(false);
        });
    }

    @FXML
    private void onForfeitCancelled() {
        hideForfeitOverlay(null);
    }

    private void doDisconnectAndLeave() {
        if (confettiTimer != null) { confettiTimer.stop(); confettiTimer = null; }
        if (serverConnection != null) {
            try { serverConnection.disconnect(); }
            catch (Exception e) { System.err.println("[OnlineBattle] Failed to close connection: " + e.getMessage()); }
        }
        MusicManager.getInstance().playRandomBGM(); // restore game OST
        SceneManager.clearData();
        SceneManager.switchSceneWithLoading("new_game.fxml", "Battle Setup", 1200, 700);
    }

    //  Button handlers 
    @FXML
    private void onFightClicked() {
        if (battlingOrWaiting()) return;
        showMoveSelection();
        updateMoveButtons();
    }

    @FXML
    private void onChangePokemonClicked() {
        if (battlingOrWaiting()) return;
        showPokemonSelection();
        updatePokemonButtons();
    }

    private void onRunClicked() {
        hideBattleQuickMenu();
        if (battleEnded) { doDisconnectAndLeave(); return; }
        MusicManager.getInstance().stopBGM();
        showForfeitOverlay();
    }

    @FXML
    private void onBackToActions() {
        showActionButtons();
    }

    private void onItemsClicked() {
        battleStatusLabel.setText("No items in online battle.");
    }

    private void onMoveSelected(Move move) {
        if (moveSent || battleEnded) return;
        String logEntry = "You chose " + cap(move.getName()) + "!";
        ActionMessage msg = ActionMessage.attack(battleId, move.getId(), move.getName(), turnCount);
        submitTurnAction(msg, logEntry, "Error sending move: ");
    }

    private void onPokemonSelected(PokemonInstance pokemon) {
        if (moveSent || battleEnded) return;
        int teamIndex = player.getTeam().indexOf(pokemon);
        String logEntry = "Switching to " + cap(pokemon.getName()) + "!";
        ActionMessage msg = ActionMessage.switchPokemon(battleId, teamIndex, turnCount);
        submitTurnAction(msg, logEntry, "Error sending switch: ");
    }

    private void submitTurnAction(ActionMessage actionMessage, String logEntry, String errorPrefix) {
        moveSent = true;
        disableMoveButtons();
        showActionButtons();
        setVisible(waitingLabel, true);
        waitingLabel.setText("⏳ Waiting for opponent's action...");
        attackButton.setDisable(true);
        changePokemonMainButton.setDisable(true);

        battleStatusLabel.setText(logEntry + " Waiting for opponent...");
        battleLog.add(logEntry);

        try {
            serverConnection.sendMessage(actionMessage);
        } catch (Exception e) {
            System.err.println("[OnlineBattle] Failed to send action: " + e.getMessage());
            battleStatusLabel.setText(errorPrefix + e.getMessage());
            moveSent = false;
            attackButton.setDisable(false);
            changePokemonMainButton.setDisable(false);
        }
    }

    //  Move buttons 

    private void updateMoveButtons() {
        activeMoveButtons.clear();
        moveButtonsContainer.getChildren().clear();
        PokemonInstance cur = player.getCurrentPokemon();
        PokemonInstance foe = opponent != null ? opponent.getCurrentPokemon() : null;
        var moves = cur.getBattleMoves();
        for (int i = 0; i < 4; i++) {
            if (i < moves.size()) {
                Move m = moves.get(i).getMove();
                int pp = moves.get(i).getCurrentPp();
                float eff = getMoveEffectiveness(m, foe);
                moveButtonsContainer.getChildren().add(createMoveRow(m, pp, eff));
            }
        }
    }

    private HBox createMoveRow(Move move, int currentPp, float effectiveness) {
        String type = (move.getType() != null) ? move.getType() : "normal";
        int maxPp = move.getPp() > 0 ? move.getPp() : currentPp;
        String grad = getTypeGradient(type);
        String border = getTypeBorderColor(type);

        Button moveBtn = new Button(cap(move.getName()));
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

        Label typeLabel = new Label(cap(type));
        typeLabel.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.75);-fx-font-style:italic;");
        Label ppLabel = new Label("PP: " + currentPp + "/" + maxPp);
        ppLabel.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.90);");
        VBox rightInfo = new VBox(2, typeLabel, ppLabel);
        rightInfo.setAlignment(Pos.CENTER_RIGHT);
        rightInfo.setMouseTransparent(true);
        StackPane.setAlignment(rightInfo, Pos.CENTER_RIGHT);
        StackPane.setMargin(rightInfo, new Insets(0, INFO_BTN_WIDTH + INFO_BTN_INSET_RIGHT + 6, 0, 0));

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
        iBtn.setOnAction(e -> {});

        Label effLabel = createEffectivenessIndicator(effectiveness);
        if (effLabel != null) {
            StackPane.setAlignment(effLabel, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(effLabel, new Insets(0, EFF_ICON_INSET_RIGHT, EFF_ICON_INSET_BOTTOM, 0));
        }

        StackPane wrapper = new StackPane();
        wrapper.getChildren().add(moveBtn);
        wrapper.getChildren().add(rightInfo);
        wrapper.getChildren().add(iBtn);
        if (effLabel != null) wrapper.getChildren().add(effLabel);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        HBox row = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return row;
    }

    private void disableMoveButtons() {
        for (Button b : activeMoveButtons) b.setDisable(true);
        hideInfoOverlay();
    }

    private float getMoveEffectiveness(Move move, PokemonInstance defender) {
        if (defender == null || move == null) return 1.0f;
        return Battle.getTypeEffectivenessMultiplier(move.getType(), defender.getTypes());
    }

    private Label createEffectivenessIndicator(float effectiveness) {
        if (effectiveness > 1.0f) return buildEffLabel("▲", "move-eff-indicator move-eff-super");
        if (effectiveness > 0.0f && effectiveness < 1.0f) return buildEffLabel("▼", "move-eff-indicator move-eff-notvery");
        if (effectiveness == 0.0f) return buildEffLabel("x", "move-eff-indicator move-eff-immune");
        return null;
    }

    private Label buildEffLabel(String text, String classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes.split("\\s+"));
        label.setMouseTransparent(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(900), label);
        tt.setByY(-2.5);
        tt.setAutoReverse(true);
        tt.setCycleCount(TranslateTransition.INDEFINITE);
        tt.play();
        return label;
    }

    //  Type colour helpers 
    private String getTypeGradient(String type) {
        if (type == null) return "linear-gradient(to bottom,#546e7a,#37474f)";
        return switch (type.toLowerCase()) {
            case "normal"   -> "linear-gradient(to bottom,#b5c1d4,#bdd9e1)";
            case "fire"     -> "linear-gradient(to bottom,#F08030,#A84820)";
            case "water"    -> "linear-gradient(to bottom,#6890F0,#3860C0)";
            case "electric" -> "linear-gradient(to bottom,#C8A800,#906800)";
            case "grass"    -> "linear-gradient(to bottom,#78C850,#489820)";
            case "ice"      -> "linear-gradient(to bottom,#68B8B8,#3888A0)";
            case "fighting" -> "linear-gradient(to bottom,#C03028,#801010)";
            case "poison"   -> "linear-gradient(to bottom,#A040A0,#702070)";
            case "ground"   -> "linear-gradient(to bottom,#B89838,#806818)";
            case "flying"   -> "linear-gradient(to bottom,#7868C0,#584890)";
            case "psychic"  -> "linear-gradient(to bottom,#F85888,#A81040)";
            case "bug"      -> "linear-gradient(to bottom,#788800,#506000)";
            case "rock"     -> "linear-gradient(to bottom,#B8A038,#887010)";
            case "ghost"    -> "linear-gradient(to bottom,#705898,#402870)";
            case "dragon"   -> "linear-gradient(to bottom,#7038F8,#4008C8)";
            case "dark"     -> "linear-gradient(to bottom,#705848,#402818)";
            case "steel"    -> "linear-gradient(to bottom,#8898A8,#607080)";
            case "fairy"    -> "linear-gradient(to bottom,#D87898,#A05070)";
            default         -> "linear-gradient(to bottom,#546e7a,#37474f)";
        };
    }

    private String getTypeBorderColor(String type) {
        if (type == null) return "#263238";
        return switch (type.toLowerCase()) {
            case "normal"   -> "#b5c1d4"; case "fire"     -> "#7A2800"; case "water"    -> "#183890";
            case "electric" -> "#604800"; case "grass"    -> "#286800"; case "ice"      -> "#186070";
            case "fighting" -> "#500000"; case "poison"   -> "#480048"; case "ground"   -> "#604808";
            case "flying"   -> "#382880"; case "psychic"  -> "#780028"; case "bug"      -> "#304800";
            case "rock"     -> "#584808"; case "ghost"    -> "#200048"; case "dragon"   -> "#200098";
            case "dark"     -> "#201008"; case "steel"    -> "#384860"; case "fairy"    -> "#783048";
            default         -> "#263238";
        };
    }

    private String getTypeColor(String type) {
        if (type == null) return "#68A090";
        return switch (type.toLowerCase()) {
            case "grass"    -> "#78C850"; case "fire"     -> "#F08030"; case "water"    -> "#6890F0";
            case "electric" -> "#F8D030"; case "psychic"  -> "#F85888"; case "ice"      -> "#98D8D8";
            case "dragon"   -> "#7038F8"; case "dark"     -> "#705848"; case "fairy"    -> "#EE99AC";
            case "normal"   -> "#A8A878"; case "fighting" -> "#C03028"; case "flying"   -> "#A890F0";
            case "poison"   -> "#A040A0"; case "ground"   -> "#E0C068"; case "rock"     -> "#B8A038";
            case "bug"      -> "#A8B820"; case "ghost"    -> "#705898"; case "steel"    -> "#B8B8D0";
            default         -> "#68A090";
        };
    }

    //  Display helpers 
    private void updateBattleDisplay() {
        updateSide(true, player);
        updateSide(false, opponent);
    }

    private void updateSide(boolean isPlayer, Player p) {
        PokemonInstance pok = p.getCurrentPokemon();
        if (pok == null) return;
        String direction = isPlayer ? "back" : "front";
        ImageView target = isPlayer ? playerSpriteImage : opponentSpriteImage;
        double basePx = isPlayer ? SPRITE_PLAYER_BASE_PX : SPRITE_OPPONENT_BASE_PX;
        double minPx  = isPlayer ? SPRITE_PLAYER_MIN_PX  : SPRITE_OPPONENT_MIN_PX;
        double maxPx  = isPlayer ? SPRITE_PLAYER_MAX_PX  : SPRITE_OPPONENT_MAX_PX;
        double scaledPx = getScaledSpritePx(pok.getId(), basePx, minPx, maxPx);
        target.setFitWidth(scaledPx);
        target.setFitHeight(scaledPx);
        loadSpriteWithFallback(target, pok.getId(), direction);
        String nameHp = cap(pok.getName()) + "  Lv." + pok.getLevel();
        if (isPlayer) {
            playerPokemonNameLabel.setText(nameHp);
            playerPokemonHpLabel.setText(pok.getCurrentHp() + " / " + pok.getMaxHp());
            updateHpBar(playerHpBar, pok.getCurrentHp(), pok.getMaxHp());
            updateTypeBadges(playerTypesBox, pok.getTypes());
        } else {
            opponentPokemonNameLabel.setText(nameHp);
            opponentPokemonHpLabel.setText(pok.getCurrentHp() + " / " + pok.getMaxHp());
            updateHpBar(opponentHpBar, pok.getCurrentHp(), pok.getMaxHp());
            updateTypeBadges(opponentTypesBox, pok.getTypes());
        }
    }

    private void updateTypeBadges(HBox typesBox, List<String> types) {
        if (typesBox == null) return;
        typesBox.getChildren().clear();
        for (String type : types) {
            Label typeLabel = new Label(type.substring(0, Math.min(3, type.length())).toUpperCase());
            typeLabel.setStyle("-fx-font-size: 9px; -fx-padding: 2 5; -fx-background-color: " +
                    getTypeColor(type) + "; -fx-text-fill: white; -fx-background-radius: 3; -fx-font-weight: bold;");
            typesBox.getChildren().add(typeLabel);
        }
    }

    //  Sprite helpers 

    private static java.util.Map<Integer, Double> loadPokemonHeights() {
        java.util.Map<Integer, Double> map = new java.util.HashMap<>();
        try (var stream = OnlineBattleController.class.getResourceAsStream(
                "/com/example/pokemonbattle/data/pokemon_heights.json")) {
            if (stream == null) { System.err.println("[OnlineBattleController] pokemon_heights.json not found"); return map; }
            String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher idM = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
            java.util.regex.Matcher hM  = java.util.regex.Pattern.compile("\"height\"\\s*:\\s*([\\d.]+)").matcher(json);
            java.util.List<long[]> ids = new java.util.ArrayList<>(), heights = new java.util.ArrayList<>();
            while (idM.find()) ids.add(new long[]{idM.start(), Long.parseLong(idM.group(1))});
            while (hM.find())  heights.add(new long[]{hM.start(), Double.doubleToLongBits(Double.parseDouble(hM.group(1)))});
            int hi = 0;
            for (long[] idEntry : ids) {
                long idPos = idEntry[0]; int id = (int) idEntry[1];
                while (hi < heights.size() && heights.get(hi)[0] < idPos) hi++;
                if (hi < heights.size()) { map.put(id, Double.longBitsToDouble(heights.get(hi)[1])); hi++; }
            }
            System.out.println("[OnlineBattleController] Loaded heights for " + map.size() + " Pokemon");
        } catch (Exception e) { System.err.println("[OnlineBattleController] Failed to load heights: " + e.getMessage()); }
        return map;
    }

    private double getScaledSpritePx(int pokemonId, double basePx, double minPx, double maxPx) {
        Double h = POKEMON_HEIGHTS.get(pokemonId);
        if (h == null || h <= 0) return basePx;
        return Math.max(minPx, Math.min(maxPx, basePx * Math.pow(h / SPRITE_STANDARD_HEIGHT_M, SPRITE_SCALE_EXPONENT)));
    }

    private void loadSpriteWithFallback(ImageView target, int pokemonId, String direction) {
        String gifPath = String.format("/com/example/pokemonbattle/sprites/%s/gif/%d.gif", direction, pokemonId);
        var gifUrl = getClass().getResource(gifPath);
        if (gifUrl != null) {
            try {
                Image gif = new Image(gifUrl.toExternalForm(),
                        target.getFitWidth() > 0 ? target.getFitWidth() : 0,
                        target.getFitHeight() > 0 ? target.getFitHeight() : 0, true, true, true);
                if (!gif.isError()) { target.setImage(gif); return; }
            } catch (Exception e) { System.err.println("GIF load error: " + e.getMessage()); }
        }
        String pngPath = String.format("/com/example/pokemonbattle/sprites/%s/%d.png", direction, pokemonId);
        try {
            var pngStream = getClass().getResourceAsStream(pngPath);
            if (pngStream != null) {
                Image png = new Image(pngStream,
                        target.getFitWidth() > 0 ? target.getFitWidth() : 0,
                        target.getFitHeight() > 0 ? target.getFitHeight() : 0, true, true);
                if (!png.isError()) target.setImage(png);
            }
        } catch (Exception e) { System.err.println("PNG fallback error: " + e.getMessage()); }
    }

    private void loadPokemonPngSprite(ImageView sprite, int pokemonId, double size) {
        String pngPath = "/com/example/pokemonbattle/sprites/front/" + pokemonId + ".png";
        try {
            var pngStream = getClass().getResourceAsStream(pngPath);
            if (pngStream != null) {
                Image png = new Image(pngStream, size, size, true, true);
                if (!png.isError()) sprite.setImage(png);
            }
        } catch (Exception e) { System.err.println("Failed to load Pokemon sprite: " + e.getMessage()); }
    }

    private void updateHpBar(Rectangle bar, int hp, int maxHp) {
        if (bar == null || maxHp <= 0) return;
        double ratio = Math.max(0, (double) hp / maxHp);
        bar.setWidth(HP_BAR_MAX_WIDTH * ratio);
        if (ratio > 0.5)      bar.setFill(Color.web("#78C850"));
        else if (ratio > 0.2) bar.setFill(Color.web("#F8D030"));
        else                  bar.setFill(Color.web("#F85888"));
    }

    //  Pokemon buttons 
    private void updatePokemonButtons() {
        pokemonButtonsBox.getChildren().clear();
        for (PokemonInstance pokemon : player.getTeam()) {
            pokemonButtonsBox.getChildren().add(createPokemonRow(pokemon));
        }
    }

    private HBox createPokemonRow(PokemonInstance pokemon) {
        boolean isFainted = pokemon.isFainted();
        boolean isCurrent = pokemon == player.getCurrentPokemon();

        ImageView sprite = new ImageView();
        sprite.setFitWidth(POKEMON_SPRITE_SIZE);
        sprite.setFitHeight(POKEMON_SPRITE_SIZE);
        sprite.setPreserveRatio(true);

        String gifPath = "/com/example/pokemonbattle/sprites/front/gif/" + pokemon.getId() + ".gif";
        var gifUrl = getClass().getResource(gifPath);
        if (gifUrl != null) {
            try {
                Image gif = new Image(gifUrl.toExternalForm(), POKEMON_SPRITE_SIZE, POKEMON_SPRITE_SIZE, true, true, true);
                if (!gif.isError()) sprite.setImage(gif);
                else loadPokemonPngSprite(sprite, pokemon.getId(), POKEMON_SPRITE_SIZE);
            } catch (Exception e) { loadPokemonPngSprite(sprite, pokemon.getId(), POKEMON_SPRITE_SIZE); }
        } else { loadPokemonPngSprite(sprite, pokemon.getId(), POKEMON_SPRITE_SIZE); }

        if (isFainted) sprite.setOpacity(0.4);

        VBox textContent = new VBox(2);
        textContent.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(cap(pokemon.getName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white;");
        Label levelLabel = new Label("Lv. " + pokemon.getLevel() + "  •  HP: " + pokemon.getCurrentHp() + "/" + pokemon.getMaxHp());
        levelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85);");
        textContent.getChildren().addAll(nameLabel, levelLabel);

        HBox contentBox = new HBox(10, sprite, textContent);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        StackPane buttonBase = new StackPane(contentBox);
        buttonBase.setPadding(new Insets(8, 50, 8, 8));
        buttonBase.setPrefHeight(POKEMON_BTN_HEIGHT);
        buttonBase.setMaxWidth(Double.MAX_VALUE);

        String bgColor = isFainted ? "rgba(100, 100, 100, 0.3)"
                : isCurrent ? "rgba(120, 200, 80, 0.25)"
                : "linear-gradient(to right, rgba(106, 173, 140, 0.4), rgba(126, 189, 185, 0.3))";

        buttonBase.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.3);" +
                "-fx-border-radius: 10; -fx-border-width: 1.5;" +
                "-fx-cursor: " + (isFainted || isCurrent ? "default" : "hand") + ";");

        Button infoBtn = new Button("i");
        infoBtn.getStyleClass().add("move-info-btn");
        infoBtn.setStyle(infoBtn.getStyle() +
                "-fx-min-width:" + POKEMON_INFO_BTN_SIZE + ";-fx-max-width:" + POKEMON_INFO_BTN_SIZE + ";" +
                "-fx-min-height:" + POKEMON_INFO_BTN_SIZE + ";-fx-max-height:" + POKEMON_INFO_BTN_SIZE + ";");
        StackPane.setAlignment(infoBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(infoBtn, new Insets(0, 8, 0, 0));
        infoBtn.setOnMouseEntered(e -> showPokemonInfoOverlay(infoBtn, pokemon));
        infoBtn.setOnMouseExited(e -> hideInfoOverlay());
        infoBtn.setOnAction(e -> {});
        buttonBase.getChildren().add(infoBtn);

        if (!isFainted && !isCurrent) {
            buttonBase.setOnMouseClicked(e -> onPokemonSelected(pokemon));
            buttonBase.setOnMouseEntered(e -> buttonBase.setStyle(buttonBase.getStyle() + "-fx-background-color: rgba(126, 189, 185, 0.5);"));
            buttonBase.setOnMouseExited(e -> buttonBase.setStyle(buttonBase.getStyle() + "-fx-background-color: " + bgColor + ";"));
        }

        HBox row = new HBox(buttonBase);
        HBox.setHgrow(buttonBase, Priority.ALWAYS);
        return row;
    }

    //  Panel visibility 

    private void showActionButtons() {
        actionButtonsBox.setDisable(false);
        actionButtonsBox.setMouseTransparent(false);
        actionButtonsBox.setVisible(true);
        actionButtonsBox.setManaged(true);
        actionButtonsBox.toFront();

        if (attackButton != null)
            attackButton.setDisable(false);
        if (changePokemonMainButton != null)
            changePokemonMainButton.setDisable(false);
        if (itemsButton != null)
            itemsButton.setDisable(false);
        if (backButton != null)
            backButton.setDisable(false);

        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        moveSelectionBox.setMouseTransparent(true);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
        pokemonSelectionBox.setMouseTransparent(true);

        // Keep chat section visible
        showChatSection();
    }

    private void showMoveSelection() {
        moveSelectionBox.setVisible(true);
        moveSelectionBox.setManaged(true);
        moveSelectionBox.setMouseTransparent(false);
        moveSelectionBox.toFront();

        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        actionButtonsBox.setMouseTransparent(true);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
        pokemonSelectionBox.setMouseTransparent(true);

        // Keep chat section visible
        showChatSection();
    }

    private void showPokemonSelection() {
        pokemonSelectionBox.setVisible(true);
        pokemonSelectionBox.setManaged(true);
        pokemonSelectionBox.setMouseTransparent(false);
        pokemonSelectionBox.toFront();

        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        actionButtonsBox.setMouseTransparent(true);
        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        moveSelectionBox.setMouseTransparent(true);

        // Keep chat section visible
        showChatSection();
    }

    private void setVisible(javafx.scene.Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
    }

    private void disableAllButtons() {
        attackButton.setDisable(true);
        changePokemonMainButton.setDisable(true);
        if (itemsButton != null) itemsButton.setDisable(true);
        disableMoveButtons();
    }

    private boolean battlingOrWaiting() {
        if (battleEnded) { battleStatusLabel.setText("The battle has ended."); return true; }
        if (moveSent)    { battleStatusLabel.setText("Waiting for opponent's move..."); return true; }
        return false;
    }

    //  Options panel background 

    private void drawOptionsPanelPattern() {
        if (optionsSection == null) return;
        optionsSection.widthProperty().addListener((o, ov, nv) -> repaintPattern());
        optionsSection.heightProperty().addListener((o, ov, nv) -> repaintPattern());
        repaintPattern();
    }

    private void repaintPattern() {
        double w = optionsSection.getWidth(), h = optionsSection.getHeight();
        if (w <= 0 || h <= 0) return;
        optionsSection.getChildren().removeIf(n -> "patternCanvas".equals(n.getId()));
        Canvas canvas = new Canvas(w, h);
        canvas.setId("patternCanvas");
        canvas.setMouseTransparent(true);
        canvas.setManaged(false);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(new javafx.scene.paint.LinearGradient(0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#5cbdb0")),
                new javafx.scene.paint.Stop(0.5, Color.web("#3a9e8f")),
                new javafx.scene.paint.Stop(1, Color.web("#2d8a7c"))));
        gc.fillRect(0, 0, w, h);
        gc.setStroke(Color.color(1, 1, 1, 0.08));
        gc.setLineWidth(12);
        for (double i = -h; i < w + h; i += 36) gc.strokeLine(i, 0, i + h, h);
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

    //  Confetti 

    private void startConfetti() {
        if (confettiCanvas != null) rootPane.getChildren().remove(confettiCanvas);
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
            Color.web("#96CEB4"), Color.web("#FFEAA7"), Color.web("#DDA0DD"), Color.web("#98D8C8"), Color.web("#F7DC6F")
        };
        Color[] colors = new Color[N];
        Random rng = new Random();
        double sw = rootPane.getWidth() > 0 ? rootPane.getWidth() : 1200;
        for (int i = 0; i < N; i++) {
            x[i] = rng.nextDouble() * sw; y[i] = -rng.nextDouble() * 300;
            vx[i] = (rng.nextDouble() - 0.5) * 3.5; vy[i] = 2.5 + rng.nextDouble() * 3;
            ang[i] = rng.nextDouble() * Math.PI * 2; av[i] = (rng.nextDouble() - 0.5) * 0.14;
            sz[i] = 6 + rng.nextDouble() * 9; colors[i] = palette[rng.nextInt(palette.length)];
        }
        long[] t0 = {-1L};
        confettiTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (t0[0] < 0) t0[0] = now;
                double elapsed = (now - t0[0]) / 1_000_000_000.0;
                double alpha = Math.max(0.0, 1.0 - elapsed / 4.0);
                GraphicsContext gc = confettiCanvas.getGraphicsContext2D();
                gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());
                for (int i = 0; i < N; i++) {
                    x[i] += vx[i]; y[i] += vy[i]; ang[i] += av[i];
                    if (y[i] > confettiCanvas.getHeight() + 20) { y[i] = -12; x[i] = rng.nextDouble() * confettiCanvas.getWidth(); }
                    gc.save(); gc.setGlobalAlpha(alpha); gc.setFill(colors[i]);
                    gc.translate(x[i], y[i]); gc.rotate(Math.toDegrees(ang[i]));
                    gc.fillRect(-sz[i] / 2, -sz[i] / 4, sz[i], sz[i] / 2); gc.restore();
                }
                if (elapsed >= 4.0) {
                    stop(); gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());
                    rootPane.getChildren().remove(confettiCanvas);
                }
            }
        };
        confettiTimer.start();
    }

    //  DB persistence 

    private void saveBattleResult(boolean playerWon, String winnerName) {
        User user = PlayerSession.getInstance().getCurrentUser();
        if (user == null) return;
        String pokemonUsed = player.getTeam().stream().map(p -> p.getName().toLowerCase()).collect(Collectors.joining(","));
        String result = playerWon ? "WIN" : "LOSS";
        String logStr = String.join("\n", battleLog);
        Thread t = new Thread(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                String sql = "INSERT INTO battle_history (user_id,result,pokemon_used,opponent_type,opponent_name,battle_log) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, user.getId()); ps.setString(2, result); ps.setString(3, pokemonUsed);
                    ps.setString(4, "ONLINE"); ps.setString(5, opponent.getName()); ps.setString(6, logStr);
                    ps.executeUpdate();
                }
                String upsert = "INSERT INTO user_profiles (user_id,wins,losses,total_battles) VALUES (?,?,?,1) " +
                        "ON CONFLICT(user_id) DO UPDATE SET wins=wins+?,losses=losses+?,total_battles=total_battles+1";
                try (PreparedStatement ups = conn.prepareStatement(upsert)) {
                    int w = playerWon ? 1 : 0, l = playerWon ? 0 : 1;
                    ups.setInt(1, user.getId()); ups.setInt(2, w); ups.setInt(3, l);
                    ups.setInt(4, w); ups.setInt(5, l); ups.executeUpdate();
                }
                System.out.println("[OnlineBattle] Battle result saved: " + result + " vs " + opponent.getName());
            } catch (Exception e) { System.err.println("[OnlineBattle] Failed to save result: " + e.getMessage()); }
        });
        t.setDaemon(true);
        t.start();
    }

    // ---- Chat Section Methods ----

    /**
     * Initialize the chat section and manager
     */
    private void initializeChat() {
        if (chatSection != null && chatMessagesContainer != null && chatScrollPane != null) {
            chatManager = new ChatManager(chatMessagesContainer, chatScrollPane);

            // Set up quick message button handlers
            if (quickMsg1 != null) quickMsg1.setOnAction(e -> onQuickMessage1());
            if (quickMsg2 != null) quickMsg2.setOnAction(e -> onQuickMessage2());
            if (quickMsg3 != null) quickMsg3.setOnAction(e -> onQuickMessage3());
            if (quickMsg4 != null) quickMsg4.setOnAction(e -> onQuickMessage4());
            if (quickMsg5 != null) quickMsg5.setOnAction(e -> onQuickMessage5());
            if (quickMsg6 != null) quickMsg6.setOnAction(e -> onQuickMessage6());
            if (chatSendButton != null) chatSendButton.setOnAction(e -> onChatSendClicked());
            if (chatInputField != null) chatInputField.setOnAction(e -> onChatSendClicked());

            // Set up toggle button handler
            if (chatToggleButton != null) {
                chatToggleButton.setOnAction(e -> onChatToggleClicked());
            }

            chatExpanded = true;
            applyChatExpansionState();

            // Show chat section - it's always visible for online battles
            showChatSection();

            // Add welcome message
            chatManager.addSystemMessage("Online battle chat is ready!");
        }
    }

    /**
     * Show the chat section
     */
    private void showChatSection() {
        if (chatSection != null) {
            chatSection.setVisible(true);
            chatSection.setManaged(true);
        }
    }

    /**
     * Hide the chat section
     */
    private void hideChatSection() {
        if (chatSection != null) {
            chatSection.setVisible(false);
            chatSection.setManaged(false);
        }
    }

    /**
     * Toggle chat messages visibility
     */
    @FXML
    private void onChatToggleClicked() {
        if (chatScrollPane != null && quickMessagesBox != null && chatToggleButton != null) {
            chatExpanded = !chatExpanded;
            applyChatExpansionState();
        }
    }

    private void applyChatExpansionState() {
        if (chatSection == null || chatScrollPane == null || quickMessagesBox == null || chatToggleButton == null) {
            return;
        }

        chatScrollPane.setVisible(chatExpanded);
        chatScrollPane.setManaged(chatExpanded);
        quickMessagesBox.setVisible(chatExpanded);
        quickMessagesBox.setManaged(chatExpanded);

        double targetHeight = chatExpanded ? CHAT_SECTION_EXPANDED_HEIGHT : CHAT_SECTION_COLLAPSED_HEIGHT;
        chatSection.setMinHeight(chatExpanded ?  0: targetHeight);
        chatSection.setPrefHeight(targetHeight);
        chatSection.setMaxHeight(targetHeight);
        chatToggleButton.setText(chatExpanded ? "−" : "+");
    }

    private void sendQuickChatMessage(String text) {
        if (text == null || text.isBlank() || chatManager == null) {
            return;
        }

        chatManager.sendPlayerMessage(text);

        if (serverConnection == null || !serverConnection.isConnected() || battleId == null) {
            return;
        }

        String senderName = (player != null && player.getName() != null) ? player.getName() : "Player";
        try {
            serverConnection.sendMessage(new BattleChatMessage(battleId, senderName, text));
        } catch (IOException e) {
            chatManager.addSystemMessage("Failed to deliver chat message.");
            System.err.println("[OnlineBattle] Failed to send chat message: " + e.getMessage());
        }
    }

    /**
     * Quick message handlers
     */
    @FXML
    private void onQuickMessage1() {
        sendQuickChatMessage(ChatManager.QUICK_MESSAGES[0]);
    }

    @FXML
    private void onQuickMessage2() {
        sendQuickChatMessage(ChatManager.QUICK_MESSAGES[1]);
    }

    @FXML
    private void onQuickMessage3() {
        sendQuickChatMessage(ChatManager.QUICK_MESSAGES[2]);
    }

    @FXML
    private void onQuickMessage4() {
        sendQuickChatMessage(ChatManager.QUICK_MESSAGES[3]);
    }

    @FXML
    private void onQuickMessage5() {
        sendQuickChatMessage(ChatManager.QUICK_MESSAGES[4]);
    }

    @FXML
    private void onQuickMessage6() {
        sendQuickChatMessage(ChatManager.QUICK_MESSAGES[5]);
    }

    @FXML
    private void onChatSendClicked() {
        if (chatInputField == null) return;
        String text = chatInputField.getText();
        if (text == null || text.isBlank()) return;

        sendQuickChatMessage(text.trim());
        chatInputField.clear();
    }

    //  Utility
    public List<String> getBattleLog() {
        return new ArrayList<>(battleLog);
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}