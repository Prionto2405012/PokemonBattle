package com.example.pokemonbattle.server;

/**
 * Server → Client notification that a player switched their active pokemon.
 * Sent to BOTH clients so each side can update its display.
 */
public class SwitchNotifyMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;

    /** Name of the player who switched (matches Player.getName()). */
    private String  playerName;

    /** The new active pokemon's data. */
    private Integer newPokemonId;
    private String  newPokemonName;
    private Integer newPokemonLevel;
    private Integer newPokemonHp;
    private Integer newPokemonMaxHp;

    public SwitchNotifyMessage(Integer battleId, String playerName,
                               Integer newPokemonId, String newPokemonName,
                               Integer newPokemonLevel,
                               Integer newPokemonHp, Integer newPokemonMaxHp) {
        super("SWITCH_NOTIFY");
        this.battleId        = battleId;
        this.playerName      = playerName;
        this.newPokemonId    = newPokemonId;
        this.newPokemonName  = newPokemonName;
        this.newPokemonLevel = newPokemonLevel;
        this.newPokemonHp    = newPokemonHp;
        this.newPokemonMaxHp = newPokemonMaxHp;
    }

    public Integer getBattleId()        { return battleId; }
    public String  getPlayerName()      { return playerName; }
    public Integer getNewPokemonId()    { return newPokemonId; }
    public String  getNewPokemonName()  { return newPokemonName; }
    public Integer getNewPokemonLevel() { return newPokemonLevel; }
    public Integer getNewPokemonHp()    { return newPokemonHp; }
    public Integer getNewPokemonMaxHp() { return newPokemonMaxHp; }
}
