package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.service.AuthService;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Controller for the Authentication Screen.
 * Handles login and signup functionality for players.
 */
@SuppressWarnings("unused") // Methods are called by FXML
public class WcController {
    @FXML
    private StackPane rootPane; // Root container for background image
    @FXML
    private ImageView bgImage; // Background image
    @FXML
    private VBox authCard;

    // Tab buttons
    @FXML
    private Button loginTabButton;
    @FXML
    private Button signupTabButton;

    // Login form fields
    @FXML
    private VBox loginForm;
    @FXML
    private TextField loginUsernameField;
    @FXML
    private PasswordField loginPasswordField;
    @FXML
    private Label loginErrorLabel;
    @FXML
    private Label loginUsernameError;
    @FXML
    private Label loginPasswordError;

    // Signup form fields
    @FXML
    private VBox signupForm;
    @FXML
    private TextField signupUsernameField;
    @FXML
    private TextField signupEmailField;
    @FXML
    private PasswordField signupPasswordField;
    @FXML
    private PasswordField signupConfirmPasswordField;
    @FXML
    private Label signupErrorLabel;
    @FXML
    private Label signupUsernameError;
    @FXML
    private Label signupEmailError;
    @FXML
    private Label signupPasswordError;
    @FXML
    private Label signupConfirmPasswordError;
    
    // Authentication service
    private final AuthService authService;
    
    // Store currently authenticated user
    private static User currentUser;

    public WcController() {
        this.authService = new AuthService();
    }


    private Color getAverageColor(javafx.scene.image.Image image) {
        PixelReader reader = image.getPixelReader();
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

        double r = 0, g = 0, b = 0;
        int count = 0;

        for (int x = 0; x < w; x += 10) {
            for (int y = 0; y < h; y += 10) {
                Color c = reader.getColor(x, y);
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                count++;
            }
        }
        return new Color(r / count, g / count, b / count, 1.0);
    }

    private String toRGBA(Color c, double alpha) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255),
                alpha);
    }

    /**
     * Initialize the controller - bind background image to fill the container.
     */
    @FXML
    public void initialize() {
        // Phase 3: WC scene fades in from full black (completes the 3-phase transition)
        if (rootPane != null) {
            javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle();
            overlay.setFill(javafx.scene.paint.Color.BLACK);
            overlay.widthProperty().bind(rootPane.widthProperty());
            overlay.heightProperty().bind(rootPane.heightProperty());
            overlay.setManaged(false);
            overlay.setOpacity(0.8);
            rootPane.getChildren().add(overlay);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(70), overlay);
            fadeIn.setFromValue(0.9);
            fadeIn.setToValue(0.0);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            fadeIn.setOnFinished(e -> {
                overlay.widthProperty().unbind();
                overlay.heightProperty().unbind();
                rootPane.getChildren().remove(overlay);
            });
            fadeIn.play();
        }

        // Bind background image to fill the container (with null check)
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        if (bgImage.getImage() != null) {
            Color avg = getAverageColor(bgImage.getImage());
            Color lighter = avg.brighter();
            Color darker  = avg.darker();
            authCard.setStyle(
                "-fx-background-color: " + toRGBA(avg,0.7) + ";" +
                "-fx-border-color: " + toRGBA(lighter,0.55) + ";" +
                "-fx-border-radius:16;" +
                "-fx-background-radius:16;"
            );
            loginTabButton.setStyle(
                "-fx-background-color: " + toRGBA(darker,0.6)
            );
            signupTabButton.setStyle(
                "-fx-background-color: " + toRGBA(darker,0.6)
            );
        }
        
        // Focus on the first field in login form
        if (loginUsernameField != null) {
            loginUsernameField.requestFocus();
        }
    }

    /**
     * Handle "Login" tab click.
     */
    @FXML
    protected void onLoginTabClick() {
        // Show login form, hide signup form
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        signupForm.setVisible(false);
        signupForm.setManaged(false);

        // Update tab button styles
        loginTabButton.getStyleClass().add("tab-button-active");
        signupTabButton.getStyleClass().remove("tab-button-active");

        // Clear any error messages
        clearAllLoginErrors();
        clearAllSignupErrors();

        // Focus on login username field
        loginUsernameField.requestFocus();
    }

    /**
     * Handle "Sign Up" tab click.
     */
    @FXML
    protected void onSignupTabClick() {
        // Show signup form, hide login form
        signupForm.setVisible(true);
        signupForm.setManaged(true);
        loginForm.setVisible(false);
        loginForm.setManaged(false);

        // Update tab button styles
        signupTabButton.getStyleClass().add("tab-button-active");
        loginTabButton.getStyleClass().remove("tab-button-active");

        // Clear any error messages
        clearAllLoginErrors();
        clearAllSignupErrors();

        // Focus on signup username field
        signupUsernameField.requestFocus();
    }

    /**
     * Handle "Login" button click.
     */
    @FXML
    protected void onLoginButtonClick() {
        // Clear previous errors
        clearAllLoginErrors();
        
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        // Validate input - show field-specific errors
        boolean hasErrors = false;
        
        if (username.isEmpty()) {
            showFieldError(loginUsernameError, "Username or email is required");
            hasErrors = true;
        }

        if (password.isEmpty()) {
            showFieldError(loginPasswordError, "Password is required");
            hasErrors = true;
        }
        
        if (hasErrors) {
            return;
        }

        // Attempt authentication
        AuthService.AuthResult result = authService.login(username, password);
        
        if (result.isSuccess()) {
            // Store authenticated user
            currentUser = result.getUser();
            System.out.println("Login successful - User: " + currentUser.getUsername());
            
            // Navigate to main menu
            SceneManager.switchSceneWithLoading("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
        } else {
            // Show error message - could be invalid credentials
            System.err.println("Login failed: " + result.getMessage());
            showLoginError(result.getMessage());
        }
    }

    /**
     * Handle "Sign Up" button click.
     */
    @FXML
    protected void onSignupButtonClick() {
        // Clear previous errors
        clearAllSignupErrors();
        
        String username = signupUsernameField.getText().trim();
        String email = signupEmailField.getText().trim();
        String password = signupPasswordField.getText();
        String confirmPassword = signupConfirmPasswordField.getText();

        // Validate all fields with field-specific errors
        boolean hasErrors = false;
        
        // Username validation
        if (username.isEmpty()) {
            showFieldError(signupUsernameError, "Username is required");
            hasErrors = true;
        } else if (username.length() < 3) {
            showFieldError(signupUsernameError, "Username must be at least 3 characters");
            hasErrors = true;
        } else if (username.length() > 20) {
            showFieldError(signupUsernameError, "Username must be at most 20 characters");
            hasErrors = true;
        } else if (!username.matches("^[A-Za-z0-9_]+$")) {
            showFieldError(signupUsernameError, "Username can only contain letters, numbers, and underscores");
            hasErrors = true;
        }
        
        // Email validation
        if (email.isEmpty()) {
            showFieldError(signupEmailError, "Email is required");
            hasErrors = true;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showFieldError(signupEmailError, "Please enter a valid email address");
            hasErrors = true;
        }
        
        // Password validation
        if (password.isEmpty()) {
            showFieldError(signupPasswordError, "Password is required");
            hasErrors = true;
        } else if (password.length() < 6) {
            showFieldError(signupPasswordError, "Password must be at least 6 characters");
            hasErrors = true;
        }
        
        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            showFieldError(signupConfirmPasswordError, "Please confirm your password");
            hasErrors = true;
        } else if (!password.equals(confirmPassword)) {
            showFieldError(signupConfirmPasswordError, "Passwords do not match");
            hasErrors = true;
        }
        
        if (hasErrors) {
            return;
        }

        // Attempt registration
        AuthService.AuthResult result = authService.register(username, email, password);
        
        if (result.isSuccess()) {
            // Store authenticated user
            currentUser = result.getUser();
            System.out.println("Registration successful - User: " + currentUser.getUsername());
            
            // Navigate to main menu
            SceneManager.switchSceneWithLoading("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
        } else {
            // Show error message (e.g., username/email already exists)
            System.err.println("Registration failed: " + result.getMessage());
            showSignupError(result.getMessage());
        }
    }

    /**
     * Show error message on login form.
     */
    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
        loginErrorLabel.setManaged(true);
    }

    /**
     * Show error message on signup form.
     */
    private void showSignupError(String message) {
        signupErrorLabel.setText(message);
        signupErrorLabel.setVisible(true);
        signupErrorLabel.setManaged(true);
    }
    
    /**
     * Show error message for a specific field.
     */
    private void showFieldError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
    
    /**
     * Clear all login form errors.
     */
    private void clearAllLoginErrors() {
        hideError(loginErrorLabel);
        hideError(loginUsernameError);
        hideError(loginPasswordError);
    }
    
    /**
     * Clear all signup form errors.
     */
    private void clearAllSignupErrors() {
        hideError(signupErrorLabel);
        hideError(signupUsernameError);
        hideError(signupEmailError);
        hideError(signupPasswordError);
        hideError(signupConfirmPasswordError);
    }
    
    /**
     * Hide an error label.
     */
    private void hideError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            errorLabel.setText("");
        }
    }

    /**
     * Handle "Back" button click.
     */
    @FXML
    protected void onBackButtonClick() {
        SceneManager.switchSceneWithLoading("start.fxml", "Pokemon Battle - Start", 1200, 700);
    }
    
    /**
     * Get the currently authenticated user.
     * @return The current user, or null if no user is authenticated
     */
    public static User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Clear the current user session (logout).
     */
    public static void logout() {
        currentUser = null;
    }
}
