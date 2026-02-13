package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Battle Screen.
 * This is a minimal placeholder implementation - backend battle logic to be implemented.
 */
public class BattleController {

    // FXML Components
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
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

    // Battle Data
    private Player player;
    private Player opponent;

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
        System.out.println("Player: " + player.getName() + " - " + player.getTeam().size() + " Pokemon");
        System.out.println("Opponent: " + opponent.getName() + " - " + opponent.getTeam().size() + " Pokemon");
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
     * Start the actual battle (placeholder for now)
     */
    private void onStartBattle() {
        // Create player with random team
        player = new Player("Player");
        player.generateRandomTeam();
        
        // Create opponent with random team
        opponent = new Player("Opponent");
        opponent.generateRandomTeam();
        
        // Display teams
        playerNameLabel.setText(player.getName());
        displayPlayerTeam();
        
        opponentNameLabel.setText(opponent.getName());
        displayOpponentTeam();
        
        battleStatusLabel.setText("Battle Started!");
        battleStatusLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        startBattleButton.setDisable(true);
        
        System.out.println("\n=== BATTLE START ===");
        System.out.println("Player's team: " + player.getTeam().size() + " Pokemon");
        System.out.println("Player's first Pokemon: " + player.getTeam().get(0).getName());
        System.out.println("Opponent's team: " + opponent.getTeam().size() + " Pokemon");
        System.out.println("Opponent's first Pokemon: " + opponent.getTeam().get(0).getName());
    }

    /**
     * Go back to battle setup screen
     */
    private void onBack() {
        SceneManager.clearData();
        SceneManager.switchScene("new_game.fxml", "Pokemon Battle - Setup", 1200, 700);
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
