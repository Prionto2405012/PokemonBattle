package com.example.pokemonbattle.server;

public class TurnReadyMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;
    private Integer turnNumber;

    public TurnReadyMessage(Integer battleId, Integer turnNumber) {
        super("TURN_READY");
        this.battleId   = battleId;
        this.turnNumber = turnNumber;
    }

    public Integer getBattleId()   { return battleId; }
    public Integer getTurnNumber() { return turnNumber; }
}
