package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.model.Battle;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Battle Screen.
 * Manages battle UI and user interactions during combat.
 */
public class BattleController implements Battle.BattleListener {

    // FXML Components
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
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
    private VBox moveSelectionBox;
    @FXML
    private VBox pokemonSelectionBox;
    @FXML
    private HBox pokemonButtonsBox;
    @FXML
    private HBox actionButtonsBox;

    // Battle Data
    private Player player;
    private Player opponent;
    private Battle battle;
    private Move selectedMove = null;

    @FXML
    public void initialize() {
        // Bind background image
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        // Setup button actions
        startBattleButton.setOnAction(e -> onStartBattle());
        backButton.setOnAction(e -> onBack());
        attackButton.setOnAction(e -> onAttackClicked());
        changePokemonMainButton.setOnAction(e -> onChangePokemonClicked());
        
        // Setup move buttons (initially disabled until battle starts)
        moveButton1.setDisable(true);
        moveButton2.setDisable(true);
        moveButton3.setDisable(true);
        moveButton4.setDisable(true);

        // Load player and opponent data from SceneManager
        loadBattleData();
    }

    /**
     * Load player and opponent data passed from NewGameController
     */
    private void loadBattleData() {
        player = (Player) SceneManager.getData("player");
        opponent = (Player) SceneManager.getData("opponent");

        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Battle data not found!");
            battleStatusLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            startBattleButton.setDisable(true);
            return;
        }

        // Display player information
        playerNameLabel.setText(player.getName());
        displayPlayerTeam();

        // Display opponent information
        opponentNameLabel.setText(opponent.getName());
        displayOpponentTeam();

        // Update status
        battleStatusLabel.setText("Teams loaded! Ready to battle.");
        battleStatusLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        
        System.out.println("\n=== BATTLE SCREEN LOADED ===");
        System.out.println("Player: " + player.getName() + " vs " + opponent.getName());
    }

    /**
     * Display player team
     */
    private void displayPlayerTeam() {
        playerPokemonBox.getChildren().clear();
        
        StringBuilder teamInfo = new StringBuilder("Team Size: " + player.getTeam().size() + "\n");
        
        for (PokemonInstance pokemon : player.getTeam()) {
            teamInfo.append("• ").append(capitalize(pokemon.getName()))
                    .append(" (Lv.").append(pokemon.getLevel()).append(")\n");
            
            // Create a simple label for each Pokemon
            Label pokemonLabel = new Label(capitalize(pokemon.getName()) + 
                    " - Lv." + pokemon.getLevel() + " - HP: " + pokemon.getMaxHp());
            pokemonLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-padding: 5;");
            playerPokemonBox.getChildren().add(pokemonLabel);
        }
        
        playerTeamLabel.setText(teamInfo.toString().trim());
    }

    /**
     * Display opponent team
     */
    private void displayOpponentTeam() {
        opponentPokemonBox.getChildren().clear();
        
        StringBuilder teamInfo = new StringBuilder("Team Size: " + opponent.getTeam().size() + "\n");
        
        for (PokemonInstance pokemon : opponent.getTeam()) {
            teamInfo.append("• ").append(capitalize(pokemon.getName()))
                    .append(" (Lv.").append(pokemon.getLevel()).append(")\n");
            
            // Create a simple label for each Pokemon
            Label pokemonLabel = new Label(capitalize(pokemon.getName()) + 
                    " - Lv." + pokemon.getLevel() + " - HP: " + pokemon.getMaxHp());
            pokemonLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-padding: 5;");
            opponentPokemonBox.getChildren().add(pokemonLabel);
        }
        
        opponentTeamLabel.setText(teamInfo.toString().trim());
    }

    /**
     * Start the actual battle
     */
    private void onStartBattle() {
        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Teams not loaded!");
            return;
        }
        
        // Create battle instance with loaded teams
        battle = new Battle(player, opponent);
        
        // Add this controller as a battle listener
        battle.addListener(this);
        
        // Load and display Pokemon sprites
        updateBattleDisplay();
        
        // Show main action buttons
        showActionButtons();
        
        battleStatusLabel.setText("Battle Started! " + player.getCurrentPokemon().getName() + 
                " vs " + opponent.getCurrentPokemon().getName());
        battleStatusLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        startBattleButton.setDisable(true);
    }
    
    /**
     * Update battle display with current Pokemon sprites and info
     */
    private void updateBattleDisplay() {
        if (player == null || opponent == null) {
            return;
        }
        
        PokemonInstance playerPok = player.getCurrentPokemon();
        PokemonInstance opponentPok = opponent.getCurrentPokemon();
        
        // Load player back sprite
        String playerSpritePath = String.format("/com/example/pokemonbattle/sprites/back/%d.png", 
                playerPok.getId());
        try {
            Image playerImage = new Image(getClass().getResourceAsStream(playerSpritePath));
            if (!playerImage.isError()) {
                playerSpriteImage.setImage(playerImage);
            }
        } catch (Exception e) {
            System.err.println("Error loading player sprite: " + e.getMessage());
        }
        
        // Load opponent front sprite
        String opponentSpritePath = String.format("/com/example/pokemonbattle/sprites/front/%d.png", 
                opponentPok.getId());
        try {
            Image opponentImage = new Image(getClass().getResourceAsStream(opponentSpritePath));
            if (!opponentImage.isError()) {
                opponentSpriteImage.setImage(opponentImage);
            }
        } catch (Exception e) {
            System.err.println("Error loading opponent sprite: " + e.getMessage());
        }
        
        // Update labels
        playerPokemonNameLabel.setText(capitalize(playerPok.getName()) + " (Lv." + playerPok.getLevel() + ")");
        playerPokemonHpLabel.setText("HP: " + playerPok.getCurrentHp() + "/" + playerPok.getMaxHp());
        
        opponentPokemonNameLabel.setText(capitalize(opponentPok.getName()) + " (Lv." + opponentPok.getLevel() + ")");
        opponentPokemonHpLabel.setText("HP: " + opponentPok.getCurrentHp() + "/" + opponentPok.getMaxHp());
    }
    
    /**
     * Update move buttons based on current Pokemon's moves
     */
    private void updateMoveButtons() {
        PokemonInstance currentPok = player.getCurrentPokemon();
        var moves = currentPok.getBattleMoves();
        
        Button[] moveButtons = {moveButton1, moveButton2, moveButton3, moveButton4};
        
        for (int i = 0; i < moveButtons.length; i++) {
            if (i < moves.size()) {
                Move move = moves.get(i).getMove();
                int pp = moves.get(i).getCurrentPp();
                
                moveButtons[i].setText(capitalize(move.getName()) + "\n(PP: " + pp + ")");
                moveButtons[i].setDisable(false);
                
                final int moveIndex = i;
                moveButtons[i].setOnAction(e -> onMoveSelected(moveIndex, move));
            } else {
                moveButtons[i].setText("---");
                moveButtons[i].setDisable(true);
                moveButtons[i].setOnAction(null);
            }
        }
    }
    
    /**
     * Handle move selection
     */
    private void onMoveSelected(int moveIndex, Move move) {
        if (battle.isFinished()) {
            battleStatusLabel.setText("Battle is already finished!");
            return;
        }
        
        selectedMove = move;
        battleStatusLabel.setText("Player selected " + capitalize(move.getName()) + "!");
        battleStatusLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#6890F0; -fx-font-weight:bold;");
        
        // Disable action buttons during round execution
        disableMoveButtons();
        
        // Get AI move
        Move aiMove = battle.getAIMove(opponent.getCurrentPokemon());
        
        // Execute round
        battle.executeRound(move, aiMove);
        
        // Update display after round
        updateBattleDisplay();
        
        // Update move buttons (PP might have changed, Pokemon might have switched)
        updateMoveButtons();
        
        // Check if battle is finished
        if (battle.isFinished()) {
            Player winner = battle.getWinner();
            battleStatusLabel.setText(winner.getName() + " wins the battle!");
            battleStatusLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
            disableMoveButtons();
            moveSelectionBox.setVisible(false);
            moveSelectionBox.setManaged(false);
            startBattleButton.setDisable(false);
            startBattleButton.setText("Back to Setup");
        } else {
            // Return to action buttons after a successful move
            showActionButtons();
        }
    }
    
    /**
     * Disable all move buttons
     */
    private void disableMoveButtons() {
        moveButton1.setDisable(true);
        moveButton2.setDisable(true);
        moveButton3.setDisable(true);
        moveButton4.setDisable(true);
    }

    /**
     * Go back to battle setup screen
     */
    private void onBack() {
        SceneManager.clearData();
        SceneManager.switchScene("new_game.fxml", "Pokemon Battle - Setup", 1200, 700);
    }
    
    /**
     * Handle Attack button click - show move selection panel
     */
    private void onAttackClicked() {
        if (battle == null || battle.isFinished()) {
            battleStatusLabel.setText("Battle is not active!");
            return;
        }
        
        // Hide action buttons and pokemon selection, show move selection
        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
        moveSelectionBox.setVisible(true);
        moveSelectionBox.setManaged(true);
        
        // Update move buttons
        updateMoveButtons();
    }
    
    /**
     * Handle Change Pokemon button click - show pokemon selection panel
     */
    private void onChangePokemonClicked() {
        // Hide action buttons and move selection, show pokemon selection
        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        pokemonSelectionBox.setVisible(true);
        pokemonSelectionBox.setManaged(true);
        
        // Populate pokemon buttons
        updatePokemonButtons();
    }
    
    /**
     * Update pokemon selection buttons with available (non-fainted) Pokemon
     */
    private void updatePokemonButtons() {
        pokemonButtonsBox.getChildren().clear();
        
        for (PokemonInstance pokemon : player.getTeam()) {
            Button pokemonBtn = new Button(capitalize(pokemon.getName()) + 
                    (pokemon.isFainted() ? " (Fainted)" : " (Lv." + pokemon.getLevel() + ")"));
            pokemonBtn.setPrefWidth(120);
            pokemonBtn.setPrefHeight(40);
            pokemonBtn.setStyle("-fx-font-size: 11px;");
            
            if (pokemon.isFainted()) {
                pokemonBtn.setDisable(true);
            } else if (pokemon.getId() == player.getCurrentPokemon().getId()) {
                pokemonBtn.setText(pokemonBtn.getText() + " ✓");
                pokemonBtn.setDisable(true);
            } else {
                pokemonBtn.setOnAction(e -> onPokemonSelected(pokemon));
            }
            
            pokemonButtonsBox.getChildren().add(pokemonBtn);
        }
    }
    
    /**
     * Handle Pokemon selection for switching
     */
    private void onPokemonSelected(PokemonInstance pokemon) {
        player.setCurrentPokemon(pokemon);
        battleStatusLabel.setText(player.getName() + " switched to " + capitalize(pokemon.getName()) + "!");
        battleStatusLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#6890F0; -fx-font-weight:bold;");
        
        // Update display and return to main action buttons
        updateBattleDisplay();
        showActionButtons();
        
        System.out.println("Player switched to: " + pokemon.getName());
    }
    
    /**
     * Show main action buttons and hide selection panels
     */
    private void showActionButtons() {
        actionButtonsBox.setVisible(true);
        actionButtonsBox.setManaged(true);
        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // ==================== BattleListener Implementation ====================

    @Override
    public void onDamageDealt(String attacker, String move, String defender, int damage) {
        System.out.println(attacker + " used " + move + " on " + defender + " for " + damage + " damage!");
        battleStatusLabel.setText(attacker + " used " + move + "! " + damage + " damage!");
        battleStatusLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
    }

    @Override
    public void onPokemonFainted(String pokemonName) {
        System.out.println(pokemonName + " fainted!");
        battleStatusLabel.setText(capitalize(pokemonName) + " fainted!");
        battleStatusLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#FF5959; -fx-font-weight:bold;");
    }

    @Override
    public void onPokemonSwitched(String playerName, String pokemonName) {
        System.out.println(playerName + " sent out " + pokemonName + "!");
        battleStatusLabel.setText(playerName + " sent out " + capitalize(pokemonName) + "!");
        battleStatusLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#6890F0; -fx-font-weight:bold;");
        
        // Update UI to show new Pokemon
        if (playerName.equals(player.getName())) {
            displayPlayerTeam();
            updateBattleDisplay();
            updateMoveButtons();
        } else {
            displayOpponentTeam();
            updateBattleDisplay();
        }
    }

    @Override
    public void onBattleEnd(String winnerName) {
        System.out.println("\n=== BATTLE END ===");
        System.out.println("Winner: " + winnerName);
        
        battleStatusLabel.setText(winnerName + " wins the battle!");
        battleStatusLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        startBattleButton.setDisable(false);
        startBattleButton.setText("Back to Menu");
        startBattleButton.setOnAction(e -> onBack());
    }
}
