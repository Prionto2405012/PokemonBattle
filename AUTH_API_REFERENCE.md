# 🔐 Authentication API - Quick Reference

## Core Classes

```
AuthService      → Business logic layer (use this in controllers)
UserDAO          → Data access layer (low-level database operations)
PasswordUtil     → Password hashing/verification utilities
DatabaseManager  → SQLite connection management
User             → Entity model representing a user
```

---

## 🎯 AuthService (Primary API)

### Register New User
```java
AuthService authService = new AuthService();
AuthService.AuthResult result = authService.register(username, email, password);

if (result.isSuccess()) {
    User user = result.getUser();
    // Registration successful
} else {
    String error = result.getMessage();
    // Show error to user
}
```

**Validation:**
- Username: 3-20 chars, alphanumeric + underscore only
- Email: Valid RFC email format
- Password: Min 8 chars
- Username must be unique
- Email must be unique

---

### Login User
```java
AuthService authService = new AuthService();
AuthService.AuthResult result = authService.login(usernameOrEmail, password);

if (result.isSuccess()) {
    User user = result.getUser();
    // Login successful, updates last_login timestamp
} else {
    String error = result.getMessage();
    // Invalid credentials or account not found
}
```

**Features:**
- Accepts username OR email
- Constant-time password comparison
- Updates `last_login` on success

---

## 👤 Session Management (WcController)

### Get Current User
```java
User currentUser = WcController.getCurrentUser();

if (currentUser != null) {
    String username = currentUser.getUsername();
    String email = currentUser.getEmail();
    Integer userId = currentUser.getId();
    LocalDateTime createdAt = currentUser.getCreatedAt();
    LocalDateTime lastLogin = currentUser.getLastLogin();
    boolean isActive = currentUser.isActive();
}
```

### Logout
```java
WcController.logout(); // Clears current user session
```

---

## 🗄️ User Model

### Properties
```java
User user = ...;

Integer id            = user.getId();
String username       = user.getUsername();
String email          = user.getEmail();
String passwordHash   = user.getPasswordHash();
LocalDateTime created = user.getCreatedAt();
LocalDateTime login   = user.getLastLogin();
boolean active        = user.isActive();
```

### Setters (for updates)
```java
user.setId(1);
user.setUsername("newname");
user.setEmail("new@example.com");
user.setPasswordHash("hash");
user.setCreatedAt(LocalDateTime.now());
user.setLastLogin(LocalDateTime.now());
user.setActive(true);
```

---

## 🔒 Password Utilities

### Hash Password
```java
String plainPassword = "MySecurePassword123";
String hashedPassword = PasswordUtil.hashPassword(plainPassword);
// Returns: "210000:Base64Salt:Base64Hash"
```

### Verify Password
```java
String plainPassword = "MySecurePassword123";
String storedHash = "210000:Base64Salt:Base64Hash";
boolean isValid = PasswordUtil.verifyPassword(plainPassword, storedHash);
// Returns: true if match, false otherwise
```

**Security:**
- PBKDF2WithHmacSHA512
- 210,000 iterations (OWASP 2023)
- 32-byte cryptographic salt
- Constant-time comparison

---

## 💾 UserDAO (Advanced)

### Create User
```java
UserDAO userDAO = new UserDAO();
User user = userDAO.createUser(username, email, hashedPassword);
// Returns User with generated ID
```

### Find by Username
```java
Optional<User> user = userDAO.findByUsername("testuser");
if (user.isPresent()) {
    // User found
}
```

### Find by Email
```java
Optional<User> user = userDAO.findByEmail("test@example.com");
if (user.isPresent()) {
    // User found
}
```

### Check Existence
```java
boolean usernameExists = userDAO.usernameExists("testuser");
boolean emailExists = userDAO.emailExists("test@example.com");
```

### Update Last Login
```java
userDAO.updateLastLogin(userId);
// Sets last_login to current timestamp
```

### Authenticate (low-level)
```java
Optional<User> user = userDAO.authenticateUser(usernameOrEmail, hashedPassword);
// Note: Password must already be hashed
// Prefer using AuthService.login() instead
```

### Deactivate User (soft delete)
```java
userDAO.deactivateUser(userId);
// Sets is_active = 0, does not delete from database
```

---

## 🔌 Database Connection

### Get Connection
```java
Connection conn = DatabaseManager.getInstance().getConnection();
// Singleton instance, auto-initializes database
```

**Auto-initialization:**
- Creates database file at `~/pokemon_battle/battle.db`
- Creates `users` and `user_profiles` tables
- Creates indexes on `username` and `email`
- Reads from `schema.sql` or uses hardcoded schema

---

## 📋 Error Messages

### Registration Errors
```
"Username is required"
"Email is required"
"Password is required"
"Username must be 3-20 characters and contain only letters, numbers, and underscores"
"Invalid email format"
"Password must be at least 8 characters long"
"Username already exists"
"Email already exists"
"Failed to create user account"
```

### Login Errors
```
"Username or email is required"
"Password is required"
"Invalid username/email or password"
"Account not found or deactivated"
```

---

## 🧪 Testing Examples

### Test Registration
```java
@Test
public void testRegistration() {
    AuthService authService = new AuthService();
    
    // Valid registration
    AuthService.AuthResult result = authService.register(
        "testuser", 
        "test@example.com", 
        "SecurePass123"
    );
    assertTrue(result.isSuccess());
    assertNotNull(result.getUser());
    assertEquals("testuser", result.getUser().getUsername());
    
    // Duplicate username (should fail)
    AuthService.AuthResult duplicate = authService.register(
        "testuser", 
        "another@example.com", 
        "Pass123"
    );
    assertFalse(duplicate.isSuccess());
    assertTrue(duplicate.getMessage().contains("already exists"));
}
```

### Test Login
```java
@Test
public void testLogin() {
    AuthService authService = new AuthService();
    
    // Register user first
    authService.register("testuser", "test@example.com", "SecurePass123");
    
    // Test login with username
    AuthService.AuthResult result1 = authService.login("testuser", "SecurePass123");
    assertTrue(result1.isSuccess());
    
    // Test login with email
    AuthService.AuthResult result2 = authService.login("test@example.com", "SecurePass123");
    assertTrue(result2.isSuccess());
    
    // Test wrong password
    AuthService.AuthResult result3 = authService.login("testuser", "WrongPass");
    assertFalse(result3.isSuccess());
}
```

### Test Password Hashing
```java
@Test
public void testPasswordHashing() {
    String password = "MySecurePassword123";
    
    // Hash password
    String hash1 = PasswordUtil.hashPassword(password);
    String hash2 = PasswordUtil.hashPassword(password);
    
    // Same password should produce different hashes (different salts)
    assertNotEquals(hash1, hash2);
    
    // Verify both hashes
    assertTrue(PasswordUtil.verifyPassword(password, hash1));
    assertTrue(PasswordUtil.verifyPassword(password, hash2));
    
    // Wrong password should fail
    assertFalse(PasswordUtil.verifyPassword("WrongPassword", hash1));
}
```

---

## 🎯 Common Patterns

### Controller Login Handler
```java
@FXML
protected void onLoginButtonClick() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();
    
    if (username.isEmpty() || password.isEmpty()) {
        showError("Please enter username and password");
        return;
    }
    
    AuthService.AuthResult result = authService.login(username, password);
    
    if (result.isSuccess()) {
        // Navigate to menu
        SceneManager.switchScene("menu.fxml", "Main Menu", 1200, 700);
    } else {
        showError(result.getMessage());
    }
}
```

### Controller Registration Handler
```java
@FXML
protected void onSignupButtonClick() {
    String username = usernameField.getText().trim();
    String email = emailField.getText().trim();
    String password = passwordField.getText();
    String confirm = confirmPasswordField.getText();
    
    if (!password.equals(confirm)) {
        showError("Passwords do not match");
        return;
    }
    
    AuthService.AuthResult result = authService.register(username, email, password);
    
    if (result.isSuccess()) {
        // Auto-login and navigate to menu
        SceneManager.switchScene("menu.fxml", "Main Menu", 1200, 700);
    } else {
        showError(result.getMessage());
    }
}
```

### Protected Screen Access
```java
@FXML
public void initialize() {
    User currentUser = WcController.getCurrentUser();
    
    if (currentUser == null) {
        // Not logged in, redirect to login
        SceneManager.switchScene("wc.fxml", "Login", 1200, 700);
        return;
    }
    
    // Show user-specific content
    welcomeLabel.setText("Welcome, " + currentUser.getUsername() + "!");
    loadUserData(currentUser.getId());
}
```

### Logout Button
```java
@FXML
protected void onLogoutButtonClick() {
    WcController.logout();
    SceneManager.switchScene("start.fxml", "Pokemon Battle", 1200, 700);
}
```

---

## 🔐 Security Checklist

✅ Passwords hashed with PBKDF2 (210k iterations)  
✅ Unique salt per password (32 bytes)  
✅ Constant-time comparison (timing attack prevention)  
✅ SQL injection prevention (PreparedStatement)  
✅ Input validation (regex patterns)  
✅ Minimum password length (8 chars)  
✅ Email format validation  
✅ Username format validation  
✅ Unique username constraint  
✅ Unique email constraint  

---

## 📦 Maven Dependency

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.0.0</version>
</dependency>
```

---

## 📁 File Locations

```
src/main/java/com/example/pokemonbattle/
├── controller/WcController.java          ← UI integration
├── model/User.java                       ← Entity model
├── service/AuthService.java              ← Business logic (use this!)
├── dao/UserDAO.java                      ← Data access
└── util/
    ├── PasswordUtil.java                 ← Password utilities
    └── DatabaseManager.java              ← DB connection

src/main/resources/com/example/pokemonbattle/
└── database/schema.sql                   ← Database schema

Runtime:
~/pokemon_battle/battle.db                ← SQLite database file
```

---

**💡 Pro Tip:** Use `AuthService` for all authentication operations in your controllers. Only use `UserDAO` directly if you need low-level database access.

For complete documentation, see **[AUTHENTICATION_INTEGRATION.md](AUTHENTICATION_INTEGRATION.md)**.
