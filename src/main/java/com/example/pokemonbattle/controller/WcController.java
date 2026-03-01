package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.service.AuthService;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class WcController {
    @FXML private StackPane rootPane;
    @FXML private ImageView bgImage;
    @FXML private Region glassBlurLayer;
    @FXML private VBox authCard;
    @FXML private HBox authTabsBox;
    @FXML private Button loginTabButton;
    @FXML private Button signupTabButton;
    @FXML private VBox loginForm;
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label loginErrorLabel;
    @FXML private Label loginUsernameError;
    @FXML private Label loginPasswordError;
    @FXML private VBox signupForm;
    @FXML private TextField signupUsernameField;
    @FXML private TextField signupEmailField;
    @FXML private PasswordField signupPasswordField;
    @FXML private PasswordField signupConfirmPasswordField;
    @FXML private Label signupErrorLabel;
    @FXML private Label signupUsernameError;
    @FXML private Label signupEmailError;
    @FXML private Label signupPasswordError;
    @FXML private Label signupConfirmPasswordError;
    private final AuthService authService;
    private static User currentUser;

    private Color base, lighter, darker, darkest, textPrimary, textSecondary;
    private Color neuBase, neuLight, neuDark;

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
                r += c.getRed(); g += c.getGreen(); b += c.getBlue();
                count++;
            }
        }
        return new Color(r / count, g / count, b / count, 1.0);
    }

    private String rgba(Color c, double a) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), a);
    }

    private String hex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    private double luminance(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private void buildPalette() {
        if (bgImage == null || bgImage.getImage() == null) return;
        base = getAverageColor(bgImage.getImage());
        lighter = base.interpolate(Color.WHITE, 0.4);
        darker  = base.interpolate(Color.BLACK, 0.45);
        darkest = base.interpolate(Color.BLACK, 0.7);
        boolean bright = luminance(base) > 0.45;
        textPrimary   = bright ? darkest : lighter.interpolate(Color.WHITE, 0.5);
        textSecondary = bright ? darker  : lighter;
        neuBase = base.desaturate();
        neuLight = neuBase.interpolate(Color.WHITE, 0.6);
        neuDark = neuBase.interpolate(Color.BLACK, 0.3);
    }

    private void applyDynamicTheme() {
        if (base == null) return;

        // Glass blur layer — frosted neumorphic card container
        if (glassBlurLayer != null) {
            glassBlurLayer.setStyle(
                "-fx-background-color: " + rgba(base, 0.45) + ";" +
                "-fx-background-radius: 30;" +
                "-fx-border-radius: 30;" +
                "-fx-border-color: " + rgba(neuLight, 0.2) + ";" +
                "-fx-border-width: 1;"
            );
            DropShadow lightSh = new DropShadow(BlurType.GAUSSIAN,
                Color.color(neuLight.getRed(), neuLight.getGreen(), neuLight.getBlue(), 0.5),
                20, 0.05, -8, -8);
            DropShadow darkSh = new DropShadow(BlurType.GAUSSIAN,
                Color.color(neuDark.getRed(), neuDark.getGreen(), neuDark.getBlue(), 0.35),
                20, 0.05, 8, 8);
            darkSh.setInput(lightSh);
            glassBlurLayer.setEffect(darkSh);
        }

        // Auth card — semi-transparent content surface
        authCard.setStyle(
            "-fx-background-color: " + rgba(base, 0.55) + ";" +
            "-fx-background-radius: 28;" +
            "-fx-border-color: transparent;"
        );

        // Title
        for (Node n : authCard.lookupAll(".auth-title")) {
            n.setStyle("-fx-text-fill: " + hex(textPrimary) + ";");
        }

        // Tabs divider
        if (authTabsBox != null) {
            authTabsBox.setStyle(
                "-fx-border-color: " + rgba(neuDark, 0.12) + ";" +
                "-fx-border-width: 0 0 2 0;" +
                "-fx-padding: 0 0 12 0;"
            );
        }

        // Tab buttons — subtle raised neumorphic
        String tabStyle =
            "-fx-background-color: " + rgba(neuLight, 0.2) + ", " +
                rgba(neuDark, 0.08) + ", " + rgba(base, 0.35) + ";" +
            "-fx-background-insets: -2 2 2 -2, 2 -2 -2 2, 0;" +
            "-fx-background-radius: 12 12 0 0, 12 12 0 0, 12 12 0 0;" +
            "-fx-text-fill: " + hex(textPrimary) + ";";
        loginTabButton.setStyle(tabStyle);
        signupTabButton.setStyle(tabStyle);

        // Field labels
        for (Node n : authCard.lookupAll(".field-label")) {
            n.setStyle("-fx-text-fill: " + hex(textPrimary) + ";");
        }

        // Text fields — inset neumorphic with compound inner shadow
        String fieldStyle =
            "-fx-font-family: 'menu';" +
            "-fx-background-color: " + rgba(neuDark, 0.12) + ", " +
                rgba(neuLight, 0.3) + ", " + rgba(base, 0.25) + ";" +
            "-fx-background-insets: 0 2 2 0, 2 0 0 2, 2;" +
            "-fx-background-radius: 15, 15, 13;" +
            "-fx-text-fill: " + hex(textPrimary) + ";" +
            "-fx-prompt-text-fill: " + rgba(textSecondary, 0.4) + ";" +
            "-fx-border-color: transparent;";

        InnerShadow fieldDark = new InnerShadow(BlurType.GAUSSIAN,
            Color.color(neuDark.getRed(), neuDark.getGreen(), neuDark.getBlue(), 0.2),
            6, 0, 2, 2);
        InnerShadow fieldLight = new InnerShadow(BlurType.GAUSSIAN,
            Color.color(neuLight.getRed(), neuLight.getGreen(), neuLight.getBlue(), 0.4),
            6, 0, -2, -2);
        fieldLight.setInput(fieldDark);

        DropShadow focusGlow = new DropShadow(BlurType.GAUSSIAN,
            Color.color(neuLight.getRed(), neuLight.getGreen(), neuLight.getBlue(), 0.6),
            12, 0.15, 0, 0);
        focusGlow.setInput(fieldLight);

        for (Node n : authCard.lookupAll(".auth-text-field")) {
            n.setStyle(fieldStyle);
            n.setEffect(fieldLight);
            n.focusedProperty().addListener((obs, old, focused) ->
                n.setEffect(focused ? focusGlow : fieldLight));
        }

        // Action buttons — raised neumorphic
        String actionBtnStyle =
            "-fx-background-color: " + rgba(neuLight, 0.35) + ", " +
                rgba(neuDark, 0.12) + ", " + rgba(darker, 0.5) + ";" +
            "-fx-background-insets: -3 3 3 -3, 3 -3 -3 3, 0;" +
            "-fx-background-radius: 15, 15, 15;" +
            "-fx-text-fill: " + hex(lighter.interpolate(Color.WHITE, 0.6)) + ";" +
            "-fx-border-color: transparent;";
        for (Node n : authCard.lookupAll(".login-btn")) n.setStyle(actionBtnStyle);
        for (Node n : authCard.lookupAll(".signup-btn")) n.setStyle(actionBtnStyle);

        // Back buttons — subtle raised neumorphic
        String backStyle =
            "-fx-background-color: " + rgba(neuLight, 0.2) + ", " +
                rgba(neuDark, 0.08) + ", " + rgba(base, 0.25) + ";" +
            "-fx-background-insets: -2 2 2 -2, 2 -2 -2 2, 0;" +
            "-fx-background-radius: 10, 10, 10;" +
            "-fx-text-fill: " + hex(textPrimary) + ";" +
            "-fx-border-color: transparent;";
        for (Node n : authCard.lookupAll(".auth-back-button")) n.setStyle(backStyle);

        // Error labels
        for (Node n : authCard.lookupAll(".error-label")) {
            n.setStyle(
                "-fx-background-color: rgba(255,59,92,0.08);" +
                "-fx-border-color: rgba(255,59,92,0.2);" +
                "-fx-text-fill: #ff3b5c;"
            );
        }

        // Scrollbar
        rootPane.applyCss();
        for (Node track : rootPane.lookupAll(".scroll-bar .track")) {
            track.setStyle("-fx-background-color: " + rgba(neuBase, 0.15) + ";" +
                "-fx-background-radius: 6;");
        }
        for (Node thumb : rootPane.lookupAll(".scroll-bar .thumb")) {
            thumb.setStyle("-fx-background-color: " + rgba(neuDark, 0.3) + ";" +
                "-fx-background-radius: 6;");
        }
    }

    @FXML
    public void initialize() {
        MusicManager mm = MusicManager.getInstance();
        if (mm.getCurrentTrack() == null) {
            mm.playRandomBGM();
        }
        PokeballOverlay pokeball = (PokeballOverlay) SceneManager.getData("pokeballOverlay");
        if (pokeball != null) {
            SceneManager.setData("pokeballOverlay", null);
            rootPane.getChildren().add(pokeball);
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(400));
            pause.setOnFinished(e -> PokeballOverlay.hideFrom(rootPane, pokeball, null));
            pause.play();
        }
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
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        buildPalette();
        applyDynamicTheme();

        if (loginUsernameField != null) {
            loginUsernameField.requestFocus();
        }

        MusicManager.getInstance().attachClickSounds(rootPane);
    }
    @FXML
    protected void onLoginTabClick() {
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        signupForm.setVisible(false);
        signupForm.setManaged(false);
        loginTabButton.getStyleClass().add("tab-button-active");
        signupTabButton.getStyleClass().remove("tab-button-active");
        clearAllLoginErrors();
        clearAllSignupErrors();
        loginUsernameField.requestFocus();
    }
    @FXML
    protected void onSignupTabClick() {
        signupForm.setVisible(true);
        signupForm.setManaged(true);
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        signupTabButton.getStyleClass().add("tab-button-active");
        loginTabButton.getStyleClass().remove("tab-button-active");
        clearAllLoginErrors();
        clearAllSignupErrors();
        signupUsernameField.requestFocus();
    }
    @FXML
    protected void onLoginButtonClick() {
        clearAllLoginErrors();
        
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();
        boolean hasErrors = false;
        
        if (username.isEmpty()) {
            showFieldError(loginUsernameError, "Username or email is required");
            hasErrors = true;
        }
        if (password.isEmpty()) {
            showFieldError(loginPasswordError, "Password is required");
            hasErrors = true;
        }
        
        if (hasErrors) return;
        AuthService.AuthResult result = authService.login(username, password);
        
        if (result.isSuccess()) {
            currentUser = result.getUser();
            PlayerSession.getInstance().setCurrentUser(currentUser);
            System.out.println("Login successful - User: " + currentUser.getUsername());
            SceneManager.switchSceneWithLoading("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
        } else {
            System.err.println("Login failed: " + result.getMessage());
            showLoginError(result.getMessage());
        }
    }
    @FXML
    protected void onSignupButtonClick() {
        clearAllSignupErrors();
        
        String username = signupUsernameField.getText().trim();
        String email = signupEmailField.getText().trim();
        String password = signupPasswordField.getText();
        String confirmPassword = signupConfirmPasswordField.getText();
        boolean hasErrors = false;
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
        if (email.isEmpty()) {
            showFieldError(signupEmailError, "Email is required");
            hasErrors = true;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showFieldError(signupEmailError, "Please enter a valid email address");
            hasErrors = true;
        }
        if (password.isEmpty()) {
            showFieldError(signupPasswordError, "Password is required");
            hasErrors = true;
        } else if (password.length() < 6) {
            showFieldError(signupPasswordError, "Password must be at least 6 characters");
            hasErrors = true;
        }
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
        AuthService.AuthResult result = authService.register(username, email, password);
        
        if (result.isSuccess()) {
            currentUser = result.getUser();
            PlayerSession.getInstance().setCurrentUser(currentUser);
            System.out.println("Registration successful - User: " + currentUser.getUsername());
            SceneManager.switchSceneWithLoading("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
        } else {
            System.err.println("Registration failed: " + result.getMessage());
            showSignupError(result.getMessage());
        }
    }
    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
        loginErrorLabel.setManaged(true);
    }
    private void showSignupError(String message) {
        signupErrorLabel.setText(message);
        signupErrorLabel.setVisible(true);
        signupErrorLabel.setManaged(true);
    }
    private void showFieldError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
    private void clearAllLoginErrors() {
        hideError(loginErrorLabel);
        hideError(loginUsernameError);
        hideError(loginPasswordError);
    }
    private void clearAllSignupErrors() {
        hideError(signupErrorLabel);
        hideError(signupUsernameError);
        hideError(signupEmailError);
        hideError(signupPasswordError);
        hideError(signupConfirmPasswordError);
    }
    private void hideError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            errorLabel.setText("");
        }
    }
    @FXML
    protected void onBackButtonClick() {
        SceneManager.switchSceneWithLoading("start.fxml", "Pokemon Battle - Start", 1200, 700);
    }
    public static User getCurrentUser() {
        return currentUser;
    }
    public static void logout() {
        currentUser = null;
    }
}
