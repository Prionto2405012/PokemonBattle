package com.example.pokemonbattle.server;

import java.io.Serializable;

/**
 * Base message class for all client-server communications.
 * Uses Java serialization for transmission over TCP.
 */
public abstract class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String messageType;
    private long timestamp;
    
    protected GameMessage(String messageType) {
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] at %d", messageType, timestamp);
    }
}

// All concrete message classes are in their own public files in this package.
