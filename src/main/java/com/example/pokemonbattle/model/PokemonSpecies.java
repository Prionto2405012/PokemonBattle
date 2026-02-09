package com.example.pokemonbattle.model;

import java.util.ArrayList;
import java.util.List;

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
