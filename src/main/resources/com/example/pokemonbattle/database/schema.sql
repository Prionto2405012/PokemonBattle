CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active INTEGER DEFAULT 1,
    CHECK(length(username) >= 3),
    CHECK(length(email) >= 5)
);
CREATE INDEX IF NOT EXISTS idx_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_email ON users(email);
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id INTEGER PRIMARY KEY,
    display_name TEXT,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    total_battles INTEGER DEFAULT 0,
    favorite_pokemon TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ===== Game data tables (populated from JSON on first launch) =====

CREATE TABLE IF NOT EXISTS moves (
    id          INTEGER PRIMARY KEY,
    name        TEXT    NOT NULL,
    power       INTEGER,
    accuracy    INTEGER,
    pp          INTEGER NOT NULL,
    type        TEXT    NOT NULL,
    damage_class TEXT   NOT NULL
);

CREATE TABLE IF NOT EXISTS pokemon_species (
    id              INTEGER PRIMARY KEY,
    name            TEXT    NOT NULL,
    types           TEXT    NOT NULL,   -- comma-separated, e.g. "grass,poison"
    hp              INTEGER NOT NULL,
    attack          INTEGER NOT NULL,
    defense         INTEGER NOT NULL,
    special_attack  INTEGER NOT NULL,
    special_defense INTEGER NOT NULL,
    speed           INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS pokemon_moves (
    pokemon_id  INTEGER NOT NULL,
    move_id     INTEGER NOT NULL,
    PRIMARY KEY (pokemon_id, move_id),
    FOREIGN KEY (pokemon_id) REFERENCES pokemon_species(id),
    FOREIGN KEY (move_id)    REFERENCES moves(id)
);
CREATE INDEX IF NOT EXISTS idx_pokemon_moves_pokemon ON pokemon_moves(pokemon_id);

CREATE TABLE IF NOT EXISTS battle_items (
    id       INTEGER PRIMARY KEY,
    name     TEXT NOT NULL,
    category TEXT NOT NULL,
    effect   TEXT
);

CREATE TABLE IF NOT EXISTS battle_history (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    result          TEXT    NOT NULL CHECK(result IN ('WIN','LOSS')),
    pokemon_used    TEXT    NOT NULL,    -- comma-separated pokemon names
    opponent_type   TEXT    NOT NULL CHECK(opponent_type IN ('AI','LOCAL','ONLINE')),
    opponent_name   TEXT,
    battle_log      TEXT,
    timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_battle_history_user ON battle_history(user_id);

-- Metadata table to track whether JSON import has been done
CREATE TABLE IF NOT EXISTS game_data_meta (
    key   TEXT PRIMARY KEY,
    value TEXT
);
