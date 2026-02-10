package com.example.pokemonbattle.model;

/**
 * Represents an item that can be used in battle or collected.
 * Maps to the battle_items.json structure.
 */
public class Item {
    private int id;
    private String name;
    private String effect;
    private String category;
    private int quantity;

    public Item() {}

    public Item(int id, String name, String effect, String category) {
        this.id = id;
        this.name = name;
        this.effect = effect;
        this.category = category;
        this.quantity = 1;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(0, quantity); }

    public void addQuantity(int amount) {
        this.quantity = Math.max(0, this.quantity + amount);
    }

    public boolean use() {
        if (quantity <= 0) return false;
        quantity--;
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s x%d", name, quantity);
    }
}
