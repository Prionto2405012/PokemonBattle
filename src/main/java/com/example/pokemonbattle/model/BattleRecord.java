package com.example.pokemonbattle.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data model for a single battle history record.
 * Immutable after construction — use setters only for DB mapping.
 */
public class BattleRecord {

    private int id;
    private int userId;
    private String result;            // "WIN" or "LOSS"
    private List<String> pokemonUsed; // Names of Pokémon used
    private String opponentType;      // "AI", "ONLINE", or "LOCAL"
    private String opponentName;
    private String battleLog;         // Newline-separated log entries
    private LocalDateTime timestamp;

    /** Default constructor for DB mapping. */
    public BattleRecord() {}

    /** Convenience constructor for creating new records. */
    public BattleRecord(int userId, String result, List<String> pokemonUsed,
                        String opponentType, String opponentName) {
        this.userId = userId;
        this.result = result;
        this.pokemonUsed = pokemonUsed;
        this.opponentType = opponentType;
        this.opponentName = opponentName;
        this.timestamp = LocalDateTime.now();
    }

    // ── Getters / Setters ───────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public List<String> getPokemonUsed() { return pokemonUsed; }
    public void setPokemonUsed(List<String> pokemonUsed) { this.pokemonUsed = pokemonUsed; }

    public String getOpponentType() { return opponentType; }
    public void setOpponentType(String opponentType) { this.opponentType = opponentType; }

    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getBattleLog() { return battleLog; }
    public void setBattleLog(String battleLog) { this.battleLog = battleLog; }

    @Override
    public String toString() {
        return String.format("BattleRecord[%s vs %s (%s) — %s at %s]",
                result, opponentName, opponentType,
                pokemonUsed != null ? pokemonUsed.size() + " pokémon" : "0",
                timestamp);
    }
}
