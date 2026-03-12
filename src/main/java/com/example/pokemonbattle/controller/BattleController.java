package com.example.pokemonbattle.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.pokemonbattle.database.DatabaseManager;
import com.example.pokemonbattle.model.Battle;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.util.MusicManager;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class BattleController implements Battle.BattleListener {

    // Existing battle UI
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView bgImage;
    @FXML
    private StackPane battleSection;
    @FXML
    private AnchorPane battleField;
    @FXML
    private StackPane optionsSection;
    @FXML
    private ImageView playerSpriteImage;
    @FXML
    private ImageView opponentSpriteImage;
    @FXML
    private Label playerPokemonNameLabel;
    @FXML
    private Label playerPokemonHpLabel;
    @FXML
    private Label opponentPokemonNameLabel;
    @FXML
    private Label opponentPokemonHpLabel;
    @FXML
    private Rectangle playerHpBar;
    @FXML
    private Rectangle opponentHpBar;
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label playerTeamLabel;
    @FXML
    private VBox playerPokemonBox;
    @FXML
    private Label opponentNameLabel;
    @FXML
    private Label opponentTeamLabel;
    @FXML
    private VBox opponentPokemonBox;
    @FXML
    private Label battleStatusLabel;
    @FXML
    private Button startBattleButton;
    @FXML
    private Button backButton;
    @FXML
    private Button moveButton1;
    @FXML
    private Button moveButton2;
    @FXML
    private Button moveButton3;
    @FXML
    private Button moveButton4;
    @FXML
    private Button attackButton;
    @FXML
    private Button changePokemonMainButton;
    @FXML
    private Button itemsButton;
    @FXML
    private VBox moveSelectionBox;
    @FXML
    private VBox pokemonSelectionBox;
    @FXML
    private VBox pokemonButtonsBox;
    @FXML
    private VBox actionButtonsBox;

    /** Wrapper HBox for the main battle UI — hidden until VS intro ends. */
    @FXML
    private HBox mainBattleLayout;

    // Battle Result Overlay nodes
    @FXML
    private StackPane battleResultOverlay;
    @FXML
    private VBox battleResultCard;
    @FXML
    private Label resultTitleLabel;
    @FXML
    private Label resultMessageLabel;
    @FXML
    private Button goBackResultButton;
    @FXML
    private Button battleAgainResultButton;

    // VS Intro Screen node
    @FXML
    private AnchorPane vsScreenPane;
    @FXML
    private ImageView vsBgImage;
    @FXML
    private ImageView vsPlayerSprite;
    @FXML
    private ImageView vsOpponentSprite;

    /** Full-screen black Region that sits at rootPane level for the cross-fade. */
    @FXML
    private Region rootBlackFade;

    // VS Intro Animation Constants
    // All values are intentionally named and extracted so you can tweak them.

    /**
     * px from the screen edge where each trainer sprite comes to rest (~6–7 mm at
     * 96 dpi).
     */
    private static final double VS_SLIDE_STOP_OFFSET = 25.0;

    /**
     * Total px each sprite drifts during the hold phase (toward its exit
     * direction).
     */
    private static final double VS_DRIFT_AMOUNT = 35.0;

    /** Duration (ms) of the fast slide-in. */
    private static final double VS_SLIDE_IN_MS = 320.0;

    /**
     * Total hold time (ms) after slide-in — sprites are visible and drifting.
     * The slide-out begins VS_SLIDE_OUT_EARLY_MS before this window closes.
     */
    private static final double VS_DRIFT_HOLD_MS = 2700.0;

    /**
     * How many ms before the end of VS_DRIFT_HOLD_MS the slide-out starts.
     * E.g. 650 → sprites exit 650 ms before the hold window would have ended.
     */
    private static final double VS_SLIDE_OUT_EARLY_MS = 500.0;

    /** Duration (ms) of the fast slide-out. */
    private static final double VS_SLIDE_OUT_MS = 280.0;

    /** Duration (ms) of the black overlay fade-in (VS screen → black). */
    private static final double VS_FADE_TO_BLACK_MS = 420.0;

    /** Duration (ms) of the black overlay fade-out (black → battle screen). */
    private static final double VS_FADE_FROM_BLACK_MS = 380.0;

    /**
     * How far off-screen (px) the sprites start / end.
     * Must comfortably exceed the scene width (1200 px default).
     */
    private static final double VS_OFFSCREEN_OFFSET = 700.0;

    /**
     * Number of NPC trainer sprites available in sprites/trainer/npc/ (1.png …
     * N.png).
     */
    private static final int VS_NPC_COUNT = 7;

    // Battle model
    private Player player;
    private Player opponent;
    private Battle battle;
    private boolean randomTeam;
    private static final double HP_BAR_MAX_WIDTH = 180.0;

    // Confetti animation state
    private Canvas confettiCanvas;
    private AnimationTimer confettiTimer;

    // Battle log for history persistence
    private final java.util.List<String> battleLog = new java.util.ArrayList<>();

    // ── Sprite scaling constants ─────────────────────────────────────────────
    /** The Pokemon height (in metres) that maps to the "standard" pixel size. */
    private static final double SPRITE_STANDARD_HEIGHT_M = 1.0;

    /** Base pixel height for the opponent sprite at standard height. */
    private static final double SPRITE_OPPONENT_BASE_PX = 200.0;

    /** Base pixel height for the player sprite at standard height. */
    private static final double SPRITE_PLAYER_BASE_PX = 300.0;

    /** Minimum rendered sprite height in px — prevents tiny Pokemon vanishing. */
    private static final double SPRITE_MIN_PX = 130.0;

    /** Maximum rendered sprite height in px — prevents huge Pokemon overflowing. */
    private static final double SPRITE_MAX_PX = 380.0;

    /**
     * Exponent applied to the height ratio — values below 1.0 compress size
     * differences (0.5 = square-root curve, gentler scaling for large Pokemon).
     */
    private static final double SPRITE_SCALE_EXPONENT = 0.5;
    // Loaded once from resources/pokemon_heights.json — maps pokemonId → height in metres
    private static final java.util.Map<Integer, Double> POKEMON_HEIGHTS = loadPokemonHeights();
    private static java.util.Map<Integer, Double> loadPokemonHeights() {
        java.util.Map<Integer, Double> map = new java.util.HashMap<>();
        try (var stream = BattleController.class.getResourceAsStream(
                    "/com/example/pokemonbattle/data/pokemon_heights.json")) {
            if (stream == null) {
                System.err.println("[BattleController] pokemon_heights.json not found — using default sizes");
                return map;
            }
            String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // JSON structure per entry (each field on its own line):
            //   "1": {
            //     "id": 1,
            //     "name": "bulbasaur",
            //     "height": 0.7
            //   },
            // Strategy: track the most recently seen "id" value, then pair it
            // with the next "height" value we encounter.
            int currentId = -1;
            java.util.regex.Matcher idMatcher =
                java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
            java.util.regex.Matcher hMatcher =
                java.util.regex.Pattern.compile("\"height\"\\s*:\\s*([\\d.]+)").matcher(json);
            // Collect all id positions and height positions, then pair them up
            java.util.List<long[]> ids     = new java.util.ArrayList<>(); // [charPos, id]
            java.util.List<long[]> heights = new java.util.ArrayList<>(); // [charPos, heightBits]

            while (idMatcher.find()) {
                ids.add(new long[]{ idMatcher.start(), Long.parseLong(idMatcher.group(1)) });
            }
            while (hMatcher.find()) {
                heights.add(new long[]{ hMatcher.start(),
                    Double.doubleToLongBits(Double.parseDouble(hMatcher.group(1))) });
            }

            // Each id appears just before its height in the file — pair greedily
            int hi = 0;
            for (long[] idEntry : ids) {
                long idPos = idEntry[0];
                int  id    = (int) idEntry[1];
                // Find the first height that comes after this id's position
                while (hi < heights.size() && heights.get(hi)[0] < idPos) hi++;
                if (hi < heights.size()) {
                    double height = Double.longBitsToDouble(heights.get(hi)[1]);
                    map.put(id, height);
                    hi++; // consume this height
                }
            }

            System.out.println("[BattleController] Loaded heights for " + map.size() + " Pokemon");
        } catch (Exception e) {
            System.err.println("[BattleController] Failed to load pokemon_heights.json: " + e.getMessage());
        }
        return map;
    }
    /**
     * Returns a scaled pixel size for a sprite based on the Pokemon's real height.
     * A Pokemon of SPRITE_STANDARD_HEIGHT_M gets exactly basePx pixels.
     * Clamped to [SPRITE_MIN_PX, SPRITE_MAX_PX].
     */
    private double getScaledSpritePx(int pokemonId, double basePx) {
        Double heightM = POKEMON_HEIGHTS.get(pokemonId);
        if (heightM == null || heightM <= 0) return basePx; // unknown → default
        double ratio  = Math.pow(heightM / SPRITE_STANDARD_HEIGHT_M, SPRITE_SCALE_EXPONENT);
        double scaled = basePx * ratio;
        double result = Math.max(SPRITE_MIN_PX, Math.min(SPRITE_MAX_PX, scaled));
        System.out.printf("[Sprite] id=%d height=%.1fm → %.0fpx%n", pokemonId, heightM, result);
        return result;
    }
    // INITIALISATION
    @FXML
    public void initialize() {
        // Background image fills the scene
        if (bgImage != null && rootPane != null) {
            bgImage.fitWidthProperty().bind(rootPane.widthProperty());
            bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        // VS background fills the VS pane (same dimensions as rootPane)
        if (vsBgImage != null && rootPane != null) {
            vsBgImage.fitWidthProperty().bind(rootPane.widthProperty());
            vsBgImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        // Wire up existing battle buttons
        startBattleButton.setOnAction(e -> onBack());
        backButton.setOnAction(e -> onBack());
        attackButton.setOnAction(e -> onAttackClicked());
        changePokemonMainButton.setOnAction(e -> onChangePokemonClicked());
        if (itemsButton != null) {
            itemsButton.setOnAction(e -> onItemsClicked());
        }

        moveButton1.setDisable(true);
        moveButton2.setDisable(true);
        moveButton3.setDisable(true);
        moveButton4.setDisable(true);

        // Load battle data and prepare the options-panel background pattern
        // (these are fast operations — both run before the VS intro plays)
        loadBattleData();
        drawOptionsPanelPattern();
        SceneManager.enableCoordDebug(rootPane);
        MusicManager.getInstance().attachClickSounds(rootPane);

        // Kick off the VS intro on the next frame so the scene is fully laid out
        Platform.runLater(this::playVSIntro);
    }

    // VS INTRO — play on scene entry, then reveal main battle UI
    /**
     * Runs the full VS intro sequence:
     *
     * 1. Both trainer sprites slide in fast from opposite edges.
     * 2. Sprites hold on screen and drift slowly toward the centre.
     * 3. ~VS_SLIDE_OUT_EARLY_MS ms before the hold ends both sprites exit
     * fast in the same direction they were drifting.
     * 4. The black root-overlay fades to opaque.
     * 5. Main battle layout is revealed; black overlay fades back out.
     */
    private void playVSIntro() {
        // Load trainer sprites
        // Random NPC opponent (sprites/trainer/npc/1.png … N.png)
        int npcId = new Random().nextInt(VS_NPC_COUNT) + 1;
        String npcPath = "/com/example/pokemonbattle/sprites/trainer/npc/" + npcId + ".png";
        var npcUrl = getClass().getResource(npcPath);
        if (npcUrl != null) {
            vsOpponentSprite.setImage(new Image(npcUrl.toExternalForm(), 0, 0, true, true));
        }

        // Player trainer (avatar chosen in new-game screen)
        String avatarPath = PlayerSession.getInstance().getAvatarPath();
        if (avatarPath != null) {
            var avatarUrl = getClass().getResource(avatarPath);
            if (avatarUrl != null) {
                vsPlayerSprite.setImage(new Image(avatarUrl.toExternalForm(), 0, 0, true, true));
            }
        }
        // vsPlayerSprite has AnchorPane.leftAnchor = 0
        // translateX = -VS_OFFSCREEN_OFFSET → sprite is off-screen LEFT
        // translateX = +VS_SLIDE_STOP_OFFSET → left edge is STOP_OFFSET px from left
        // border
        // exits via large positive translateX → off-screen RIGHT
        //
        // vsOpponentSprite has AnchorPane.rightAnchor = 0
        // translateX = +VS_OFFSCREEN_OFFSET → sprite is off-screen RIGHT
        // translateX = -VS_SLIDE_STOP_OFFSET → right edge is STOP_OFFSET px from right
        // border
        // exits via large negative translateX → off-screen LEFT

        vsPlayerSprite.setTranslateX(-VS_OFFSCREEN_OFFSET);
        vsOpponentSprite.setTranslateX(VS_OFFSCREEN_OFFSET);

        // Phase 1: Slide in (fast, ease-out so they feel weighty)
        TranslateTransition playerSlideIn = new TranslateTransition(
                Duration.millis(VS_SLIDE_IN_MS), vsPlayerSprite);
        playerSlideIn.setToX(VS_SLIDE_STOP_OFFSET);
        playerSlideIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition opponentSlideIn = new TranslateTransition(
                Duration.millis(VS_SLIDE_IN_MS), vsOpponentSprite);
        opponentSlideIn.setToX(-VS_SLIDE_STOP_OFFSET);
        opponentSlideIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition slideIn = new ParallelTransition(playerSlideIn, opponentSlideIn);

        // Phase 2: Slow drift (sprites inch toward each other
        // Duration is shorter than VS_DRIFT_HOLD_MS by VS_SLIDE_OUT_EARLY_MS
        // so the slide-out starts before the "expected" hold window closes.
        double driftMs = VS_DRIFT_HOLD_MS - VS_SLIDE_OUT_EARLY_MS;

        TranslateTransition playerDrift = new TranslateTransition(
                Duration.millis(driftMs), vsPlayerSprite);
        playerDrift.setByX(VS_DRIFT_AMOUNT); // drifts right
        playerDrift.setInterpolator(Interpolator.LINEAR);

        TranslateTransition opponentDrift = new TranslateTransition(
                Duration.millis(driftMs), vsOpponentSprite);
        opponentDrift.setByX(-VS_DRIFT_AMOUNT); // drifts left
        opponentDrift.setInterpolator(Interpolator.LINEAR);

        ParallelTransition drift = new ParallelTransition(playerDrift, opponentDrift);

        // Phase 3: Slide out fast (ease-in so they feel like they launch)
        // Player exits RIGHT (same direction it was drifting).
        // Opponent exits LEFT (same direction it was drifting).
        // setByX uses a large enough value to clear any screen width.
        TranslateTransition playerSlideOut = new TranslateTransition(
                Duration.millis(VS_SLIDE_OUT_MS), vsPlayerSprite);
        playerSlideOut.setByX(VS_OFFSCREEN_OFFSET * 2.2);
        playerSlideOut.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition opponentSlideOut = new TranslateTransition(
                Duration.millis(VS_SLIDE_OUT_MS), vsOpponentSprite);
        opponentSlideOut.setByX(-VS_OFFSCREEN_OFFSET * 2.2);
        opponentSlideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition slideOut = new ParallelTransition(playerSlideOut, opponentSlideOut);

        // Wire up the full sequenc
        SequentialTransition vsSequence = new SequentialTransition(slideIn, drift, slideOut);

        vsSequence.setOnFinished(e -> fadeToBlackAndRevealBattle());

        vsSequence.play();
    }

    /**
     * Fades the root black overlay to opaque, swaps VS screen for the main
     * battle layout, then fades the overlay back to transparent.
     */
    private void fadeToBlackAndRevealBattle() {
        // Make sure the overlay is visible (it starts at opacity 0)
        rootBlackFade.setOpacity(0.0);
        rootBlackFade.setVisible(true);

        FadeTransition fadeToBlack = new FadeTransition(
                Duration.millis(VS_FADE_TO_BLACK_MS), rootBlackFade);
        fadeToBlack.setToValue(1.0);

        fadeToBlack.setOnFinished(e -> {
            // Swap screens while fully black — no jarring pop
            vsScreenPane.setVisible(false);
            vsScreenPane.setManaged(false);
            mainBattleLayout.setVisible(true);
            mainBattleLayout.setManaged(true);

            // Auto-start the battle while screen is still black so sprites
            // are loaded by the time the fade-in completes.
            onStartBattle();

            // Fade back in to reveal the battle scene
            FadeTransition fadeFromBlack = new FadeTransition(
                    Duration.millis(VS_FADE_FROM_BLACK_MS), rootBlackFade);
            fadeFromBlack.setToValue(0.0);
            fadeFromBlack.setOnFinished(ev -> {
                // Cleanup: make overlay non-interactive once invisible
                rootBlackFade.setVisible(false);
            });
            fadeFromBlack.play();
        });

        fadeToBlack.play();
    }

    // OPTIONS PANEL BACKGROUND PATTERN
    private void drawOptionsPanelPattern() {
        if (optionsSection == null)
            return;
        optionsSection.widthProperty().addListener((obs, o, n) -> repaintPattern());
        optionsSection.heightProperty().addListener((obs, o, n) -> repaintPattern());
        repaintPattern();
    }

    private void repaintPattern() {
        double w = optionsSection.getWidth();
        double h = optionsSection.getHeight();
        if (w <= 0 || h <= 0)
            return;

        optionsSection.getChildren().removeIf(n -> "patternCanvas".equals(n.getId()));

        Canvas canvas = new Canvas(w, h);
        canvas.setId("patternCanvas");
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#5cbdb0")),
                new javafx.scene.paint.Stop(0.5, Color.web("#3a9e8f")),
                new javafx.scene.paint.Stop(1, Color.web("#2d8a7c"))));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.color(1, 1, 1, 0.08));
        gc.setLineWidth(12);
        for (double i = -h; i < w + h; i += 36) {
            gc.strokeLine(i, 0, i + h, h);
        }

        gc.setStroke(Color.color(1, 1, 1, 0.06));
        gc.setLineWidth(2);
        double spacing = 80;
        for (double y = spacing / 2; y < h; y += spacing) {
            for (double x = spacing / 2; x < w; x += spacing) {
                gc.strokeOval(x - 14, y - 14, 28, 28);
                gc.strokeLine(x - 14, y, x + 14, y);
            }
        }

        optionsSection.getChildren().addFirst(canvas);
    }

    // BATTLE DATA LOADING
    private void loadBattleData() {
        player = (Player) SceneManager.getData("player");
        opponent = (Player) SceneManager.getData("opponent");
        Object rt = SceneManager.getData("randomTeam");
        randomTeam = (rt instanceof Boolean b) && b;

        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Battle data not found!");
            return;
        }

        playerNameLabel.setText(player.getName());
        displayPlayerTeam();
        opponentNameLabel.setText(opponent.getName());
        displayOpponentTeam();
        battleStatusLabel.setText("Teams loaded! Ready to battle.");

        System.out.println("\n=== BATTLE SCREEN LOADED ===");
        System.out.println("Player: " + player.getName() + " vs " + opponent.getName());
    }

    private void displayPlayerTeam() {
        playerPokemonBox.getChildren().clear();
        StringBuilder teamInfo = new StringBuilder("Team Size: " + player.getTeam().size() + "\n");
        for (PokemonInstance pokemon : player.getTeam()) {
            teamInfo.append("• ").append(capitalize(pokemon.getName()))
                    .append(" (Lv.").append(pokemon.getLevel()).append(")\n");
        }
        playerTeamLabel.setText(teamInfo.toString().trim());
    }

    private void displayOpponentTeam() {
        opponentPokemonBox.getChildren().clear();
        StringBuilder teamInfo = new StringBuilder("Team Size: " + opponent.getTeam().size() + "\n");
        for (PokemonInstance pokemon : opponent.getTeam()) {
            teamInfo.append("• ").append(capitalize(pokemon.getName()))
                    .append(" (Lv.").append(pokemon.getLevel()).append(")\n");
        }
        opponentTeamLabel.setText(teamInfo.toString().trim());
    }

    // BATTLE START & DISPLAY
    private void onStartBattle() {
        if (player == null || opponent == null) {
            battleStatusLabel.setText("Error: Teams not loaded!");
            return;
        }
        battle = new Battle(player, opponent);
        battle.addListener(this);
        updateBattleDisplay();
        showActionButtons();
        battleStatusLabel.setText("Battle Started! " +
                capitalize(player.getCurrentPokemon().getName()) +
                " vs " + capitalize(opponent.getCurrentPokemon().getName()));
    }

    private void updateBattleDisplay() {
        if (player == null || opponent == null)
            return;

        PokemonInstance playerPok = player.getCurrentPokemon();
        PokemonInstance opponentPok = opponent.getCurrentPokemon();

        // Animated GIF sprites with PNG fallback
        double playerPx   = getScaledSpritePx(playerPok.getId(),   SPRITE_PLAYER_BASE_PX);
        double opponentPx = getScaledSpritePx(opponentPok.getId(), SPRITE_OPPONENT_BASE_PX);
        playerSpriteImage.setFitWidth(playerPx);
        playerSpriteImage.setFitHeight(playerPx);
        opponentSpriteImage.setFitWidth(opponentPx);
        opponentSpriteImage.setFitHeight(opponentPx);
        loadSpriteWithFallback(playerSpriteImage, playerPok.getId(), "back");
        loadSpriteWithFallback(opponentSpriteImage, opponentPok.getId(), "front");

        playerPokemonNameLabel.setText(capitalize(playerPok.getName()) + "  Lv." + playerPok.getLevel());
        playerPokemonHpLabel.setText(playerPok.getCurrentHp() + " / " + playerPok.getMaxHp());
        opponentPokemonNameLabel.setText(capitalize(opponentPok.getName()) + "  Lv." + opponentPok.getLevel());
        opponentPokemonHpLabel.setText(opponentPok.getCurrentHp() + " / " + opponentPok.getMaxHp());

        updateHpBar(playerHpBar, playerPok.getCurrentHp(), playerPok.getMaxHp());
        updateHpBar(opponentHpBar, opponentPok.getCurrentHp(), opponentPok.getMaxHp());
    }

    /**
     * Loads an animated GIF sprite (sprites/{dir}/gif/{id}.gif) for the battle
     * field ImageViews. Falls back to the static PNG if no GIF exists.
     * JavaFX animates GIF images natively via ImageView — no Timeline needed.
     */
    private void loadSpriteWithFallback(ImageView target, int pokemonId, String direction) {
        String gifPath = String.format(
                "/com/example/pokemonbattle/sprites/%s/gif/%d.gif", direction, pokemonId);
        var gifUrl = getClass().getResource(gifPath);
        if (gifUrl != null) {
            try {
                Image gifImage = new Image(gifUrl.toExternalForm(),
                        target.getFitWidth() > 0 ? target.getFitWidth() : 0,
                        target.getFitHeight() > 0 ? target.getFitHeight() : 0,
                        true, true, true); // backgroundLoading = true
                if (!gifImage.isError()) {
                    target.setImage(gifImage);
                    return;
                }
            } catch (Exception e) {
                System.err.println("GIF load error (" + gifPath + "): " + e.getMessage());
            }
        }

        // PNG fallback
        String pngPath = String.format(
                "/com/example/pokemonbattle/sprites/%s/%d.png", direction, pokemonId);
        try {
            var pngStream = getClass().getResourceAsStream(pngPath);
            if (pngStream != null) {
                Image pngImage = new Image(pngStream);
                if (!pngImage.isError())
                    target.setImage(pngImage);
            }
        } catch (Exception e) {
            System.err.println("PNG fallback error (" + pngPath + "): " + e.getMessage());
        }
    }

    private void updateHpBar(Rectangle hpBar, int currentHp, int maxHp) {
        if (hpBar == null || maxHp <= 0)
            return;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        hpBar.setWidth(HP_BAR_MAX_WIDTH * ratio);
        if (ratio > 0.5)
            hpBar.setFill(Color.web("#78C850"));
        else if (ratio > 0.2)
            hpBar.setFill(Color.web("#F8D030"));
        else
            hpBar.setFill(Color.web("#F85888"));
    }

    // MOVE BUTTONS
    private void updateMoveButtons() {
        PokemonInstance currentPok = player.getCurrentPokemon();
        var moves = currentPok.getBattleMoves();
        Button[] moveButtons = { moveButton1, moveButton2, moveButton3, moveButton4 };

        for (int i = 0; i < moveButtons.length; i++) {
            if (i < moves.size()) {
                Move move = moves.get(i).getMove();
                int pp = moves.get(i).getCurrentPp();
                moveButtons[i].setText(capitalize(move.getName()) + "\nPP: " + pp);
                moveButtons[i].setDisable(false);
                moveButtons[i].setOnAction(e -> onMoveSelected(move));
            } else {
                moveButtons[i].setText("---");
                moveButtons[i].setDisable(true);
                moveButtons[i].setOnAction(null);
            }
        }
    }

    private void onMoveSelected(Move move) {
        if (battle.isFinished()) {
            battleStatusLabel.setText("Battle is already finished!");
            return;
        }
        battleStatusLabel.setText("Used " + capitalize(move.getName()) + "!");
        disableMoveButtons();

        Move aiMove = battle.getAIMove(opponent.getCurrentPokemon(), player.getCurrentPokemon());
        battle.executeRound(move, aiMove);
        updateBattleDisplay();
        updateMoveButtons();

        if (!battle.isFinished()) {
            showActionButtons();
        }
        // If battle finished, the onBattleEnd listener handles UI.
    }

    private void disableMoveButtons() {
        moveButton1.setDisable(true);
        moveButton2.setDisable(true);
        moveButton3.setDisable(true);
        moveButton4.setDisable(true);
    }

    // ACTION / PANEL SWITCHING
    private void onBack() {
        SceneManager.clearData();
        SceneManager.switchSceneWithLoading("new_game.fxml", "Pokemon Battle - Setup", 1200, 700);
    }

    private void onAttackClicked() {
        if (battle == null || battle.isFinished()) {
            battleStatusLabel.setText("Start the battle first!");
            return;
        }
        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
        moveSelectionBox.setVisible(true);
        moveSelectionBox.setManaged(true);
        updateMoveButtons();
    }

    private void onChangePokemonClicked() {
        if (battle == null || battle.isFinished()) {
            battleStatusLabel.setText("Start the battle first!");
            return;
        }
        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);
        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        pokemonSelectionBox.setVisible(true);
        pokemonSelectionBox.setManaged(true);
        updatePokemonButtons();
    }

    private void onItemsClicked() {
        battleStatusLabel.setText("No items available in this battle.");
    }

    @FXML
    private void onBackToActions() {
        showActionButtons();
    }

    private void updatePokemonButtons() {
        pokemonButtonsBox.getChildren().clear();
        for (PokemonInstance pokemon : player.getTeam()) {
            Button pokemonBtn = new Button(capitalize(pokemon.getName()) +
                    (pokemon.isFainted() ? " (Fainted)" : "  Lv." + pokemon.getLevel()));
            pokemonBtn.setPrefWidth(260);
            pokemonBtn.setPrefHeight(42);
            pokemonBtn.getStyleClass().addAll("option-btn", "option-btn-green");
            pokemonBtn.setStyle("-fx-font-size: 13px;");

            if (pokemon.isFainted()) {
                pokemonBtn.setDisable(true);
                pokemonBtn.setStyle("-fx-font-size: 13px; -fx-opacity: 0.5;");
            } else if (pokemon.getId() == player.getCurrentPokemon().getId()) {
                pokemonBtn.setText(pokemonBtn.getText() + " ✓");
                pokemonBtn.setDisable(true);
            } else {
                pokemonBtn.setOnAction(e -> onPokemonSelected(pokemon));
            }
            pokemonButtonsBox.getChildren().add(pokemonBtn);
        }
    }

    private void onPokemonSelected(PokemonInstance pokemon) {
        player.setCurrentPokemon(pokemon);
        battleStatusLabel.setText("Go, " + capitalize(pokemon.getName()) + "!");
        updateBattleDisplay();
        showActionButtons();
    }

    private void showActionButtons() {
        actionButtonsBox.setVisible(true);
        actionButtonsBox.setManaged(true);
        moveSelectionBox.setVisible(false);
        moveSelectionBox.setManaged(false);
        pokemonSelectionBox.setVisible(false);
        pokemonSelectionBox.setManaged(false);
    }

    // BATTLE LISTENER CALLBACKS
    @Override
    public void onDamageDealt(String attacker, String move, String defender, int damage) {
        System.out.println(attacker + " used " + move + " on " + defender + " for " + damage + " damage!");
        String entry = capitalize(attacker) + " used " + capitalize(move) + "! " + damage + " dmg!";
        battleStatusLabel.setText(entry);
        battleLog.add(entry);
    }

    @Override
    public void onPokemonFainted(String pokemonName) {
        System.out.println(pokemonName + " fainted!");
        String entry = capitalize(pokemonName) + " fainted!";
        battleStatusLabel.setText(entry);
        battleLog.add(entry);
    }

    @Override
    public void onPokemonSwitched(String playerName, String pokemonName) {
        System.out.println(playerName + " sent out " + pokemonName + "!");
        String entry = playerName + " sent out " + capitalize(pokemonName) + "!";
        battleStatusLabel.setText(entry);
        battleLog.add(entry);
        if (playerName.equals(player.getName())) {
            displayPlayerTeam();
        } else {
            displayOpponentTeam();
        }
        updateBattleDisplay();
        updateMoveButtons();
    }

    @Override
    public void onBattleEnd(String winnerName) {
        System.out.println("\n=== BATTLE END ===");
        System.out.println("Winner: " + winnerName);

        boolean playerWon = winnerName.equals(player.getName());

        // Persist result to database in background
        saveBattleResult(playerWon);

        // Victory music (stop BGM, play victory SFX) — only on win
        if (playerWon) {
            MusicManager.getInstance().stopBGM();
            MusicManager.getInstance().playVictorySFX();
        }

        // Show result overlay on FX thread (listener may come from battle thread)
        Platform.runLater(() -> showResultOverlay(playerWon));
    }

    // RESULT OVERLAY

    private void showResultOverlay(boolean playerWon) {
        // Confetti only for victory
        if (playerWon)
            startConfetti();

        // Text
        resultTitleLabel.setText(playerWon ? "Victory!" : "Defeat...");
        resultMessageLabel.setText(playerWon
                ? "Congratulations! You won against " + opponent.getName() + "!"
                : "You lost against " + opponent.getName() + ". Better luck next time!");

        // Palette: swap style classes
        battleResultCard.getStyleClass().removeAll("result-card-victory", "result-card-defeat");
        resultTitleLabel.getStyleClass().removeAll("result-title-victory", "result-title-defeat");
        resultMessageLabel.getStyleClass().removeAll("result-message-victory", "result-message-defeat");
        battleAgainResultButton.getStyleClass().removeAll("result-btn-again-victory", "result-btn-again-defeat");

        String variant = playerWon ? "victory" : "defeat";
        battleResultCard.getStyleClass().add("result-card-" + variant);
        resultTitleLabel.getStyleClass().add("result-title-" + variant);
        resultMessageLabel.getStyleClass().add("result-message-" + variant);
        battleAgainResultButton.getStyleClass().add("result-btn-again-" + variant);

        // Fade in the overlay
        battleResultOverlay.setOpacity(0);
        battleResultOverlay.setVisible(true);
        battleResultOverlay.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(450), battleResultOverlay);
        ft.setToValue(1.0);
        ft.play();
    }

    /** Particle confetti shower that auto-stops after ~4 s. */
    private void startConfetti() {
        if (confettiCanvas != null)
            rootPane.getChildren().remove(confettiCanvas);
        confettiCanvas = new Canvas();
        confettiCanvas.widthProperty().bind(rootPane.widthProperty());
        confettiCanvas.heightProperty().bind(rootPane.heightProperty());
        confettiCanvas.setMouseTransparent(true);
        // Insert just below the result overlay (second-to-last child of rootPane)
        int insertIdx = Math.max(0, rootPane.getChildren().size() - 2);
        rootPane.getChildren().add(insertIdx, confettiCanvas);

        final int N = 140;
        double[] x = new double[N], y = new double[N];
        double[] vx = new double[N], vy = new double[N];
        double[] ang = new double[N], av = new double[N], sz = new double[N];
        Color[] palette = {
                Color.web("#FFD700"), Color.web("#FF6B6B"), Color.web("#4ECDC4"),
                Color.web("#45B7D1"), Color.web("#96CEB4"), Color.web("#FFEAA7"),
                Color.web("#DDA0DD"), Color.web("#98D8C8"), Color.web("#F7DC6F")
        };
        Color[] colors = new Color[N];
        Random rng = new Random();
        double sw = rootPane.getWidth() > 0 ? rootPane.getWidth() : 1200;
        for (int i = 0; i < N; i++) {
            x[i] = rng.nextDouble() * sw;
            y[i] = -rng.nextDouble() * 300;
            vx[i] = (rng.nextDouble() - 0.5) * 3.5;
            vy[i] = 2.5 + rng.nextDouble() * 3;
            ang[i] = rng.nextDouble() * Math.PI * 2;
            av[i] = (rng.nextDouble() - 0.5) * 0.14;
            sz[i] = 6 + rng.nextDouble() * 9;
            colors[i] = palette[rng.nextInt(palette.length)];
        }

        long[] t0 = { -1L };
        confettiTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (t0[0] < 0)
                    t0[0] = now;
                double elapsed = (now - t0[0]) / 1_000_000_000.0;
                double alpha = Math.max(0.0, 1.0 - elapsed / 4.0);

                GraphicsContext gc = confettiCanvas.getGraphicsContext2D();
                gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());

                for (int i = 0; i < N; i++) {
                    x[i] += vx[i];
                    y[i] += vy[i];
                    ang[i] += av[i];
                    if (y[i] > confettiCanvas.getHeight() + 20) {
                        y[i] = -12;
                        x[i] = rng.nextDouble() * confettiCanvas.getWidth();
                    }
                    gc.save();
                    gc.setGlobalAlpha(alpha);
                    gc.setFill(colors[i]);
                    gc.translate(x[i], y[i]);
                    gc.rotate(Math.toDegrees(ang[i]));
                    gc.fillRect(-sz[i] / 2, -sz[i] / 4, sz[i], sz[i] / 2);
                    gc.restore();
                }
                if (elapsed >= 4.0) {
                    stop();
                    gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());
                    rootPane.getChildren().remove(confettiCanvas);
                }
            }
        };
        confettiTimer.start();
    }

    /** Persist battle outcome to the battle_history and user_profiles tables. */
    private void saveBattleResult(boolean playerWon) {
        var user = PlayerSession.getInstance().getCurrentUser();
        if (user == null)
            return; // not logged-in session — skip

        String pokemonUsed = player.getTeam().stream()
                .map(p -> p.getName().toLowerCase())
                .collect(Collectors.joining(","));
        String result = playerWon ? "WIN" : "LOSS";
        String logStr = String.join("\n", battleLog);

        Thread t = new Thread(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                String sql = "INSERT INTO battle_history "
                        + "(user_id, result, pokemon_used, opponent_type, opponent_name, battle_log) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, user.getId());
                    ps.setString(2, result);
                    ps.setString(3, pokemonUsed);
                    ps.setString(4, "AI");
                    ps.setString(5, opponent.getName());
                    ps.setString(6, logStr);
                    ps.executeUpdate();
                }
                // Upsert win/loss counters in user_profiles
                String upsert = "INSERT INTO user_profiles "
                        + "(user_id, wins, losses, total_battles) VALUES (?,?,?,1) "
                        + "ON CONFLICT(user_id) DO UPDATE SET "
                        + "wins=wins+?, losses=losses+?, total_battles=total_battles+1";
                try (PreparedStatement ups = conn.prepareStatement(upsert)) {
                    int w = playerWon ? 1 : 0, l = playerWon ? 0 : 1;
                    ups.setInt(1, user.getId());
                    ups.setInt(2, w);
                    ups.setInt(3, l);
                    ups.setInt(4, w);
                    ups.setInt(5, l);
                    ups.executeUpdate();
                }
            } catch (Exception e) {
                System.err.println("[BattleController] Failed to save battle result: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // RESULT OVERLAY BUTTONS

    @FXML
    private void onGoBackClicked() {
        if (confettiTimer != null) {
            confettiTimer.stop();
            confettiTimer = null;
        }
        onBack();
    }

    @FXML
    private void onBattleAgainClicked() {
        if (confettiTimer != null) {
            confettiTimer.stop();
            confettiTimer = null;
        }
        if (confettiCanvas != null) {
            rootPane.getChildren().remove(confettiCanvas);
            confettiCanvas = null;
        }
        // Re-build player team: re-randomise if original was random, otherwise
        // create fresh PokemonInstances from the same IDs (full HP/PP restored).
        Player freshPlayer = new Player(player.getName());
        if (randomTeam) {
            freshPlayer.generateRandomTeam();
        } else {
            for (PokemonInstance p : player.getTeam()) {
                freshPlayer.addToTeam(new PokemonInstance(p.getId(), p.getLevel()));
            }
        }
        // Always a brand-new random AI team
        Player freshOpponent = new Player("AI Trainer");
        freshOpponent.generateRandomTeam();

        SceneManager.switchSceneWithLoading("battle.fxml", "Pokemon Battle - Arena", 1200, 700,
                Map.of("player", freshPlayer, "opponent", freshOpponent, "randomTeam", randomTeam));
    }

    // HELPERS
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}