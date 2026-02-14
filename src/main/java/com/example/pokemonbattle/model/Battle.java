package com.example.pokemonbattle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core battle logic engine for Pokemon battles.
 * Manages turn-based combat between two players.
 */
public class Battle {
    private final Player player1;
    private final Player player2;
    private boolean finished = false;
    private Player winner = null;
    private int turn = 0;
    private final Random random = new Random();
    private final List<BattleListener> listeners = new ArrayList<>();

    // Type effectiveness chart [attackingType][defendingType]
    private static final float[][] TYPE_EFFECTIVENESS = initTypeEffectiveness();

    // Pokemon types enum
    private enum PokemonType {
        NORMAL(0), FIRE(1), WATER(2), ELECTRIC(3), GRASS(4), ICE(5),
        FIGHTING(6), POISON(7), GROUND(8), FLYING(9), PSYCHIC(10),
        BUG(11), ROCK(12), GHOST(13), DRAGON(14), DARK(15), STEEL(16), FAIRY(17);

        private final int index;
        PokemonType(int index) {
            this.index = index;
        }

        public static PokemonType fromString(String type) {
            try {
                return PokemonType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return NORMAL; // Default type
            }
        }
    }

    /**
     * Initialize type effectiveness chart
     * Each row represents attacking type, each column represents defending type
     * Values: <1.0 = not very effective, 1.0 = neutral, >1.0 = super effective
     */
    private static float[][] initTypeEffectiveness() {
        float[][] chart = new float[18][18];
        
        // Initialize all as neutral (1.0)
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 18; j++) {
                chart[i][j] = 1.0f;
            }
        }

        // Normal type effectiveness
        chart[0][12] = 0.5f; // Normal vs Rock
        chart[0][13] = 0.0f; // Normal vs Ghost

        // Fire type
        chart[1][4] = 2.0f;  // Fire vs Grass
        chart[1][5] = 2.0f;  // Fire vs Ice
        chart[1][11] = 2.0f; // Fire vs Bug
        chart[1][12] = 0.5f; // Fire vs Rock
        chart[1][16] = 2.0f; // Fire vs Steel
        chart[1][2] = 0.5f;  // Fire vs Water
        chart[1][3] = 0.5f;  // Fire vs Electric
        chart[1][8] = 0.5f;  // Fire vs Ground

        // Water type
        chart[2][1] = 2.0f;  // Water vs Fire
        chart[2][8] = 2.0f;  // Water vs Ground
        chart[2][12] = 2.0f; // Water vs Rock
        chart[2][4] = 0.5f;  // Water vs Grass
        chart[2][3] = 0.5f;  // Water vs Electric

        // Electric type
        chart[3][2] = 2.0f;  // Electric vs Water
        chart[3][9] = 2.0f;  // Electric vs Flying
        chart[3][4] = 0.5f;  // Electric vs Grass

        // Grass type
        chart[4][2] = 2.0f;  // Grass vs Water
        chart[4][8] = 2.0f;  // Grass vs Ground
        chart[4][12] = 2.0f; // Grass vs Rock
        chart[4][1] = 0.5f;  // Grass vs Fire
        chart[4][7] = 0.5f;  // Grass vs Poison
        chart[4][9] = 0.5f;  // Grass vs Flying

        // Ice type
        chart[5][8] = 2.0f;  // Ice vs Ground
        chart[5][9] = 2.0f;  // Ice vs Flying
        chart[5][4] = 2.0f;  // Ice vs Grass
        chart[5][14] = 2.0f; // Ice vs Dragon
        chart[5][1] = 0.5f;  // Ice vs Fire
        chart[5][3] = 0.5f;  // Ice vs Electric
        chart[5][5] = 0.5f;  // Ice vs Ice

        // Fighting type
        chart[6][0] = 2.0f;  // Fighting vs Normal
        chart[6][12] = 2.0f; // Fighting vs Rock
        chart[6][15] = 2.0f; // Fighting vs Dark
        chart[6][9] = 0.5f;  // Fighting vs Flying
        chart[6][7] = 0.5f;  // Fighting vs Poison
        chart[6][13] = 0.0f; // Fighting vs Ghost

        // Poison type
        chart[7][4] = 2.0f;  // Poison vs Grass
        chart[7][17] = 2.0f; // Poison vs Fairy
        chart[7][12] = 0.5f; // Poison vs Rock

        // Ground type
        chart[8][1] = 2.0f;  // Ground vs Fire
        chart[8][3] = 2.0f;  // Ground vs Electric
        chart[8][7] = 2.0f;  // Ground vs Poison
        chart[8][12] = 2.0f; // Ground vs Rock
        chart[8][4] = 0.5f;  // Ground vs Grass
        chart[8][9] = 0.0f;  // Ground vs Flying

        // Flying type
        chart[9][6] = 2.0f;  // Flying vs Fighting
        chart[9][11] = 2.0f; // Flying vs Bug
        chart[9][4] = 2.0f;  // Flying vs Grass
        chart[9][12] = 0.5f; // Flying vs Rock

        // Psychic type
        chart[10][6] = 2.0f; // Psychic vs Fighting
        chart[10][7] = 2.0f; // Psychic vs Poison
        chart[10][15] = 0.5f; // Psychic vs Dark

        // Bug type
        chart[11][4] = 2.0f; // Bug vs Grass
        chart[11][7] = 0.5f; // Bug vs Poison
        chart[11][15] = 2.0f; // Bug vs Dark
        chart[11][1] = 0.5f; // Bug vs Fire
        chart[11][9] = 0.5f; // Bug vs Flying

        // Rock type
        chart[12][1] = 2.0f; // Rock vs Fire
        chart[12][5] = 2.0f; // Rock vs Ice
        chart[12][9] = 2.0f; // Rock vs Flying
        chart[12][11] = 2.0f; // Rock vs Bug
        chart[12][6] = 0.5f; // Rock vs Fighting
        chart[12][8] = 0.5f; // Rock vs Ground

        // Ghost type
        chart[13][13] = 2.0f; // Ghost vs Ghost
        chart[13][10] = 2.0f; // Ghost vs Psychic
        chart[13][0] = 0.0f; // Ghost vs Normal

        // Dragon type
        chart[14][14] = 2.0f; // Dragon vs Dragon

        // Dark type
        chart[15][10] = 2.0f; // Dark vs Psychic
        chart[15][13] = 2.0f; // Dark vs Ghost
        chart[15][6] = 0.5f; // Dark vs Fighting

        // Steel type
        chart[16][5] = 2.0f; // Steel vs Ice
        chart[16][12] = 2.0f; // Steel vs Rock
        chart[16][17] = 2.0f; // Steel vs Fairy
        chart[16][1] = 0.5f; // Steel vs Fire
        chart[16][3] = 0.5f; // Steel vs Electric
        chart[16][8] = 0.5f; // Steel vs Ground

        // Fairy type
        chart[17][6] = 2.0f; // Fairy vs Fighting
        chart[17][15] = 2.0f; // Fairy vs Dark
        chart[17][14] = 2.0f; // Fairy vs Dragon
        chart[17][7] = 0.5f; // Fairy vs Poison

        return chart;
    }

    /**
     * Constructor - initialize battle with two players
     */
    public Battle(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.turn = 0;
        this.finished = false;
        this.winner = null;
    }

    /**
     * Add a listener for battle events
     */
    public void addListener(BattleListener listener) {
        listeners.add(listener);
    }

    /**
     * Execute a single round of battle
     */
    public void executeRound(Move player1Move, Move player2Move) {
        if (finished) return;

        turn++;
        PokemonInstance p1Pokemon = player1.getCurrentPokemon();
        PokemonInstance p2Pokemon = player2.getCurrentPokemon();

        if (p1Pokemon == null || p2Pokemon == null) {
            checkBattleEnd();
            return;
        }

        // Determine who attacks first (based on speed)
        boolean player1First = p1Pokemon.getSpeed() >= p2Pokemon.getSpeed();

        if (player1First) {
            executeMove(p1Pokemon, player1Move, p2Pokemon);
            if (!p2Pokemon.isFainted()) {
                executeMove(p2Pokemon, player2Move, p1Pokemon);
            }
        } else {
            executeMove(p2Pokemon, player2Move, p1Pokemon);
            if (!p1Pokemon.isFainted()) {
                executeMove(p1Pokemon, player1Move, p2Pokemon);
            }
        }

        // Handle fainted Pokemon - auto switch if needed
        handleFaintedPokemon(player1);
        handleFaintedPokemon(player2);

        // Check if battle is finished
        checkBattleEnd();
    }

    /**
     * Execute a single move
     */
    private void executeMove(PokemonInstance attacker, Move move, PokemonInstance defender) {
        if (move == null || attacker.isFainted()) return;

        // Calculate damage
        int damage = calculateDamage(move, attacker, defender);

        // Apply damage
        defender.takeDamage(damage);

        // Notify listeners
        notifyDamageDealt(attacker.getName(), move.getName(), defender.getName(), damage);

        if (defender.isFainted()) {
            notifyPokemonFainted(defender.getName());
        }
    }

    /**
     * Calculate damage with type effectiveness
     */
    private int calculateDamage(Move move, PokemonInstance attacker, PokemonInstance defender) {
        if (move.getPower() == null) {
            return 0; // Status move, no damage
        }

        // Base damage formula (simplified)
        double damage = ((2.0 * attacker.getLevel() / 5.0 + 2.0) * move.getPower() * 
                        attacker.getAttack() / defender.getDefense()) / 50.0 + 2.0;

        // Apply type effectiveness
        float effectiveness = getTypeEffectiveness(move.getType(), defender.getTypes());
        damage *= effectiveness;

        // Add randomness (85-100% of calculated damage)
        double randomFactor = 0.85 + (random.nextDouble() * 0.15);
        damage *= randomFactor;

        return Math.max(1, (int) damage);
    }

    /**
     * Get type effectiveness multiplier
     */
    private float getTypeEffectiveness(String attackType, List<String> defendTypes) {
        PokemonType atkType = PokemonType.fromString(attackType);
        
        float effectiveness = 1.0f;
        for (String defType : defendTypes) {
            PokemonType defTypeEnum = PokemonType.fromString(defType);
            effectiveness *= TYPE_EFFECTIVENESS[atkType.index][defTypeEnum.index];
        }
        
        return effectiveness;
    }

    /**
     * Handle fainted Pokemon - auto switch to next available
     */
    private void handleFaintedPokemon(Player player) {
        PokemonInstance current = player.getCurrentPokemon();
        
        if (current != null && current.isFainted()) {
            // Auto switch to first available (non-fainted) Pokemon
            PokemonInstance next = player.getFirstAvailablePokemon();
            
            if (next != null) {
                player.setCurrentPokemon(next);
                notifyPokemonSwitched(player.getName(), next.getName());
            }
        }
    }

    /**
     * Check if battle has ended
     */
    private void checkBattleEnd() {
        // Check if player1 has no available Pokemon
        if (!player1.hasTeamRemaining()) {
            finished = true;
            winner = player2;
            notifyBattleEnd(winner.getName());
            return;
        }

        // Check if player2 has no available Pokemon
        if (!player2.hasTeamRemaining()) {
            finished = true;
            winner = player1;
            notifyBattleEnd(winner.getName());
        }
    }

    /**
     * Check if battle is finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Get the winner of the battle
     */
    public Player getWinner() {
        return winner;
    }

    /**
     * Get current turn number
     */
    public int getTurn() {
        return turn;
    }

    /**
     * Get player 1
     */
    public Player getPlayer1() {
        return player1;
    }

    /**
     * Get player 2
     */
    public Player getPlayer2() {
        return player2;
    }

    /**
     * Get a random move for AI opponent
     */
    public Move getAIMove(PokemonInstance pokemon) {
        List<PokemonInstance.MoveSlot> battleMoves = pokemon.getBattleMoves();
        if (battleMoves.isEmpty()) {
            return null;
        }
        return battleMoves.get(random.nextInt(battleMoves.size())).getMove();
    }

    // ==================== Listener Notifications ====================

    private void notifyDamageDealt(String attacker, String move, String defender, int damage) {
        for (BattleListener listener : listeners) {
            listener.onDamageDealt(attacker, move, defender, damage);
        }
    }

    private void notifyPokemonFainted(String pokemonName) {
        for (BattleListener listener : listeners) {
            listener.onPokemonFainted(pokemonName);
        }
    }

    private void notifyPokemonSwitched(String playerName, String pokemonName) {
        for (BattleListener listener : listeners) {
            listener.onPokemonSwitched(playerName, pokemonName);
        }
    }

    private void notifyBattleEnd(String winnerName) {
        for (BattleListener listener : listeners) {
            listener.onBattleEnd(winnerName);
        }
    }

    // ==================== Battle Listener Interface ====================

    /**
     * Interface for battles events
     */
    public interface BattleListener {
        void onDamageDealt(String attacker, String move, String defender, int damage);
        void onPokemonFainted(String pokemonName);
        void onPokemonSwitched(String playerName, String pokemonName);
        void onBattleEnd(String winnerName);
    }
}
