package com.example.pokemonbattle.controller;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.pokemonbattle.database.GameDataDAO;
import com.example.pokemonbattle.model.BattleRecord;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.PokemonSpecies;
import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.service.BattleHistoryManager;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class NewGameController {

    // FXML Components
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
    @FXML
    private Region dashboardOverlay;
    @FXML
    private VBox contentVBox;

    // Dashboard — Avatar Panel
    @FXML
    private VBox avatarPanel;
    @FXML
    private ImageView avatarDisplay;
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label winsLabel;
    @FXML
    private Label lossesLabel;
    @FXML
    private Button battleHistoryButton;
    @FXML
    private Button changeAvatarButton;
    @FXML
    private Button viewSelectedPokemonButton;

    // Mode Selection
    @FXML
    private HBox modeSelectionBox;
    @FXML
    private ToggleButton soloModeButton;
    @FXML
    private Label soloInfoBadge;
    @FXML
    private ToggleButton duoModeButton;
    @FXML
    private Label duoInfoBadge;
    private ToggleGroup modeToggleGroup;

    // Opponent Selection
    @FXML
    private HBox opponentSelectionBox;
    @FXML
    private ToggleButton aiOpponentButton;
    @FXML
    private ToggleButton onlineOpponentButton;
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
    @FXML
    private Button newGameSettingsButton;
    @FXML
    private Button quickGuideButton;

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
    private String menuFontFamily = "System";

    @FXML
    public void initialize() {
        loadMenuFontForGuides();

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

        // Dashboard: load avatar + stats
        loadDashboardData();

        // Start subtle avatar idle animation
        // startAvatarIdleAnimation();

        MusicManager.getInstance().attachClickSounds(rootPane);

        // First-time user: show avatar selection overlay
        if (PlayerSession.getInstance().isFirstTime()) {
            javafx.application.Platform.runLater(this::showAvatarSelectionOverlay);
        }
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
        onlineOpponentButton.setToggleGroup(opponentToggleGroup);

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
                if (btn.getText().toUpperCase().contains("AI")) {
                    selectedOpponent = "AI";
                } else {
                    selectedOpponent = "ONLINE";
                }

                // Apply border style only after interaction
                if (opponentInteracted) {
                    applySelectionStyle(aiOpponentButton, aiOpponentButton == newToggle);
                    applySelectionStyle(onlineOpponentButton, onlineOpponentButton == newToggle);
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
            applySelectionStyle(onlineOpponentButton, onlineOpponentButton.isSelected());
        });
        onlineOpponentButton.setOnMouseClicked(e -> {
            opponentInteracted = true;
            applySelectionStyle(aiOpponentButton, aiOpponentButton.isSelected());
            applySelectionStyle(onlineOpponentButton, onlineOpponentButton.isSelected());
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

    private void loadGameData() {
        try {
            GameDataDAO dao = new GameDataDAO();
            dao.ensureDataLoaded();

            // Load moves from DB
            allMoves = dao.loadAllMoves();

            // Load pokemon from DB
            allPokemon = dao.loadAllPokemon();

            // Select random 4 moves for each Pokemon
            for (PokemonSpecies pokemon : allPokemon) {
                pokemon.selectRandomMoves(allMoves);
            }
            PokemonInstance.setAllPokemonSpecies(allPokemon);
            PokemonInstance.setAllMoves(allMoves);

            System.out.println("Loaded " + allPokemon.size() + " Pokemon + " + allMoves.size() + " moves from DB");
        } catch (Exception e) {
            System.err.println("Error loading game data: " + e.getMessage());
            e.printStackTrace();
            allMoves = new HashMap<>();
            allPokemon = new ArrayList<>();
        }
    }

    private void setupUI() {
        updateTeamCountLabel();

        if (duoModeButton != null) {
            duoModeButton.setDisable(true);
        }

        if (duoInfoBadge != null) {
            Tooltip duoModeTooltip = new Tooltip("Will be added in the next patch");
            duoModeTooltip.setShowDelay(Duration.millis(120));
            duoModeTooltip.getStyleClass().add("duo-info-tooltip");
            Tooltip.install(duoInfoBadge, duoModeTooltip);
        }

        if (soloInfoBadge != null) {
            Tooltip soloModeTooltip = new Tooltip("Solo: single-trainer battle (you vs AI or online rival)");
            soloModeTooltip.setShowDelay(Duration.millis(120));
            soloModeTooltip.getStyleClass().add("duo-info-tooltip");
            Tooltip.install(soloInfoBadge, soloModeTooltip);
        }

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

    private void setDefaultSelections() {
        soloModeButton.setSelected(false);
        aiOpponentButton.setSelected(false);
        randomTeamButton.setSelected(false);
        selectedMode = null;
        selectedOpponent = null;
        selectedTeamType = null;
        updateUIForMode();
        updateOpponentStatus();
        updateTeamSelectionUI();
    }

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

    private void updateOpponentStatus() {
        if (selectedOpponent == null) {
            matchingStatusLabel.setText("Please select an opponent type");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ffffff; -fx-font-style:italic;");
        } else if ("AI".equals(selectedOpponent)) {
            matchingStatusLabel.setText("AI Opponent Ready ✓");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
        } else {
            matchingStatusLabel.setText("Will search for server on WiFi network");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#a890f0; -fx-font-style:italic;");
        }
        updateStartButtonState();
    }

    private void updateTeamSelectionUI() {
        if (pokemonScrollPane == null || selectedTeamBox == null)
            return;

        if (selectedTeamType == null) {
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
        if (pokemonGrid == null)
            return;

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
                .anyMatch(p -> p.getId() == species.getId());

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
                .anyMatch(p -> p.getId() == species.getId());

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

        matchingStatusLabel
                .setText(capitalize(species.getName()) + " added! (" + playerTeam.size() + "/" + MAX_TEAM_SIZE + ")");
        matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#78C850; -fx-font-weight:bold;");
    }

    /**
     * Remove Pokemon from team by species
     */
    private void removePokemonFromTeamBySpecies(PokemonSpecies species) {
        playerTeam.removeIf(p -> p.getId() == species.getId());

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
        if (selectedTeamBox == null)
            return;

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

        Label nameLabel = new Label(capitalize(pokemon.getName()));
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
            matchingStatusLabel.setText(capitalize(removed.getName()) + " removed from team");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#6890F0; -fx-font-weight:bold;");
        }
    }

    /**
     * Update team count label and start button
     */
    private void updateTeamCountLabel() {
        if (teamCountLabel == null)
            return;

        teamCountLabel.setText("Team: " + playerTeam.size() + "/" + MAX_TEAM_SIZE);
        teamCountLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; " +
                "-fx-text-fill:" + (playerTeam.isEmpty() ? "#F08030" : "#78C850") + ";");
    }

    /**
     * Update start button enabled state
     */
    private void updateStartButtonState() {
        if (startBattleButton == null)
            return;

        // ONLINE battles don't need a mode selection (server handles it)
        boolean isOnlineReady = "ONLINE".equals(selectedOpponent) &&
                selectedTeamType != null &&
                !playerTeam.isEmpty();

        boolean isAiReady = "AI".equals(selectedOpponent) &&
                selectedTeamType != null &&
                !playerTeam.isEmpty() &&
                "SOLO".equals(selectedMode);

        boolean canStart = isOnlineReady || isAiReady;

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
        // (mode is optional for ONLINE battles — server handles pairing)
        if (selectedMode == null && !"ONLINE".equals(selectedOpponent)) {
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

        // Determine player name from session (fall back to "Player 1")
        User sessionUser = PlayerSession.getInstance().getCurrentUser();
        String playerName = (sessionUser != null && sessionUser.getUsername() != null)
                ? sessionUser.getUsername()
                : "Player 1";

        // Build the Player object with the selected team
        Player player = new Player(playerName);
        playerTeam.forEach(player::addToTeam);

        // LOCAL battle (same machine → localhost)
        // if ("LOCAL".equals(selectedOpponent)) {
        // System.out.println("\n=== LOCAL ONLINE BATTLE — Connecting to localhost
        // ===");
        // System.out.println("Player: " + playerName);
        // playerTeam.forEach(p -> System.out.println(" - " + p.getName() + " Lv." +
        // p.getLevel()));

        // SceneManager.switchSceneWithData("waiting_online.fxml",
        // "Pokemon Battle - Matchmaking", 1200, 700,
        // Map.of("player", player, "connectionMode", "LOCAL"));
        // return;
        // }

        // ONLINE battle (LAN discovery → find server on WiFi)
        if ("ONLINE".equals(selectedOpponent)) {
            System.out.println("\n=== ONLINE BATTLE — Discovering server on LAN ===");
            System.out.println("Player: " + playerName);
            playerTeam.forEach(p -> System.out.println("  - " + p.getName() + " Lv." + p.getLevel()));

            SceneManager.switchSceneWithData("waiting_online.fxml",
                    "Pokemon Battle - Matchmaking", 1200, 700,
                    Map.of("player", player, "connectionMode", "ONLINE"));
            return;
        }

        // LOCAL AI battle
        if (!"SOLO".equals(selectedMode)) {
            matchingStatusLabel.setText("Please select SOLO mode for AI battles!");
            matchingStatusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#F08030; -fx-font-weight:bold;");
            return;
        }
        // Generate AI opponent team
        aiOpponent = generateAIOpponent();

        // Log battle info
        System.out.println("\n=== BATTLE STARTING ===");
        System.out.println("Player Team:");
        playerTeam.forEach(p -> System.out.println("  - " + p.getName() + " Lv." + p.getLevel()));
        System.out.println("\nAI Opponent Team:");
        aiOpponent.getTeam().forEach(p -> System.out.println("  - " + p.getName() + " Lv." + p.getLevel()));

        // Navigate to battle scene and pass player and opponent data
        boolean isRandomTeam = "RANDOM".equals(selectedTeamType);
        SceneManager.switchSceneWithLoading("battle.fxml", "Pokemon Battle - Arena", 1200, 700,
                Map.of("player", player, "opponent", aiOpponent, "randomTeam", isRandomTeam));
    }

    /**
     * Generate AI opponent with random team (matching player team size, no
     * duplicates)
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
                        .noneMatch(p -> p.getId() == species.getId()))
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pokemonbattle/view/pokemon_selection_overlay.fxml"));
            overlayNode = loader.load();
            overlayController = loader.getController();

            // Initialize overlay with data and callback
            overlayController.initializeData(
                    allPokemon,
                    allMoves,
                    playerTeam,
                    this::onOverlayDone);

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

        // Show "View Selected Pokémon" button on dashboard
        showViewPokemonButton();
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
        SceneManager.switchSceneWithLoading("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
    }

    @FXML
    private void onNewGameSettingsClicked() {
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
            Node overlay = loader.load();
            SettingsController settingsController = loader.getController();
            if (settingsController != null) {
                settingsController.setQuitToDesktopVisible(true);
            }
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

    @FXML
    private void onQuickGuideClicked() {
        showNewGameGuideOverlay();
    }

    private void showNewGameGuideOverlay() {
        if (rootPane == null)
            return;

        // Dim backdrop
        StackPane overlayRoot = new StackPane();
        overlayRoot.getStyleClass().add("guide-overlay-root");

        // Card (wider, shorter)
        VBox card = new VBox(0);
        card.getStyleClass().add("guide-overlay-card");
        card.setMaxWidth(820);
        card.setMinWidth(700);
        card.setMaxHeight(440);
        card.setFillWidth(true);

        // Header row: title + ✕ button
        Label titleLbl = new Label("⚔  Quick Battle Guide");
        titleLbl.getStyleClass().add("guide-overlay-title");
        titleLbl.setFont(Font.font(menuFontFamily, FontWeight.BOLD, 24));
        titleLbl.setMaxWidth(Double.MAX_VALUE);
        titleLbl.setWrapText(false);

        Button xBtn = new Button("✕");
        xBtn.getStyleClass().add("guide-overlay-x-btn");
        xBtn.setFont(Font.font(menuFontFamily, FontWeight.BOLD, 16));
        xBtn.setOnAction(e -> dismissGuideOverlay(overlayRoot));

        HBox header = new HBox(titleLbl, xBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFillHeight(true);
        HBox.setHgrow(titleLbl, javafx.scene.layout.Priority.ALWAYS);
        header.getStyleClass().add("guide-overlay-header");
        header.setPadding(new Insets(18, 18, 14, 24));

        // Divider
        Region divider = new Region();
        divider.getStyleClass().add("guide-overlay-divider");
        divider.setPrefHeight(2);
        divider.setMaxHeight(2);

        // Content area
        VBox body = new VBox(12);
        body.setPadding(new Insets(18, 28, 16, 28));
        body.setFillWidth(true);

        String[][] steps = {
                { "1.  Select Mode",
                        "SOLO is available now — one trainer vs opponent.  DUO mode (2v2) is coming in a future patch." },
                { "2.  Choose Opponent",
                        "AI  →  instant local match, no server needed.\nOnline  →  discovers a server on your WiFi and waits for a real opponent to join." },
                { "3.  Build Your Team",
                        "RANDOM  →  auto-generates a full team of 6 Pokémon.\nCUSTOM  →  hand-pick up to 6 Pokémon and their move sets from the selection overlay." },
                { "4.  Start Battle",
                        "Hit  START  once you have an opponent type and a team ready.  Good luck, Trainer!" }
        };

        for (String[] step : steps) {
            Label heading = new Label(step[0]);
            heading.getStyleClass().add("guide-overlay-step-heading");
            heading.setFont(Font.font(menuFontFamily, FontWeight.BOLD, 16));
            heading.setMaxWidth(Double.MAX_VALUE);
            heading.setWrapText(false);

            Label detail = new Label(step[1]);
            detail.getStyleClass().add("guide-overlay-body");
            detail.setFont(Font.font(menuFontFamily, 14));
            detail.setWrapText(true);
            detail.setMaxWidth(Double.MAX_VALUE);
            detail.setTextAlignment(TextAlignment.LEFT);
            detail.setAlignment(Pos.TOP_LEFT);

            VBox stepBox = new VBox(3, heading, detail);
            stepBox.setFillWidth(true);
            body.getChildren().add(stepBox);
        }

        // Footer: close button
        Button closeBtn = new Button("Close Guide");
        closeBtn.getStyleClass().add("guide-overlay-close-btn");
        closeBtn.setFont(Font.font(menuFontFamily, FontWeight.BOLD, 15));
        closeBtn.setPrefWidth(180);
        closeBtn.setPrefHeight(40);
        closeBtn.setOnAction(e -> dismissGuideOverlay(overlayRoot));

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 24, 20, 24));

        card.getChildren().addAll(header, divider, body, footer);

        // Wire-up
        overlayRoot.getChildren().add(card);
        overlayRoot.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayRoot)
                dismissGuideOverlay(overlayRoot);
        });
        card.setOnMouseClicked(javafx.event.Event::consume);

        overlayRoot.setOpacity(0.0);
        card.setScaleX(0.93);
        card.setScaleY(0.93);
        rootPane.getChildren().add(overlayRoot);
        MusicManager.getInstance().attachClickSounds(overlayRoot);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), overlayRoot);
        fadeIn.setToValue(1.0);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(220), card);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        fadeIn.play();
        scaleIn.play();
    }

    private void dismissGuideOverlay(StackPane overlayRoot) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(160), overlayRoot);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(ev -> rootPane.getChildren().remove(overlayRoot));
        fadeOut.play();
    }

    private void loadMenuFontForGuides() {
        try {
            Font loaded = Font.loadFont(
                    getClass().getResourceAsStream("/com/example/pokemonbattle/fonts/menu.ttf"),
                    14);
            if (loaded != null && loaded.getFamily() != null && !loaded.getFamily().isBlank()) {
                menuFontFamily = loaded.getFamily();
            }
        } catch (Exception ignored) {
            menuFontFamily = "System";
        }
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
            case "flying" -> "#b0ceee";
            case "poison" -> "#A040A0";
            case "ground" -> "#E0C068";
            case "rock" -> "#B8A038";
            case "bug" -> "#A8B820";
            case "ghost" -> "#705898";
            case "steel" -> "#B8B8D0";
            default -> "#68A090";
        };
    }

    /**
     * Load avatar, player name, and battle stats from PlayerSession +
     * BattleHistoryManager.
     */
    private void loadDashboardData() {
        PlayerSession session = PlayerSession.getInstance();
        User user = session.getCurrentUser();

        // Player name
        if (user != null && playerNameLabel != null) {
            playerNameLabel.setText(user.getUsername());
        }

        // Avatar
        if (session.getAvatarPath() != null && avatarDisplay != null) {
            try {
                var url = getClass().getResource(session.getAvatarPath());
                if (url != null) {
                    avatarDisplay.setImage(new Image(url.toExternalForm(), 180, 180, true, true));
                }
            } catch (Exception e) {
                System.err.println("[Dashboard] Failed to load avatar: " + e.getMessage());
            }
        }

        // Battle stats
        if (user != null && user.getId() != null) {
            try {
                BattleHistoryManager history = BattleHistoryManager.getInstance();
                int wins = history.getWinCount(user.getId());
                int losses = history.getLossCount(user.getId());
                if (winsLabel != null)
                    winsLabel.setText(String.valueOf(wins));
                if (lossesLabel != null)
                    lossesLabel.setText(String.valueOf(losses));
            } catch (Exception e) {
                System.err.println("[Dashboard] Failed to load stats: " + e.getMessage());
            }
        }
    }
    // Change Avatar

    @FXML
    void onChangeAvatarClick() {
        showAvatarSelectionOverlay();
    }

    private void showAvatarSelectionOverlay() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pokemonbattle/view/avatar_selection.fxml"));
            Node overlay = loader.load();
            AvatarSelectionController ctrl = loader.getController();

            ctrl.setOnAvatarSelected(args -> {
                // args[0] = path, args[1] = gender
                loadDashboardData(); // Refresh avatar display
            });

            // Blur everything currently in rootPane before the overlay is added
            GaussianBlur blur = new GaussianBlur(9);
            for (Node n : rootPane.getChildren()) {
                n.setEffect(blur);
            }

            overlay.setOpacity(0.0);
            rootPane.getChildren().add(overlay);
            MusicManager.getInstance().attachClickSounds((Parent) overlay);

            FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        } catch (IOException e) {
            System.err.println("[Dashboard] Error loading avatar selection: " + e.getMessage());
        }
    }

    // Battle History Overlay

    @FXML
    void onBattleHistoryClick() {
        showBattleHistoryOverlay();
    }

    private void showBattleHistoryOverlay() {
        PlayerSession session = PlayerSession.getInstance();
        User user = session.getCurrentUser();
        if (user == null || user.getId() == null) {
            matchingStatusLabel.setText("Please log in to view battle history");
            matchingStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#F08030; -fx-font-style:italic;");
            return;
        }

        BattleHistoryManager historyMgr = BattleHistoryManager.getInstance();

        // Root overlay (semi-transparent dark background)
        StackPane overlayRoot = new StackPane();
        overlayRoot.setAlignment(Pos.CENTER);
        overlayRoot.setStyle("-fx-background-color: rgba(0,0,0,0.65);");

        // Inner card
        VBox container = new VBox(15);
        container.setAlignment(Pos.TOP_CENTER);
        container.setMaxWidth(620);
        container.setMaxHeight(560);
        container.setPadding(new Insets(28, 30, 24, 30));
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a3a35, #122b27);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(120,200,80,0.35);" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 24, 0, 0, 6);");

        // Title
        Label title = new Label("⚔ Battle History");
        title.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #78C850;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 4, 0, 0, 1);");

        // Summary row
        HBox summary = new HBox(30);
        summary.setAlignment(Pos.CENTER);
        summary.setPadding(new Insets(6, 0, 4, 0));

        Label winLbl = new Label();
        winLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #78C850;");

        Label lossLbl = new Label();
        lossLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #F08030;");

        Label totalLbl = new Label();
        totalLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #b0eedf;");

        summary.getChildren().addAll(winLbl, lossLbl, totalLbl);

        // Filter buttons
        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER);
        filterRow.setPadding(new Insets(2, 0, 4, 0));

        String filterBtnBase = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 16; " +
                "-fx-background-radius: 14; -fx-cursor: hand; -fx-border-radius: 14; -fx-border-width: 1.5;";
        String filterActive = filterBtnBase +
                "-fx-background-color: #2d7a6e; -fx-text-fill: white; -fx-border-color: #78C850;";
        String filterInactive = filterBtnBase +
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #90aea6; -fx-border-color: rgba(120,200,80,0.2);";

        Button filterAll = new Button("All");
        Button filterAI = new Button("🤖 AI");
        Button filterOnline = new Button("🌐 Online");
        filterAll.setStyle(filterActive);
        filterAI.setStyle(filterInactive);
        filterOnline.setStyle(filterInactive);

        filterRow.getChildren().addAll(filterAll, filterAI, filterOnline);

        // Divider
        javafx.scene.shape.Rectangle divider = new javafx.scene.shape.Rectangle();
        divider.setHeight(1);
        divider.setWidth(480);
        divider.setFill(javafx.scene.paint.Color.web("#78C850", 0.25));

        // Records list
        VBox recordsList = new VBox(8);
        recordsList.setPadding(new Insets(4, 6, 4, 6));

        ScrollPane scroll = new ScrollPane(recordsList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(310);
        scroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;");
        scroll.skinProperty().addListener((obs, o, n) -> {
            if (scroll.lookup(".viewport") != null)
                scroll.lookup(".viewport").setStyle("-fx-background-color: transparent;");
        });

        // Shared state for active filter
        final String[] activeFilter = { null }; // null = all

        // Populate helper
        Runnable refreshList = () -> {
            List<BattleRecord> records = historyMgr.getBattleHistoryByType(user.getId(), activeFilter[0]);
            long wins = records.stream().filter(r -> "WIN".equals(r.getResult())).count();
            long losses = records.stream().filter(r -> "LOSS".equals(r.getResult())).count();

            winLbl.setText("✔  Wins: " + wins);
            lossLbl.setText("✘  Losses: " + losses);
            totalLbl.setText("▣  Total: " + records.size());

            recordsList.getChildren().clear();
            if (records.isEmpty()) {
                Label empty = new Label("No battle records yet — start your first battle!");
                empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #90aea6; -fx-font-style: italic;");
                recordsList.getChildren().add(empty);
            } else {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");
                for (BattleRecord record : records) {
                    recordsList.getChildren().add(buildRecordCard(record, fmt));
                }
            }
        };

        // Apply filter on click
        filterAll.setOnAction(e -> {
            activeFilter[0] = null;
            filterAll.setStyle(filterActive);
            filterAI.setStyle(filterInactive);
            filterOnline.setStyle(filterInactive);
            refreshList.run();
        });
        filterAI.setOnAction(e -> {
            activeFilter[0] = "AI";
            filterAll.setStyle(filterInactive);
            filterAI.setStyle(filterActive);
            filterOnline.setStyle(filterInactive);
            refreshList.run();
        });
        filterOnline.setOnAction(e -> {
            activeFilter[0] = "ONLINE";
            filterAll.setStyle(filterInactive);
            filterAI.setStyle(filterInactive);
            filterOnline.setStyle(filterActive);
            refreshList.run();
        });

        // Initial load
        refreshList.run();

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 28;" +
                        "-fx-background-color: #2d7a6e;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeBtn.getStyle().replace("#2d7a6e", "#3a9e8f")));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(closeBtn.getStyle().replace("#3a9e8f", "#2d7a6e")));
        closeBtn.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), overlayRoot);
            ft.setToValue(0.0);
            ft.setOnFinished(ev -> rootPane.getChildren().remove(overlayRoot));
            ft.play();
        });

        container.getChildren().addAll(title, summary, filterRow, divider, scroll, closeBtn);
        overlayRoot.getChildren().add(container);

        // Click outside card to close
        overlayRoot.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayRoot) {
                FadeTransition ft = new FadeTransition(Duration.millis(180), overlayRoot);
                ft.setToValue(0.0);
                ft.setOnFinished(ev -> rootPane.getChildren().remove(overlayRoot));
                ft.play();
            }
        });
        container.setOnMouseClicked(e -> e.consume());

        // Fade in
        overlayRoot.setOpacity(0.0);
        rootPane.getChildren().add(overlayRoot);
        MusicManager.getInstance().attachClickSounds(overlayRoot);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), overlayRoot);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /** Build a single battle-record row card with expandable battle log. */
    private HBox buildRecordCard(BattleRecord record, DateTimeFormatter fmt) {
        boolean won = "WIN".equals(record.getResult());

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));

        String baseBorderColor = won ? "rgba(120,200,80,0.3)" : "rgba(240,128,48,0.25)";
        String hoverBorderColor = won ? "rgba(120,200,80,0.55)" : "rgba(240,128,48,0.5)";
        String baseStyle = "-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 8; " +
                "-fx-border-color: " + baseBorderColor
                + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: rgba(255,255,255,0.11); -fx-background-radius: 8; " +
                "-fx-border-color: " + hoverBorderColor
                + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        // Result badge
        Label badge = new Label(record.getResult());
        badge.setMinWidth(46);
        badge.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-alignment: CENTER;" +
                        "-fx-padding: 4 8;" +
                        "-fx-background-radius: 6;" +
                        "-fx-background-color: " + (won ? "rgba(120,200,80,0.22)" : "rgba(240,128,48,0.22)") + ";" +
                        "-fx-text-fill: " + (won ? "#78C850" : "#F08030") + ";");

        // Opponent type icon (AI vs ONLINE)
        String opponentType = record.getOpponentType() != null ? record.getOpponentType() : "AI";
        String typeIcon = "ONLINE".equalsIgnoreCase(opponentType) ? "🌐" : "🤖";

        // Info column
        VBox info = new VBox(3);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        String opponentName = record.getOpponentName() != null ? record.getOpponentName() : "Unknown";
        Label opponent = new Label(typeIcon + " vs " + opponentName + "  [" + opponentType + "]");
        opponent.setStyle("-fx-font-size: 13px; -fx-text-fill: #e0f0ec; -fx-font-weight: bold;");

        // Pokemon list — cap at a reasonable display length
        String pokemonStr = record.getPokemonUsed() != null
                ? String.join(", ", record.getPokemonUsed())
                : "—";
        if (pokemonStr.length() > 60)
            pokemonStr = pokemonStr.substring(0, 57) + "…";
        Label pokemonLbl = new Label("🎴 " + pokemonStr);
        pokemonLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #90aea6;");
        pokemonLbl.setWrapText(false);

        Label timeLbl = new Label(record.getTimestamp() != null
                ? "🕐 " + record.getTimestamp().format(fmt)
                : "");
        timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #5d8a80;");

        info.getChildren().addAll(opponent, pokemonLbl, timeLbl);

        // Battle log expandable section
        boolean hasLog = record.getBattleLog() != null && !record.getBattleLog().isBlank();
        if (hasLog) {
            VBox logBox = new VBox(3);
            logBox.setPadding(new Insets(6, 0, 0, 0));
            logBox.setVisible(false);
            logBox.setManaged(false);

            Label logHeader = new Label("📜 Battle Log");
            logHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #78C850;");

            VBox logEntries = new VBox(1);
            logEntries.setPadding(new Insets(2, 0, 0, 8));
            for (String line : record.getBattleLog().split("\n")) {
                if (line.isBlank())
                    continue;
                Label entry = new Label("• " + line);
                entry.setStyle("-fx-font-size: 10px; -fx-text-fill: #8ab0a6;");
                entry.setWrapText(true);
                logEntries.getChildren().add(entry);
            }

            ScrollPane logScroll = new ScrollPane(logEntries);
            logScroll.setFitToWidth(true);
            logScroll.setMaxHeight(120);
            logScroll.setPrefHeight(100);
            logScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; " +
                    "-fx-border-color: transparent;");
            logScroll.skinProperty().addListener((obs, o, n) -> {
                if (logScroll.lookup(".viewport") != null)
                    logScroll.lookup(".viewport").setStyle("-fx-background-color: transparent;");
            });

            logBox.getChildren().addAll(logHeader, logScroll);
            info.getChildren().add(logBox);

            Label expandHint = new Label("▸ Click to show log");
            expandHint.setStyle("-fx-font-size: 9px; -fx-text-fill: #5d8a80; -fx-font-style: italic;");
            info.getChildren().add(expandHint);

            card.setOnMouseClicked(e -> {
                boolean showing = logBox.isVisible();
                logBox.setVisible(!showing);
                logBox.setManaged(!showing);
                expandHint.setText(showing ? "▸ Click to show log" : "▾ Click to hide log");
            });
        }

        card.getChildren().addAll(badge, info);

        return card;
    }
    // View Selected Pokémon Overlay

    @FXML
    void onViewSelectedPokemonClick() {
        if (playerTeam.isEmpty())
            return;
        showViewSelectedPokemonOverlay();
    }

    /**
     * Show the "View Selected Pokémon" button after team selection.
     */
    private void showViewPokemonButton() {
        if (viewSelectedPokemonButton != null && !playerTeam.isEmpty()) {
            viewSelectedPokemonButton.setVisible(true);
            viewSelectedPokemonButton.setManaged(true);

            // Fade in
            viewSelectedPokemonButton.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(250), viewSelectedPokemonButton);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    private void hideViewPokemonButton() {
        if (viewSelectedPokemonButton != null) {
            viewSelectedPokemonButton.setVisible(false);
            viewSelectedPokemonButton.setManaged(false);
        }
    }

    private void showViewSelectedPokemonOverlay() {
        StackPane overlayRoot = new StackPane();
        overlayRoot.getStyleClass().add("view-pokemon-overlay-root");
        overlayRoot.setAlignment(Pos.CENTER);

        VBox container = new VBox(15);
        container.getStyleClass().add("view-pokemon-container");
        container.setAlignment(Pos.TOP_CENTER);
        container.setMaxWidth(720);
        container.setMaxHeight(520);
        container.setPadding(new Insets(20, 25, 20, 25));

        Label title = new Label("Your Selected Team");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #155c56; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 1);");

        // Pokemon cards in a grid
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10));

        int col = 0;
        int row = 0;
        for (PokemonInstance pokemon : playerTeam) {
            VBox card = createPokemonPreviewCard(pokemon);
            grid.add(card, col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(380);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("button-gray");
        closeBtn.setPrefWidth(120);
        closeBtn.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(200), overlayRoot);
            ft.setToValue(0.0);
            ft.setOnFinished(ev -> rootPane.getChildren().remove(overlayRoot));
            ft.play();
        });

        container.getChildren().addAll(title, scroll, closeBtn);
        overlayRoot.getChildren().add(container);

        overlayRoot.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayRoot) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), overlayRoot);
                ft.setToValue(0.0);
                ft.setOnFinished(ev -> rootPane.getChildren().remove(overlayRoot));
                ft.play();
            }
        });
        container.setOnMouseClicked(e -> e.consume());

        // Animate in with scale + fade
        overlayRoot.setOpacity(0);
        overlayRoot.setScaleX(0.92);
        overlayRoot.setScaleY(0.92);
        rootPane.getChildren().add(overlayRoot);
        MusicManager.getInstance().attachClickSounds(overlayRoot);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), overlayRoot);
        fadeIn.setToValue(1.0);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(250), overlayRoot);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        fadeIn.play();
        scaleIn.play();
    }

    /**
     * Create a styled card for the View Selected Pokemon overlay.
     */
    private VBox createPokemonPreviewCard(PokemonInstance pokemon) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("pokemon-preview-card");
        card.setPrefWidth(200);
        card.setPrefHeight(220);

        // Sprite
        ImageView sprite = new ImageView();
        sprite.setFitWidth(80);
        sprite.setFitHeight(80);
        sprite.setPreserveRatio(true);
        String spritePath = "/com/example/pokemonbattle/sprites/front/" + pokemon.getId() + ".png";
        try {
            var url = getClass().getResource(spritePath);
            if (url != null)
                sprite.setImage(new Image(url.toExternalForm(), 80, 80, true, true));
        } catch (Exception ignored) {
        }

        // Name
        Label name = new Label(capitalize(pokemon.getName()));
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #155c56;");

        // Level
        Label level = new Label("Lv." + pokemon.getLevel());
        level.setStyle("-fx-font-size: 12px; -fx-text-fill: #f8d030; -fx-font-weight: bold;");

        // HP
        Label hp = new Label("HP: " + pokemon.getMaxHp());
        hp.setStyle("-fx-font-size: 12px; -fx-text-fill: #78C850;");

        // Types
        HBox types = new HBox(4);
        types.setAlignment(Pos.CENTER);
        if (pokemon.getTypes() != null) {
            for (String type : pokemon.getTypes()) {
                Label typeLbl = new Label(type.substring(0, Math.min(3, type.length())).toUpperCase());
                typeLbl.setStyle("-fx-font-size: 9px; -fx-padding: 2 5; -fx-background-color: " +
                        getTypeColor(type)
                        + "; -fx-text-fill: white; -fx-background-radius: 3; -fx-font-weight: bold;");
                types.getChildren().add(typeLbl);
            }
        }

        card.getChildren().addAll(sprite, name, level, hp, types);

        // Hover scale
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.04);
            card.setScaleY(1.04);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }
}