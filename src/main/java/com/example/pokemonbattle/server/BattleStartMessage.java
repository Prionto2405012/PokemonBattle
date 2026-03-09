package com.example.pokemonbattle.server;

public class BattleStartMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;
    private String  opponentName;
    private Integer opponentUserId;
    private Integer[] opponentPokemonIds;
    private Integer[] opponentPokemonLevels;
    private String[]  opponentPokemonNames;
    private Integer[] opponentMoveIds; // flat: 4 per pokemon
    private String opponentAvatarPath; // opponent's avatar resource path

    public BattleStartMessage(Integer battleId, String opponentName, Integer opponentUserId,
                              Integer[] pokemonIds, Integer[] pokemonLevels, String[] pokemonNames,
                              Integer[] moveIds, String opponentAvatarPath) {
        super("BATTLE_START");
        this.battleId              = battleId;
        this.opponentName          = opponentName;
        this.opponentUserId        = opponentUserId;
        this.opponentPokemonIds    = pokemonIds;
        this.opponentPokemonLevels = pokemonLevels;
        this.opponentPokemonNames  = pokemonNames;
        this.opponentMoveIds       = moveIds;
        this.opponentAvatarPath    = opponentAvatarPath;
    }

    public Integer   getBattleId()              { return battleId; }
    public String    getOpponentName()          { return opponentName; }
    public Integer   getOpponentUserId()        { return opponentUserId; }
    public Integer[] getOpponentPokemonIds()    { return opponentPokemonIds; }
    public Integer[] getOpponentPokemonLevels() { return opponentPokemonLevels; }
    public String[]  getOpponentPokemonNames()  { return opponentPokemonNames; }
    public Integer[] getOpponentMoveIds()       { return opponentMoveIds; }
    public String    getOpponentAvatarPath()    { return opponentAvatarPath; }
}
