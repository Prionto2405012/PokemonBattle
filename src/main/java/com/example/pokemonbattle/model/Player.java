package com.example.pokemonbattle.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Represents a player in a battle.
 * Holds a team of up to 6 Pokémon, current active Pokémon, and items.
 */
public class Player {
    private String name;
    private final List<PokemonInstance> team = new ArrayList<>();
    private PokemonInstance currentPokemon;
    private final List<Item> items = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<PokemonInstance> getTeam() { return team; }

    public void addToTeam(PokemonInstance pokemon) {
        if (pokemon != null && team.size() < 6) {
            team.add(pokemon);
        }
    }

    public PokemonInstance getCurrentPokemon() { return currentPokemon; }

    public void setCurrentPokemon(PokemonInstance pokemon) {
        if (team.contains(pokemon)) {
            this.currentPokemon = pokemon;
        }
    }

    /**
     * Get the first non-fainted Pokémon from the team.
     * Returns null if all are fainted.
     */
    public PokemonInstance getFirstAvailablePokemon() {
        return team.stream()
                .filter(p -> !p.isFainted())
                .findFirst()
                .orElse(null);
    }

    public List<Item> getItems() { return items; }

    public void addItem(Item item) {
        if (item != null) {
            items.add(item);
        }
    }

    public void removeItem(Item item) {
        if (item != null) {
            items.remove(item);
        }
    }

    public boolean hasTeamRemaining() {
        return team.stream().anyMatch(p -> !p.isFainted());
    }

    /**
     * Generate a random team of 6 unique Pokemon (IDs 1-493).
     * Sets the first Pokemon as the current active Pokemon.
     */
    public void generateRandomTeam() {
        team.clear();
        currentPokemon = null;
        
        Random random = new Random();
        Set<Integer> usedIds = new HashSet<>();
        int teamSize = 0;
        int maxAttempts = 100; // Prevent infinite loop
        int attempts = 0;
        
        // Generate 6 unique random Pokemon
        while (teamSize < 6 && attempts < maxAttempts) {
            int randomId = random.nextInt(493) + 1; // IDs from 1 to 493
            
            if (!usedIds.contains(randomId)) {
                try {
                    PokemonInstance pokemon = new PokemonInstance(randomId, 50);
                    team.add(pokemon);
                    usedIds.add(randomId);
                    teamSize++;
                } catch (IllegalArgumentException e) {
                    // Pokemon ID not found, try again
                    attempts++;
                }
            }
            attempts++;
        }
        
        // Set the first Pokemon as current
        if (!team.isEmpty()) {
            currentPokemon = team.get(0);
        }
        
        System.out.println("Generated random team with " + team.size() + " Pokemon");
    }

    @Override
    public String toString() {
        return String.format("Player[%s, team=%d, current=%s]", 
                name, team.size(), currentPokemon != null ? currentPokemon.getName() : "none");
    }
}
