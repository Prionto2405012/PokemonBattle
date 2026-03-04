package com.example.pokemonbattle.server;

public class FindOpponentRequest extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String playerName;
    private Integer[] pokemonIds;
    private Integer[] pokemonLevels;
    private String[]  pokemonNames;
    private Integer[] moveIds; // flat: 4 moves per pokemon, index i*4+j

    public FindOpponentRequest(Integer userId, String playerName,
                               Integer[] pokemonIds, Integer[] pokemonLevels,
                               String[] pokemonNames, Integer[] moveIds) {
        super("FIND_OPPONENT_REQUEST");
        this.userId        = userId;
        this.playerName    = playerName;
        this.pokemonIds    = pokemonIds;
        this.pokemonLevels = pokemonLevels;
        this.pokemonNames  = pokemonNames;
        this.moveIds       = moveIds;
    }

    public Integer   getUserId()       { return userId; }
    public String    getPlayerName()   { return playerName; }
    public Integer[] getPokemonIds()   { return pokemonIds; }
    public Integer[] getPokemonLevels(){ return pokemonLevels; }
    public String[]  getPokemonNames() { return pokemonNames; }
    public Integer[] getMoveIds()      { return moveIds; }
}
