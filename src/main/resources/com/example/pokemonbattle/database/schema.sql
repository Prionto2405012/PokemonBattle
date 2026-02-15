-- =====================================================
-- Pokemon Battle Authentication Database Schema
-- SQLite Database Schema for User Authentication
-- =====================================================

-- Users table
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

-- Index for faster username lookups
CREATE INDEX IF NOT EXISTS idx_username ON users(username);

-- Index for faster email lookups
CREATE INDEX IF NOT EXISTS idx_email ON users(email);

-- Optional: Game-specific user data (for future use)
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id INTEGER PRIMARY KEY,
    display_name TEXT,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    total_battles INTEGER DEFAULT 0,
    favorite_pokemon TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
