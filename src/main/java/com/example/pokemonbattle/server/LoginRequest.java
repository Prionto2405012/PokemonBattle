package com.example.pokemonbattle.server;

public class LoginRequest extends GameMessage {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        super("LOGIN_REQUEST");
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
