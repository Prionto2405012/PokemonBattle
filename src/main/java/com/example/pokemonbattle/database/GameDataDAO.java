package com.example.pokemonbattle.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.pokemonbattle.model.Item;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.PokemonSpecies;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GameDataDAO {

    private static final String META_KEY_IMPORTED = "json_imported";
    private static final String META_VALUE_DONE = "1";

    private static final String MOVES_JSON = "/com/example/pokemonbattle/database/moves_gen4.json";
    private static final String POKEMON_JSON = "/com/example/pokemonbattle/database/pokemon_gen4.json";
    private static final String ITEMS_JSON = "/com/example/pokemonbattle/database/battle_items.json";

    private final DatabaseManager dbManager;

    public GameDataDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void ensureDataLoaded() {
        try {
            Connection conn = dbManager.getConnection();
            if (isDataImported(conn)) {
                System.out.println("[GameDataDAO] Game data already in DB — skipping import.");
                return;
            }
            long start = System.currentTimeMillis();
            System.out.println("[GameDataDAO] First launch — importing JSON → SQLite ...");
            importAllData(conn);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[GameDataDAO] Import complete in " + elapsed + " ms.");
        } catch (Exception e) {
            System.err.println("[GameDataDAO] Error ensuring data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public Map<Integer, Move> loadAllMoves() {
        Map<Integer, Move> map = new HashMap<>();
        String sql = "SELECT id, name, power, accuracy, pp, type, damage_class FROM moves";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Move m = new Move(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getObject("power") != null ? rs.getInt("power") : null,
                    rs.getObject("accuracy") != null ? rs.getInt("accuracy") : null,
                    rs.getInt("pp"),
                    rs.getString("type"),
                    rs.getString("damage_class")
                );
                map.put(m.getId(), m);
            }
        } catch (SQLException e) {
            System.err.println("[GameDataDAO] Error loading moves: " + e.getMessage());
        }
        System.out.println("[GameDataDAO] Loaded " + map.size() + " moves from DB.");
        return map;
    }

    public List<PokemonSpecies> loadAllPokemon() {
        List<PokemonSpecies> list = new ArrayList<>();
        String sql = "SELECT id, name, types, hp, attack, defense, special_attack, special_defense, speed FROM pokemon_species ORDER BY id";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PokemonSpecies p = new PokemonSpecies();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                String typesStr = rs.getString("types");
                List<String> types = new ArrayList<>();
                if (typesStr != null && !typesStr.isBlank()) {
                    for (String t : typesStr.split(",")) {
                        types.add(t.trim());
                    }
                }
                p.setTypes(types);

                PokemonSpecies.Stats stats = new PokemonSpecies.Stats();
                stats.setHp(rs.getInt("hp"));
                stats.setAttack(rs.getInt("attack"));
                stats.setDefense(rs.getInt("defense"));
                stats.setSpecial_attack(rs.getInt("special_attack"));
                stats.setSpecial_defense(rs.getInt("special_defense"));
                stats.setSpeed(rs.getInt("speed"));
                p.setStats(stats);

                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("[GameDataDAO] Error loading pokemon: " + e.getMessage());
        }
        Map<Integer, List<Integer>> movesMap = loadPokemonMovesMap();
        for (PokemonSpecies p : list) {
            List<Integer> moveIds = movesMap.getOrDefault(p.getId(), List.of());
            p.setMoves(new ArrayList<>(moveIds));
        }

        System.out.println("[GameDataDAO] Loaded " + list.size() + " pokemon from DB.");
        return list;
    }
    public List<Item> loadAllItems() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT id, name, category, effect FROM battle_items ORDER BY id";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Item item = new Item(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("effect"),
                    rs.getString("category")
                );
                list.add(item);
            }
        } catch (SQLException e) {
            System.err.println("[GameDataDAO] Error loading items: " + e.getMessage());
        }
        System.out.println("[GameDataDAO] Loaded " + list.size() + " items from DB.");
        return list;
    }
    private void importAllData(Connection conn) throws Exception {
        conn.setAutoCommit(false);
        try {
            importMoves(conn);
            importPokemon(conn);
            importItems(conn);
            markDataImported(conn);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void importMoves(Connection conn) throws Exception {
        String json = readResource(MOVES_JSON);
        Gson gson = new Gson();
        List<Move> moves = gson.fromJson(json, new TypeToken<List<Move>>(){}.getType());
        String sql = "INSERT OR IGNORE INTO moves (id, name, power, accuracy, pp, type, damage_class) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Move m : moves) {
                ps.setInt(1, m.getId());
                ps.setString(2, m.getName());
                if (m.getPower() != null) ps.setInt(3, m.getPower()); else ps.setNull(3, java.sql.Types.INTEGER);
                if (m.getAccuracy() != null) ps.setInt(4, m.getAccuracy()); else ps.setNull(4, java.sql.Types.INTEGER);
                ps.setInt(5, m.getPp());
                ps.setString(6, m.getType());
                ps.setString(7, m.getDamage_class());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        System.out.println("[GameDataDAO] Imported " + moves.size() + " moves.");
    }

    private void importPokemon(Connection conn) throws Exception {
        String json = readResource(POKEMON_JSON);
        Gson gson = new Gson();
        List<PokemonSpecies> pokemonList = gson.fromJson(json, new TypeToken<List<PokemonSpecies>>(){}.getType());

        String speciesSql = "INSERT OR IGNORE INTO pokemon_species (id, name, types, hp, attack, defense, special_attack, special_defense, speed) VALUES (?,?,?,?,?,?,?,?,?)";
        String moveSql = "INSERT OR IGNORE INTO pokemon_moves (pokemon_id, move_id) VALUES (?,?)";

        try (PreparedStatement speciesPs = conn.prepareStatement(speciesSql);
             PreparedStatement movePs = conn.prepareStatement(moveSql)) {

            for (PokemonSpecies p : pokemonList) {
                speciesPs.setInt(1, p.getId());
                speciesPs.setString(2, p.getName());
                speciesPs.setString(3, String.join(",", p.getTypes()));

                PokemonSpecies.Stats s = p.getStats();
                speciesPs.setInt(4, s.getHp());
                speciesPs.setInt(5, s.getAttack());
                speciesPs.setInt(6, s.getDefense());
                speciesPs.setInt(7, s.getSpecial_attack());
                speciesPs.setInt(8, s.getSpecial_defense());
                speciesPs.setInt(9, s.getSpeed());
                speciesPs.addBatch();
                for (Integer moveId : p.getMoves()) {
                    movePs.setInt(1, p.getId());
                    movePs.setInt(2, moveId);
                    movePs.addBatch();
                }
            }
            speciesPs.executeBatch();
            movePs.executeBatch();
        }
        System.out.println("[GameDataDAO] Imported " + pokemonList.size() + " pokemon species + move links.");
    }

    private void importItems(Connection conn) throws Exception {
        String json = readResource(ITEMS_JSON);
        Gson gson = new Gson();
        List<Item> items = gson.fromJson(json, new TypeToken<List<Item>>(){}.getType());

        String sql = "INSERT OR IGNORE INTO battle_items (id, name, category, effect) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Item item : items) {
                ps.setInt(1, item.getId());
                ps.setString(2, item.getName());
                ps.setString(3, item.getCategory());
                ps.setString(4, item.getEffect());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        System.out.println("[GameDataDAO] Imported " + items.size() + " battle items.");
    }
    private boolean isDataImported(Connection conn) throws SQLException {
        String sql = "SELECT value FROM game_data_meta WHERE key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, META_KEY_IMPORTED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && META_VALUE_DONE.equals(rs.getString("value"));
            }
        }
    }

    private void markDataImported(Connection conn) throws SQLException {
        String sql = "INSERT OR REPLACE INTO game_data_meta (key, value) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, META_KEY_IMPORTED);
            ps.setString(2, META_VALUE_DONE);
            ps.executeUpdate();
        }
    }
    private Map<Integer, List<Integer>> loadPokemonMovesMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        String sql = "SELECT pokemon_id, move_id FROM pokemon_moves ORDER BY pokemon_id, move_id";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int pokemonId = rs.getInt("pokemon_id");
                int moveId = rs.getInt("move_id");
                map.computeIfAbsent(pokemonId, k -> new ArrayList<>()).add(moveId);
            }
        } catch (SQLException e) {
            System.err.println("[GameDataDAO] Error loading pokemon_moves: " + e.getMessage());
        }
        return map;
    }

    private String readResource(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new Exception("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
