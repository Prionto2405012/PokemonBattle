package com.example.pokemonbattle.server;

/**
 * Message carrying a player's turn action — either ATTACK or SWITCH.
 * Replaces the old MoveMessage for online battles so the server can
 * process both move usage and pokemon switches in a unified turn system.
 */
public class ActionMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    /** "ATTACK" or "SWITCH" */
    private String  actionType;
    private Integer battleId;
    private Integer turn;

    // ── ATTACK fields ──
    private Integer moveId;
    private String  moveName;

    // ── SWITCH fields ──
    /** Index of the pokemon in the player's team to switch to. */
    private Integer switchPokemonIndex;

    // ────────────────── constructors ──────────────────

    /** Create an ATTACK action. */
    public static ActionMessage attack(Integer battleId, Integer moveId, String moveName, Integer turn) {
        ActionMessage msg = new ActionMessage(battleId, "ATTACK", turn);
        msg.moveId   = moveId;
        msg.moveName = moveName;
        return msg;
    }

    /** Create a SWITCH action. */
    public static ActionMessage switchPokemon(Integer battleId, Integer switchPokemonIndex, Integer turn) {
        ActionMessage msg = new ActionMessage(battleId, "SWITCH", turn);
        msg.switchPokemonIndex = switchPokemonIndex;
        return msg;
    }

    private ActionMessage(Integer battleId, String actionType, Integer turn) {
        super("ACTION");
        this.battleId   = battleId;
        this.actionType = actionType;
        this.turn       = turn;
    }

    // ────────────────── getters ──────────────────

    public String  getActionType()        { return actionType; }
    public Integer getBattleId()          { return battleId; }
    public Integer getTurn()              { return turn; }
    public Integer getMoveId()            { return moveId; }
    public String  getMoveName()          { return moveName; }
    public Integer getSwitchPokemonIndex(){ return switchPokemonIndex; }

    public boolean isAttack() { return "ATTACK".equals(actionType); }
    public boolean isSwitch() { return "SWITCH".equals(actionType); }
}
