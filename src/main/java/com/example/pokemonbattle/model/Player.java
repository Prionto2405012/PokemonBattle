package com.example.pokemonbattle.model;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public String toString() {
        return String.format("Player[%s, team=%d, current=%s]", 
                name, team.size(), currentPokemon != null ? currentPokemon.getSpecies().getName() : "none");
    }
}
