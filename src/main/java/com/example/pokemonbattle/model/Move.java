package com.example.pokemonbattle.model;

/**
 * Represents a move entry from the moves JSON.
 */
public class Move {
    private int id;
    private String name;
    private Integer power; // nullable for status moves
    private Integer accuracy; // nullable when not applicable
    private int pp;
    private String type;
    private String damage_class;

    public Move() {}

    public Move(int id, String name, Integer power, Integer accuracy, int pp, String type, String damage_class) {
        this.id = id;
        this.name = name;
        this.power = power;
        this.accuracy = accuracy;
        this.pp = pp;
        this.type = type;
        this.damage_class = damage_class;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPower() { return power; }
    public void setPower(Integer power) { this.power = power; }

    public Integer getAccuracy() { return accuracy; }
    public void setAccuracy(Integer accuracy) { this.accuracy = accuracy; }

    public int getPp() { return pp; }
    public void setPp(int pp) { this.pp = pp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDamage_class() { return damage_class; }
    public void setDamage_class(String damage_class) { this.damage_class = damage_class; }

    @Override
    public String toString() {
        return String.format("Move[id=%d,name=%s,power=%s,pp=%d,type=%s,dc=%s]",
                id, name, power == null ? "null" : power.toString(), pp, type, damage_class);
    }
}
