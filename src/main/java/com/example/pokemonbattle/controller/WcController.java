package com.example.pokemonbattle.controller;

import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.service.AuthService;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.PokeballOverlay;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class WcController {
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
    @FXML
    private Region glassBlurLayer;
    @FXML
    private StackPane cardWrapper;
    @FXML
    private HBox splitContainer;
    @FXML
    private StackPane imagePanel;
    @FXML
    private ImageView formImage;
    @FXML
    private Region seamStrip;
    @FXML
    private VBox authCard;
    @FXML
    private HBox authTabsBox;
    @FXML
    private StackPane loginTabWrap;
    @FXML
    private StackPane signupTabWrap;
    @FXML
    private Button loginTabButton;
    @FXML
    private Button signupTabButton;
    // Login tab decoration lines
    @FXML private Line loginBorderBottom;
    @FXML private Line loginBorderTopLeft;
    @FXML private Line loginBorderTopRight;
    @FXML private Line loginBorderLeftTop;
    @FXML private Line loginBorderRightTop;
    // Signup tab decoration lines
    @FXML private Line signupBorderBottom;
    @FXML private Line signupBorderTopLeft;
    @FXML private Line signupBorderTopRight;
    @FXML private Line signupBorderLeftTop;
    @FXML private Line signupBorderRightTop;
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

    private final AuthService authService;
    private static User currentUser;
    private boolean isLoginMode = true;
    private boolean sliding = false;

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
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                count++;
            }
        }
        return new Color(r / count, g / count, b / count, 1.0);
    }

    private String rgba(Color c, double a) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255), a);
    }

    private String hex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private double luminance(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private void buildPalette() {
        if (bgImage == null || bgImage.getImage() == null)
            return;
        base = getAverageColor(bgImage.getImage());
        lighter = base.interpolate(Color.WHITE, 0.4);
        darker = base.interpolate(Color.BLACK, 0.45);
        darkest = base.interpolate(Color.BLACK, 0.7);
        boolean bright = luminance(base) > 0.45;
        textPrimary = bright ? darkest : Color.color(0.93, 0.93, 0.97);
        textSecondary = bright ? darker : Color.color(0.72, 0.74, 0.82);
        neuBase = base.desaturate();
        neuLight = neuBase.interpolate(Color.WHITE, 0.6);
        neuDark = neuBase.interpolate(Color.BLACK, 0.3);
    }

    private void applyDynamicTheme() {
        if (base == null)
            return;
        // 4-tier colour system
Color titleColor   = Color.web("#c8f0f7"); 
Color labelColor   = Color.web("#abf3eb");  
Color inputColor   = Color.web("#a8d6e8");  
Color buttonColor  = Color.web("#8bb7bf");   
        if (glassBlurLayer != null) {
            glassBlurLayer.setStyle(
                    "-fx-background-color: " + rgba(darkest, 0.55) + ";" +
                            "-fx-background-radius: 24;" +
                            "-fx-border-radius: 24;" +
                            "-fx-border-color: " + rgba(neuLight, 0.08) + ";" +
                            "-fx-border-width: 1;");
            // Offset shadow: visible on right and bottom edges, like a real card shadow
            DropShadow outerShadow = new DropShadow(BlurType.GAUSSIAN,
                    Color.color(0, 0, 0, 0.65), 18, 0.0, 7, 7);
            glassBlurLayer.setEffect(outerShadow);
        }

        authCard.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 0;");

        for (Node n : authCard.lookupAll(".auth-title")) {
            n.setStyle("-fx-text-fill: " + hex(titleColor) + ";");
        }

        if (authTabsBox != null) {
            authTabsBox.setStyle(
                    "-fx-border-color: " + rgba(neuLight, 0.1) + ";" +
                            "-fx-border-width: 0 0 1 0;" +
                            "-fx-padding: 0 0 10 0;");
        }

        // Tab buttons: fully transparent — line decoration handled by JavaFX Lines
        String tabStyle = "-fx-background-color: transparent;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: transparent;" +
                "-fx-border-width: 0;" +
                "-fx-text-fill: #a8e8ec;";
        loginTabButton.setStyle(tabStyle);
        signupTabButton.setStyle(tabStyle);

        for (Node n : authCard.lookupAll(".field-label")) {
            n.setStyle("-fx-text-fill: " + hex(labelColor) + ";");
        }

        Color underlineNormal = neuLight.interpolate(Color.GRAY, 0.3);
        Color underlineFocus = neuLight.interpolate(Color.web("#56ccf2"), 0.6);
        String fieldStyle = "-fx-font-family: 'SPACE NOVA';" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent transparent " + rgba(underlineNormal, 0.5) + " transparent;" +
                "-fx-border-width: 0 0 1.5 0;" +
                "-fx-border-radius: 0;" +
                "-fx-background-radius: 0;" +
                "-fx-text-fill: " + hex(inputColor) + ";" +
                "-fx-prompt-text-fill: " + rgba(labelColor, 0.35) + ";";

        String fieldFocusStyle = "-fx-font-family: 'SPACE NOVA';" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent transparent " + rgba(underlineFocus, 0.85) + " transparent;" +
                "-fx-border-width: 0 0 2 0;" +
                "-fx-border-radius: 0;" +
                "-fx-background-radius: 0;" +
                "-fx-text-fill: " + hex(inputColor) + ";" +
                "-fx-prompt-text-fill: " + rgba(labelColor, 0.35) + ";";

        DropShadow focusGlow = new DropShadow(BlurType.GAUSSIAN,
                Color.color(underlineFocus.getRed(), underlineFocus.getGreen(), underlineFocus.getBlue(), 0.5),
                10, 0.15, 0, 2);

        for (Node n : authCard.lookupAll(".auth-text-field")) {
            n.setStyle(fieldStyle);
            n.setEffect(null);
            n.focusedProperty().addListener((obs, old, focused) -> {
                n.setStyle(focused ? fieldFocusStyle : fieldStyle);
                n.setEffect(focused ? focusGlow : null);
            });
        }

        // Dark-teal gradient: login & sign-up buttons
        // Right edge (#1aa0a0) matches left edge of back button → seamless blend
        String actionBtnStyle =
                "-fx-background-color: " +
                        "linear-gradient(from 0% 0% to 0% 100%, rgba(255,255,255,0.10) 0%, transparent 55%), " +
                        "linear-gradient(from 0% 50% to 100% 50%, #093f55 0%, #0d7070 50%, #1aa0a0 100%);" +
                "-fx-background-insets: 0, 0;" +
                "-fx-background-radius: 8, 8;" +
                "-fx-text-fill: #e8fafa;" +
                "-fx-border-color: rgba(126,232,232,0.35);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;";
        for (Node n : authCard.lookupAll(".login-btn"))
            n.setStyle(actionBtnStyle);
        for (Node n : authCard.lookupAll(".signup-btn"))
            n.setStyle(actionBtnStyle);

        // Light-teal gradient: back button
        // Left edge (#1aa0a0) matches right edge of dark-teal buttons → seamless blend
        String backStyle =
                "-fx-background-color: " +
                        "linear-gradient(from 0% 0% to 0% 100%, rgba(255,255,255,0.10) 0%, transparent 55%), " +
                        "linear-gradient(from 0% 50% to 100% 50%, #a0dada 0%, #6adcdc 50%, #3cd8d8 100%);" +
                "-fx-background-insets: 0, 0;" +
                "-fx-background-radius: 8, 8;" +
                "-fx-text-fill: #e8fafa;" +
                "-fx-border-color: rgba(126,232,232,0.35);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;";
        for (Node n : authCard.lookupAll(".auth-back-button"))
            n.setStyle(backStyle);

        for (Node n : authCard.lookupAll(".error-label")) {
            n.setStyle(
                    "-fx-background-color: rgba(255,59,92,0.08);" +
                            "-fx-border-color: rgba(255,59,92,0.2);" +
                            "-fx-text-fill: #ff3b5c;");
        }

        if (seamStrip != null) {
            seamStrip.setStyle("-fx-background-color: rgba(0,0,0,0.3);");
        }
        if (imagePanel != null && formImage != null) {
            imagePanel.setStyle("-fx-background-color: " + rgba(darkest, 0.3) + ";");
            formImage.fitWidthProperty().bind(imagePanel.widthProperty());
            formImage.fitHeightProperty().bind(imagePanel.heightProperty());
            // Rounded clip for form.jpg — left side only (top-left, bottom-left corners)
            Rectangle imgClip = new Rectangle();
            imgClip.widthProperty().bind(imagePanel.widthProperty());
            imgClip.heightProperty().bind(imagePanel.heightProperty());
            imgClip.setArcWidth(48); // ← was 0, now rounds the left-side corners
            imgClip.setArcHeight(48);
            imagePanel.setClip(imgClip);
        }
        // Clip the whole cardWrapper so glass layer + image both respect the rounded boundary
        if (cardWrapper != null) {
            cardWrapper.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                Rectangle cardClip = new Rectangle(newBounds.getWidth(), newBounds.getHeight());
                cardClip.setArcWidth(48);
                cardClip.setArcHeight(48);
                cardWrapper.setClip(cardClip);
            });
        }
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
        setupInitialLayout();

        // Wire Line-based corner decorations for both tab buttons
        wireTabLines(loginTabWrap,
                loginBorderBottom, loginBorderTopLeft, loginBorderTopRight,
                loginBorderLeftTop, loginBorderRightTop);
        wireTabLines(signupTabWrap,
                signupBorderBottom, signupBorderTopLeft, signupBorderTopRight,
                signupBorderLeftTop, signupBorderRightTop);

        if (loginUsernameField != null) {
            loginUsernameField.requestFocus();
        }
        MusicManager.getInstance().attachClickSounds(rootPane);
    }

    /**
     * Attaches a layoutBounds listener to a tab StackPane so the 5 Line
     * decorations (underline + two short top-corner brackets) are always
     * pixel-perfect regardless of the actual rendered size.
     */
    private void wireTabLines(StackPane wrap,
                              Line bottom,
                              Line topLeft, Line topRight,
                              Line leftTop,  Line rightTop) {
        for (Line l : new Line[]{bottom, topLeft, topRight, leftTop, rightTop}) {
            l.setManaged(false);       // don't affect layout
            l.setStrokeWidth(1.8);
        }
        wrap.layoutBoundsProperty().addListener((obs, oldV, bounds) -> {
            double w = bounds.getWidth();
            double h = bounds.getHeight();
            double arm = Math.max(12, Math.min(w, h) * 0.28); // ~28 % of shorter side

            // Full-width underline
            bottom.setStartX(0);   bottom.setEndX(w);
            bottom.setStartY(h - 1); bottom.setEndY(h - 1);

            // Top-left horizontal arm
            topLeft.setStartX(0);   topLeft.setEndX(arm);
            topLeft.setStartY(0);   topLeft.setEndY(0);

            // Top-right horizontal arm
            topRight.setStartX(w - arm); topRight.setEndX(w);
            topRight.setStartY(0);       topRight.setEndY(0);

            // Left vertical arm (downward from top-left corner)
            leftTop.setStartX(0); leftTop.setEndX(0);
            leftTop.setStartY(0); leftTop.setEndY(arm);

            // Right vertical arm (downward from top-right corner)
            rightTop.setStartX(w); rightTop.setEndX(w);
            rightTop.setStartY(0); rightTop.setEndY(arm);
        });
    }

    private void setupInitialLayout() {
        isLoginMode = true;
        setImageLeft();
    }

    private void setImageLeft() {
        splitContainer.getChildren().clear();
        splitContainer.getChildren().addAll(imagePanel, seamStrip, authCard);
    }

    private void setImageRight() {
        splitContainer.getChildren().clear();
        splitContainer.getChildren().addAll(authCard, seamStrip, imagePanel);
    }

    private void slideTransition(boolean toSignup) {
        if (sliding)
            return;
        sliding = true;

        // Determine how far the image panel needs to slide to cover the form.
        // authCard.getWidth() is the form panel's actual layout width.
        double formWidth = authCard.getWidth() > 10
                ? authCard.getWidth()
                : cardWrapper.getWidth() * 0.52;
        double slideX = toSignup ? formWidth : -formWidth;

        // Reset any leftover transforms
        imagePanel.setTranslateX(0);
        authCard.setTranslateX(0);
        authCard.setOpacity(1.0);
        imagePanel.setOpacity(1.0);

        // ── Phase 1: form fades out; image slides over it ──────────────────
        Timeline phase1 = new Timeline(
                new KeyFrame(Duration.millis(340),
                        new KeyValue(authCard.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(imagePanel.translateXProperty(), slideX, Interpolator.EASE_BOTH)));

        phase1.setOnFinished(e -> {
            // Swap sides in the HBox (invisible because authCard is at opacity 0)
            if (toSignup)
                setImageRight();
            else
                setImageLeft();
            // Clear the translate — image is now in its correct physical slot
            imagePanel.setTranslateX(0);
            authCard.setOpacity(0.0);

            // ── Phase 2: form fades back in on the opposite side ───────────
            Timeline phase2 = new Timeline(
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(authCard.opacityProperty(), 1.0, Interpolator.EASE_OUT)));
            phase2.setOnFinished(ev -> sliding = false);
            phase2.play();
        });

        phase1.play();
    }

    @FXML
    protected void onLoginTabClick() {
        if (isLoginMode)
            return;
        isLoginMode = true;
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        signupForm.setVisible(false);
        signupForm.setManaged(false);
        loginTabButton.getStyleClass().add("tab-button-active");
        signupTabButton.getStyleClass().remove("tab-button-active");
        if (loginTabWrap != null)  loginTabWrap.getStyleClass().add("tab-button-wrap-active");
        if (signupTabWrap != null) signupTabWrap.getStyleClass().remove("tab-button-wrap-active");
        clearAllLoginErrors();
        clearAllSignupErrors();
        slideTransition(false);
        loginUsernameField.requestFocus();
    }

    @FXML
    protected void onSignupTabClick() {
        if (!isLoginMode)
            return;
        isLoginMode = false;
        signupForm.setVisible(true);
        signupForm.setManaged(true);
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        signupTabButton.getStyleClass().add("tab-button-active");
        loginTabButton.getStyleClass().remove("tab-button-active");
        if (signupTabWrap != null) signupTabWrap.getStyleClass().add("tab-button-wrap-active");
        if (loginTabWrap != null)  loginTabWrap.getStyleClass().remove("tab-button-wrap-active");
        clearAllLoginErrors();
        clearAllSignupErrors();
        slideTransition(true);
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
        if (hasErrors)
            return;
        AuthService.AuthResult result = authService.login(username, password);
        if (result.isSuccess()) {
            currentUser = result.getUser();
            PlayerSession.getInstance().setCurrentUser(currentUser);
            SceneManager.switchSceneWithLoading("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
        } else {
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
        if (hasErrors)
            return;
        AuthService.AuthResult result = authService.register(username, email, password);
        if (result.isSuccess()) {
            currentUser = result.getUser();
            PlayerSession.getInstance().setCurrentUser(currentUser);
            SceneManager.switchSceneWithLoading("menu.fxml", "Pokemon Battle - Menu", 1200, 700);
        } else {
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