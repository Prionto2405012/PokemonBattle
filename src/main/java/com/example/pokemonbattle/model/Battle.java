package com.example.pokemonbattle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core battle logic engine for Pokemon battles.
 * Manages turn-based combat between two players.
 */
public class Battle {
    private static final double GLOBAL_DAMAGE_MULTIPLIER = 2.0;

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
                return NORMAL;
            }
        }
    }

    private static float[][] initTypeEffectiveness() {
        float[][] chart = new float[18][18];

        // Default = 1.0
        for (int i = 0; i < 18; i++)
            for (int j = 0; j < 18; j++)
                chart[i][j] = 1.0f;

        // NORMAL
        chart[0][12] = 0.5f;
        chart[0][13] = 0.0f;
        chart[0][16] = 0.5f;

        // FIRE
        chart[1][4] = 2.0f;
        chart[1][5] = 2.0f;
        chart[1][11] = 2.0f;
        chart[1][16] = 2.0f;
        chart[1][1] = 0.5f;
        chart[1][2] = 0.5f;
        chart[1][12] = 0.5f;
        chart[1][14] = 0.5f;

        // WATER
        chart[2][1] = 2.0f;
        chart[2][8] = 2.0f;
        chart[2][12] = 2.0f;
        chart[2][2] = 0.5f;
        chart[2][4] = 0.5f;
        chart[2][14] = 0.5f;

        // ELECTRIC
        chart[3][2] = 2.0f;
        chart[3][9] = 2.0f;
        chart[3][3] = 0.5f;
        chart[3][4] = 0.5f;
        chart[3][14] = 0.5f;
        chart[3][8] = 0.0f; // IMMUNITY

        // GRASS
        chart[4][2] = 2.0f;
        chart[4][8] = 2.0f;
        chart[4][12] = 2.0f;
        chart[4][1] = 0.5f;
        chart[4][4] = 0.5f;
        chart[4][7] = 0.5f;
        chart[4][9] = 0.5f;
        chart[4][11] = 0.5f;
        chart[4][14] = 0.5f;
        chart[4][16] = 0.5f;

        // ICE
        chart[5][4] = 2.0f;
        chart[5][8] = 2.0f;
        chart[5][9] = 2.0f;
        chart[5][14] = 2.0f;
        chart[5][1] = 0.5f;
        chart[5][2] = 0.5f;
        chart[5][5] = 0.5f;
        chart[5][16] = 0.5f;

        // FIGHTING
        chart[6][0] = 2.0f;
        chart[6][5] = 2.0f;
        chart[6][12] = 2.0f;
        chart[6][15] = 2.0f;
        chart[6][16] = 2.0f;
        chart[6][7] = 0.5f;
        chart[6][9] = 0.5f;
        chart[6][10] = 0.5f;
        chart[6][11] = 0.5f;
        chart[6][17] = 0.5f;
        chart[6][13] = 0.0f; // IMMUNITY

        // POISON
        chart[7][4] = 2.0f;
        chart[7][17] = 2.0f;
        chart[7][7] = 0.5f;
        chart[7][8] = 0.5f;
        chart[7][12] = 0.5f;
        chart[7][13] = 0.5f;
        chart[7][16] = 0.0f; // IMMUNITY

        // GROUND
        chart[8][1] = 2.0f;
        chart[8][3] = 2.0f;
        chart[8][7] = 2.0f;
        chart[8][12] = 2.0f;
        chart[8][16] = 2.0f;
        chart[8][4] = 0.5f;
        chart[8][11] = 0.5f;
        chart[8][9] = 0.0f; // IMMUNITY

        // FLYING
        chart[9][4] = 2.0f;
        chart[9][6] = 2.0f;
        chart[9][11] = 2.0f;
        chart[9][3] = 0.5f;
        chart[9][12] = 0.5f;
        chart[9][16] = 0.5f;

        // PSYCHIC
        chart[10][6] = 2.0f;
        chart[10][7] = 2.0f;
        chart[10][10] = 0.5f;
        chart[10][16] = 0.5f;
        chart[10][15] = 0.0f; // IMMUNITY

        // BUG
        chart[11][4] = 2.0f;
        chart[11][10] = 2.0f;
        chart[11][15] = 2.0f;
        chart[11][1] = 0.5f;
        chart[11][6] = 0.5f;
        chart[11][7] = 0.5f;
        chart[11][9] = 0.5f;
        chart[11][13] = 0.5f;
        chart[11][16] = 0.5f;
        chart[11][17] = 0.5f;

        // ROCK
        chart[12][1] = 2.0f;
        chart[12][5] = 2.0f;
        chart[12][9] = 2.0f;
        chart[12][11] = 2.0f;
        chart[12][6] = 0.5f;
        chart[12][8] = 0.5f;
        chart[12][16] = 0.5f;

        // GHOST
        chart[13][10] = 2.0f;
        chart[13][13] = 2.0f;
        chart[13][15] = 0.5f;
        chart[13][0] = 0.0f; // IMMUNITY

        // DRAGON
        chart[14][14] = 2.0f;
        chart[14][16] = 0.5f;
        chart[14][17] = 0.0f; // IMMUNITY

        // DARK
        chart[15][10] = 2.0f;
        chart[15][13] = 2.0f;
        chart[15][6] = 0.5f;
        chart[15][15] = 0.5f;
        chart[15][17] = 0.5f;

        // STEEL
        chart[16][5] = 2.0f;
        chart[16][12] = 2.0f;
        chart[16][17] = 2.0f;
        chart[16][1] = 0.5f;
        chart[16][2] = 0.5f;
        chart[16][3] = 0.5f;
        chart[16][16] = 0.5f;

        // FAIRY
        chart[17][6] = 2.0f;
        chart[17][14] = 2.0f;
        chart[17][15] = 2.0f;
        chart[17][1] = 0.5f;
        chart[17][7] = 0.5f;
        chart[17][16] = 0.5f;

        return chart;
    }

    public Battle(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.turn = 0;
        this.finished = false;
        this.winner = null;
    }

    public void addListener(BattleListener listener) {
        listeners.add(listener);
    }

    public void executeRound(Move player1Move, Move player2Move) {
        if (finished)
            return;

        turn++;
        PokemonInstance p1Pokemon = player1.getCurrentPokemon();
        PokemonInstance p2Pokemon = player2.getCurrentPokemon();

        if (p1Pokemon == null || p2Pokemon == null) {
            checkBattleEnd();
            return;
        }

        boolean player1First;
        if (p1Pokemon.getSpeed() == p2Pokemon.getSpeed()) {
            player1First = random.nextBoolean();
        } else {
            player1First = p1Pokemon.getSpeed() > p2Pokemon.getSpeed();
        }

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

        // AI (player2) auto-switches; player1 switch is handled by the controller via
        // the overlay
        handleFaintedAI(player2);

        // Check if player1's pokemon fainted — notify controller to show switch overlay
        PokemonInstance current1 = player1.getCurrentPokemon();
        if (current1 != null && current1.isFainted()) {
            notifyPlayerPokemonFaintedNeedsSwitch(player1.getName());
        }

        checkBattleEnd();
    }

    private void executeMove(PokemonInstance attacker, Move move, PokemonInstance defender) {
        if (move == null || attacker.isFainted())
            return;

        // Handle lunar-blessing as a self-heal status move
        if ("lunar-blessing".equals(move.getName())) {
            int healAmount = Math.max(1, attacker.getMaxHp() / 2);
            int actualHeal = Math.min(healAmount, attacker.getMaxHp() - attacker.getCurrentHp());
            if (actualHeal > 0) {
                attacker.heal(actualHeal);
            }
            notifyHealApplied(attacker.getName(), move.getName(), actualHeal);
            return;
        }

        int damage = calculateDamage(move, attacker, defender);
        defender.takeDamage(damage);
        notifyDamageDealt(attacker.getName(), move.getName(), defender.getName(), damage);

        // Drain moves restore HP to the attacker
        if (isDrainMove(move.getName())) {
            int healAmount = Math.max(1, damage / 2);
            int actualHeal = Math.min(healAmount, attacker.getMaxHp() - attacker.getCurrentHp());
            if (actualHeal > 0) {
                attacker.heal(actualHeal);
            }
            notifyHealApplied(attacker.getName(), move.getName(), actualHeal);
        }

        if (defender.isFainted()) {
            notifyPokemonFainted(defender.getName());
        }
    }

    private boolean isDrainMove(String moveName) {
        return switch (moveName) {
            case "drain-punch", "giga-drain", "mega-drain", "absorb",
                    "leech-life", "draining-kiss", "dream-eater",
                    "horn-leech", "oblivion-wing", "parabolic-charge" ->
                true;
            default -> false;
        };
    }

    private int calculateDamage(Move move, PokemonInstance attacker, PokemonInstance defender) {
        if (move.getPower() == null || move.getPower() == 0
                || "status".equals(move.getDamage_class())) {
            return 0;
        }
        int atkStat = "special".equals(move.getDamage_class())
                ? attacker.getSpAttack()
                : attacker.getAttack();
        int defStat = "special".equals(move.getDamage_class())
                ? defender.getSpDefense()
                : defender.getDefense();
        double damage = ((2.0 * attacker.getLevel() / 5.0 + 2.0) * move.getPower()
                * atkStat / (double) defStat) / 50.0 + 2.0;
        float effectiveness = getTypeEffectivenessMultiplier(move.getType(), defender.getTypes());
        damage *= effectiveness;
        double stab = 1.0;
        if (attacker.getTypes() != null && move.getType() != null) {
            for (String type : attacker.getTypes()) {
                if (type.equalsIgnoreCase(move.getType())) {
                    stab = 1.5;
                    break;
                }
            }
        }
        damage *= stab;
        double randomFactor = 0.85 + (random.nextDouble() * 0.15);
        damage *= randomFactor;
        damage *= GLOBAL_DAMAGE_MULTIPLIER;
        return Math.max(1, (int) damage);
    }

    public static float getTypeEffectivenessMultiplier(String attackType, List<String> defendTypes) {
        PokemonType atkType = PokemonType.fromString(attackType);
        float effectiveness = 1.0f;
        for (String defType : defendTypes) {
            PokemonType defTypeEnum = PokemonType.fromString(defType);
            effectiveness *= TYPE_EFFECTIVENESS[atkType.index][defTypeEnum.index];
        }
        return effectiveness;
    }

    /**
     * Auto-switch for the AI player only.
     */
    private void handleFaintedAI(Player aiPlayer) {
        PokemonInstance current = aiPlayer.getCurrentPokemon();
        if (current != null && current.isFainted()) {
            PokemonInstance next = aiPlayer.getFirstAvailablePokemon();
            if (next != null) {
                aiPlayer.setCurrentPokemon(next);
                notifyPokemonSwitched(aiPlayer.getName(), next.getName());
            }
        }
    }

    /**
     * Called by the controller after the player manually picks a replacement.
     * Switches the player's active Pokemon and continues the battle check.
     */
    public void playerSwitchAfterFaint(PokemonInstance chosen) {
        player1.setCurrentPokemon(chosen);
        notifyPokemonSwitched(player1.getName(), chosen.getName());
        checkBattleEnd();
    }

    private void checkBattleEnd() {
        if (!player1.hasTeamRemaining()) {
            finished = true;
            winner = player2;
            notifyBattleEnd(winner.getName());
            return;
        }
        if (!player2.hasTeamRemaining()) {
            finished = true;
            winner = player1;
            notifyBattleEnd(winner.getName());
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public Player getWinner() {
        return winner;
    }

    public int getTurn() {
        return turn;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Move getAIMove(PokemonInstance aiPokemon, PokemonInstance target) {
        List<PokemonInstance.MoveSlot> battleMoves = aiPokemon.getBattleMoves();
        if (battleMoves.isEmpty())
            return null;

        List<PokemonInstance.MoveSlot> offensiveMoves = battleMoves.stream()
                .filter(ms -> ms.getMove() != null
                        && ms.getMove().getPower() != null
                        && ms.getMove().getPower() > 0
                        && !"status".equals(ms.getMove().getDamage_class())
                        && ms.getCurrentPp() > 0)
                .toList();

        if (offensiveMoves.isEmpty()) {
            List<PokemonInstance.MoveSlot> usable = battleMoves.stream()
                    .filter(ms -> ms.getCurrentPp() > 0).toList();
            if (usable.isEmpty())
                usable = battleMoves;
            return usable.get(random.nextInt(usable.size())).getMove();
        }

        if (random.nextDouble() < 0.2) {
            return offensiveMoves.get(random.nextInt(offensiveMoves.size())).getMove();
        }

        Move bestMove = null;
        double bestScore = -1;
        List<String> aiTypes = aiPokemon.getTypes();
        List<String> defenderTypes = target.getTypes();

        for (PokemonInstance.MoveSlot slot : offensiveMoves) {
            Move move = slot.getMove();
            double score = move.getPower();
            float effectiveness = getTypeEffectivenessMultiplier(move.getType(), defenderTypes);
            score *= effectiveness;
            if (aiTypes != null && move.getType() != null) {
                for (String t : aiTypes) {
                    if (t.equalsIgnoreCase(move.getType())) {
                        score *= 1.5;
                        break;
                    }
                }
            }
            if ("special".equals(move.getDamage_class()))
                score *= aiPokemon.getSpAttack() / 100.0;
            else
                score *= aiPokemon.getAttack() / 100.0;
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove != null ? bestMove : offensiveMoves.get(0).getMove();
    }

    // Listener Notifications

    private void notifyDamageDealt(String attacker, String move, String defender, int damage) {
        for (BattleListener l : listeners)
            l.onDamageDealt(attacker, move, defender, damage);
    }

    private void notifyPokemonFainted(String pokemonName) {
        for (BattleListener l : listeners)
            l.onPokemonFainted(pokemonName);
    }

    private void notifyPokemonSwitched(String playerName, String pokemonName) {
        for (BattleListener l : listeners)
            l.onPokemonSwitched(playerName, pokemonName);
    }

    private void notifyBattleEnd(String winnerName) {
        for (BattleListener l : listeners)
            l.onBattleEnd(winnerName);
    }

    private void notifyHealApplied(String pokemonName, String moveName, int healAmount) {
        for (BattleListener l : listeners)
            l.onHealApplied(pokemonName, moveName, healAmount);
    }

    private void notifyPlayerPokemonFaintedNeedsSwitch(String playerName) {
        for (BattleListener l : listeners)
            l.onPlayerPokemonFaintedNeedsSwitch(playerName);
    }

    // Battle Listener Interface

    public interface BattleListener {
        void onDamageDealt(String attacker, String move, String defender, int damage);
        void onPokemonFainted(String pokemonName);
        void onPokemonSwitched(String playerName, String pokemonName);
        void onBattleEnd(String winnerName);
        void onHealApplied(String pokemonName, String moveName, int healAmount);
        /**
         * Called when player1's active pokemon faints — controller must show switch overlay.
         */
        void onPlayerPokemonFaintedNeedsSwitch(String playerName);
    }
}