package com.example.pokemonbattle.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.PokemonSpecies;
import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Battle Setup Screen.
 * Handles game mode selection, opponent matching, and team building.
 */
@SuppressWarnings("unused")
public class NewGameController {

    // FXML Components
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
    @FXML
    private VBox contentVBox;

    // Mode Selection
    @FXML
    private HBox modeSelectionBox;
    @FXML
    private ToggleButton soloModeButton;
    @FXML
    private ToggleButton duoModeButton;
    private ToggleGroup modeToggleGroup;

    // Opponent Selection
    @FXML
    private HBox opponentSelectionBox;
    @FXML
    private ToggleButton aiOpponentButton;
    @FXML
    private ToggleButton localOpponentButton;
    @FXML
    private Label matchingStatusLabel;

    // Team Selection
    @FXML
    private HBox teamSelectionBox;
    @FXML
    private ToggleButton randomTeamButton;
    @FXML
    private ToggleButton customTeamButton;
    private ToggleGroup teamToggleGroup;

    // Pokemon Display
    @FXML
    private ScrollPane pokemonScrollPane;
    @FXML
    private GridPane pokemonGrid;
    @FXML
    private VBox selectedTeamBox;
    @FXML
    private Label teamCountLabel;

    // Action Buttons
    @FXML
    private Button startBattleButton;
    @FXML
    private Button backButton;

    // Data
    private List<PokemonSpecies> allPokemon;
    private Map<Integer, Move> allMoves;
    private List<PokemonInstance> playerTeam;
    private String selectedMode = "SOLO";
    private String selectedOpponent = "AI";
    private String selectedTeamType = "RANDOM";
    private static final int MAX_TEAM_SIZE = 6;
    private static final int POKEMON_LEVEL = 50;

    @FXML
    public void initialize() {
        // Bind background image
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        // Initialize data structures
        playerTeam = new ArrayList<>();

        // Setup toggle groups
        setupToggleGroups();

        // Load game data
        loadGameData();

        // Setup UI
        setupUI();

        // Set default selections
        setDefaultSelections();
    }

    /**
     * Setup toggle groups for mutually exclusive selections
     */
    private void setupToggleGroups() {
        modeToggleGroup = new ToggleGroup();
        soloModeButton.setToggleGroup(modeToggleGroup);
        duoModeButton.setToggleGroup(modeToggleGroup);

        teamToggleGroup = new ToggleGroup();
        randomTeamButton.setToggleGroup(teamToggleGroup);
        customTeamButton.setToggleGroup(teamToggleGroup);

        // Add listeners
        modeToggleGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle != null) {
                ToggleButton btn = (ToggleButton) newToggle;
                selectedMode = btn.getText().toUpperCase();
                updateUIForMode();
            }
        });

        teamToggleGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle != null) {
                ToggleButton btn = (ToggleButton) newToggle;
                selectedTeamType = btn.getText().toUpperCase();
                updateTeamSelectionUI();
            }
        });
    }

    /**
     * Load Pokemon and Move data (hardcoded sample data)
     */
    private void loadGameData() {
        // Load sample moves
        allMoves = new HashMap<>();
        allMoves.put(1, createMove(1, "Tackle", 40, 100, 35, "Normal", "physical"));
        allMoves.put(2, createMove(2, "Thunder Shock", 40, 100, 30, "Electric", "special"));
        allMoves.put(3, createMove(3, "Quick Attack", 40, 100, 30, "Normal", "physical"));
        allMoves.put(4, createMove(4, "Thunderbolt", 90, 100, 15, "Electric", "special"));
        allMoves.put(5, createMove(5, "Ember", 40, 100, 25, "Fire", "special"));
        allMoves.put(6, createMove(6, "Flamethrower", 90, 100, 15, "Fire", "special"));
        allMoves.put(7, createMove(7, "Scratch", 40, 100, 35, "Normal", "physical"));
        allMoves.put(8, createMove(8, "Fire Blast", 110, 85, 5, "Fire", "special"));
        allMoves.put(9, createMove(9, "Water Gun", 40, 100, 25, "Water", "special"));
        allMoves.put(10, createMove(10, "Hydro Pump", 110, 80, 5, "Water", "special"));
        allMoves.put(11, createMove(11, "Bubble", 40, 100, 30, "Water", "special"));
        allMoves.put(12, createMove(12, "Surf", 90, 100, 15, "Water", "special"));
        allMoves.put(13, createMove(13, "Vine Whip", 45, 100, 25, "Grass", "physical"));
        allMoves.put(14, createMove(14, "Solar Beam", 120, 100, 10, "Grass", "special"));
        allMoves.put(15, createMove(15, "Razor Leaf", 55, 95, 25, "Grass", "physical"));
        System.out.println("Loaded " + allMoves.size() + " moves");

        // Load sample Pokemon
        allPokemon = new ArrayList<>();
        allPokemon.add(createPokemon(25, "Pikachu", List.of("Electric"), 35, 55, 40, 50, 50, 90, List.of(1, 2, 3, 4)));
        allPokemon.add(createPokemon(6, "Charizard", List.of("Fire", "Flying"), 78, 84, 78, 109, 85, 100,
                List.of(1, 5, 6, 7, 8)));
        allPokemon.add(
                createPokemon(9, "Blastoise", List.of("Water"), 79, 83, 100, 85, 105, 78, List.of(1, 9, 10, 11, 12)));
        allPokemon.add(createPokemon(3, "Venusaur", List.of("Grass", "Poison"), 80, 82, 83, 100, 100, 80,
                List.of(1, 13, 14, 15)));
        allPokemon.add(
                createPokemon(1, "Bulbasaur", List.of("Grass", "Poison"), 45, 49, 49, 65, 65, 45, List.of(1, 13, 15)));
        allPokemon.add(createPokemon(4, "Charmander", List.of("Fire"), 39, 52, 43, 60, 50, 65, List.of(1, 5, 7)));
        allPokemon.add(createPokemon(7, "Squirtle", List.of("Water"), 44, 48, 65, 50, 64, 43, List.of(1, 9, 11)));
        allPokemon.add(
                createPokemon(94, "Gengar", List.of("Ghost", "Poison"), 60, 65, 60, 130, 75, 110, List.of(1, 3, 7)));
        allPokemon
                .add(createPokemon(131, "Lapras", List.of("Water", "Ice"), 130, 85, 80, 85, 95, 60, List.of(1, 9, 12)));
        allPokemon.add(createPokemon(143, "Snorlax", List.of("Normal"), 160, 110, 65, 65, 110, 30, List.of(1, 3)));
        System.out.println("Loaded " + allPokemon.size() + " Pokemon species");
    }

    private Move createMove(int id, String name, Integer power, Integer accuracy, int pp, String type,
            String damageClass) {
        Move move = new Move();
        move.setId(id);
        move.setName(name);
        move.setPower(power);
        move.setAccuracy(accuracy);
        move.setPp(pp);
        move.setType(type);
        move.setDamage_class(damageClass);
        return move;
    }

    private PokemonSpecies createPokemon(int id, String name, List<String> types,
            int hp, int atk, int def, int spAtk, int spDef, int speed,
            List<Integer> moveIds) {
        PokemonSpecies pokemon = new PokemonSpecies();
        pokemon.setId(id);
        pokemon.setName(name);
        pokemon.setTypes(new ArrayList<>(types));

        PokemonSpecies.Stats stats = new PokemonSpecies.Stats();
        stats.setHp(hp);
        stats.setAttack(atk);
        stats.setDefense(def);
        stats.setSpecial_attack(spAtk);
        stats.setSpecial_defense(spDef);
        stats.setSpeed(speed);
        pokemon.setStats(stats);

        pokemon.setMoves(new ArrayList<>(moveIds));
        return pokemon;
    }

    /**
     * Setup initial UI state
     */
    private void setupUI() {
        // Setup team count label
        updateTeamCountLabel();

        // Setup start button
        startBattleButton.setOnAction(e -> onStartBattle());
        backButton.setOnAction(e -> onBack());

        // Setup opponent buttons
        aiOpponentButton.setOnAction(e -> onOpponentSelected("AI"));
        localOpponentButton.setOnAction(e -> onOpponentSelected("LOCAL"));
    }

    /**
     * Set default selections
     */
    private void setDefaultSelections() {
        soloModeButton.setSelected(true);
        randomTeamButton.setSelected(true);
        aiOpponentButton.getStyleClass().add("button-selected");
        selectedOpponent = "AI";
        matchingStatusLabel.setText("AI Opponent Ready");

        updateTeamSelectionUI();
    }

    /**
     * Update UI based on selected game mode
     */
    private void updateUIForMode() {
        if ("DUO".equals(selectedMode)) {
            matchingStatusLabel.setText("Duo mode: 2v2 battles (Coming Soon)");
        } else {
            matchingStatusLabel.setText("Solo mode: 1v1 battles");
        }
    }

    /**
     * Handle opponent selection
     */
    private void onOpponentSelected(String opponent) {
        selectedOpponent = opponent;

        // Update button styles
        aiOpponentButton.getStyleClass().remove("button-selected");
        localOpponentButton.getStyleClass().remove("button-selected");

        if ("AI".equals(opponent)) {
            aiOpponentButton.getStyleClass().add("button-selected");
            matchingStatusLabel.setText("AI Opponent Ready");
        } else {
            localOpponentButton.getStyleClass().add("button-selected");
            matchingStatusLabel.setText("Waiting for local player...");
        }
    }

    /**
     * Update team selection UI based on random/custom choice
     */
    private void updateTeamSelectionUI() {

        if (pokemonScrollPane == null || selectedTeamBox == null)
            return;

        if ("RANDOM".equals(selectedTeamType)) {
            pokemonScrollPane.setVisible(false);
            selectedTeamBox.setVisible(false);
            generateRandomTeam();
            displayTeamPreview();
        } else {
            pokemonScrollPane.setVisible(true);
            selectedTeamBox.setVisible(true);
            playerTeam.clear();
            displayPokemonGrid();
            displayTeamPreview();
        }

        updateTeamCountLabel();
    }

    /**
     * Generate a random team of 6 Pokemon
     */
    private void generateRandomTeam() {
        playerTeam.clear();

        if (allPokemon.isEmpty()) {
            return;
        }

        Random random = new Random();
        Set<Integer> usedIndices = new HashSet<>();

        while (playerTeam.size() < MAX_TEAM_SIZE && usedIndices.size() < allPokemon.size()) {
            int index = random.nextInt(allPokemon.size());
            if (!usedIndices.contains(index)) {
                usedIndices.add(index);
                PokemonSpecies species = allPokemon.get(index);

                // Select random moves for this Pokemon
                List<Move> moves = getRandomMovesForPokemon(species, 4);
                PokemonInstance pokemon = PokemonInstance.fromSpeciesWithMoves(species, POKEMON_LEVEL, moves);
                playerTeam.add(pokemon);
            }
        }

        System.out.println("Generated random team of " + playerTeam.size() + " Pokemon");
    }

    /**
     * Get random moves for a Pokemon species
     */
    private List<Move> getRandomMovesForPokemon(PokemonSpecies species, int count) {
        List<Move> availableMoves = species.getMoves().stream()
                .map(allMoves::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (availableMoves.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.shuffle(availableMoves);
        return availableMoves.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Display Pokemon grid for custom selection
     */
    private void displayPokemonGrid() {
        pokemonGrid.getChildren().clear();

        int columns = 6;
        int row = 0;
        int col = 0;

        for (PokemonSpecies species : allPokemon) {
            VBox pokemonCard = createPokemonCard(species);
            pokemonGrid.add(pokemonCard, col, row);

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }

    /**
     * Create a Pokemon card for selection
     */
    private VBox createPokemonCard(PokemonSpecies species) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("pokemon-card");
        card.setPrefWidth(150);
        card.setPrefHeight(180);

        // Pokemon sprite (placeholder)
        ImageView sprite = new ImageView();
        sprite.setFitWidth(80);
        sprite.setFitHeight(80);
        sprite.setPreserveRatio(true);

        // Try to load sprite
        String spritePath = "/com/example/pokemonbattle/sprites/front/" + species.getId() + ".png";
        try {
            Image image = new Image(getClass().getResourceAsStream(spritePath));
            sprite.setImage(image);
        } catch (Exception e) {
            // Use placeholder if sprite not found
            sprite.setStyle("-fx-background-color: #e0e0e0;");
        }

        // Pokemon name
        Label nameLabel = new Label(capitalize(species.getName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Pokemon types
        HBox typesBox = new HBox(5);
        typesBox.setAlignment(Pos.CENTER);
        for (String type : species.getTypes()) {
            Label typeLabel = new Label(type.toUpperCase());
            typeLabel.getStyleClass().add("type-badge");
            typeLabel.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: " + getTypeColor(type)
                    + "; -fx-text-fill: white; -fx-background-radius: 3;");
            typesBox.getChildren().add(typeLabel);
        }

        // Add button
        Button addButton = new Button("+");
        addButton.getStyleClass().add("button-green");
        addButton.setStyle("-fx-font-size: 16px; -fx-padding: 5 15;");
        addButton.setOnAction(e -> addPokemonToTeam(species));

        card.getChildren().addAll(sprite, nameLabel, typesBox, addButton);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-scale-x: 1.05; -fx-scale-y: 1.05; -fx-effect: dropshadow(gaussian, rgba(0,150,150,0.6), 15, 0, 0, 0);"));
        card.setOnMouseExited(e -> card.setStyle(""));

        return card;
    }

    /**
     * Add Pokemon to team
     */
    private void addPokemonToTeam(PokemonSpecies species) {
        if (playerTeam.size() >= MAX_TEAM_SIZE) {
            matchingStatusLabel.setText("Team is full! (Max 6 Pokemon)");
            return;
        }

        // Check if already in team
        boolean alreadyInTeam = playerTeam.stream()
                .anyMatch(p -> p.getSpecies().getId() == species.getId());

        if (alreadyInTeam) {
            matchingStatusLabel.setText(capitalize(species.getName()) + " is already in your team!");
            return;
        }

        // Add to team
        List<Move> moves = getRandomMovesForPokemon(species, 4);
        PokemonInstance pokemon = PokemonInstance.fromSpeciesWithMoves(species, POKEMON_LEVEL, moves);
        playerTeam.add(pokemon);

        displayTeamPreview();
        updateTeamCountLabel();
        matchingStatusLabel.setText(capitalize(species.getName()) + " added to team!");
    }

    /**
     * Display team preview
     */
    private void displayTeamPreview() {
        selectedTeamBox.getChildren().clear();

        Label header = new Label("Your Team:");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        selectedTeamBox.getChildren().add(header);

        for (int i = 0; i < playerTeam.size(); i++) {
            PokemonInstance pokemon = playerTeam.get(i);
            HBox teamEntry = createTeamEntryBox(pokemon, i);
            selectedTeamBox.getChildren().add(teamEntry);
        }
    }

    /**
     * Create a team entry box
     */
    private HBox createTeamEntryBox(PokemonInstance pokemon, int index) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5));
        box.getStyleClass().add("team-entry");
        box.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5;");

        Label numberLabel = new Label((index + 1) + ".");
        numberLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 25;");

        Label nameLabel = new Label(capitalize(pokemon.getSpecies().getName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 120;");

        Label levelLabel = new Label("Lv." + pokemon.getLevel());
        levelLabel.setStyle("-fx-min-width: 60;");

        Label hpLabel = new Label("HP: " + pokemon.getMaxHp());
        hpLabel.setStyle("-fx-min-width: 80;");

        // Remove button (only for custom teams)
        if ("CUSTOM".equals(selectedTeamType)) {
            Button removeButton = new Button("×");
            removeButton.getStyleClass().add("button-dark");
            removeButton.setStyle("-fx-font-size: 16px; -fx-padding: 2 8;");
            removeButton.setOnAction(e -> removePokemonFromTeam(index));
            box.getChildren().addAll(numberLabel, nameLabel, levelLabel, hpLabel, removeButton);
        } else {
            box.getChildren().addAll(numberLabel, nameLabel, levelLabel, hpLabel);
        }

        return box;
    }

    /**
     * Remove Pokemon from team
     */
    private void removePokemonFromTeam(int index) {
        if (index >= 0 && index < playerTeam.size()) {
            PokemonInstance removed = playerTeam.remove(index);
            displayTeamPreview();
            updateTeamCountLabel();
            matchingStatusLabel.setText(capitalize(removed.getSpecies().getName()) + " removed from team");
        }
    }

    /**
     * Update team count label
     */
    private void updateTeamCountLabel() {
        teamCountLabel.setText("Team: " + playerTeam.size() + "/" + MAX_TEAM_SIZE);

        // Enable/disable start button based on team
        startBattleButton.setDisable(playerTeam.isEmpty());
    }

    /**
     * Start battle
     */
    private void onStartBattle() {
        if (playerTeam.isEmpty()) {
            matchingStatusLabel.setText("Please build a team first!");
            return;
        }

        matchingStatusLabel.setText("Starting battle...");

        // Create player
        Player player = new Player("Player 1");
        playerTeam.forEach(player::addToTeam);

        // For now, just show a message (you'll implement actual battle scene later)
        System.out.println("Starting battle with team:");
        playerTeam.forEach(p -> System.out.println("  - " + p.getSpecies().getName() + " Lv." + p.getLevel()));

        // TODO: Navigate to battle scene
        // SceneManager.switchScene("battle.fxml", "Pokemon Battle", 1200, 700);

        matchingStatusLabel.setText("Battle scene not yet implemented!");
    }

    /**
     * Go back to menu
     */
    private void onBack() {
        SceneManager.switchScene("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Get type color for badge
     */
    private String getTypeColor(String type) {
        return switch (type.toLowerCase()) {
            case "grass" -> "#78C850";
            case "fire" -> "#F08030";
            case "water" -> "#6890F0";
            case "electric" -> "#F8D030";
            case "psychic" -> "#F85888";
            case "ice" -> "#98D8D8";
            case "dragon" -> "#7038F8";
            case "dark" -> "#705848";
            case "fairy" -> "#EE99AC";
            case "normal" -> "#A8A878";
            case "fighting" -> "#C03028";
            case "flying" -> "#A890F0";
            case "poison" -> "#A040A0";
            case "ground" -> "#E0C068";
            case "rock" -> "#B8A038";
            case "bug" -> "#A8B820";
            case "ghost" -> "#705898";
            case "steel" -> "#B8B8D0";
            default -> "#68A090";
        };
    }
}
