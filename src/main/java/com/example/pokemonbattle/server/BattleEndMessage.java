package com.example.pokemonbattle.server;

public class BattleEndMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;
    private String  winnerName;
    private Integer winnerId;
    private String  reason;

    public BattleEndMessage(Integer battleId, String winnerName, Integer winnerId, String reason) {
        super("BATTLE_END");
        this.battleId   = battleId;
        this.winnerName = winnerName;
        this.winnerId   = winnerId;
        this.reason     = reason;
    }

    public Integer getBattleId()   { return battleId; }
    public String  getWinnerName() { return winnerName; }
    public Integer getWinnerId()   { return winnerId; }
    public String  getReason()     { return reason; }
}
