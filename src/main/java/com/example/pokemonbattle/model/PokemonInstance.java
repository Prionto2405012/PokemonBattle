package com.example.pokemonbattle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Pokémon instance used in battle. Inherits species data and adds
 * instance-level state: level, current HP, computed battle stats and up to 4 moves with remaining PP.
 */
public class PokemonInstance extends PokemonSpecies {
    private final int level;

    // Battle stats
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private int spAttack;
    private int spDefense;
    private int speed;
    private boolean fainted = false;

    private final List<MoveSlot> moves = new ArrayList<>();

    // Static reference to all available Pokemon species (set during data loading)
    private static List<PokemonSpecies> allPokemonSpecies = new ArrayList<>();
    // Static reference to all available moves (set during data loading)
    private static Map<Integer, Move> allMoves = new HashMap<>();

    /**
     * Set the global list of all available Pokemon species.
     * Call this after loading Pokemon from JSON data.
     */
    public static void setAllPokemonSpecies(List<PokemonSpecies> pokemonList) {
        allPokemonSpecies = pokemonList;
    }

    /**
     * Set the global map of all available moves.
     * Call this after loading moves from JSON data.
     */
    public static void setAllMoves(Map<Integer, Move> movesMap) {
        allMoves = movesMap;
    }

    /**
     * Constructor by Pokemon ID (1-493)
     */
    public PokemonInstance(int pokemonId, int level) {
        super();
        PokemonSpecies species = findSpeciesById(pokemonId);
        if (species == null) {
            throw new IllegalArgumentException("Pokemon with ID " + pokemonId + " not found");
        }
        
        // Copy species data to this instance
        this.setId(species.getId());
        this.setName(species.getName());
        this.setTypes(new ArrayList<>(species.getTypes()));
        this.setStats(species.getStats());
        this.setMoves(new ArrayList<>(species.getMoves()));
        this.setSelectedMoves(new ArrayList<>(species.getSelectedMoves()));
        
        this.level = Math.max(1, level);
        calculateStatsFromSpecies();
        populateMovesFromSelectedMoves();
    }

    /**
     * Constructor by PokemonSpecies object
     */
    public PokemonInstance(PokemonSpecies species, int level) {
        // Copy species data to this instance
        super();
        this.setId(species.getId());
        this.setName(species.getName());
        this.setTypes(new ArrayList<>(species.getTypes()));
        this.setStats(species.getStats());
        this.setMoves(new ArrayList<>(species.getMoves()));
        this.setSelectedMoves(new ArrayList<>(species.getSelectedMoves()));
        
        this.level = Math.max(1, level);
        calculateStatsFromSpecies();
        populateMovesFromSelectedMoves();
    }

    /**
     * Find a Pokemon species by ID from the loaded species list
     */
    private static PokemonSpecies findSpeciesById(int id) {
        return allPokemonSpecies.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Populate the moves list from selectedMoves using the allMoves map
     */
    private void populateMovesFromSelectedMoves() {
        moves.clear();
        for (Integer moveId : this.getSelectedMoves()) {
            Move move = allMoves.get(moveId);
            if (move != null) {
                addMove(move);
            }
        }
    }

    /**
     * Factory helper - creates a `PokemonInstance` and attaches up to 4 moves.
     */
    public static PokemonInstance fromSpeciesWithMoves(PokemonSpecies species, int level, List<Move> chosenMoves) {
        PokemonInstance inst = new PokemonInstance(species, level);
        if (chosenMoves != null) {
            for (int i = 0; i < chosenMoves.size() && i < 4; i++) {
                inst.addMove(chosenMoves.get(i));
            }
        }
        return inst;
    }

    private void calculateStatsFromSpecies() {
        Stats s = this.getStats();
        // Standard Pokémon stat formulas (simplified, no IV/EVs)
        this.maxHp = Math.max(1, ((s.getHp() * 2 * level) / 100) + level + 10);
        this.attack = Math.max(1, ((s.getAttack() * 2 * level) / 100) + 5);
        this.defense = Math.max(1, ((s.getDefense() * 2 * level) / 100) + 5);
        this.spAttack = Math.max(1, ((s.getSpecial_attack() * 2 * level) / 100) + 5);
        this.spDefense = Math.max(1, ((s.getSpecial_defense() * 2 * level) / 100) + 5);
        this.speed = Math.max(1, ((s.getSpeed() * 2 * level) / 100) + 5);

        this.currentHp = this.maxHp;
    }

    public int getLevel() { return level; }

    public int getMaxHp() { return maxHp; }
    public int getCurrentHp() { return currentHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpAttack() { return spAttack; }
    public int getSpDefense() { return spDefense; }
    public int getSpeed() { return speed; }

    public List<MoveSlot> getBattleMoves() { return Collections.unmodifiableList(moves); }

    public void addMove(Move move) {
        if (moves.size() >= 4) return;
        if (move == null) return;
        moves.add(new MoveSlot(move));
    }

    public boolean isFainted() { return fainted; }

    public void setFainted(boolean fainted) { this.fainted = fainted; }

    public void takeDamage(int amt) {
        if (amt <= 0) return;
        currentHp = Math.max(0, currentHp - amt);
        if (currentHp <= 0) {
            fainted = true;
        }
    }

    public void heal(int amt) {
        if (amt <= 0) return;
        currentHp = Math.min(maxHp, currentHp + amt);
        if (currentHp > 0) {
            fainted = false;
        }
    }

    public void restoreAllPp() {
        moves.forEach(ms -> ms.currentPp = ms.move.getPp());
    }

    @Override
    public String toString() {
        return String.format("%s (Lv%d) %d/%dHP", this.getName(), level, currentHp, maxHp);
    }

    public static class MoveSlot {
        private final Move move;
        private int currentPp;

        public MoveSlot(Move move) {
            this.move = move;
            this.currentPp = move != null ? move.getPp() : 0;
        }

        public Move getMove() { return move; }
        public int getCurrentPp() { return currentPp; }

        /**
         * Consume 1 PP for this move. Returns true if there was PP to consume.
         */
        public boolean useOnePp() {
            if (currentPp <= 0) return false;
            currentPp -= 1;
            return true;
        }
    }
}
