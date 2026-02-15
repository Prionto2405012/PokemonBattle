package com.example.pokemonbattle.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing and verification utility using PBKDF2.
 * Provides secure password hashing with salt for user authentication.
 * 
 * Uses PBKDF2WithHmacSHA512 algorithm with 210,000 iterations (OWASP 2023 recommendation).
 */
public class PasswordUtil {
    
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int ITERATIONS = 210000; // OWASP 2023 recommendation
    private static final int KEY_LENGTH = 512; // bits
    private static final int SALT_LENGTH = 32; // bytes
    private static final String DELIMITER = ":";

    /**
     * Hash a password using PBKDF2 with a random salt.
     * 
     * @param password Plain text password to hash
     * @return Hashed password string in format: iterations:salt:hash
     * @throws RuntimeException if hashing fails
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash password
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            // Encode and format: iterations:salt:hash
            String saltEncoded = Base64.getEncoder().encodeToString(salt);
            String hashEncoded = Base64.getEncoder().encodeToString(hash);

            return ITERATIONS + DELIMITER + saltEncoded + DELIMITER + hashEncoded;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verify a password against a stored hash.
     * 
     * @param password Plain text password to verify
     * @param storedHash Stored hash string in format: iterations:salt:hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }

        try {
            // Parse stored hash
            String[] parts = storedHash.split(DELIMITER);
            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] hash = Base64.getDecoder().decode(parts[2]);

            // Hash input password with same salt and iterations
            byte[] testHash = pbkdf2(password.toCharArray(), salt, iterations, KEY_LENGTH);

            // Compare hashes using constant-time comparison
            return slowEquals(hash, testHash);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate PBKDF2 hash.
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /**
     * Validate password strength.
     * 
     * @param password Password to validate
     * @return true if password meets minimum requirements
     */
    public static boolean isPasswordValid(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // Add more complex rules as needed
        return true;
    }

    /**
     * Test method (for development only).
     */
    public static void main(String[] args) {
        String password = "TestPassword123";
        String hashed = hashPassword(password);
        System.out.println("Hashed: " + hashed);
        System.out.println("Verified: " + verifyPassword(password, hashed));
        System.out.println("Wrong password: " + verifyPassword("WrongPassword", hashed));
    }
}
