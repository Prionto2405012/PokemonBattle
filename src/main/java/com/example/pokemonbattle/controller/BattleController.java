package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.model.Battle;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.SceneManager;

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

public class BattleController implements Battle.BattleListener {
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
    private Button moveButton1;
    @FXML
    private Button moveButton2;
    @FXML
    private Button moveButton3;
    @FXML
    private Button moveButton4;
    @FXML
    private Button attackButton;
    @FXML
    private Button changePokemonMainButton;
    @FXML
    private Button itemsButton;
    @FXML
    private VBox moveSelectionBox;
    @FXML
    private VBox pokemonSelectionBox;
    @FXML
    private VBox pokemonButtonsBox;
    @FXML
    private VBox actionButtonsBox;

    private Player player;
    private Player opponent;
    private Battle battle;
    private static final double HP_BAR_MAX_WIDTH = 180.0;

    @FXML
    public void initialize() {
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        startBattleButton.setOnAction(e -> onStartBattle());
        backButton.setOnAction(e -> onBack());
        attackButton.setOnAction(e -> onAttackClicked());
        changePokemonMainButton.setOnAction(e -> onChangePokemonClicked());
        if (itemsButton != null) {
            itemsButton.setOnAction(e -> onItemsClicked());
        }

        moveButton1.setDisable(true);
        moveButton2.setDisable(true);
        moveButton3.setDisable(true);
        moveButton4.setDisable(true);

        loadBattleData();
        drawOptionsPanelPattern();
        SceneManager.enableCoordDebug(rootPane);
        MusicManager.getInstance().attachClickSounds(rootPane);
    }

    /**
     * Draw the teal pattern (diagonal stripes + pokeball circles) on the options panel background.
     */
    private void drawOptionsPanelPattern() {
        if (optionsSection == null) return;
        optionsSection.widthProperty().addListener((obs, o, n) -> repaintPattern());
        optionsSection.heightProperty().addListener((obs, o, n) -> repaintPattern());
        repaintPattern();
    }

    private void repaintPattern() {
        double w = optionsSection.getWidth();
        double h = optionsSection.getHeight();
        if (w <= 0 || h <= 0) return;

        // Remove previous canvas if exists
        optionsSection.getChildren().removeIf(n -> "patternCanvas".equals(n.getId()));

        Canvas canvas = new Canvas(w, h);
        canvas.setId("patternCanvas");
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Base teal gradient
        gc.setFill(new javafx.scene.paint.LinearGradient(
            0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.web("#5cbdb0")),
            new javafx.scene.paint.Stop(0.5, Color.web("#3a9e8f")),
            new javafx.scene.paint.Stop(1, Color.web("#2d8a7c"))
        ));
        gc.fillRect(0, 0, w, h);

        // Diagonal stripes
        gc.setStroke(Color.color(1, 1, 1, 0.08));
        gc.setLineWidth(12);
        for (double i = -h; i < w + h; i += 36) {
            gc.strokeLine(i, 0, i + h, h);
        }

        // Subtle pokeball circles pattern
        gc.setStroke(Color.color(1, 1, 1, 0.06));
        gc.setLineWidth(2);
        double spacing = 80;
        for (double y = spacing / 2; y < h; y += spacing) {
            for (double x = spacing / 2; x < w; x += spacing) {
                gc.strokeOval(x - 14, y - 14, 28, 28);
                gc.strokeLine(x - 14, y, x + 14, y);
            }
        }

        // Insert canvas at index 0 (behind other children)
        optionsSection.getChildren().addFirst(canvas);
    }

    private void loadBattleData() {
        player = (Player) SceneManager.getData("player");
        opponent = (Player) SceneManager.getData("opponent");
        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Battle data not found!");
            startBattleButton.setDisable(true);
            return;
        }
        playerNameLabel.setText(player.getName());
        displayPlayerTeam();
        opponentNameLabel.setText(opponent.getName());
        displayOpponentTeam();
        battleStatusLabel.setText("Teams loaded! Ready to battle.");
        System.out.println("\n=== BATTLE SCREEN LOADED ===");
        System.out.println("Player: " + player.getName() + " vs " + opponent.getName());
    }

    private void displayPlayerTeam() {
        playerPokemonBox.getChildren().clear();
        StringBuilder teamInfo = new StringBuilder("Team Size: " + player.getTeam().size() + "\n");
        for (PokemonInstance pokemon : player.getTeam()) {
            teamInfo.append("• ").append(capitalize(pokemon.getName()))
                    .append(" (Lv.").append(pokemon.getLevel()).append(")\n");
        }
        playerTeamLabel.setText(teamInfo.toString().trim());
    }

    private void displayOpponentTeam() {
        opponentPokemonBox.getChildren().clear();
        StringBuilder teamInfo = new StringBuilder("Team Size: " + opponent.getTeam().size() + "\n");
        for (PokemonInstance pokemon : opponent.getTeam()) {
            teamInfo.append("• ").append(capitalize(pokemon.getName()))
                    .append(" (Lv.").append(pokemon.getLevel()).append(")\n");
        }
        opponentTeamLabel.setText(teamInfo.toString().trim());
    }

    private void onStartBattle() {
        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Teams not loaded!");
            return;
        }
        battle = new Battle(player, opponent);
        battle.addListener(this);
        updateBattleDisplay();
        showActionButtons();
        battleStatusLabel.setText("Battle Started! " + capitalize(player.getCurrentPokemon().getName()) +
                " vs " + capitalize(opponent.getCurrentPokemon().getName()));
        startBattleButton.setDisable(true);
    }

    private void updateBattleDisplay() {
        if (player == null || opponent == null) return;

        PokemonInstance playerPok = player.getCurrentPokemon();
        PokemonInstance opponentPok = opponent.getCurrentPokemon();

        // Player sprite (back)
        String playerSpritePath = String.format("/com/example/pokemonbattle/sprites/back/%d.png", playerPok.getId());
        try {
            Image playerImage = new Image(getClass().getResourceAsStream(playerSpritePath));
            if (!playerImage.isError()) playerSpriteImage.setImage(playerImage);
        } catch (Exception e) {
            System.err.println("Error loading player sprite: " + e.getMessage());
        }

        // Opponent sprite (front)
        String opponentSpritePath = String.format("/com/example/pokemonbattle/sprites/front/%d.png", opponentPok.getId());
        try {
            Image opponentImage = new Image(getClass().getResourceAsStream(opponentSpritePath));
            if (!opponentImage.isError()) opponentSpriteImage.setImage(opponentImage);
        } catch (Exception e) {
            System.err.println("Error loading opponent sprite: " + e.getMessage());
        }

        // Name + HP text
        playerPokemonNameLabel.setText(capitalize(playerPok.getName()) + "  Lv." + playerPok.getLevel());
        playerPokemonHpLabel.setText(playerPok.getCurrentHp() + " / " + playerPok.getMaxHp());
        opponentPokemonNameLabel.setText(capitalize(opponentPok.getName()) + "  Lv." + opponentPok.getLevel());
        opponentPokemonHpLabel.setText(opponentPok.getCurrentHp() + " / " + opponentPok.getMaxHp());

        // HP bars
        updateHpBar(playerHpBar, playerPok.getCurrentHp(), playerPok.getMaxHp());
        updateHpBar(opponentHpBar, opponentPok.getCurrentHp(), opponentPok.getMaxHp());
    }

    private void updateHpBar(Rectangle hpBar, int currentHp, int maxHp) {
        if (hpBar == null || maxHp <= 0) return;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        hpBar.setWidth(HP_BAR_MAX_WIDTH * ratio);

        // Color: green > yellow > red
        if (ratio > 0.5) {
            hpBar.setFill(Color.web("#78C850"));
        } else if (ratio > 0.2) {
            hpBar.setFill(Color.web("#F8D030"));
        } else {
            hpBar.setFill(Color.web("#F85888"));
        }
    }

    private void updateMoveButtons() {
        PokemonInstance currentPok = player.getCurrentPokemon();
        var moves = currentPok.getBattleMoves();
        Button[] moveButtons = {moveButton1, moveButton2, moveButton3, moveButton4};

        for (int i = 0; i < moveButtons.length; i++) {
            if (i < moves.size()) {
                Move move = moves.get(i).getMove();
                int pp = moves.get(i).getCurrentPp();
                moveButtons[i].setText(capitalize(move.getName()) + "\nPP: " + pp);
                moveButtons[i].setDisable(false);
                moveButtons[i].setOnAction(e -> onMoveSelected(move));
            } else {
                moveButtons[i].setText("---");
                moveButtons[i].setDisable(true);
                moveButtons[i].setOnAction(null);
            }
        }
    }

    private void onMoveSelected(Move move) {
        if (battle.isFinished()) {
            battleStatusLabel.setText("Battle is already finished!");
            return;
        }
        battleStatusLabel.setText("Used " + capitalize(move.getName()) + "!");
        disableMoveButtons();

        Move aiMove = battle.getAIMove(opponent.getCurrentPokemon());
        battle.executeRound(move, aiMove);
        updateBattleDisplay();
        updateMoveButtons();

        if (battle.isFinished()) {
            Player winner = battle.getWinner();
            battleStatusLabel.setText(winner.getName() + " wins the battle!");
            disableMoveButtons();
            moveSelectionBox.setVisible(false);
            moveSelectionBox.setManaged(false);
            startBattleButton.setDisable(false);
            startBattleButton.setText("Back to Menu");
        } else {
            showActionButtons();
        }
    }

    private void disableMoveButtons() {
        moveButton1.setDisable(true);
        moveButton2.setDisable(true);
        moveButton3.setDisable(true);
        moveButton4.setDisable(true);
    }

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
            Button pokemonBtn = new Button(capitalize(pokemon.getName()) +
                    (pokemon.isFainted() ? " (Fainted)" : "  Lv." + pokemon.getLevel()));
            pokemonBtn.setPrefWidth(260);
            pokemonBtn.setPrefHeight(42);
            pokemonBtn.getStyleClass().addAll("option-btn", "option-btn-green");
            pokemonBtn.setStyle("-fx-font-size: 13px;");

            if (pokemon.isFainted()) {
                pokemonBtn.setDisable(true);
                pokemonBtn.setStyle("-fx-font-size: 13px; -fx-opacity: 0.5;");
            } else if (pokemon.getId() == player.getCurrentPokemon().getId()) {
                pokemonBtn.setText(pokemonBtn.getText() + " ✓");
                pokemonBtn.setDisable(true);
            } else {
                pokemonBtn.setOnAction(e -> onPokemonSelected(pokemon));
            }
            pokemonButtonsBox.getChildren().add(pokemonBtn);
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

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Override
    public void onDamageDealt(String attacker, String move, String defender, int damage) {
        System.out.println(attacker + " used " + move + " on " + defender + " for " + damage + " damage!");
        battleStatusLabel.setText(capitalize(attacker) + " used " + capitalize(move) + "! " + damage + " dmg!");
    }

    @Override
    public void onPokemonFainted(String pokemonName) {
        System.out.println(pokemonName + " fainted!");
        battleStatusLabel.setText(capitalize(pokemonName) + " fainted!");
    }

    @Override
    public void onPokemonSwitched(String playerName, String pokemonName) {
        System.out.println(playerName + " sent out " + pokemonName + "!");
        battleStatusLabel.setText(playerName + " sent out " + capitalize(pokemonName) + "!");
        if (playerName.equals(player.getName())) {
            displayPlayerTeam();
        } else {
            displayOpponentTeam();
        }
        updateBattleDisplay();
        updateMoveButtons();
    }

    @Override
    public void onBattleEnd(String winnerName) {
        System.out.println("\n=== BATTLE END ===");
        System.out.println("Winner: " + winnerName);
        battleStatusLabel.setText(winnerName + " wins the battle!");
        startBattleButton.setDisable(false);
        startBattleButton.setText("Back to Menu");
        startBattleButton.setOnAction(e -> onBack());
    }
}
