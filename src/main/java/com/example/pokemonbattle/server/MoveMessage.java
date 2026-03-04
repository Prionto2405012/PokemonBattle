package com.example.pokemonbattle.server;

public class MoveMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;
    private Integer moveId;
    private String  moveName;
    private Integer turn;

    public MoveMessage(Integer battleId, Integer moveId, String moveName, Integer turn) {
        super("MOVE");
        this.battleId = battleId;
        this.moveId   = moveId;
        this.moveName = moveName;
        this.turn     = turn;
    }

    public Integer getBattleId() { return battleId; }
    public Integer getMoveId()   { return moveId; }
    public String  getMoveName() { return moveName; }
    public Integer getTurn()     { return turn; }
}
