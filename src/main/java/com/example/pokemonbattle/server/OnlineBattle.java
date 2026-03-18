package com.example.pokemonbattle.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.pokemonbattle.database.GameDataDAO;
import com.example.pokemonbattle.model.Battle;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;

/**
 * Server-side battle management.
 * Manages a single online battle between two players.
 * Coordinates move exchanges and damage calculations.
 */
public class OnlineBattle {
    private static int battleIdCounter = 1000;
    private static final double GLOBAL_DAMAGE_MULTIPLIER = 2.0;
    
    private final Integer battleId;
    private final Integer player1Id;
    private final String player1Name;
    private final ClientHandler player1Handler;
    
    private final Integer player2Id;
    private final String player2Name;
    private final ClientHandler player2Handler;
    
    private final Battle battleEngine;
    private final GameDataDAO gameDataDAO;
    
    // Track actions from both players for the current turn
    private Map<Integer, ActionMessage> pendingActions = new HashMap<>();
    
    private boolean battleActive = true;
    private int turnCount = 0;

    private final String player1AvatarPath;
    private final String player2AvatarPath;
    
    public OnlineBattle(Integer player1Id, String player1Name, ClientHandler player1Handler,
                       Integer player2Id, String player2Name, ClientHandler player2Handler,
                       Player battlePlayer1, Player battlePlayer2,
                       String player1AvatarPath, String player2AvatarPath) {
        synchronized (OnlineBattle.class) {
            this.battleId = battleIdCounter++;
        }
        
        this.player1Id = player1Id;
        this.player1Name = player1Name;
        this.player1Handler = player1Handler;
        
        this.player2Id = player2Id;
        this.player2Name = player2Name;
        this.player2Handler = player2Handler;

        this.player1AvatarPath = player1AvatarPath;
        this.player2AvatarPath = player2AvatarPath;
        
        this.battleEngine = new Battle(battlePlayer1, battlePlayer2);
        this.gameDataDAO = new GameDataDAO();
        this.gameDataDAO.ensureDataLoaded();
        
        System.out.println("[Battle #" + battleId + "] Created battle: " + player1Name + " vs " + player2Name);
    }
    
    /**
     * Start the battle and send initial data to both clients.
     */
    public void startBattle() throws IOException {
        System.out.println("[Battle #" + battleId + "] Starting battle...");
        
        // Get pokemon data for player 2 (to send to player 1)
        Player player2 = battleEngine.getPlayer2();
        List<PokemonInstance> player2Team = player2.getTeam();
        
        Integer[] p2PokemonIds    = new Integer[player2Team.size()];
        Integer[] p2PokemonLevels = new Integer[player2Team.size()];
        String[]  p2PokemonNames  = new String[player2Team.size()];
        Integer[] p2MoveIds       = new Integer[player2Team.size() * 4];
        
        for (int i = 0; i < player2Team.size(); i++) {
            PokemonInstance pok = player2Team.get(i);
            p2PokemonIds[i]    = pok.getId();
            p2PokemonLevels[i] = pok.getLevel();
            p2PokemonNames[i]  = pok.getName();
            var moves = pok.getBattleMoves();
            for (int j = 0; j < 4; j++) {
                p2MoveIds[i * 4 + j] = (j < moves.size()) ? moves.get(j).getMove().getId() : null;
            }
        }
        
        // Send to player 1: opponent's pokemon + avatar
        BattleStartMessage msg1 = new BattleStartMessage(
            battleId, player2Name, player2Id,
            p2PokemonIds, p2PokemonLevels, p2PokemonNames, p2MoveIds,
            player2AvatarPath
        );
        player1Handler.sendMessage(msg1);
        
        // Get pokemon data for player 1 (to send to player 2)
        Player player1 = battleEngine.getPlayer1();
        List<PokemonInstance> player1Team = player1.getTeam();
        
        Integer[] p1PokemonIds    = new Integer[player1Team.size()];
        Integer[] p1PokemonLevels = new Integer[player1Team.size()];
        String[]  p1PokemonNames  = new String[player1Team.size()];
        Integer[] p1MoveIds       = new Integer[player1Team.size() * 4];
        
        for (int i = 0; i < player1Team.size(); i++) {
            PokemonInstance pok = player1Team.get(i);
            p1PokemonIds[i]    = pok.getId();
            p1PokemonLevels[i] = pok.getLevel();
            p1PokemonNames[i]  = pok.getName();
            var moves = pok.getBattleMoves();
            for (int j = 0; j < 4; j++) {
                p1MoveIds[i * 4 + j] = (j < moves.size()) ? moves.get(j).getMove().getId() : null;
            }
        }
        
        BattleStartMessage msg2 = new BattleStartMessage(
            battleId, player1Name, player1Id,
            p1PokemonIds, p1PokemonLevels, p1PokemonNames, p1MoveIds,
            player1AvatarPath
        );
        player2Handler.sendMessage(msg2);
        
        // Link this battle to both handlers so they can send moves
        player1Handler.setBattle(this);
        player2Handler.setBattle(this);
        
        System.out.println("[Battle #" + battleId + "] Battle started! Waiting for moves...");
    }
    
    /**
     * Receive an action (ATTACK or SWITCH) from a player.
     * When both players submit actions, process the turn.
     */
    public synchronized void submitAction(Integer playerId, ActionMessage action) throws IOException {
        if (playerId.equals(player1Id)) {
            pendingActions.put(player1Id, action);
            System.out.println("[Battle #" + battleId + "] Player 1 (" + player1Name + ") submitted action: " + action.getActionType()
                    + (action.isAttack() ? " — " + action.getMoveName() : " — switch to index " + action.getSwitchPokemonIndex()));
        } else if (playerId.equals(player2Id)) {
            pendingActions.put(player2Id, action);
            System.out.println("[Battle #" + battleId + "] Player 2 (" + player2Name + ") submitted action: " + action.getActionType()
                    + (action.isAttack() ? " — " + action.getMoveName() : " — switch to index " + action.getSwitchPokemonIndex()));
        }

        // If both players have submitted actions, process the turn
        if (pendingActions.size() == 2) {
            processTurn();
        }
    }
    
    /**
     * Process a complete turn with both players' actions.
     * Order: switches always happen first, then attacks (ordered by speed).
     */
    private void processTurn() throws IOException {
        turnCount++;
        System.out.println("[Battle #" + battleId + "] Processing turn " + turnCount);
        
        ActionMessage action1 = pendingActions.get(player1Id);
        ActionMessage action2 = pendingActions.get(player2Id);
        
        if (action1 == null || action2 == null) return;
        
        Player player1 = battleEngine.getPlayer1();
        Player player2 = battleEngine.getPlayer2();
        
        try {
            // ── Phase 1: Process SWITCH actions first (switches always go first in Pokemon) ──
            if (action1.isSwitch()) {
                executeSwitch(action1, player1, player1Name);
            }
            if (action2.isSwitch()) {
                executeSwitch(action2, player2, player2Name);
            }
            
            // ── Phase 2: Process ATTACK actions (ordered by speed) ──
            List<AttackEntry> attacks = new ArrayList<>();
            if (action1.isAttack()) attacks.add(new AttackEntry(action1, player1, player2, player1Name, player2Name));
            if (action2.isAttack()) attacks.add(new AttackEntry(action2, player2, player1, player2Name, player1Name));
            
            // Sort by attacker speed (descending)
            attacks.sort((a, b) -> Integer.compare(
                    b.attacker.getCurrentPokemon().getSpeed(),
                    a.attacker.getCurrentPokemon().getSpeed()));
            
            for (AttackEntry atk : attacks) {
                if (!battleActive) break;
                executeMove(atk.action, atk.attacker, atk.defender, atk.attackerName, atk.defenderName);
            }
        } catch (Exception e) {
            System.err.println("[Battle #" + battleId + "] Error processing turn: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Check if battle is over
        if (battleActive && checkBattleStatus()) {
            endBattle();
        }
        
        pendingActions.clear();
        
        // Signal both clients that the turn is complete and they may submit next action
        if (battleActive) {
            TurnReadyMessage turnReady = new TurnReadyMessage(battleId, turnCount);
            player1Handler.sendMessage(turnReady);
            player2Handler.sendMessage(turnReady);
            System.out.println("[Battle #" + battleId + "] Turn " + turnCount + " complete. Waiting for next actions.");
        }
    }
    
    /** Helper record for sorting attacks by speed. */
    private static class AttackEntry {
        final ActionMessage action;
        final Player attacker;
        final Player defender;
        final String attackerName;
        final String defenderName;
        AttackEntry(ActionMessage action, Player attacker, Player defender, String attackerName, String defenderName) {
            this.action = action;
            this.attacker = attacker;
            this.defender = defender;
            this.attackerName = attackerName;
            this.defenderName = defenderName;
        }
    }
    
    /**
     * Execute a SWITCH action: change the player's active pokemon and notify both clients.
     */
    private void executeSwitch(ActionMessage action, Player switchingPlayer, String playerName) throws IOException {
        int teamIndex = action.getSwitchPokemonIndex();
        List<PokemonInstance> team = switchingPlayer.getTeam();
        
        if (teamIndex < 0 || teamIndex >= team.size()) {
            System.err.println("[Battle #" + battleId + "] Invalid switch index: " + teamIndex);
            return;
        }
        
        PokemonInstance newPokemon = team.get(teamIndex);
        if (newPokemon.isFainted()) {
            System.err.println("[Battle #" + battleId + "] Cannot switch to fainted pokemon: " + newPokemon.getName());
            return;
        }
        
        switchingPlayer.setCurrentPokemon(newPokemon);
        
        System.out.println("[Battle #" + battleId + "] " + playerName + " switched to " + newPokemon.getName());
        
        // Notify both clients about the switch
        SwitchNotifyMessage switchMsg = new SwitchNotifyMessage(
                battleId, playerName,
                newPokemon.getId(), newPokemon.getName(), newPokemon.getLevel(),
                newPokemon.getCurrentHp(), newPokemon.getMaxHp()
        );
        player1Handler.sendMessage(switchMsg);
        player2Handler.sendMessage(switchMsg);
    }
    
    /**
     * Execute a single ATTACK action and send damage results to both players.
     */
    private void executeMove(ActionMessage actionMsg, Player attacker, Player defender,
                            String attackerName, String defenderName) throws IOException {
        // Find the move in game data
        Move move = gameDataDAO.getMove(actionMsg.getMoveId());
        if (move == null) {
            System.err.println("[Battle #" + battleId + "] Move not found: " + actionMsg.getMoveId());
            return;
        }
        
        // Calculate damage (using simplified calculation for now)
        int damage = calculateDamage(attacker, defender, move);
        
        PokemonInstance targetPokemon = defender.getCurrentPokemon();
        targetPokemon.takeDamage(damage);
        
        float effectiveness = 1.0f;  // TODO: retrieve from battle engine's type effectiveness
        boolean targetFainted = targetPokemon.isFainted();
        
        // Create and send damage message to both players
        DamageMessage damageMsg = new DamageMessage(
            battleId,
            attackerName,
            defenderName,
            damage,
            targetPokemon.getCurrentHp(),
            targetPokemon.getMaxHp(),
            targetFainted,
            effectiveness,
            move.getName()
        );
        
        player1Handler.sendMessage(damageMsg);
        player2Handler.sendMessage(damageMsg);
        
        System.out.println("[Battle #" + battleId + "] " + attackerName + " used " + move.getName() + 
                          " on " + defenderName + " for " + damage + " damage!");
        
        // If pokemon fainted, handle pokemon switch
        if (targetFainted) {
            PokemonInstance nextPokemon = defender.getFirstAvailablePokemon();
            if (nextPokemon == null) {
                // All pokemon fainted - battle is over
                battleActive = false;
            } else {
                defender.setCurrentPokemon(nextPokemon);

                // Notify both clients about the forced switch so sprites/HP update
                SwitchNotifyMessage switchMsg = new SwitchNotifyMessage(
                    battleId, defenderName,
                    nextPokemon.getId(), nextPokemon.getName(), nextPokemon.getLevel(),
                    nextPokemon.getCurrentHp(), nextPokemon.getMaxHp()
                );
                player1Handler.sendMessage(switchMsg);
                player2Handler.sendMessage(switchMsg);

                BattleUpdateMessage updateMsg = new BattleUpdateMessage(
                    battleId,
                    defenderName + "'s " + targetPokemon.getName() + " fainted! Switching to " + nextPokemon.getName(),
                    turnCount,
                    "all_waiting"
                );
                player1Handler.sendMessage(updateMsg);
                player2Handler.sendMessage(updateMsg);
            }
        }
    }
    
    /**
     * Simple damage calculation (can be enhanced with game engine logic).
     */
    private int calculateDamage(Player attacker, Player defender, Move move) {
        if (move.getPower() == null || move.getPower() == 0) {
            return 0;  // Status move
        }
        
        PokemonInstance attackerPokemon = attacker.getCurrentPokemon();
        PokemonInstance defenderPokemon = defender.getCurrentPokemon();
        
        double baseDamage = 0.5 * attackerPokemon.getLevel() / 5.0 + 2;
        double attack = move.getDamage_class().equalsIgnoreCase("special") 
            ? attackerPokemon.getSpAttack() 
            : attackerPokemon.getAttack();
        double defense = move.getDamage_class().equalsIgnoreCase("special")
            ? defenderPokemon.getSpDefense()
            : defenderPokemon.getDefense();
        
        double damage = baseDamage * move.getPower() * (attack / defense) / 50.0 + 2;
        
        // Random variation (85-100%)
        double variance = 0.85 + (Math.random() * 0.15);
        damage = damage * variance;

        damage = damage * GLOBAL_DAMAGE_MULTIPLIER;
        
        // Accuracy check
        if (move.getAccuracy() != null && move.getAccuracy() < 100) {
            if (Math.random() * 100 > move.getAccuracy()) {
                damage = 0;  // Move missed
            }
        }
        
        return Math.max(1, (int)damage);
    }
    
    /**
     * Check if the battle should end.
     */
    private boolean checkBattleStatus() {
        Player player1 = battleEngine.getPlayer1();
        Player player2 = battleEngine.getPlayer2();
        
        // Check if either player has all pokemon fainted
        return player1.getFirstAvailablePokemon() == null || player2.getFirstAvailablePokemon() == null;
    }
    
    /**
     * End the battle and determine winner.
     */
    private void endBattle() throws IOException {
        battleActive = false;
        
        Player player1 = battleEngine.getPlayer1();
        Player player2 = battleEngine.getPlayer2();
        
        String winnerName;
        Integer winnerId;
        
        if (player1.getFirstAvailablePokemon() == null) {
            winnerName = player2Name;
            winnerId = player2Id;
        } else {
            winnerName = player1Name;
            winnerId = player1Id;
        }
        
        BattleEndMessage endMsg = new BattleEndMessage(
            battleId,
            winnerName,
            winnerId,
            "All opponent's pokemon fainted"
        );
        
        player1Handler.sendMessage(endMsg);
        player2Handler.sendMessage(endMsg);
        
        System.out.println("[Battle #" + battleId + "] Battle ended! Winner: " + winnerName);
    }
    
    /**
     * Handle a player forfeiting (running away or disconnecting).
     * The forfeiting player loses; the opponent wins.
     */
    public synchronized void forfeit(Integer forfeitingPlayerId) throws IOException {
        if (!battleActive) return;
        battleActive = false;

        String winnerName;
        Integer winnerId;
        String loserName;

        if (forfeitingPlayerId.equals(player1Id)) {
            winnerName = player2Name;
            winnerId   = player2Id;
            loserName  = player1Name;
        } else {
            winnerName = player1Name;
            winnerId   = player1Id;
            loserName  = player2Name;
        }

        BattleEndMessage endMsg = new BattleEndMessage(
            battleId, winnerName, winnerId, loserName + " forfeited the battle!"
        );

        // Try to notify both clients (one may already be disconnected)
        try { player1Handler.sendMessage(endMsg); } catch (IOException ignored) {}
        try { player2Handler.sendMessage(endMsg); } catch (IOException ignored) {}

        System.out.println("[Battle #" + battleId + "] " + loserName + " forfeited! Winner: " + winnerName);
    }

    // Getters
    public Integer getBattleId() { return battleId; }
    public boolean isBattleActive() { return battleActive; }
    public Integer getPlayer1Id() { return player1Id; }
    public Integer getPlayer2Id() { return player2Id; }
}
