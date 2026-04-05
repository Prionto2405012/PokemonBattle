package com.example.pokemonbattle.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.pokemonbattle.model.PokemonSpecies;
public class PokemonSearchService {
    private static final int MAX_EDIT_DISTANCE = 3;
    public record SearchResult(PokemonSpecies species, int score) {}
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
    private int scoreMatch(String name, String query) {
        // Exact match
        if (name.equals(query)) return 0;
        // Prefix match
        if (name.startsWith(query)) return 1;
        // Substring match
        if (name.contains(query)) return 2;
        // Fuzzy match-compute Levenshtein distance
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
    static int levenshteinDistance(String a, String b) {
        if (a.equals(b)) return 0;
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();
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
