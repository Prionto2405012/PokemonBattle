package com.example.pokemonbattle.server;

public class ErrorMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private String errorCode;
    private String description;

    public ErrorMessage(String errorCode, String description) {
        super("ERROR");
        this.errorCode   = errorCode;
        this.description = description;
    }

    public String getErrorCode()   { return errorCode; }
    public String getDescription() { return description; }
}
