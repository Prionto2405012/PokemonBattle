package com.example.pokemonbattle.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

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

    @FXML private ImageView playerSpriteImage;
    @FXML private ImageView opponentSpriteImage;

    @FXML private Label playerPokemonNameLabel;
    @FXML private Label playerPokemonHpLabel;
    @FXML private Label opponentPokemonNameLabel;
    @FXML private Label opponentPokemonHpLabel;
    @FXML private Rectangle playerHpBar;
    @FXML private Rectangle opponentHpBar;

    // Hidden info refs (optional)
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

    // State
    private Player           player;
    private Player           opponent;
    private ServerConnection serverConnection;
    private Integer          battleId;
    private int              turnCount = 0;
    private boolean          battleEnded = false;
    private boolean          moveSent    = false;

    private static final double HP_BAR_MAX_WIDTH = 180.0;

    // Lifecycle

    @FXML
    public void initialize() {
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        player           = (Player)           SceneManager.getData("player");
        opponent         = (Player)           SceneManager.getData("opponent");
        serverConnection = (ServerConnection) SceneManager.getData("serverConnection");
        battleId         = (Integer)          SceneManager.getData("battleId");

        if (player == null || opponent == null || serverConnection == null) {
            battleStatusLabel.setText("Error: missing battle data!");
            disableAllButtons();
            return;
        }

        serverConnection.setMessageListener(this::handleServerMessage);

        attackButton.setOnAction(e -> onFightClicked());
        changePokemonMainButton.setOnAction(e -> onChangePokemonClicked());
        backButton.setOnAction(e -> onRunClicked());
        if (itemsButton != null) itemsButton.setOnAction(e ->
                battleStatusLabel.setText("No items in online battle."));

        setVisible(waitingLabel, false);
        drawOptionsPanelPattern();
        updateBattleDisplay();

        battleStatusLabel.setText("Online Battle! " + cap(player.getCurrentPokemon().getName()) +
                " vs " + cap(opponent.getCurrentPokemon().getName()));

        System.out.println("[OnlineBattle] Battle started vs " + opponent.getName());
        MusicManager.getInstance().attachClickSounds(rootPane);
    }

    // Server message handler

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

        battleStatusLabel.setText(cap(msg.getAttackerName()) + " used " +
                cap(msg.getMoveUsed()) + "! " + msg.getDamageDealt() + " dmg" + effText);

        if (msg.isTargetFainted()) {
            targetPokemon.setFainted(true);
            PokemonInstance next = target.getFirstAvailablePokemon();
            if (next != null) {
                target.setCurrentPokemon(next);
                battleStatusLabel.setText(cap(targetPokemon.getName()) +
                        " fainted! " + cap(target.getName()) + " sends out " + cap(next.getName()) + "!");
                updateBattleDisplay();
            }
        }
    }

    private void applyBattleUpdate(BattleUpdateMessage msg) {
        battleStatusLabel.setText(msg.getMessage());
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
        battleStatusLabel.setText(who + " switched to " + cap(msg.getNewPokemonName()) + "!");
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
        battleEnded = true;

        boolean playerWon = player.getName().equals(msg.getWinnerName());

        // Persist result (same logic as BattleController)
        saveBattleResult(playerWon, msg.getWinnerName());

        String resultText = playerWon
                ? "🏆 You WIN! " + cap(msg.getWinnerName()) + " is victorious!"
                : "❌ You lost. " + cap(msg.getWinnerName()) + " wins!";
        battleStatusLabel.setText(resultText);

        disableAllButtons();

        backButton.setText("Back to Menu");
        backButton.setDisable(false);
        backButton.setOnAction(e -> onRunClicked());
        setVisible(backButton, true);
        setVisible(waitingLabel, false);

        System.out.println("[OnlineBattle] Battle ended — winner: " + msg.getWinnerName());
    }

    // Battle result persistence

    /**
     * Saves the online battle result to battle_history and user_profiles tables.
     * Runs on a daemon thread so it never blocks the FX thread.
     */
    private void saveBattleResult(boolean playerWon, String winnerName) {
        User user = PlayerSession.getInstance().getCurrentUser();
        if (user == null) return; // not logged in — skip

        // Build comma-separated list of pokemon used
        String pokemonUsed = player.getTeam().stream()
                .map(p -> p.getName().toLowerCase())
                .collect(Collectors.joining(","));

        String result = playerWon ? "WIN" : "LOSS";

        Thread t = new Thread(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {

                // Insert battle record
                String sql = "INSERT INTO battle_history "
                        + "(user_id, result, pokemon_used, opponent_type, opponent_name) "
                        + "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, user.getId());
                    ps.setString(2, result);
                    ps.setString(3, pokemonUsed);
                    ps.setString(4, "ONLINE");          // distinguish from AI battles
                    ps.setString(5, opponent.getName()); // real opponent username
                    ps.executeUpdate();
                }

                // Upsert win/loss counters
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

    // Button handlers

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
        if (serverConnection != null) {
            if (!battleEnded && battleId != null) {
                try {
                    serverConnection.sendMessage(new ForfeitMessage(battleId));
                } catch (Exception e) {
                    System.err.println("[OnlineBattle] Failed to send forfeit: " + e.getMessage());
                }
            }
            serverConnection.disconnect();
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

        battleStatusLabel.setText("You chose " + cap(move.getName()) + "! Waiting for opponent...");

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
        battleStatusLabel.setText("Switching to " + cap(pokemon.getName()) + "! Waiting for opponent...");
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

    // Display helpers

    private void updateBattleDisplay() {
        updateSide(true,  player);
        updateSide(false, opponent);
    }

    private void updateSide(boolean isPlayer, Player p) {
        PokemonInstance pok = p.getCurrentPokemon();
        if (pok == null) return;

        String direction = isPlayer ? "back" : "front";
        ImageView target = isPlayer ? playerSpriteImage : opponentSpriteImage;
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

        // PNG fallback
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

    // Panel visibility helpers

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

    // Cosmetic canvas pattern

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

    // Utility

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}