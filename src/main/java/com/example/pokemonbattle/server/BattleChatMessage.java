package com.example.pokemonbattle.server;
public class BattleChatMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private final Integer battleId;
    private final String senderName;
    private final String messageText;

    public BattleChatMessage(Integer battleId, String senderName, String messageText) {
        super("BATTLE_CHAT");
        this.battleId = battleId;
        this.senderName = senderName;
        this.messageText = messageText;
    }

    public Integer getBattleId() {
        return battleId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessageText() {
        return messageText;
    }
}
