package com.example.pokemonbattle.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.pokemonbattle.model.Item;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.PokemonSpecies;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Data access object that loads game data directly from JSON resources
 * with in-memory caching for zero-lag repeated access.
 * No SQL dependency — all pokemon, moves, and items are parsed once from JSON
 * and held in static memory for the lifetime of the application.
 */
public class GameDataDAO {

    private static final String MOVES_JSON = "/com/example/pokemonbattle/database/moves_gen4.json";
    private static final String POKEMON_JSON = "/com/example/pokemonbattle/database/pokemon_gen4.json";
    private static final String ITEMS_JSON = "/com/example/pokemonbattle/database/battle_items.json";

    /* ── Singleton in-memory cache ── */
    private static volatile Map<Integer, Move> cachedMoves;
    private static volatile List<PokemonSpecies> cachedPokemon;
    private static volatile List<Item> cachedItems;
    private static final Object LOCK = new Object();

    public GameDataDAO() { /* no DB dependency */ }

    /**
     * Pre-load all game data from JSON into the in-memory cache.
     * Safe to call many times — only the first invocation does real work.
     */
    public void ensureDataLoaded() {
        if (cachedMoves != null && cachedPokemon != null && cachedItems != null) {
            System.out.println("[GameDataDAO] Game data already cached — skipping load.");
            return;
        }
        synchronized (LOCK) {
            if (cachedMoves != null && cachedPokemon != null && cachedItems != null) return;
            long start = System.currentTimeMillis();
            System.out.println("[GameDataDAO] Loading game data from JSON resources ...");
            try {
                Gson gson = new Gson();

                // ── Moves ──
                String movesJson = readResource(MOVES_JSON);
                List<Move> movesList = gson.fromJson(movesJson,
                        new TypeToken<List<Move>>(){}.getType());
                Map<Integer, Move> movesMap = new HashMap<>(movesList.size() * 2);
                for (Move m : movesList) movesMap.put(m.getId(), m);
                cachedMoves = Collections.unmodifiableMap(movesMap);
                System.out.println("[GameDataDAO] Cached " + cachedMoves.size() + " moves.");

                // ── Pokemon ──
                String pokemonJson = readResource(POKEMON_JSON);
                List<PokemonSpecies> pokemonList = gson.fromJson(pokemonJson,
                        new TypeToken<List<PokemonSpecies>>(){}.getType());
                cachedPokemon = Collections.unmodifiableList(pokemonList);
                System.out.println("[GameDataDAO] Cached " + cachedPokemon.size() + " pokemon.");

                // ── Items ──
                String itemsJson = readResource(ITEMS_JSON);
                List<Item> itemsList = gson.fromJson(itemsJson,
                        new TypeToken<List<Item>>(){}.getType());
                cachedItems = Collections.unmodifiableList(itemsList);
                System.out.println("[GameDataDAO] Cached " + cachedItems.size() + " items.");

                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[GameDataDAO] All game data loaded in " + elapsed + " ms.");
            } catch (Exception e) {
                System.err.println("[GameDataDAO] Error loading game data: " + e.getMessage());
                e.printStackTrace();
                if (cachedMoves == null)   cachedMoves   = Collections.emptyMap();
                if (cachedPokemon == null)  cachedPokemon  = Collections.emptyList();
                if (cachedItems == null)    cachedItems    = Collections.emptyList();
            }
        }
    }

    /** Returns the cached moves map (unmodifiable). */
    public Map<Integer, Move> loadAllMoves() {
        ensureDataLoaded();
        return cachedMoves;
    }

    /**
     * Returns a fresh shallow copy of the cached pokemon list.
     * The list itself is mutable so callers can reorder / filter freely,
     * while the underlying PokemonSpecies objects are shared (their
     * {@code selectedMoves} field is the only mutable part and is
     * overwritten each game via {@code selectRandomMoves()}).
     */
    public List<PokemonSpecies> loadAllPokemon() {
        ensureDataLoaded();
        return new ArrayList<>(cachedPokemon);
    }

    /** Returns a fresh shallow copy of the cached items list. */
    public List<Item> loadAllItems() {
        ensureDataLoaded();
        return new ArrayList<>(cachedItems);
    }

    private String readResource(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new Exception("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
