package com.example.pokemonbattle.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.PokemonSpecies;
import com.example.pokemonbattle.util.SceneManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
 * 
 * FIXED ISSUES:
 * - Toggle buttons now only show borders after user interaction
 * - Start button properly enabled/disabled based on team validity
 * - AI team generation ensures no duplicates and matches player team size
 * - Custom team selection enforces max 6 Pokemon and no duplicates
 * - Scene transitions properly pass player and opponent data
 * - Improved UX with proper spacing, fonts, and feedback
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
    private ToggleGroup opponentToggleGroup;

    // Team Selection
    @FXML
    private HBox teamSelectionBox;
    @FXML
    private ToggleButton randomTeamButton;
    @FXML
    private ToggleButton customTeamButton;
    @FXML
    private Button editTeamButton;
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
    private String selectedMode = null;
    private String selectedOpponent = null;
    
    // Interaction tracking flags
    private boolean modeInteracted = false;
    private boolean opponentInteracted = false;
    private boolean teamTypeInteracted = false;
    private String selectedTeamType = null;
    private static final int MAX_TEAM_SIZE = 6;
    private static final int POKEMON_LEVEL = 50;
    private Player aiOpponent; // Store AI opponent for battle
    
    // Overlay for custom Pokemon selection
    private Parent overlayNode;
    private PokemonSelectionOverlayController overlayController;

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

        // Set default selections (without showing borders)
        setDefaultSelections();
    }

    /**
     * Setup toggle groups for mutually exclusive selections
     */
    private void setupToggleGroups() {
        // Mode toggle group
        modeToggleGroup = new ToggleGroup();
        soloModeButton.setToggleGroup(modeToggleGroup);
        duoModeButton.setToggleGroup(modeToggleGroup);

        // Opponent toggle group
        opponentToggleGroup = new ToggleGroup();
        aiOpponentButton.setToggleGroup(opponentToggleGroup);
        localOpponentButton.setToggleGroup(opponentToggleGroup);

        // Team toggle group
        teamToggleGroup = new ToggleGroup();
        randomTeamButton.setToggleGroup(teamToggleGroup);
        customTeamButton.setToggleGroup(teamToggleGroup);

        // Add listeners with interaction tracking
        modeToggleGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle != null) {
                ToggleButton btn = (ToggleButton) newToggle;
                selectedMode = btn.getText().toUpperCase();
                
                // Apply border style only after interaction
                if (modeInteracted) {
                    applySelectionStyle(soloModeButton, soloModeButton == newToggle);
                    applySelectionStyle(duoModeButton, duoModeButton == newToggle);
                }
                
                updateUIForMode();
            }
        });

        opponentToggleGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle != null) {
                ToggleButton btn = (ToggleButton) newToggle;
                selectedOpponent = btn.getText().toUpperCase().contains("AI") ? "AI" : "LOCAL";
                
                // Apply border style only after interaction
                if (opponentInteracted) {
                    applySelectionStyle(aiOpponentButton, aiOpponentButton == newToggle);
                    applySelectionStyle(localOpponentButton, localOpponentButton == newToggle);
                }
                
                updateOpponentStatus();
            }
        });

        teamToggleGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle != null) {
                ToggleButton btn = (ToggleButton) newToggle;
                selectedTeamType = btn.getText().toUpperCase();
                
                // Apply border style only after interaction
                if (teamTypeInteracted) {
                    applySelectionStyle(randomTeamButton, randomTeamButton == newToggle);
                    applySelectionStyle(customTeamButton, customTeamButton == newToggle);
                }
                
                // Show overlay for custom selection
                if ("CUSTOM".equals(selectedTeamType)) {
                    showPokemonSelectionOverlay();
                } else {
                    updateTeamSelectionUI();
                }
            }
        });
        
        // Add mouse click handlers to mark as interacted
        soloModeButton.setOnMouseClicked(e -> {
            modeInteracted = true;
            applySelectionStyle(soloModeButton, soloModeButton.isSelected());
            applySelectionStyle(duoModeButton, duoModeButton.isSelected());
        });
        duoModeButton.setOnMouseClicked(e -> {
            modeInteracted = true;
            applySelectionStyle(soloModeButton, soloModeButton.isSelected());
            applySelectionStyle(duoModeButton, duoModeButton.isSelected());
        });
        
        aiOpponentButton.setOnMouseClicked(e -> {
            opponentInteracted = true;
            applySelectionStyle(aiOpponentButton, aiOpponentButton.isSelected());
            applySelectionStyle(localOpponentButton, localOpponentButton.isSelected());
        });
        localOpponentButton.setOnMouseClicked(e -> {
            opponentInteracted = true;
            applySelectionStyle(aiOpponentButton, aiOpponentButton.isSelected());
            applySelectionStyle(localOpponentButton, localOpponentButton.isSelected());
        });
        
        randomTeamButton.setOnMouseClicked(e -> {
            teamTypeInteracted = true;
            applySelectionStyle(randomTeamButton, randomTeamButton.isSelected());
            applySelectionStyle(customTeamButton, customTeamButton.isSelected());
            hideEditTeamButton();
        });
        customTeamButton.setOnMouseClicked(e -> {
            teamTypeInteracted = true;
            applySelectionStyle(randomTeamButton, randomTeamButton.isSelected());
            applySelectionStyle(customTeamButton, customTeamButton.isSelected());
        });
    }

    /**
     * Apply or remove selection border style
     */
    private void applySelectionStyle(ToggleButton button, boolean selected) {
        if (selected) {
            if (!button.getStyleClass().contains("toggle-selected")) {
                button.getStyleClass().add("toggle-selected");
            }
        } else {
            button.getStyleClass().remove("toggle-selected");
        }
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
        
        // Setup Edit Team button (initially hidden)
        if (editTeamButton != null) {
            editTeamButton.setVisible(false);
            editTeamButton.setManaged(false);
            editTeamButton.setOnAction(e -> showPokemonSelectionOverlay());
        }
    }

    /**
     * Set default selections (no buttons selected, all states null)
     */
    private void setDefaultSelections() {
        // No buttons selected initially
        soloModeButton.setSelected(false);
        aiOpponentButton.setSelected(false);
        randomTeamButton.setSelected(false);

        // All states remain null until user interaction
        selectedMode = null;
        selectedOpponent = null;
        selectedTeamType = null;

        // Update UI based on null states
        updateUIForMode();
        updateOpponentStatus();
        updateTeamSelectionUI();
    }

    /**
     * Update UI based on selected game mode
     */
    private void updateUIForMode() {
        if (selectedMode == null) {
            matchingStatusLabel.setText("Please select a game mode");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ffffff; -fx-font-style:italic;");
        } else if ("DUO".equals(selectedMode)) {
            matchingStatusLabel.setText("Duo mode: 2v2 battles (Coming Soon)");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#f8d030; -fx-font-style:italic;");
        } else {
            updateOpponentStatus();
        }
        updateStartButtonState();
    }

    /**
     * Update opponent status message
     */
    private void updateOpponentStatus() {
        if (selectedOpponent == null) {
            matchingStatusLabel.setText("Please select an opponent type");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ffffff; -fx-font-style:italic;");
        } else if ("AI".equals(selectedOpponent)) {
            matchingStatusLabel.setText("AI Opponent Ready ✓");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        } else {
            matchingStatusLabel.setText("Waiting for local player...");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ffffff; -fx-font-style:italic;");
        }
        updateStartButtonState();
    }

    /**
     * Update team selection UI based on random/custom choice
     */
    private void updateTeamSelectionUI() {
        if (pokemonScrollPane == null || selectedTeamBox == null)
            return;

        if (selectedTeamType == null) {
            // Hide both UIs when nothing selected
            pokemonScrollPane.setVisible(false);
            pokemonScrollPane.setManaged(false);
            selectedTeamBox.setVisible(false);
            selectedTeamBox.setManaged(false);
            hideEditTeamButton();
            
            matchingStatusLabel.setText("Please select team type (Random or Custom)");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ffffff; -fx-font-style:italic;");
        } else if ("RANDOM".equals(selectedTeamType)) {
            // Hide custom selection UI
            pokemonScrollPane.setVisible(false);
            pokemonScrollPane.setManaged(false);
            selectedTeamBox.setVisible(false);
            selectedTeamBox.setManaged(false);
            hideEditTeamButton();
            
            // Generate random team
            generateRandomTeam();
            displayTeamPreview();
            
            matchingStatusLabel.setText("Random team generated! (" + playerTeam.size() + " Pokemon)");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        } else if ("CUSTOM".equals(selectedTeamType)) {
            // Hide old custom UI (overlay replaces it)
            pokemonScrollPane.setVisible(false);
            pokemonScrollPane.setManaged(false);
            selectedTeamBox.setVisible(false);
            selectedTeamBox.setManaged(false);
        }

        updateTeamCountLabel();
        updateStartButtonState();
    }

    /**
     * Generate a random team of 6 Pokemon (no duplicates)
     */
    private void generateRandomTeam() {
        playerTeam.clear();

        if (allPokemon.isEmpty()) {
            System.err.println("No Pokemon available to generate team!");
            return;
        }

        Random random = new Random();
        List<PokemonSpecies> shuffledPokemon = new ArrayList<>(allPokemon);
        Collections.shuffle(shuffledPokemon, random);

        // Take first 6 unique Pokemon
        int count = Math.min(MAX_TEAM_SIZE, shuffledPokemon.size());
        for (int i = 0; i < count; i++) {
            PokemonSpecies species = shuffledPokemon.get(i);
            List<Move> moves = getRandomMovesForPokemon(species, 4);
            PokemonInstance pokemon = PokemonInstance.fromSpeciesWithMoves(species, POKEMON_LEVEL, moves);
            playerTeam.add(pokemon);
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
        if (pokemonGrid == null) return;
        
        pokemonGrid.getChildren().clear();
        pokemonGrid.setHgap(12);
        pokemonGrid.setVgap(12);
        pokemonGrid.setPadding(new Insets(10));

        int columns = 5;
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
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("pokemon-card");
        card.setPrefWidth(140);
        card.setPrefHeight(190);

        // Pokemon sprite
        ImageView sprite = new ImageView();
        sprite.setFitWidth(90);
        sprite.setFitHeight(90);
        sprite.setPreserveRatio(true);

        // Try to load sprite
        String spritePath = "/com/example/pokemonbattle/sprites/front/" + species.getId() + ".png";
        try {
            var spriteUrl = getClass().getResource(spritePath);
            if (spriteUrl != null) {
                Image image = new Image(spriteUrl.toExternalForm());
                sprite.setImage(image);
            } else {
                // Use placeholder
                createPlaceholderSprite(sprite, species);
            }
        } catch (Exception e) {
            // Use placeholder if sprite not found
            createPlaceholderSprite(sprite, species);
        }

        // Pokemon name
        Label nameLabel = new Label(capitalize(species.getName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        // Pokemon types
        HBox typesBox = new HBox(5);
        typesBox.setAlignment(Pos.CENTER);
        for (String type : species.getTypes()) {
            Label typeLabel = new Label(type.toUpperCase());
            typeLabel.getStyleClass().add("type-badge");
            typeLabel.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-color: " + getTypeColor(type)
                    + "; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-weight: bold;");
            typesBox.getChildren().add(typeLabel);
        }

        // Check if already in team
        boolean inTeam = playerTeam.stream()
                .anyMatch(p -> p.getSpecies().getId() == species.getId());

        // Add/Remove button
        Button addButton = new Button(inTeam ? "✓" : "+");
        addButton.setStyle("-fx-font-size: 18px; -fx-padding: 4 16; -fx-font-weight: bold;");
        
        if (inTeam) {
            addButton.getStyleClass().add("button-dark");
            addButton.setOnAction(e -> removePokemonFromTeamBySpecies(species));
        } else {
            addButton.getStyleClass().add("button-green");
            addButton.setOnAction(e -> addPokemonToTeam(species));
        }

        card.getChildren().addAll(sprite, nameLabel, typesBox, addButton);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-scale-x: 1.05; -fx-scale-y: 1.05; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,150,150,0.7), 18, 0, 0, 0);");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("");
        });

        return card;
    }

    /**
     * Create placeholder sprite when image not found
     */
    private void createPlaceholderSprite(ImageView sprite, PokemonSpecies species) {
        // Use a colored rectangle as placeholder based on primary type
        String primaryType = species.getTypes().isEmpty() ? "normal" : species.getTypes().get(0);
        String color = getTypeColor(primaryType);
        sprite.setStyle("-fx-background-color: " + color + "; " +
                "-fx-background-radius: 45; " +
                "-fx-border-color: rgba(0,0,0,0.2); " +
                "-fx-border-radius: 45; " +
                "-fx-border-width: 2;");
    }

    /**
     * Add Pokemon to team
     */
    private void addPokemonToTeam(PokemonSpecies species) {
        // Check team size
        if (playerTeam.size() >= MAX_TEAM_SIZE) {
            matchingStatusLabel.setText("Team is full! Maximum " + MAX_TEAM_SIZE + " Pokemon allowed");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }

        // Check for duplicates
        boolean alreadyInTeam = playerTeam.stream()
                .anyMatch(p -> p.getSpecies().getId() == species.getId());

        if (alreadyInTeam) {
            matchingStatusLabel.setText(capitalize(species.getName()) + " is already in your team!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }

        // Add to team
        List<Move> moves = getRandomMovesForPokemon(species, 4);
        PokemonInstance pokemon = PokemonInstance.fromSpeciesWithMoves(species, POKEMON_LEVEL, moves);
        playerTeam.add(pokemon);

        // Refresh UI
        displayPokemonGrid(); // Refresh grid to update buttons
        displayTeamPreview();
        updateTeamCountLabel();
        updateStartButtonState();
        
        matchingStatusLabel.setText(capitalize(species.getName()) + " added! (" + playerTeam.size() + "/" + MAX_TEAM_SIZE + ")");
        matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
    }

    /**
     * Remove Pokemon from team by species
     */
    private void removePokemonFromTeamBySpecies(PokemonSpecies species) {
        playerTeam.removeIf(p -> p.getSpecies().getId() == species.getId());
        
        // Refresh UI
        displayPokemonGrid();
        displayTeamPreview();
        updateTeamCountLabel();
        updateStartButtonState();
        
        matchingStatusLabel.setText(capitalize(species.getName()) + " removed");
        matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#6890F0; -fx-font-weight:bold;");
    }

    /**
     * Display team preview
     */
    private void displayTeamPreview() {
        if (selectedTeamBox == null) return;
        
        selectedTeamBox.getChildren().clear();
        selectedTeamBox.setSpacing(8);
        selectedTeamBox.setPadding(new Insets(15));
        selectedTeamBox.setStyle("-fx-background-color: rgba(255,255,255,0.08); " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: rgba(255,255,255,0.2); " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10;");

        Label header = new Label("Your Team:");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #78C850;");
        selectedTeamBox.getChildren().add(header);

        if (playerTeam.isEmpty()) {
            Label emptyLabel = new Label("No Pokemon selected");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaaaaa; -fx-font-style: italic;");
            selectedTeamBox.getChildren().add(emptyLabel);
            return;
        }

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
        box.setPadding(new Insets(8, 12, 8, 12));
        box.getStyleClass().add("team-entry");
        box.setStyle("-fx-background-color: rgba(255,255,255,0.12); " +
                "-fx-background-radius: 6; " +
                "-fx-border-color: rgba(120, 200, 80, 0.4); " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 6;");

        Label numberLabel = new Label((index + 1) + ".");
        numberLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 25; -fx-text-fill: #ffffff; -fx-font-size: 13px;");

        Label nameLabel = new Label(capitalize(pokemon.getSpecies().getName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 120; -fx-text-fill: #ffffff; -fx-font-size: 13px;");

        Label levelLabel = new Label("Lv." + pokemon.getLevel());
        levelLabel.setStyle("-fx-min-width: 50; -fx-text-fill: #f8d030; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label hpLabel = new Label("HP: " + pokemon.getMaxHp());
        hpLabel.setStyle("-fx-min-width: 70; -fx-text-fill: #78C850; -fx-font-size: 12px;");

        // Remove button (only for custom teams)
        if ("CUSTOM".equals(selectedTeamType)) {
            Button removeButton = new Button("✕");
            removeButton.getStyleClass().add("button-dark");
            removeButton.setStyle("-fx-font-size: 14px; -fx-padding: 3 10; -fx-font-weight: bold;");
            removeButton.setOnAction(e -> removePokemonFromTeam(index));
            box.getChildren().addAll(numberLabel, nameLabel, levelLabel, hpLabel, removeButton);
        } else {
            box.getChildren().addAll(numberLabel, nameLabel, levelLabel, hpLabel);
        }

        // Hover effect
        box.setOnMouseEntered(e -> {
            box.setStyle("-fx-background-color: rgba(255,255,255,0.18); " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-color: rgba(120, 200, 80, 0.6); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 6;");
        });
        box.setOnMouseExited(e -> {
            box.setStyle("-fx-background-color: rgba(255,255,255,0.12); " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-color: rgba(120, 200, 80, 0.4); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 6;");
        });

        return box;
    }

    /**
     * Remove Pokemon from team
     */
    private void removePokemonFromTeam(int index) {
        if (index >= 0 && index < playerTeam.size()) {
            PokemonInstance removed = playerTeam.remove(index);
            displayPokemonGrid(); // Refresh grid to update buttons
            displayTeamPreview();
            updateTeamCountLabel();
            updateStartButtonState();
            matchingStatusLabel.setText(capitalize(removed.getSpecies().getName()) + " removed from team");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#6890F0; -fx-font-weight:bold;");
        }
    }

    /**
     * Update team count label and start button
     */
    private void updateTeamCountLabel() {
        if (teamCountLabel == null) return;
        
        teamCountLabel.setText("Team: " + playerTeam.size() + "/" + MAX_TEAM_SIZE);
        teamCountLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; " +
                "-fx-text-fill:" + (playerTeam.isEmpty() ? "#F08030" : "#78C850") + ";");
    }

    /**
     * Update start button enabled state
     */
    private void updateStartButtonState() {
        if (startBattleButton == null) return;
        
        boolean canStart = selectedMode != null &&
                          selectedOpponent != null &&
                          selectedTeamType != null &&
                          !playerTeam.isEmpty() && 
                          "AI".equals(selectedOpponent) && 
                          "SOLO".equals(selectedMode);
        
        startBattleButton.setDisable(!canStart);
        
        if (!canStart) {
            startBattleButton.setOpacity(0.5);
        } else {
            startBattleButton.setOpacity(1.0);
        }
    }

    /**
     * Start battle
     */
    private void onStartBattle() {
        // Validate all selections are made
        if (selectedMode == null) {
            matchingStatusLabel.setText("Please select a game mode first!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }
        
        if (selectedOpponent == null) {
            matchingStatusLabel.setText("Please select an opponent type first!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }
        
        if (selectedTeamType == null) {
            matchingStatusLabel.setText("Please select a team type first!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }
        
        if (playerTeam.isEmpty()) {
            matchingStatusLabel.setText("Please build a team first!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }
        
        if (!"SOLO".equals(selectedMode)) {
            matchingStatusLabel.setText("Only SOLO mode is currently available!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }

        matchingStatusLabel.setText("Preparing battle...");
        matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#f8d030; -fx-font-weight:bold;");

        // Create player
        Player player = new Player("Player 1");
        playerTeam.forEach(player::addToTeam);

        // Generate AI opponent team
        aiOpponent = generateAIOpponent();

        // Log battle info
        System.out.println("\n=== BATTLE STARTING ===");
        System.out.println("Player Team:");
        playerTeam.forEach(p -> System.out.println("  - " + p.getSpecies().getName() + " Lv." + p.getLevel()));
        System.out.println("\nAI Opponent Team:");
        aiOpponent.getTeam().forEach(p -> System.out.println("  - " + p.getSpecies().getName() + " Lv." + p.getLevel()));

        // Navigate to battle scene and pass player and opponent data
        SceneManager.switchSceneWithData("battle.fxml", "Pokemon Battle - Arena", 1200, 700, 
            Map.of("player", player, "opponent", aiOpponent));
    }

    /**
     * Generate AI opponent with random team (matching player team size, no duplicates)
     */
    private Player generateAIOpponent() {
        Player opponent = new Player("AI Trainer");
        
        if (allPokemon.isEmpty()) {
            System.err.println("No Pokemon available for AI team!");
            return opponent;
        }

        // Match player team size
        int targetSize = Math.min(playerTeam.size(), MAX_TEAM_SIZE);
        
        // Get Pokemon not in player's team for variety
        List<PokemonSpecies> availablePokemon = allPokemon.stream()
                .filter(species -> playerTeam.stream()
                        .noneMatch(p -> p.getSpecies().getId() == species.getId()))
                .collect(Collectors.toList());
        
        // If not enough variety, use all Pokemon
        if (availablePokemon.size() < targetSize) {
            availablePokemon = new ArrayList<>(allPokemon);
        }
        
        // Shuffle and take first N
        Collections.shuffle(availablePokemon);
        
        for (int i = 0; i < targetSize && i < availablePokemon.size(); i++) {
            PokemonSpecies species = availablePokemon.get(i);
            List<Move> moves = getRandomMovesForPokemon(species, 4);
            PokemonInstance pokemon = PokemonInstance.fromSpeciesWithMoves(species, POKEMON_LEVEL, moves);
            opponent.addToTeam(pokemon);
        }

        System.out.println("Generated AI team of " + opponent.getTeam().size() + " Pokemon");
        return opponent;
    }

    /**
     * Show Pokemon selection overlay
     */
    private void showPokemonSelectionOverlay() {
        try {
            // Load overlay FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pokemonbattle/view/pokemon_selection_overlay.fxml"));
            overlayNode = loader.load();
            overlayController = loader.getController();
            
            // Initialize overlay with data and callback
            overlayController.initializeData(
                allPokemon, 
                allMoves, 
                playerTeam,
                this::onOverlayDone
            );
            
            // Add overlay to root pane
            rootPane.getChildren().add(overlayNode);
            
            // Mark interaction
            teamTypeInteracted = true;
            
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading Pokemon selection overlay: " + e.getMessage());
            System.err.println("Exception: " + e.getClass().getName());
            matchingStatusLabel.setText("Error loading selection overlay!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
        }
    }
    
    /**
     * Callback when overlay selection is done
     */
    private void onOverlayDone(List<PokemonInstance> selectedTeam) {
        // Remove overlay from view
        if (overlayNode != null && rootPane.getChildren().contains(overlayNode)) {
            rootPane.getChildren().remove(overlayNode);
        }
        
        // Update player team
        playerTeam.clear();
        playerTeam.addAll(selectedTeam);
        
        // Show Edit Team button
        showEditTeamButton();
        
        // Update UI
        displayTeamPreview();
        updateTeamCountLabel();
        updateStartButtonState();
        
        // Update status
        if (playerTeam.isEmpty()) {
            matchingStatusLabel.setText("No Pokemon selected. Click Edit Team to select.");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ffffff; -fx-font-style:italic;");
        } else {
            matchingStatusLabel.setText("Custom team selected! (" + playerTeam.size() + " Pokemon)");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        }
        
        // Show team preview
        if (selectedTeamBox != null) {
            selectedTeamBox.setVisible(true);
            selectedTeamBox.setManaged(true);
        }
    }
    
    /**
     * Show Edit Team button
     */
    private void showEditTeamButton() {
        if (editTeamButton != null) {
            editTeamButton.setVisible(true);
            editTeamButton.setManaged(true);
        }
    }
    
    /**
     * Hide Edit Team button
     */
    private void hideEditTeamButton() {
        if (editTeamButton != null) {
            editTeamButton.setVisible(false);
            editTeamButton.setManaged(false);
        }
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