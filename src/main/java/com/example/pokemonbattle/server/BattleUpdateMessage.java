package com.example.pokemonbattle.server;

public class BattleUpdateMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;
    private String  message;
    private Integer turn;
    private String  currentPlayerTurn;

    public BattleUpdateMessage(Integer battleId, String message, Integer turn, String currentPlayerTurn) {
        super("BATTLE_UPDATE");
        this.battleId          = battleId;
        this.message           = message;
        this.turn              = turn;
        this.currentPlayerTurn = currentPlayerTurn;
    }

    public Integer getBattleId()          { return battleId; }
    public String  getMessage()           { return message; }
    public Integer getTurn()              { return turn; }
    public String  getCurrentPlayerTurn() { return currentPlayerTurn; }
}
