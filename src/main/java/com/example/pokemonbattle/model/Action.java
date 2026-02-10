package com.example.pokemonbattle.model;

/**
 * Represents an action a player can take during a battle turn.
 * Can be: Attack (move), Use Item, or Switch Pokémon.
 */
public class Action {
    private ActionType type;
    private Object target;

    public Action(ActionType type, Object target) {
        this.type = type;
        this.target = target;
    }

    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public Object getTarget() { return target; }
    public void setTarget(Object target) { this.target = target; }

    /**
     * Get the move (for ATTACK actions).
     */
    public Move getMove() {
        if (type == ActionType.ATTACK && target instanceof Move) {
            return (Move) target;
        }
        return null;
    }

    /**
     * Get the item (for ITEM actions).
     */
    public Item getItem() {
        if (type == ActionType.ITEM && target instanceof Item) {
            return (Item) target;
        }
        return null;
    }

    /**
     * Get the Pokémon to switch to (for SWITCH actions).
     */
    public PokemonInstance getSwitchTarget() {
        if (type == ActionType.SWITCH && target instanceof PokemonInstance) {
            return (PokemonInstance) target;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Action[%s, target=%s]", type, target);
    }

    public enum ActionType {
        ATTACK,
        ITEM,
        SWITCH
    }
}
