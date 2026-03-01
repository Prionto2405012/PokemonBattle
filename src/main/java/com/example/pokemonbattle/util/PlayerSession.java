package com.example.pokemonbattle.util;

import com.example.pokemonbattle.model.User;

import java.util.prefs.Preferences;

/**
 * Singleton session manager that tracks the current user session.
 * Persists avatar selection and first-time state using Java Preferences API.
 * Clean separation: no UI logic — only state and persistence.
 */
public class PlayerSession {

    private static PlayerSession instance;

    private User currentUser;
    private String avatarPath;
    private String avatarGender;

    private static final String PREF_AVATAR_PATH   = "avatar_path";
    private static final String PREF_AVATAR_GENDER  = "avatar_gender";

    private PlayerSession() {}

    public static PlayerSession getInstance() {
        if (instance == null) {
            instance = new PlayerSession();
        }
        return instance;
    }

    // ── User management ─────────────────────────────────────────

    /**
     * Initialize session for a logged-in user.
     * Loads persisted preferences (avatar, etc.) from disk.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            loadPreferences();
        } else {
            avatarPath = null;
            avatarGender = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // ── First-time detection ────────────────────────────────────

    /**
     * User is first-time if no avatar has been saved yet.
     */
    public boolean isFirstTime() {
        return avatarPath == null || avatarPath.isEmpty();
    }

    // ── Avatar management ───────────────────────────────────────

    /**
     * Save avatar selection and mark user as not-first-time.
     *
     * @param path   resource path to the avatar sprite
     * @param gender "male" or "female"
     */
    public void saveAvatar(String path, String gender) {
        this.avatarPath = path;
        this.avatarGender = gender;
        savePreferences();
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public String getAvatarGender() {
        return avatarGender;
    }

    // ── Persistence (Preferences API) ───────────────────────────

    private void loadPreferences() {
        if (currentUser == null || currentUser.getId() == null) return;
        Preferences prefs = getUserPreferences();
        avatarPath  = prefs.get(PREF_AVATAR_PATH, null);
        avatarGender = prefs.get(PREF_AVATAR_GENDER, null);
        System.out.println("[PlayerSession] Loaded prefs for user " + currentUser.getId()
                + " → avatar=" + avatarPath + ", gender=" + avatarGender);
    }

    private void savePreferences() {
        if (currentUser == null || currentUser.getId() == null) return;
        Preferences prefs = getUserPreferences();
        if (avatarPath != null)   prefs.put(PREF_AVATAR_PATH, avatarPath);
        if (avatarGender != null) prefs.put(PREF_AVATAR_GENDER, avatarGender);
        try {
            prefs.flush();
            System.out.println("[PlayerSession] Saved prefs for user " + currentUser.getId());
        } catch (Exception e) {
            System.err.println("[PlayerSession] Failed to flush preferences: " + e.getMessage());
        }
    }

    private Preferences getUserPreferences() {
        return Preferences.userRoot().node("/pokemonbattle/users/" + currentUser.getId());
    }

    // ── Session lifecycle ───────────────────────────────────────

    /**
     * Clear session (logout).
     */
    public void clearSession() {
        currentUser = null;
        avatarPath = null;
        avatarGender = null;
    }
}
