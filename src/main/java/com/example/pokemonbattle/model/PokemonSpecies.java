package com.example.pokemonbattle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a Pokémon species as defined in the JSON dataset.
 * Maps closely to the structure in `pokemon_gen4.json`.
 */
public class PokemonSpecies {
    private int id;
    private String name;
    private List<String> types = new ArrayList<>();
    private Stats stats = new Stats();
    private List<Integer> moves = new ArrayList<>();
    private List<Integer> selectedMoves = new ArrayList<>();

    public PokemonSpecies() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }

    public Stats getStats() { return stats; }
    public void setStats(Stats stats) { this.stats = stats; }

    public List<Integer> getMoves() { return moves; }
    public void setMoves(List<Integer> moves) { this.moves = moves; }

    public List<Integer> getSelectedMoves() { return selectedMoves; }
    public void setSelectedMoves(List<Integer> selectedMoves) { this.selectedMoves = selectedMoves; }

    /**
     * Randomly selects 4 moves from this Pokemon's available move list.
     * Uses the provided moves map to ensure moves exist in the database.
     * If Pokemon has fewer than 4 moves, all available moves are selected.
     * 
     * @param allMovesMap Map of all available moves (Move ID -> Move object)
     */
    public void selectRandomMoves(Map<Integer, Move> allMovesMap) {
        selectedMoves.clear();
        
        // Filter available moves that exist in the moves database
        List<Integer> availableMoveIds = new ArrayList<>();
        for (Integer moveId : moves) {
            if (allMovesMap.containsKey(moveId)) {
                availableMoveIds.add(moveId);
            }
        }
        
        // If no valid moves found, select up to 4 from the moves list anyway
        if (availableMoveIds.isEmpty()) {
            availableMoveIds = new ArrayList<>(moves);
        }
        
        // Shuffle and select up to 4 moves
        Collections.shuffle(availableMoveIds);
        int count = Math.min(4, availableMoveIds.size());
        selectedMoves.addAll(availableMoveIds.subList(0, count));
    }

    @Override
    public String toString() {
        return String.format("%s (id=%d, types=%s)", name, id, types);
    }

    public static class Stats {
        private int hp;
        private int attack;
        private int defense;
        private int special_attack;
        private int special_defense;
        private int speed;

        public Stats() {}

        public int getHp() { return hp; }
        public void setHp(int hp) { this.hp = hp; }

        public int getAttack() { return attack; }
        public void setAttack(int attack) { this.attack = attack; }

        public int getDefense() { return defense; }
        public void setDefense(int defense) { this.defense = defense; }

        public int getSpecial_attack() { return special_attack; }
        public void setSpecial_attack(int special_attack) { this.special_attack = special_attack; }

        public int getSpecial_defense() { return special_defense; }
        public void setSpecial_defense(int special_defense) { this.special_defense = special_defense; }

        public int getSpeed() { return speed; }
        public void setSpeed(int speed) { this.speed = speed; }
    }
}
