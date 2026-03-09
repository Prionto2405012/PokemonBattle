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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
/**
 * Controller for ONLINE battle screen.
 * Sends moves to the TCP server and updates HP from server-authoritative DamageMessages.
 * No local damage calculation — the server is the single source of truth.
 */
public class OnlineBattleController {

    // FXML injections
    @FXML private StackPane  rootPane;
    @FXML private ImageView  bgImage;
    @FXML private StackPane  battleSection;
    @FXML private AnchorPane battleField;
    @FXML private StackPane  optionsSection;
    @FXML private HBox       mainBattleLayout;

    @FXML private ImageView playerSpriteImage;
    @FXML private ImageView opponentSpriteImage;

    @FXML private Label playerPokemonNameLabel;
    @FXML private Label playerPokemonHpLabel;
    @FXML private Label opponentPokemonNameLabel;
    @FXML private Label opponentPokemonHpLabel;
    @FXML private Rectangle playerHpBar;
    @FXML private Rectangle opponentHpBar;

    // Hidden info refs
    @FXML private Label playerNameLabel;
    @FXML private Label opponentNameLabel;
    @FXML private VBox  playerPokemonBox;
    @FXML private VBox  opponentPokemonBox;
    @FXML private Label playerTeamLabel;
    @FXML private Label opponentTeamLabel;

    @FXML private Label  battleStatusLabel;

    // Action panel
    @FXML private VBox   actionButtonsBox;
    @FXML private Button attackButton;
    @FXML private Button changePokemonMainButton;
    @FXML private Button itemsButton;
    @FXML private Button backButton;
    @FXML private Label  waitingLabel;

    // Move selection panel
    @FXML private VBox   moveSelectionBox;
    @FXML private Button moveButton1;
    @FXML private Button moveButton2;
    @FXML private Button moveButton3;
    @FXML private Button moveButton4;

    // Pokemon switch panel
    @FXML private VBox   pokemonSelectionBox;
    @FXML private VBox   pokemonButtonsBox;

    // VS Intro Screen nodes
    @FXML private AnchorPane vsScreenPane;
    @FXML private ImageView  vsBgImage;
    @FXML private ImageView  vsPlayerSprite;
    @FXML private ImageView  vsOpponentSprite;

    // Black fade overlay
    @FXML private Region rootBlackFade;

    // Battle Result Overlay nodes
    @FXML private StackPane battleResultOverlay;
    @FXML private VBox      battleResultCard;
    @FXML private Label     resultTitleLabel;
    @FXML private Label     resultMessageLabel;
    @FXML private Button    goBackResultButton;

    // Forfeit Confirmation Overlay nodes
    @FXML private StackPane forfeitOverlay;
    @FXML private Region    forfeitBackdrop;
    @FXML private VBox      forfeitDialog;
    @FXML private Button    forfeitYesButton;
    @FXML private Button    forfeitNoButton;

    // State
    private Player           player;
    private Player           opponent;
    private ServerConnection serverConnection;
    private Integer          battleId;
    private String           opponentAvatarPath;
    private int              turnCount = 0;
    private boolean          battleEnded = false;
    private boolean          moveSent    = false;

    // Battle log
    private final List<String> battleLog = new ArrayList<>();

    // Confetti and battle overlay
    private Canvas confettiCanvas;
    private AnimationTimer confettiTimer;
    private static final double HP_BAR_MAX_WIDTH = 180.0;

    // ── Sprite scaling constants (same as BattleController) ──────────────
    private static final double SPRITE_STANDARD_HEIGHT_M = 1.0;
    private static final double SPRITE_OPPONENT_BASE_PX = 200.0;
    private static final double SPRITE_PLAYER_BASE_PX = 300.0;
    private static final double SPRITE_MIN_PX = 130.0;
    private static final double SPRITE_MAX_PX = 380.0;
    private static final double SPRITE_SCALE_EXPONENT = 0.5;
    private static final java.util.Map<Integer, Double> POKEMON_HEIGHTS = loadPokemonHeights();

    // ── VS Intro Animation Constants ─────────────────────────────────────
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

        player           = (Player)           SceneManager.getData("player");
        opponent         = (Player)           SceneManager.getData("opponent");
        serverConnection = (ServerConnection) SceneManager.getData("serverConnection");
        battleId         = (Integer)          SceneManager.getData("battleId");
        opponentAvatarPath = (String)         SceneManager.getData("opponentAvatarPath");

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
        if (itemsButton != null) itemsButton.setOnAction(e ->
                battleStatusLabel.setText("No items in online battle."));

        setVisible(waitingLabel, false);
        drawOptionsPanelPattern();

        battleStatusLabel.setText("Online Battle! " + cap(player.getCurrentPokemon().getName()) +
                " vs " + cap(opponent.getCurrentPokemon().getName()));

        System.out.println("[OnlineBattle] Battle started vs " + opponent.getName());
        MusicManager.getInstance().attachClickSounds(rootPane);

        // Kick off VS intro on next frame
        Platform.runLater(this::playVSIntro);
    }

    // ── VS INTRO ────────────────────────────────────────────────────────────

    private void playVSIntro() {
        // Load opponent avatar (use opponent's actual avatar, fall back to random NPC)
        boolean opponentAvatarLoaded = false;
        if (opponentAvatarPath != null && !opponentAvatarPath.isEmpty()) {
            var oppUrl = getClass().getResource(opponentAvatarPath);
            if (oppUrl != null) {
                vsOpponentSprite.setImage(new Image(oppUrl.toExternalForm(), 0, 0, true, true));
                opponentAvatarLoaded = true;
            }
        }
        if (!opponentAvatarLoaded) {
            int npcId = new Random().nextInt(7) + 1;
            String npcPath = "/com/example/pokemonbattle/sprites/trainer/npc/" + npcId + ".png";
            var npcUrl = getClass().getResource(npcPath);
            if (npcUrl != null) {
                vsOpponentSprite.setImage(new Image(npcUrl.toExternalForm(), 0, 0, true, true));
            }
        }

        // Player trainer avatar
        String avatarPath = PlayerSession.getInstance().getAvatarPath();
        if (avatarPath != null) {
            var avatarUrl = getClass().getResource(avatarPath);
            if (avatarUrl != null) {
                vsPlayerSprite.setImage(new Image(avatarUrl.toExternalForm(), 0, 0, true, true));
            }
        }

        vsPlayerSprite.setTranslateX(-VS_OFFSCREEN_OFFSET);
        vsOpponentSprite.setTranslateX(VS_OFFSCREEN_OFFSET);

        // Phase 1: Slide in
        TranslateTransition playerSlideIn = new TranslateTransition(
                Duration.millis(VS_SLIDE_IN_MS), vsPlayerSprite);
        playerSlideIn.setToX(VS_SLIDE_STOP_OFFSET);
        playerSlideIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition opponentSlideIn = new TranslateTransition(
                Duration.millis(VS_SLIDE_IN_MS), vsOpponentSprite);
        opponentSlideIn.setToX(-VS_SLIDE_STOP_OFFSET);
        opponentSlideIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition slideIn = new ParallelTransition(playerSlideIn, opponentSlideIn);

        // Phase 2: Slow drift
        double driftMs = VS_DRIFT_HOLD_MS - VS_SLIDE_OUT_EARLY_MS;

        TranslateTransition playerDrift = new TranslateTransition(
                Duration.millis(driftMs), vsPlayerSprite);
        playerDrift.setByX(VS_DRIFT_AMOUNT);
        playerDrift.setInterpolator(Interpolator.LINEAR);

        TranslateTransition opponentDrift = new TranslateTransition(
                Duration.millis(driftMs), vsOpponentSprite);
        opponentDrift.setByX(-VS_DRIFT_AMOUNT);
        opponentDrift.setInterpolator(Interpolator.LINEAR);

        ParallelTransition drift = new ParallelTransition(playerDrift, opponentDrift);

        // Phase 3: Slide out
        TranslateTransition playerSlideOut = new TranslateTransition(
                Duration.millis(VS_SLIDE_OUT_MS), vsPlayerSprite);
        playerSlideOut.setByX(VS_OFFSCREEN_OFFSET * 2.2);
        playerSlideOut.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition opponentSlideOut = new TranslateTransition(
                Duration.millis(VS_SLIDE_OUT_MS), vsOpponentSprite);
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

        FadeTransition fadeToBlack = new FadeTransition(
                Duration.millis(VS_FADE_TO_BLACK_MS), rootBlackFade);
        fadeToBlack.setToValue(1.0);

        fadeToBlack.setOnFinished(e -> {
            vsScreenPane.setVisible(false);
            vsScreenPane.setManaged(false);
            mainBattleLayout.setVisible(true);
            mainBattleLayout.setManaged(true);

            updateBattleDisplay();

            FadeTransition fadeFromBlack = new FadeTransition(
                    Duration.millis(VS_FADE_FROM_BLACK_MS), rootBlackFade);
            fadeFromBlack.setToValue(0.0);
            fadeFromBlack.setOnFinished(ev -> rootBlackFade.setVisible(false));
            fadeFromBlack.play();
        });

        fadeToBlack.play();
    }

    // ── Server message handler ──────────────────────────────────────────────

    private void handleServerMessage(GameMessage msg) {
        Platform.runLater(() -> {
            switch (msg.getMessageType()) {
                case "DAMAGE"         -> applyDamage((DamageMessage)        msg);
                case "SWITCH_NOTIFY"  -> applySwitchNotify((SwitchNotifyMessage) msg);
                case "BATTLE_UPDATE"  -> applyBattleUpdate((BattleUpdateMessage) msg);
                case "TURN_READY"     -> onTurnReady((TurnReadyMessage)     msg);
                case "BATTLE_END"     -> applyBattleEnd((BattleEndMessage)  msg);
                default               -> System.out.println("[OnlineBattle] Unknown msg: " + msg.getMessageType());
            }
        });
    }

    private void applyDamage(DamageMessage msg) {
        boolean targetIsMe = player.getName().equals(msg.getTargetName());
        Player  target     = targetIsMe ? player : opponent;

        PokemonInstance targetPokemon = target.getCurrentPokemon();
        if (targetPokemon == null) return;

        targetPokemon.setCurrentHp(msg.getTargetCurrentHp());
        updateBattleDisplay();

        String effText = "";
        if      (msg.getEffectiveness() != null && msg.getEffectiveness() > 1.0f)  effText = " (Super effective!)";
        else if (msg.getEffectiveness() != null && msg.getEffectiveness() < 1.0f && msg.getEffectiveness() > 0)
            effText = " (Not very effective...)";
        else if (msg.getEffectiveness() != null && msg.getEffectiveness() == 0f)   effText = " (No effect)";

        String logEntry = cap(msg.getAttackerName()) + " used " +
                cap(msg.getMoveUsed()) + "! " + msg.getDamageDealt() + " dmg" + effText;
        battleStatusLabel.setText(logEntry);
        battleLog.add(logEntry);

        if (msg.isTargetFainted()) {
            targetPokemon.setFainted(true);
            PokemonInstance next = target.getFirstAvailablePokemon();
            if (next != null) {
                target.setCurrentPokemon(next);
                String faintEntry = cap(targetPokemon.getName()) +
                        " fainted! " + cap(target.getName()) + " sends out " + cap(next.getName()) + "!";
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
        Player  side = isMe ? player : opponent;

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
        moveSent  = false;

        setVisible(waitingLabel, false);
        showActionButtons();
        attackButton.setDisable(false);
        changePokemonMainButton.setDisable(false);
    }

    /** Handle battle end: save result to DB, then update UI. */
    private void applyBattleEnd(BattleEndMessage msg) {
        if (battleEnded) return;
        battleEnded = true;

        boolean playerWon = player.getName().equals(msg.getWinnerName());
        saveBattleResult(playerWon, msg.getWinnerName());
        showResultOverlay(playerWon);

        System.out.println("[OnlineBattle] Battle ended — winner: " + msg.getWinnerName());
    }

    /** Handle unexpected disconnect (server crash, network loss, opponent quit). */
    private void handleDisconnect() {
        if (battleEnded) return;
        battleEnded = true;

        battleStatusLabel.setText("Connection lost!");
        battleLog.add("Connection to server lost.");
        disableAllButtons();
        setVisible(waitingLabel, false);

        // Treat disconnect as a draw — show a neutral overlay
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

        System.out.println("[OnlineBattle] Disconnected from server during battle");
    }

    // ── Result Overlay (same as AI battle) ──────────────────────────────────

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

        // Palette: swap style classes
        battleResultCard.getStyleClass().removeAll("result-card-victory", "result-card-defeat");
        resultTitleLabel.getStyleClass().removeAll("result-title-victory", "result-title-defeat");
        resultMessageLabel.getStyleClass().removeAll("result-message-victory", "result-message-defeat");

        String variant = playerWon ? "victory" : "defeat";
        battleResultCard.getStyleClass().add("result-card-" + variant);
        resultTitleLabel.getStyleClass().add("result-title-" + variant);
        resultMessageLabel.getStyleClass().add("result-message-" + variant);

        // Fade in
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

    /** Particle confetti shower that auto-stops after ~4 s. */
    private void startConfetti() {
        if (confettiCanvas != null) rootPane.getChildren().remove(confettiCanvas);
        confettiCanvas = new Canvas();
        confettiCanvas.widthProperty().bind(rootPane.widthProperty());
        confettiCanvas.heightProperty().bind(rootPane.heightProperty());
        confettiCanvas.setMouseTransparent(true);
        int insertIdx = Math.max(0, rootPane.getChildren().size() - 2);
        rootPane.getChildren().add(insertIdx, confettiCanvas);

        final int N = 140;
        double[] x = new double[N], y = new double[N];
        double[] vx = new double[N], vy = new double[N];
        double[] ang = new double[N], av = new double[N], sz = new double[N];
        Color[] palette = {
            Color.web("#FFD700"), Color.web("#FF6B6B"), Color.web("#4ECDC4"),
            Color.web("#45B7D1"), Color.web("#96CEB4"), Color.web("#FFEAA7"),
            Color.web("#DDA0DD"), Color.web("#98D8C8"), Color.web("#F7DC6F")
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
                    if (y[i] > confettiCanvas.getHeight() + 20) {
                        y[i] = -12; x[i] = rng.nextDouble() * confettiCanvas.getWidth();
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

    // ── Battle result persistence ───────────────────────────────────────────

    private void saveBattleResult(boolean playerWon, String winnerName) {
        User user = PlayerSession.getInstance().getCurrentUser();
        if (user == null) return;

        String pokemonUsed = player.getTeam().stream()
                .map(p -> p.getName().toLowerCase())
                .collect(Collectors.joining(","));

        String result = playerWon ? "WIN" : "LOSS";
        String logStr = String.join("\n", battleLog);

        Thread t = new Thread(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {

                String sql = "INSERT INTO battle_history "
                        + "(user_id, result, pokemon_used, opponent_type, opponent_name, battle_log) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, user.getId());
                    ps.setString(2, result);
                    ps.setString(3, pokemonUsed);
                    ps.setString(4, "ONLINE");
                    ps.setString(5, opponent.getName());
                    ps.setString(6, logStr);
                    ps.executeUpdate();
                }

                String upsert = "INSERT INTO user_profiles "
                        + "(user_id, wins, losses, total_battles) VALUES (?,?,?,1) "
                        + "ON CONFLICT(user_id) DO UPDATE SET "
                        + "wins=wins+?, losses=losses+?, total_battles=total_battles+1";
                try (PreparedStatement ups = conn.prepareStatement(upsert)) {
                    int w = playerWon ? 1 : 0, l = playerWon ? 0 : 1;
                    ups.setInt(1, user.getId());
                    ups.setInt(2, w);
                    ups.setInt(3, l);
                    ups.setInt(4, w);
                    ups.setInt(5, l);
                    ups.executeUpdate();
                }

                System.out.println("[OnlineBattle] Battle result saved: " + result
                        + " vs " + opponent.getName());

            } catch (Exception e) {
                System.err.println("[OnlineBattle] Failed to save battle result: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Button handlers ─────────────────────────────────────────────────────

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
        if (battleEnded) {
            doDisconnectAndLeave();
            return;
        }
        showForfeitOverlay();
    }

    // ── Forfeit Confirmation Overlay ────────────────────────────────────────

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
                new KeyValue(forfeitDialog.opacityProperty(), 0)
            ),
            new KeyFrame(Duration.millis(260),
                new KeyValue(forfeitDialog.scaleXProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                new KeyValue(forfeitDialog.scaleYProperty(), 1.0, Interpolator.SPLINE(0.2, 0.9, 0.3, 1)),
                new KeyValue(forfeitDialog.opacityProperty(), 1.0, Interpolator.EASE_OUT)
            )
        );

        backdropFade.play();
        dialogPop.play();
    }

    private void hideForfeitOverlay(Runnable onFinished) {
        Timeline dialogDismiss = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(forfeitDialog.scaleXProperty(), 1.0),
                new KeyValue(forfeitDialog.scaleYProperty(), 1.0),
                new KeyValue(forfeitDialog.opacityProperty(), 1.0)
            ),
            new KeyFrame(Duration.millis(180),
                new KeyValue(forfeitDialog.scaleXProperty(), 0.88, Interpolator.EASE_IN),
                new KeyValue(forfeitDialog.scaleYProperty(), 0.88, Interpolator.EASE_IN),
                new KeyValue(forfeitDialog.opacityProperty(), 0.0, Interpolator.EASE_IN)
            )
        );
        FadeTransition backdropFade = new FadeTransition(Duration.millis(200), forfeitOverlay);
        backdropFade.setFromValue(1);
        backdropFade.setToValue(0);
        backdropFade.setInterpolator(Interpolator.EASE_IN);
        backdropFade.setOnFinished(e -> {
            forfeitOverlay.setVisible(false);
            forfeitOverlay.setManaged(false);
            if (onFinished != null) onFinished.run();
        });
        dialogDismiss.play();
        backdropFade.play();
    }

    @FXML
    private void onForfeitConfirmed() {
        hideForfeitOverlay(() -> {
            if (serverConnection != null && battleId != null) {
                try {
                    serverConnection.sendMessage(new ForfeitMessage(battleId));
                } catch (Exception e) {
                    System.err.println("[OnlineBattle] Failed to send forfeit message: " + e.getMessage());
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
                System.err.println("[OnlineBattle] Failed to close server connection: " + e.getMessage());
            }
        }
        SceneManager.clearData();
        SceneManager.switchSceneWithLoading("new_game.fxml", "Battle Setup", 1200, 700);
    }

    @FXML
    private void onBackToActions() {
        showActionButtons();
    }

    private void onMoveSelected(Move move) {
        if (moveSent || battleEnded) return;
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
        if (moveSent || battleEnded) return;
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
            System.err.println("[OnlineBattle] Failed to send switch action: " + e.getMessage());
            battleStatusLabel.setText("Error sending switch: " + e.getMessage());
            moveSent = false;
            attackButton.setDisable(false);
            changePokemonMainButton.setDisable(false);
        }
    }

    // ── Display helpers ─────────────────────────────────────────────────────

    private void updateBattleDisplay() {
        updateSide(true,  player);
        updateSide(false, opponent);
    }

    private void updateSide(boolean isPlayer, Player p) {
        PokemonInstance pok = p.getCurrentPokemon();
        if (pok == null) return;

        String direction = isPlayer ? "back" : "front";
        ImageView target = isPlayer ? playerSpriteImage : opponentSpriteImage;

        double basePx = isPlayer ? SPRITE_PLAYER_BASE_PX : SPRITE_OPPONENT_BASE_PX;
        double scaledPx = getScaledSpritePx(pok.getId(), basePx);
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

    // ── Sprite scaling (same logic as BattleController) ─────────────────────

    private static java.util.Map<Integer, Double> loadPokemonHeights() {
        java.util.Map<Integer, Double> map = new java.util.HashMap<>();
        try (var stream = OnlineBattleController.class.getResourceAsStream(
                    "/com/example/pokemonbattle/data/pokemon_heights.json")) {
            if (stream == null) {
                System.err.println("[OnlineBattleController] pokemon_heights.json not found — using default sizes");
                return map;
            }
            String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int currentId = -1;
            java.util.regex.Matcher idMatcher =
                java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
            java.util.regex.Matcher hMatcher =
                java.util.regex.Pattern.compile("\"height\"\\s*:\\s*([\\d.]+)").matcher(json);
            java.util.List<long[]> ids     = new java.util.ArrayList<>();
            java.util.List<long[]> heights = new java.util.ArrayList<>();

            while (idMatcher.find()) {
                ids.add(new long[]{ idMatcher.start(), Long.parseLong(idMatcher.group(1)) });
            }
            while (hMatcher.find()) {
                heights.add(new long[]{ hMatcher.start(),
                    Double.doubleToLongBits(Double.parseDouble(hMatcher.group(1))) });
            }

            int hi = 0;
            for (long[] idEntry : ids) {
                long idPos = idEntry[0];
                int  id    = (int) idEntry[1];
                while (hi < heights.size() && heights.get(hi)[0] < idPos) hi++;
                if (hi < heights.size()) {
                    double height = Double.longBitsToDouble(heights.get(hi)[1]);
                    map.put(id, height);
                    hi++;
                }
            }
            System.out.println("[OnlineBattleController] Loaded heights for " + map.size() + " Pokemon");
        } catch (Exception e) {
            System.err.println("[OnlineBattleController] Failed to load pokemon_heights.json: " + e.getMessage());
        }
        return map;
    }

    private double getScaledSpritePx(int pokemonId, double basePx) {
        Double heightM = POKEMON_HEIGHTS.get(pokemonId);
        if (heightM == null || heightM <= 0) return basePx;
        double ratio  = Math.pow(heightM / SPRITE_STANDARD_HEIGHT_M, SPRITE_SCALE_EXPONENT);
        double scaled = basePx * ratio;
        return Math.max(SPRITE_MIN_PX, Math.min(SPRITE_MAX_PX, scaled));
    }

    private void loadSpriteWithFallback(ImageView target, int pokemonId, String direction) {
        String gifPath = String.format(
                "/com/example/pokemonbattle/sprites/%s/gif/%d.gif", direction, pokemonId);
        var gifUrl = getClass().getResource(gifPath);
        if (gifUrl != null) {
            try {
                Image gifImage = new Image(gifUrl.toExternalForm(),
                        target.getFitWidth() > 0 ? target.getFitWidth() : 0,
                        target.getFitHeight() > 0 ? target.getFitHeight() : 0,
                        true, true, true);
                if (!gifImage.isError()) {
                    target.setImage(gifImage);
                    return;
                }
            } catch (Exception e) {
                System.err.println("GIF load error (" + gifPath + "): " + e.getMessage());
            }
        }

        String pngPath = String.format(
                "/com/example/pokemonbattle/sprites/%s/%d.png", direction, pokemonId);
        try {
            var pngStream = getClass().getResourceAsStream(pngPath);
            if (pngStream != null) {
                Image pngImage = new Image(pngStream);
                if (!pngImage.isError())
                    target.setImage(pngImage);
            }
        } catch (Exception e) {
            System.err.println("PNG fallback error (" + pngPath + "): " + e.getMessage());
        }
    }

    private void updateHpBar(Rectangle bar, int hp, int maxHp) {
        if (bar == null || maxHp <= 0) return;
        double ratio = Math.max(0, (double) hp / maxHp);
        bar.setWidth(HP_BAR_MAX_WIDTH * ratio);
        if (ratio > 0.5)      bar.setFill(Color.web("#78C850"));
        else if (ratio > 0.2) bar.setFill(Color.web("#F8D030"));
        else                  bar.setFill(Color.web("#F85888"));
    }

    private void updateMoveButtons() {
        PokemonInstance cur   = player.getCurrentPokemon();
        var             moves = cur.getBattleMoves();
        Button[]        btns  = {moveButton1, moveButton2, moveButton3, moveButton4};

        for (int i = 0; i < btns.length; i++) {
            if (i < moves.size()) {
                Move m  = moves.get(i).getMove();
                int  pp = moves.get(i).getCurrentPp();
                btns[i].setText(cap(m.getName()) + "\nPP: " + pp);
                btns[i].setDisable(false);
                btns[i].setOnAction(e -> onMoveSelected(m));
            } else {
                btns[i].setText("---");
                btns[i].setDisable(true);
                btns[i].setOnAction(null);
            }
        }
    }

    private void updatePokemonButtons() {
        pokemonButtonsBox.getChildren().clear();
        for (PokemonInstance p : player.getTeam()) {
            Button btn = new Button(cap(p.getName()) +
                    (p.isFainted() ? " (Fainted)" : "  Lv." + p.getLevel()));
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

    // ── Panel visibility helpers ────────────────────────────────────────────

    private void showActionButtons() {
        setVisible(actionButtonsBox,    true);
        setVisible(moveSelectionBox,    false);
        setVisible(pokemonSelectionBox, false);
    }

    private void showMoveSelection() {
        setVisible(moveSelectionBox,    true);
        setVisible(actionButtonsBox,    false);
        setVisible(pokemonSelectionBox, false);
    }

    private void showPokemonSelection() {
        setVisible(pokemonSelectionBox, true);
        setVisible(actionButtonsBox,    false);
        setVisible(moveSelectionBox,    false);
    }

    private void setVisible(javafx.scene.Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
    }

    private void disableMoveButtons() {
        moveButton1.setDisable(true);
        moveButton2.setDisable(true);
        moveButton3.setDisable(true);
        moveButton4.setDisable(true);
    }

    private void disableAllButtons() {
        attackButton.setDisable(true);
        changePokemonMainButton.setDisable(true);
        if (itemsButton != null) itemsButton.setDisable(true);
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

    // ── Options panel background pattern ─────────────────────────────────────

    private void drawOptionsPanelPattern() {
        if (optionsSection == null) return;
        optionsSection.widthProperty().addListener((o, oldV, newV) -> repaintPattern());
        optionsSection.heightProperty().addListener((o, oldV, newV) -> repaintPattern());
        repaintPattern();
    }

    private void repaintPattern() {
        double w = optionsSection.getWidth();
        double h = optionsSection.getHeight();
        if (w <= 0 || h <= 0) return;

        optionsSection.getChildren().removeIf(n -> "patternCanvas".equals(n.getId()));
        Canvas canvas = new Canvas(w, h);
        canvas.setId("patternCanvas");
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(new javafx.scene.paint.LinearGradient(0,0,0,1,true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#5cbdb0")),
                new javafx.scene.paint.Stop(0.5, Color.web("#3a9e8f")),
                new javafx.scene.paint.Stop(1, Color.web("#2d8a7c"))));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.color(1,1,1,0.08));
        gc.setLineWidth(12);
        for (double i = -h; i < w + h; i += 36) gc.strokeLine(i, 0, i + h, h);

        gc.setStroke(Color.color(1,1,1,0.06));
        gc.setLineWidth(2);
        double spacing = 80;
        for (double y = spacing / 2; y < h; y += spacing)
            for (double x = spacing / 2; x < w; x += spacing) {
                gc.strokeOval(x - 14, y - 14, 28, 28);
                gc.strokeLine(x - 14, y, x + 14, y);
            }

        optionsSection.getChildren().addFirst(canvas);
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    /** Get the battle log for persistence. */
    public List<String> getBattleLog() {
        return new ArrayList<>(battleLog);
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}