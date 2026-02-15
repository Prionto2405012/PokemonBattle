package com.example.pokemonbattle.service;

import com.example.pokemonbattle.database.UserDAO;
import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.security.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Authentication Service - Business logic layer for user authentication.
 * Handles registration, login, and validation logic.
 * Acts as intermediary between controllers and data access layer.
 */
public class AuthService {
    
    private final UserDAO userDAO;
    
    // Email validation regex (basic)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    // Username validation regex (alphanumeric, underscore, 3-20 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_]{3,20}$"
    );
    
    public AuthService() {
        this.userDAO = new UserDAO();
    }
    
    /**
     * Register a new user.
     * Validates input and creates user account.
     * 
     * @param username Username
     * @param email Email address
     * @param password Plain text password
     * @return AuthResult indicating success or failure with message
     */
    public AuthResult register(String username, String email, String password) {
        try {
            // Validate inputs
            ValidationResult validation = validateRegistration(username, email, password);
            if (!validation.isValid()) {
                return AuthResult.failure(validation.getMessage());
            }
            
            // Check if username already exists
            if (userDAO.usernameExists(username)) {
                return AuthResult.failure("Username already taken");
            }
            
            // Check if email already exists
            if (userDAO.emailExists(email)) {
                return AuthResult.failure("Email already registered");
            }
            
            // Hash password
            String passwordHash = PasswordUtil.hashPassword(password);
            
            // Create user
            User user = new User(username, email, passwordHash);
            Optional<User> createdUser = userDAO.createUser(user);
            
            if (createdUser.isPresent()) {
                return AuthResult.success(createdUser.get(), "Registration successful!");
            } else {
                return AuthResult.failure("Failed to create user account");
            }
            
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            return AuthResult.failure("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during registration: " + e.getMessage());
            return AuthResult.failure("An unexpected error occurred");
        }
    }
    
    /**
     * Authenticate user login.
     * 
     * @param usernameOrEmail Username or email
     * @param password Plain text password
     * @return AuthResult indicating success or failure with message
     */
    public AuthResult login(String usernameOrEmail, String password) {
        try {
            // Validate inputs
            if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
                return AuthResult.failure("Please enter your username or email");
            }
            
            if (password == null || password.isEmpty()) {
                return AuthResult.failure("Please enter your password");
            }
            
            // Find user
            Optional<User> user = userDAO.findByUsername(usernameOrEmail);
            if (user.isEmpty()) {
                user = userDAO.findByEmail(usernameOrEmail);
            }
            
            if (user.isEmpty()) {
                return AuthResult.failure("Invalid username/email or password");
            }
            
            // Verify password
            boolean passwordValid = PasswordUtil.verifyPassword(password, user.get().getPasswordHash());
            
            if (passwordValid) {
                // Update last login
                userDAO.updateLastLogin(user.get().getId());
                return AuthResult.success(user.get(), "Login successful!");
            } else {
                return AuthResult.failure("Invalid username/email or password");
            }
            
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
            return AuthResult.failure("Database error occurred");
        } catch (Exception e) {
            System.err.println("Unexpected error during login: " + e.getMessage());
            return AuthResult.failure("An unexpected error occurred");
        }
    }
    
    /**
     * Validate registration input.
     */
    private ValidationResult validateRegistration(String username, String email, String password) {
        // Username validation
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.invalid("Username is required");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.invalid(
                "Username must be 3-20 characters and contain only letters, numbers, and underscores"
            );
        }
        
        // Email validation
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.invalid("Email is required");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.invalid("Invalid email format");
        }
        
        // Password validation
        if (password == null || password.isEmpty()) {
            return ValidationResult.invalid("Password is required");
        }
        
        if (password.length() < 6) {
            return ValidationResult.invalid("Password must be at least 6 characters long");
        }
        
        if (password.length() > 128) {
            return ValidationResult.invalid("Password is too long (max 128 characters)");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Result of authentication operation.
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;
        
        private AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
        
        public static AuthResult success(User user, String message) {
            return new AuthResult(true, message, user);
        }
        
        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public User getUser() {
            return user;
        }
    }
    
    /**
     * Result of validation operation.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
