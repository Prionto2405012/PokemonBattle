package com.example.pokemonbattle.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.PokemonSpecies;
import com.example.pokemonbattle.util.MusicManager;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the Pokemon Selection Overlay.
 * Displays a glassmorphism-styled overlay for custom Pokemon team selection.
 */
public class PokemonSelectionOverlayController {

    @FXML
    private StackPane overlayRoot;
    @FXML
    private ScrollPane pokemonScrollPane;
    @FXML
    private GridPane pokemonGrid;
    @FXML
    private Label selectionCountLabel;
    
    // Stats Panel Components
    @FXML
    private VBox statsPanel;
    @FXML
    private Label statsPokemonName;
    @FXML
    private VBox statsSpriteContainer;
    @FXML
    private HBox statsTypesBox;
    @FXML
    private VBox statsDetailsBox;
    @FXML
    private VBox statsMovesBox;
    
    @FXML
    private Button clearButton;
    @FXML
    private Button doneButton;

    // Data
    private List<PokemonSpecies> allPokemon;
    private Map<Integer, Move> allMoves;
    private List<PokemonInstance> selectedPokemon;
    private Consumer<List<PokemonInstance>> onDoneCallback;
    private static final int MAX_TEAM_SIZE = 6;
    private static final int POKEMON_LEVEL = 50;
    private PokemonSpecies currentlyViewedPokemon;
    private static final Map<Integer, Image> spriteCache = new HashMap<>();
    private final Map<Integer, VBox> cardMap = new HashMap<>();
    public void initializeData(List<PokemonSpecies> allPokemon, Map<Integer, Move> allMoves, 
                                List<PokemonInstance> existingTeam, Consumer<List<PokemonInstance>> onDoneCallback) {
        this.allPokemon = allPokemon;
        this.allMoves = allMoves;
        this.onDoneCallback = onDoneCallback;
        this.selectedPokemon = new ArrayList<>(existingTeam);
        
        // Display Pokemon grid
        displayPokemonGrid();
        
        // Update UI
        updateSelectionCount();
        
        // Show initial stats message
        if (!allPokemon.isEmpty()) {
            statsPokemonName.setText("Hover over a Pokémon\nto view details");
            statsPokemonName.setStyle("-fx-font-size: 16px; -fx-text-fill: #aaaaaa; -fx-font-style: italic; -fx-text-alignment: center;");
        }
        
        // Animate in
        animateIn();

        MusicManager.getInstance().attachClickSounds(overlayRoot);
    }

    /**
     * Display Pokemon grid with all available Pokemon.
     * Pre-caches all sprite images on a background thread for instant display.
     */
    private void displayPokemonGrid() {
        pokemonGrid.getChildren().clear();
        cardMap.clear();
        pokemonGrid.setHgap(12);
        pokemonGrid.setVgap(12);

        // Pre-cache all sprites in background (non-blocking)
        for (PokemonSpecies species : allPokemon) {
            getCachedSprite(species.getId());
        }

        int columns = 4;
        int row = 0;
        int col = 0;

        for (PokemonSpecies species : allPokemon) {
            VBox pokemonCard = createPokemonCard(species);
            cardMap.put(species.getId(), pokemonCard);
            pokemonGrid.add(pokemonCard, col, row);

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }

    /**
     * Get a cached sprite image, loading it in the background if not yet cached.
     */
    private Image getCachedSprite(int pokemonId) {
        return spriteCache.computeIfAbsent(pokemonId, id -> {
            String path = "/com/example/pokemonbattle/sprites/front/" + id + ".png";
            var url = getClass().getResource(path);
            if (url != null) {
                // backgroundLoading=true makes the image load on a background thread
                return new Image(url.toExternalForm(), 80, 80, true, true, true);
            }
            return null;
        });
    }

    /**
     * Create a Pokemon card for the grid
     */
    private VBox createPokemonCard(PokemonSpecies species) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefWidth(130);
        card.setPrefHeight(170);
        
        // Check if already selected
        boolean isSelected = selectedPokemon.stream()
                .anyMatch(p -> p.getId() == species.getId());
        
        // Card styling
        updateCardStyle(card, isSelected);

        // Pokemon sprite (from cache — instant)
        ImageView sprite = new ImageView();
        sprite.setFitWidth(80);
        sprite.setFitHeight(80);
        sprite.setPreserveRatio(true);
        
        Image cachedImage = getCachedSprite(species.getId());
        if (cachedImage != null) {
            sprite.setImage(cachedImage);
        } else {
            createPlaceholderSprite(sprite, species);
        }

        // Pokemon name
        Label nameLabel = new Label(capitalize(species.getName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #ffffff;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        // Types
        HBox typesBox = new HBox(4);
        typesBox.setAlignment(Pos.CENTER);
        for (String type : species.getTypes()) {
            Label typeLabel = new Label(type.substring(0, Math.min(3, type.length())).toUpperCase());
            typeLabel.setStyle("-fx-font-size: 9px; -fx-padding: 2 5; -fx-background-color: " + 
                    getTypeColor(type) + "; -fx-text-fill: white; -fx-background-radius: 3; -fx-font-weight: bold;");
            typesBox.getChildren().add(typeLabel);
        }

        // Select indicator
        Label selectIndicator = new Label(isSelected ? "✓ SELECTED" : "CLICK TO SELECT");
        selectIndicator.setStyle("-fx-font-size: 10px; -fx-text-fill: " + 
                (isSelected ? "#163c03" : "#05273b") + "; -fx-font-weight: bold;");

        card.getChildren().addAll(sprite, nameLabel, typesBox, selectIndicator);

        // Hover effect - show stats
        card.setOnMouseEntered(e -> {
            if (!isSelected) {
                card.setStyle(card.getStyle() + "-fx-scale-x: 1.05; -fx-scale-y: 1.05;");
            }
            showPokemonStats(species);
        });
        
        card.setOnMouseExited(e -> {
            if (!isSelected) {
                updateCardStyle(card, false);
            }
        });

        // Click to select/deselect — updates ONLY this card (fast)
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                togglePokemonSelection(species);
                boolean nowSelected = selectedPokemon.stream()
                        .anyMatch(p -> p.getId() == species.getId());
                updateCardStyle(card, nowSelected);
                if (card.getChildren().size() >= 4) {
                    Label indicator = (Label) card.getChildren().get(3);
                    indicator.setText(nowSelected ? "\u2713 SELECTED" : "CLICK TO SELECT");
                    indicator.setStyle("-fx-font-size: 10px; -fx-text-fill: " +
                            (nowSelected ? "#163c03" : "#05273b") + "; -fx-font-weight: bold;");
                }
            }
        });

        return card;
    }

    private void updateCardStyle(VBox card, boolean isSelected) {
        if (isSelected) {
            card.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(126, 189, 185, 0.92), rgba(106, 173, 140, 0.95)); " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: rgba(120, 200, 160, 0.5); " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 80, 70, 0.3), 8, 0, 0, 2); " +
                    "-fx-cursor: hand;");
        } else {
            card.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(175, 239, 235, 0.92), rgba(167, 231, 199, 0.95)); " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: rgba(120, 200, 160, 0.5); " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 80, 70, 0.3), 8, 0, 0, 2); " +
                    "-fx-cursor: hand;");
        }
    }

    private void togglePokemonSelection(PokemonSpecies species) {
        boolean alreadySelected = selectedPokemon.stream()
                .anyMatch(p -> p.getId() == species.getId());

        if (alreadySelected) {
            selectedPokemon.removeIf(p -> p.getId() == species.getId());
        } else {
            if (selectedPokemon.size() >= MAX_TEAM_SIZE) {
                // Show error feedback
                selectionCountLabel.setText("Team Full! (Max " + MAX_TEAM_SIZE + ")");
                selectionCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F08030;");
                return;
            }
            
            List<Move> moves = getRandomMovesForPokemon(species, 4);
            PokemonInstance pokemon = PokemonInstance.fromSpeciesWithMoves(species, POKEMON_LEVEL, moves);
            selectedPokemon.add(pokemon);
        }

        updateSelectionCount();
    }
    private void showPokemonStats(PokemonSpecies species) {
        currentlyViewedPokemon = species;
        
        // Update name
        statsPokemonName.setText(capitalize(species.getName()));
        statsPokemonName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        // Update sprite (from cache — instant)
        statsSpriteContainer.getChildren().clear();
        ImageView sprite = new ImageView();
        sprite.setFitWidth(90);
        sprite.setFitHeight(90);
        sprite.setPreserveRatio(true);
        
        Image cachedImage = getCachedSprite(species.getId());
        if (cachedImage != null) {
            sprite.setImage(cachedImage);
        } else {
            createPlaceholderSprite(sprite, species);
        }
        statsSpriteContainer.getChildren().add(sprite);

        // Update types
        statsTypesBox.getChildren().clear();
        for (String type : species.getTypes()) {
            Label typeLabel = new Label(type.toUpperCase());
            typeLabel.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-color: " + 
                    getTypeColor(type) + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-weight: bold;");
            statsTypesBox.getChildren().add(typeLabel);
        }

        // Update stats
        statsDetailsBox.getChildren().clear();
        Label statsTitle = new Label("Base Stats:");
        statsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f8d030;");
        statsDetailsBox.getChildren().add(statsTitle);
        
        PokemonSpecies.Stats stats = species.getStats();
        addStatRow("HP", stats.getHp(), "#FF5959");
        addStatRow("Attack", stats.getAttack(), "#F08030");
        addStatRow("Defense", stats.getDefense(), "#F8D030");
        addStatRow("Sp. Atk", stats.getSpecial_attack(), "#6890F0");
        addStatRow("Sp. Def", stats.getSpecial_defense(), "#78C850");
        addStatRow("Speed", stats.getSpeed(), "#F85888");

        // Update moves
        statsMovesBox.getChildren().clear();
        Label movesTitle = new Label("Available Moves:");
        movesTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6890F0;");
        statsMovesBox.getChildren().add(movesTitle);
        
        List<Move> moves = getRandomMovesForPokemon(species, 4);
        for (Move move : moves) {
            Label moveLabel = new Label("• " + move.getName() + " (" + move.getType() + ")");
            moveLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff;");
            statsMovesBox.getChildren().add(moveLabel);
        }
    }

    private void addStatRow(String statName, int value, String color) {
        HBox statRow = new HBox(8);
        statRow.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(statName + ":");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff; -fx-min-width: 70;");
        
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        statRow.getChildren().addAll(nameLabel, valueLabel);
        statsDetailsBox.getChildren().add(statRow);
    }

    private void updateSelectionCount() {
        selectionCountLabel.setText("Selected: " + selectedPokemon.size() + " / " + MAX_TEAM_SIZE);
        
        if (selectedPokemon.isEmpty()) {
            selectionCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F08030;");
        } else if (selectedPokemon.size() == MAX_TEAM_SIZE) {
            selectionCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8d030;");
        } else {
            selectionCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #78C850;");
        }
        
        // Enable/disable done button
        doneButton.setDisable(selectedPokemon.isEmpty());
    }

    /**
     * Get random moves for a Pokemon species
     */
    private List<Move> getRandomMovesForPokemon(PokemonSpecies species, int count) {
        List<Move> availableMoves = species.getMoves().stream()
                .map(allMoves::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (availableMoves.isEmpty()) {
            return new ArrayList<>();
        }

        List<Move> shuffled = new ArrayList<>(availableMoves);
        java.util.Collections.shuffle(shuffled);
        return shuffled.stream().limit(count).toList();
    }

    /**
     * Clear all selections — updates card styles in-place without rebuilding
     */
    @FXML
    private void onClearSelection() {
        // Collect IDs that were selected before clearing
        List<Integer> previouslySelectedIds = selectedPokemon.stream()
                .map(PokemonInstance::getId)
                .toList();
        selectedPokemon.clear();

        // Only update the cards that were selected (fast)
        for (int id : previouslySelectedIds) {
            VBox card = cardMap.get(id);
            if (card != null) {
                updateCardStyle(card, false);
                if (card.getChildren().size() >= 4) {
                    Label indicator = (Label) card.getChildren().get(3);
                    indicator.setText("CLICK TO SELECT");
                    indicator.setStyle("-fx-font-size: 10px; -fx-text-fill: #05273b; -fx-font-weight: bold;");
                }
            }
        }
        updateSelectionCount();
    }

    /**
     * Done button - return selections to main controller
     */
    @FXML
    private void onDone() {
        if (onDoneCallback != null) {
            animateOut(() -> {
                onDoneCallback.accept(new ArrayList<>(selectedPokemon));
            });
        }
    }

    /**
     * Background click - close overlay if clicking outside content
     */
    @FXML
    private void onBackgroundClick(javafx.scene.input.MouseEvent event) {
        if (event.getTarget() == overlayRoot) {
            onDone();
        }
    }

    /**
     * Animate overlay in
     */
    private void animateIn() {
        overlayRoot.setOpacity(0);
        overlayRoot.setScaleX(0.9);
        overlayRoot.setScaleY(0.9);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayRoot);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), overlayRoot);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1.0);
        scale.setToY(1.0);
        
        fade.play();
        scale.play();
    }

    /**
     * Animate overlay out
     */
    private void animateOut(Runnable onComplete) {
        FadeTransition fade = new FadeTransition(Duration.millis(250), overlayRoot);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), overlayRoot);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.95);
        scale.setToY(0.95);
        
        fade.setOnFinished(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        fade.play();
        scale.play();
    }

    /**
     * Create placeholder sprite
     */
    private void createPlaceholderSprite(ImageView sprite, PokemonSpecies species) {
        String primaryType = species.getTypes().isEmpty() ? "normal" : species.getTypes().get(0);
        String color = getTypeColor(primaryType);
        sprite.setStyle("-fx-background-color: " + color + "; " +
                "-fx-background-radius: 40; " +
                "-fx-border-color: rgba(255,255,255,0.3); " +
                "-fx-border-radius: 40; " +
                "-fx-border-width: 2;");
    }

    /**
     * Capitalize string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Get type color
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
