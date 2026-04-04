package com.example.pokemonbattle.server;

/**
 * Server -> Client signal that a player's active pokemon fainted and they must
 * choose a replacement before the next turn can begin.
 */
public class ForceSwitchMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private final Integer battleId;
    private final String faintedPokemonName;

    public ForceSwitchMessage(Integer battleId, String faintedPokemonName) {
        super("FORCE_SWITCH");
        this.battleId = battleId;
        this.faintedPokemonName = faintedPokemonName;
    }

    public Integer getBattleId() {
        return battleId;
    }

    public String getFaintedPokemonName() {
        return faintedPokemonName;
    }
}
