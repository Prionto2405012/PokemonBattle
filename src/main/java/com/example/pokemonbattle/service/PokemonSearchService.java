package com.example.pokemonbattle.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.pokemonbattle.model.PokemonSpecies;

/**
 * Professional search service for Pokemon selection with ranked results.
 * Supports prefix matching, substring matching, and fuzzy matching (Levenshtein distance).
 */
public class PokemonSearchService {

    /** Maximum Levenshtein distance to consider a fuzzy match. */
    private static final int MAX_EDIT_DISTANCE = 3;

    /** Search result with a relevance score (lower = better match). */
    public record SearchResult(PokemonSpecies species, int score) {}

    /**
     * Search for Pokemon by query string. Returns ranked results:
     *   1. Exact match (score 0)
     *   2. Prefix match (score 1)
     *   3. Substring match (score 2)
     *   4. Fuzzy match by Levenshtein distance (score 10 + distance)
     *
     * Results are sorted by score, then alphabetically.
     */
    public List<PokemonSpecies> search(List<PokemonSpecies> allPokemon, String query) {
        if (query == null || query.isBlank()) return allPokemon;

        String q = query.trim().toLowerCase();
        List<SearchResult> results = new ArrayList<>();

        for (PokemonSpecies species : allPokemon) {
            String name = species.getName().toLowerCase();
            int score = scoreMatch(name, q);
            if (score >= 0) {
                results.add(new SearchResult(species, score));
            }
        }

        results.sort(Comparator.comparingInt(SearchResult::score)
                .thenComparing(r -> r.species().getName().toLowerCase()));

        return results.stream().map(SearchResult::species).toList();
    }

    /**
     * Score a match between a pokemon name and query.
     * Returns -1 if no match, 0 for exact, 1 for prefix, 2 for substring,
     * 10+distance for fuzzy.
     */
    private int scoreMatch(String name, String query) {
        // Exact match
        if (name.equals(query)) return 0;

        // Prefix match
        if (name.startsWith(query)) return 1;

        // Substring match
        if (name.contains(query)) return 2;

        // Fuzzy match — compute Levenshtein distance
        // For long names, compare against each word and take the best distance
        int bestDistance = Integer.MAX_VALUE;

        // Check full name
        int dist = levenshteinDistance(name, query);
        bestDistance = Math.min(bestDistance, dist);

        // Also check if a prefix of the name matches closely
        if (name.length() > query.length()) {
            String namePrefix = name.substring(0, Math.min(name.length(), query.length() + 2));
            int prefixDist = levenshteinDistance(namePrefix, query);
            bestDistance = Math.min(bestDistance, prefixDist);
        }

        if (bestDistance <= MAX_EDIT_DISTANCE) {
            return 10 + bestDistance;
        }

        return -1; // No match
    }

    /**
     * Compute the Levenshtein (edit) distance between two strings.
     * Uses the standard dynamic-programming approach with O(min(m,n)) space.
     */
    static int levenshteinDistance(String a, String b) {
        if (a.equals(b)) return 0;
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();

        // Ensure a is the shorter string for space optimization
        if (a.length() > b.length()) {
            String tmp = a; a = b; b = tmp;
        }

        int m = a.length();
        int n = b.length();
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        for (int i = 0; i <= m; i++) prev[i] = i;

        for (int j = 1; j <= n; j++) {
            curr[0] = j;
            for (int i = 1; i <= m; i++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[i] = Math.min(
                    Math.min(curr[i - 1] + 1, prev[i] + 1),
                    prev[i - 1] + cost
                );
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }

        return prev[m];
    }
}
