package com.example.pokemonbattle.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.pokemonbattle.database.DatabaseManager;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.server.ActionMessage;
import com.example.pokemonbattle.server.BattleEndMessage;
import com.example.pokemonbattle.server.BattleUpdateMessage;
import com.example.pokemonbattle.server.DamageMessage;
import com.example.pokemonbattle.server.ForfeitMessage;
import com.example.pokemonbattle.server.GameMessage;
import com.example.pokemonbattle.server.ServerConnection;
import com.example.pokemonbattle.server.SwitchNotifyMessage;
import com.example.pokemonbattle.server.TurnReadyMessage;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
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

/**
 * Controller for ONLINE battle screen.
 * Server is the single source of truth for damage; no local calculation.
 */
public class OnlineBattleController {

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
    private HBox mainBattleLayout;

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
    private Label opponentNameLabel;
    @FXML
    private VBox playerPokemonBox;
    @FXML
    private VBox opponentPokemonBox;
    @FXML
    private Label playerTeamLabel;
    @FXML
    private Label opponentTeamLabel;
    @FXML
    private Label battleStatusLabel;

    @FXML
    private VBox actionButtonsBox;
    @FXML
    private Button attackButton;
    @FXML
    private Button changePokemonMainButton;
    @FXML
    private Button itemsButton;
    @FXML
    private Button backButton;
    @FXML
    private Label waitingLabel;

    @FXML
    private VBox moveSelectionBox;
    @FXML
    private VBox moveButtonsContainer; // replaces the 4 individual move buttons
    @FXML
    private VBox pokemonSelectionBox;
    @FXML
    private VBox pokemonButtonsBox;

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
    private StackPane forfeitOverlay;
    @FXML
    private Region forfeitBackdrop;
    @FXML
    private VBox forfeitDialog;
    @FXML
    private Button forfeitYesButton;
    @FXML
    private Button forfeitNoButton;

    // Move list constants (customise size / position of the "i" button here)
    private static final double MOVE_BTN_HEIGHT = 50.0;
    private static final double INFO_BTN_WIDTH = 18.0;
    private static final double INFO_BTN_HEIGHT = 18.0;
    private static final double INFO_BTN_INSET_TOP = 4.0;
    private static final double INFO_BTN_INSET_RIGHT = 4.0;

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

    // Info overlay (floating layer in rootPane)
    private Pane infoFloatingLayer;
    private VBox infoCard;
    private Label infoName, infoType, infoPower, infoAccuracy, infoPp, infoDescription;

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

        attackButton.setOnAction(e -> onFightClicked());
        changePokemonMainButton.setOnAction(e -> onChangePokemonClicked());
        backButton.setOnAction(e -> onRunClicked());
        if (itemsButton != null)
            itemsButton.setOnAction(e -> battleStatusLabel.setText("No items in online battle."));

        setVisible(waitingLabel, false);
        drawOptionsPanelPattern();
        setupInfoOverlay();

        battleStatusLabel.setText("Online Battle! " + cap(player.getCurrentPokemon().getName()) +
                " vs " + cap(opponent.getCurrentPokemon().getName()));

        MusicManager.getInstance().attachClickSounds(rootPane);
        Platform.runLater(this::playVSIntro);
    }

    // Info overlay

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
        infoName.setText(cap(move.getName()));
        String type = (move.getType() != null) ? move.getType() : "normal";
        infoType.setText("Type: " + cap(type));
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
        infoDescription.setText(desc);

        infoCard.setVisible(true);

        Platform.runLater(() -> {
            Bounds b = iBtn.localToScene(iBtn.getBoundsInLocal());
            Bounds r = rootPane.localToScene(rootPane.getBoundsInLocal());
            double cardW = infoCard.getWidth() > 10 ? infoCard.getWidth() : 200;
            double cardH = infoCard.getHeight() > 10 ? infoCard.getHeight() : 170;
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

    // Server message handler

    private void handleServerMessage(GameMessage msg) {
        Platform.runLater(() -> {
            switch (msg.getMessageType()) {
                case "DAMAGE" -> applyDamage((DamageMessage) msg);
                case "SWITCH_NOTIFY" -> applySwitchNotify((SwitchNotifyMessage) msg);
                case "BATTLE_UPDATE" -> applyBattleUpdate((BattleUpdateMessage) msg);
                case "TURN_READY" -> onTurnReady((TurnReadyMessage) msg);
                case "BATTLE_END" -> applyBattleEnd((BattleEndMessage) msg);
                default -> System.out.println("[OnlineBattle] Unknown msg: " + msg.getMessageType());
            }
        });
    }

    private void applyDamage(DamageMessage msg) {
        boolean targetIsMe = player.getName().equals(msg.getTargetName());
        Player target = targetIsMe ? player : opponent;
        PokemonInstance targetPok = target.getCurrentPokemon();
        if (targetPok == null)
            return;

        targetPok.setCurrentHp(msg.getTargetCurrentHp());
        updateBattleDisplay();

        String effText = "";
        if (msg.getEffectiveness() != null && msg.getEffectiveness() > 1.0f)
            effText = " (Super effective!)";
        else if (msg.getEffectiveness() != null && msg.getEffectiveness() < 1.0f && msg.getEffectiveness() > 0)
            effText = " (Not very effective...)";
        else if (msg.getEffectiveness() != null && msg.getEffectiveness() == 0f)
            effText = " (No effect)";

        String logEntry = cap(msg.getAttackerName()) + " used " + cap(msg.getMoveUsed()) + "! " + msg.getDamageDealt()
                + " dmg" + effText;
        battleStatusLabel.setText(logEntry);
        battleLog.add(logEntry);

        if (msg.isTargetFainted()) {
            targetPok.setFainted(true);
            PokemonInstance next = target.getFirstAvailablePokemon();
            if (next != null) {
                target.setCurrentPokemon(next);
                String faintEntry = cap(targetPok.getName()) + " fainted! " + cap(target.getName()) + " sends out "
                        + cap(next.getName()) + "!";
                battleStatusLabel.setText(faintEntry);
                battleLog.add(faintEntry);
                updateBattleDisplay();
            }
        }
    }

    private void applyBattleUpdate(BattleUpdateMessage msg) {
        battleStatusLabel.setText(msg.getMessage());
        battleLog.add(msg.getMessage());
        updateBattleDisplay();
    }

    private void applySwitchNotify(SwitchNotifyMessage msg) {
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
        turnCount = msg.getTurnNumber();
        moveSent = false;
        setVisible(waitingLabel, false);
        showActionButtons();
        attackButton.setDisable(false);
        changePokemonMainButton.setDisable(false);
    }

    private void applyBattleEnd(BattleEndMessage msg) {
        if (battleEnded)
            return;
        battleEnded = true;
        boolean playerWon = player.getName().equals(msg.getWinnerName());
        saveBattleResult(playerWon, msg.getWinnerName());
        showResultOverlay(playerWon);
    }

    private void handleDisconnect() {
        if (battleEnded)
            return;
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
        FadeTransition ft = new FadeTransition(Duration.millis(450), battleResultOverlay);
        ft.setToValue(1.0);
        ft.play();
    }

    // Result overlay

    private void showResultOverlay(boolean playerWon) {
        if (playerWon) {
            MusicManager.getInstance().stopBGM();
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

        String v = playerWon ? "victory" : "defeat";
        battleResultCard.getStyleClass().add("result-card-" + v);
        resultTitleLabel.getStyleClass().add("result-title-" + v);
        resultMessageLabel.getStyleClass().add("result-message-" + v);

        battleResultOverlay.setOpacity(0);
        battleResultOverlay.setVisible(true);
        battleResultOverlay.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(450), battleResultOverlay);
        ft.setToValue(1.0);
        ft.play();
        disableAllButtons();
    }

    @FXML
    private void onGoBackClicked() {
        if (confettiTimer != null) {
            confettiTimer.stop();
            confettiTimer = null;
        }
        doDisconnectAndLeave();
    }

    // Forfeit overlay

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
            if (onFinished != null)
                onFinished.run();
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
        if (confettiTimer != null) {
            confettiTimer.stop();
            confettiTimer = null;
        }
        if (serverConnection != null) {
            try {
                serverConnection.disconnect();
            } catch (Exception e) {
                System.err.println("[OnlineBattle] Failed to close connection: " + e.getMessage());
            }
        }
        SceneManager.clearData();
        SceneManager.switchSceneWithLoading("new_game.fxml", "Battle Setup", 1200, 700);
    }

    // Button handlers

    @FXML
    private void onFightClicked() {
        if (battlingOrWaiting())
            return;
        showMoveSelection();
        updateMoveButtons();
    }

    @FXML
    private void onChangePokemonClicked() {
        if (battlingOrWaiting())
            return;
        showPokemonSelection();
        updatePokemonButtons();
    }

    private void onRunClicked() {
        if (battleEnded) {
            doDisconnectAndLeave();
            return;
        }
        showForfeitOverlay();
    }

    @FXML
    private void onBackToActions() {
        showActionButtons();
    }

    private void onMoveSelected(Move move) {
        if (moveSent || battleEnded)
            return;
        moveSent = true;
        disableMoveButtons();
        showActionButtons();
        setVisible(waitingLabel, true);
        waitingLabel.setText("⏳ Waiting for opponent's move...");
        attackButton.setDisable(true);
        changePokemonMainButton.setDisable(true);

        String logEntry = "You chose " + cap(move.getName()) + "!";
        battleStatusLabel.setText(logEntry + " Waiting for opponent...");
        battleLog.add(logEntry);

        ActionMessage msg = ActionMessage.attack(battleId, move.getId(), move.getName(), turnCount);
        try {
            serverConnection.sendMessage(msg);
        } catch (Exception e) {
            System.err.println("[OnlineBattle] Failed to send action: " + e.getMessage());
            battleStatusLabel.setText("Error sending move: " + e.getMessage());
            moveSent = false;
            attackButton.setDisable(false);
        }
    }

    private void onPokemonSelected(PokemonInstance pokemon) {
        if (moveSent || battleEnded)
            return;
        moveSent = true;
        int teamIndex = player.getTeam().indexOf(pokemon);
        player.setCurrentPokemon(pokemon);
        String logEntry = "Switching to " + cap(pokemon.getName()) + "!";
        battleStatusLabel.setText(logEntry + " Waiting for opponent...");
        battleLog.add(logEntry);
        updateBattleDisplay();
        showActionButtons();
        setVisible(waitingLabel, true);
        waitingLabel.setText("⏳ Waiting for opponent's move...");
        attackButton.setDisable(true);
        changePokemonMainButton.setDisable(true);

        ActionMessage msg = ActionMessage.switchPokemon(battleId, teamIndex, turnCount);
        try {
            serverConnection.sendMessage(msg);
        } catch (Exception e) {
            System.err.println("[OnlineBattle] Failed to send switch: " + e.getMessage());
            battleStatusLabel.setText("Error sending switch: " + e.getMessage());
            moveSent = false;
            attackButton.setDisable(false);
            changePokemonMainButton.setDisable(false);
        }
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
        iBtn.setOnMouseExited(e -> hideInfoOverlay());
        iBtn.setOnAction(e -> {
        });

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

    // Type colour helpers

    private String getTypeGradient(String type) {
        if (type == null)
            return "linear-gradient(to bottom,#546e7a,#37474f)";
        return switch (type.toLowerCase()) {
            case "normal" -> "linear-gradient(to bottom,#cbe3e5,a5b4bc)";
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
            case "normal" -> "#a5b1bc";
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

    // Display helpers

    private void updateBattleDisplay() {
        updateSide(true, player);
        updateSide(false, opponent);
    }

    private void updateSide(boolean isPlayer, Player p) {
        PokemonInstance pok = p.getCurrentPokemon();
        if (pok == null)
            return;
        String direction = isPlayer ? "back" : "front";
        ImageView target = isPlayer ? playerSpriteImage : opponentSpriteImage;
        double basePx = isPlayer ? SPRITE_PLAYER_BASE_PX : SPRITE_OPPONENT_BASE_PX;
        double minPx = isPlayer ? SPRITE_PLAYER_MIN_PX : SPRITE_OPPONENT_MIN_PX;
        double maxPx = isPlayer ? SPRITE_PLAYER_MAX_PX : SPRITE_OPPONENT_MAX_PX;
        double scaledPx = getScaledSpritePx(pok.getId(), basePx, minPx, maxPx);
        target.setFitWidth(scaledPx);
        target.setFitHeight(scaledPx);
        loadSpriteWithFallback(target, pok.getId(), direction);
        String nameHp = cap(pok.getName()) + "  Lv." + pok.getLevel();
        if (isPlayer) {
            playerPokemonNameLabel.setText(nameHp);
            playerPokemonHpLabel.setText(pok.getCurrentHp() + " / " + pok.getMaxHp());
            updateHpBar(playerHpBar, pok.getCurrentHp(), pok.getMaxHp());
        } else {
            opponentPokemonNameLabel.setText(nameHp);
            opponentPokemonHpLabel.setText(pok.getCurrentHp() + " / " + pok.getMaxHp());
            updateHpBar(opponentHpBar, pok.getCurrentHp(), pok.getMaxHp());
        }
    }

    // Sprite helpers

    private static java.util.Map<Integer, Double> loadPokemonHeights() {
        java.util.Map<Integer, Double> map = new java.util.HashMap<>();
        try (var stream = OnlineBattleController.class.getResourceAsStream(
                "/com/example/pokemonbattle/data/pokemon_heights.json")) {
            if (stream == null) {
                System.err.println("[OnlineBattleController] pokemon_heights.json not found");
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
            System.out.println("[OnlineBattleController] Loaded heights for " + map.size() + " Pokemon");
        } catch (Exception e) {
            System.err.println("[OnlineBattleController] Failed to load heights: " + e.getMessage());
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
                System.err.println("GIF load error: " + e.getMessage());
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
            System.err.println("PNG fallback error: " + e.getMessage());
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

    // Pokemon buttons

    private void updatePokemonButtons() {
        pokemonButtonsBox.getChildren().clear();
        for (PokemonInstance p : player.getTeam()) {
            Button btn = new Button(cap(p.getName()) + (p.isFainted() ? " (Fainted)" : "  Lv." + p.getLevel()));
            btn.setPrefWidth(260);
            btn.setPrefHeight(42);
            btn.getStyleClass().addAll("option-btn", "option-btn-green");
            btn.setStyle("-fx-font-size: 13px;");
            if (p.isFainted()) {
                btn.setDisable(true);
                btn.setStyle("-fx-font-size: 13px; -fx-opacity: 0.5;");
            } else if (p == player.getCurrentPokemon()) {
                btn.setText(btn.getText() + " ✓");
                btn.setDisable(true);
            } else {
                btn.setOnAction(e -> onPokemonSelected(p));
            }
            pokemonButtonsBox.getChildren().add(btn);
        }
    }

    // Panel visibility

    private void showActionButtons() {
        setVisible(actionButtonsBox, true);
        setVisible(moveSelectionBox, false);
        setVisible(pokemonSelectionBox, false);
    }

    private void showMoveSelection() {
        setVisible(moveSelectionBox, true);
        setVisible(actionButtonsBox, false);
        setVisible(pokemonSelectionBox, false);
    }

    private void showPokemonSelection() {
        setVisible(pokemonSelectionBox, true);
        setVisible(actionButtonsBox, false);
        setVisible(moveSelectionBox, false);
    }

    private void setVisible(javafx.scene.Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
    }

    private void disableAllButtons() {
        attackButton.setDisable(true);
        changePokemonMainButton.setDisable(true);
        if (itemsButton != null)
            itemsButton.setDisable(true);
        disableMoveButtons();
    }

    private boolean battlingOrWaiting() {
        if (battleEnded) {
            battleStatusLabel.setText("The battle has ended.");
            return true;
        }
        if (moveSent) {
            battleStatusLabel.setText("Waiting for opponent's move...");
            return true;
        }
        return false;
    }

    // Options panel background

    private void drawOptionsPanelPattern() {
        if (optionsSection == null)
            return;
        optionsSection.widthProperty().addListener((o, ov, nv) -> repaintPattern());
        optionsSection.heightProperty().addListener((o, ov, nv) -> repaintPattern());
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

    private void saveBattleResult(boolean playerWon, String winnerName) {
        User user = PlayerSession.getInstance().getCurrentUser();
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
                    ps.setString(4, "ONLINE");
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
                System.out.println("[OnlineBattle] Battle result saved: " + result + " vs " + opponent.getName());
            } catch (Exception e) {
                System.err.println("[OnlineBattle] Failed to save result: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }
    // Utility
    public List<String> getBattleLog() {
        return new ArrayList<>(battleLog);
    }

    private String cap(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}