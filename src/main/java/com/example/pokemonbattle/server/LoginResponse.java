package com.example.pokemonbattle.server;

public class LoginResponse extends GameMessage {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Integer userId;
    private String playerName;

    public LoginResponse(boolean success, String message) {
        super("LOGIN_RESPONSE");
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, Integer userId, String playerName) {
        super("LOGIN_RESPONSE");
        this.success = success;
        this.userId = userId;
        this.playerName = playerName;
        this.message = "Login successful";
    }

    public boolean isSuccess()       { return success; }
    public String getMessage()       { return message; }
    public Integer getUserId()       { return userId; }
    public String getPlayerName()    { return playerName; }
}
