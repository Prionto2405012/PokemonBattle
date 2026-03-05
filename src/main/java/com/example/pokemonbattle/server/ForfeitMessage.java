package com.example.pokemonbattle.server;

/**
 * Client → Server message indicating a player is forfeiting / running away.
 * The server will end the battle and declare the opponent as the winner.
 */
public class ForfeitMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;

    public ForfeitMessage(Integer battleId) {
        super("FORFEIT");
        this.battleId = battleId;
    }

    public Integer getBattleId() { return battleId; }
}
