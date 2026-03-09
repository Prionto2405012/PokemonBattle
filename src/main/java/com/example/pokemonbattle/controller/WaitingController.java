package com.example.pokemonbattle.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.server.BattleStartMessage;
import com.example.pokemonbattle.server.ErrorMessage;
import com.example.pokemonbattle.server.FindOpponentRequest;
import com.example.pokemonbattle.server.GameMessage;
import com.example.pokemonbattle.server.LoginRequest;
import com.example.pokemonbattle.server.LoginResponse;
import com.example.pokemonbattle.server.ServerConnection;
import com.example.pokemonbattle.server.ServerDiscovery;
import com.example.pokemonbattle.util.PlayerSession;
import com.example.pokemonbattle.util.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Controller for the "Waiting for Opponent" screen.
 * Connects to the TCP server, logs in, and waits for a match.
 * When matched, transitions to the online battle screen.
 */
public class WaitingController {

    @FXML private Label statusLabel;
    @FXML private Label subStatusLabel;
    @FXML private Label dotsLabel;
    @FXML private ImageView pokeballImage;
    @FXML private Button cancelButton;

    private static final String SERVER_HOST = "localhost"; // fallback only
    private static final int    SERVER_PORT  = 5555;

    private ServerConnection serverConnection;
    private Player player;
    private volatile boolean cancelled = false;
    private boolean useDiscovery = false; // true = LAN discovery, false = localhost

    // Dots animation timeline
    private Timeline dotsTimeline;
    // Pokeball rotation timeline
    private javafx.animation.RotateTransition pokeballSpin;

    @FXML
    public void initialize() {
        // Load the player object built by NewGameController
        player = (Player) SceneManager.getData("player");

        // Check if LAN discovery was requested (Online Player mode)
        Object mode = SceneManager.getData("connectionMode");
        useDiscovery = "ONLINE".equals(mode);

        // Start animations
        startDotsAnimation();
        startPokeballAnimation();

        cancelButton.setOnAction(e -> onCancel());

        // Connect in a background thread to keep UI responsive
        Thread connectThread = new Thread(this::connectAndMatchmake, "WaitingConnectThread");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    /** Animated dots blinking. */
    private void startDotsAnimation() {
        final String[] dotFrames = {".", "..", "..."};
        final int[] frame = {0};
        dotsTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            dotsLabel.setText(dotFrames[frame[0] % dotFrames.length]);
            frame[0]++;
        }));
        dotsTimeline.setCycleCount(Timeline.INDEFINITE);
        dotsTimeline.play();
    }

    /** Pokeball spin animation (since it's a GIF it already animates, but add a pulse). */
    private void startPokeballAnimation() {
        if (pokeballImage == null) return;
        javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(Duration.millis(900), pokeballImage);
        pulse.setFromX(1.0);
        pulse.setToX(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse.play();
    }

    /** Main matchmaking flow — runs on background thread. */
    private void connectAndMatchmake() {
        try {
            String host;

            if (useDiscovery) {
                // ONLINE mode: try UDP broadcast then TCP subnet scan (handles AP isolation)
                updateStatus("Looking for server...", "Scanning local network for Pokemon Battle server");
                host = ServerDiscovery.discoverServer(10000,
                        status -> updateStatus("Looking for server...", status));
                if (host == null) {
                    Platform.runLater(() -> {
                        stopAnimations();
                        statusLabel.setText("No server found on network");
                        subStatusLabel.setText("Make sure the server is running on the same WiFi");
                        cancelButton.setText("Back");
                    });
                    return;
                }
                System.out.println("[WaitingController] Discovered server at: " + host);
            } else {
                // LOCAL mode: connect to localhost directly
                host = SERVER_HOST;
            }

            if (cancelled) return;

            // 2. Connect
            updateStatus("Connecting to server...", "Reaching " + host + ":" + SERVER_PORT);
            serverConnection = new ServerConnection(host, SERVER_PORT);
            serverConnection.connect();

            if (cancelled) return;

            // 3. Login
            updateStatus("Logging in...", "Authenticating your account");
            User sessionUser = PlayerSession.getInstance().getCurrentUser();
            String username, password;
            if (sessionUser != null) {
                username = sessionUser.getUsername();
                password = sessionUser.getPasswordHash() != null
                           ? sessionUser.getPasswordHash() : "GUEST_TOKEN";
            } else {
                // No session — use a generated guest name
                username = "Guest_" + (int)(Math.random() * 9999);
                password = "GUEST_TOKEN";
            }

            // Set up the message listener BEFORE sending login
            serverConnection.setMessageListener(this::handleServerMessage);

            LoginRequest loginReq = new LoginRequest(username, password);
            serverConnection.sendMessage(loginReq);

            // The rest of the flow is driven by handleServerMessage callbacks

        } catch (IOException e) {
            if (!cancelled) {
                Platform.runLater(() -> {
                    stopAnimations();
                    statusLabel.setText("Connection failed");
                    subStatusLabel.setText(e.getMessage());
                    cancelButton.setText("Back");
                });
            }
        }
    }

    /** All server messages are dispatched here (runs on receiver thread). */
    private void handleServerMessage(GameMessage message) {
        if (cancelled) return;

        switch (message.getMessageType()) {
            case "LOGIN_RESPONSE":
                onLoginResponse((LoginResponse) message);
                break;
            case "BATTLE_START":
                onBattleStart((BattleStartMessage) message);
                break;
            case "ERROR":
                onError((ErrorMessage) message);
                break;
            default:
                System.out.println("[WaitingController] Unhandled message: " + message.getMessageType());
        }
    }

    private void onLoginResponse(LoginResponse response) {
        if (!response.isSuccess()) {
            Platform.runLater(() -> {
                stopAnimations();
                statusLabel.setText("Login failed");
                subStatusLabel.setText(response.getMessage());
                cancelButton.setText("Back");
            });
            return;
        }

        // Login success — now send FindOpponentRequest with the player's team
        Platform.runLater(() -> updateStatus("Searching for opponent...",
                "Logged in as " + response.getPlayerName() + " · Waiting for match"));

        try {
            sendFindOpponentRequest(response.getUserId(), response.getPlayerName());
        } catch (IOException e) {
            Platform.runLater(() -> {
                stopAnimations();
                statusLabel.setText("Failed to enter matchmaking");
                subStatusLabel.setText(e.getMessage());
                cancelButton.setText("Back");
            });
        }
    }

    /** Build and send FindOpponentRequest with the player's actual team. */
    private void sendFindOpponentRequest(Integer userId, String playerName) throws IOException {
        if (player == null || player.getTeam().isEmpty()) {
            throw new IOException("No player team loaded");
        }

        var team = player.getTeam();
        Integer[] ids    = new Integer[team.size()];
        Integer[] levels = new Integer[team.size()];
        String[]  names  = new String[team.size()];
        // Collect up to 4 move IDs per pokemon (flat array)
        Integer[] moveIds = new Integer[team.size() * 4];

        for (int i = 0; i < team.size(); i++) {
            PokemonInstance p = team.get(i);
            ids[i]    = p.getId();
            levels[i] = p.getLevel();
            names[i]  = p.getName();
            var battleMoves = p.getBattleMoves();
            for (int j = 0; j < 4; j++) {
                moveIds[i * 4 + j] = (j < battleMoves.size())
                        ? battleMoves.get(j).getMove().getId() : null;
            }
        }

        FindOpponentRequest req = new FindOpponentRequest(userId, playerName,
                ids, levels, names, moveIds,
                PlayerSession.getInstance().getAvatarPath());
        serverConnection.sendMessage(req);
    }

    private void onBattleStart(BattleStartMessage msg) {
        // Build opponent Player from the data the server sent
        Player opponentPlayer = new Player(msg.getOpponentName());
        Integer[] oppIds     = msg.getOpponentPokemonIds();
        Integer[] oppLevels  = msg.getOpponentPokemonLevels();
        Integer[] oppMoveIds = msg.getOpponentMoveIds(); // flat: 4 per pokemon

        for (int i = 0; i < oppIds.length; i++) {
            try {
                PokemonInstance p = new PokemonInstance(oppIds[i], oppLevels[i]);

                // Override moves with the exact set the opponent has on the server
                if (oppMoveIds != null) {
                    p.clearMoves();
                    for (int j = 0; j < 4; j++) {
                        int idx = i * 4 + j;
                        if (idx < oppMoveIds.length && oppMoveIds[idx] != null) {
                            com.example.pokemonbattle.model.Move move =
                                    PokemonInstance.getMoveById(oppMoveIds[idx]);
                            if (move != null) p.addMove(move);
                        }
                    }
                }

                opponentPlayer.addToTeam(p);
            } catch (Exception e) {
                System.err.println("[WaitingController] Bad opponent pokemon id=" + oppIds[i] + ": " + e.getMessage());
            }
        }

        // Pass everything to the battle screen via SceneManager
        Map<String, Object> data = new HashMap<>();
        data.put("player",             player);
        data.put("opponent",           opponentPlayer);
        data.put("serverConnection",   serverConnection);
        data.put("battleId",           msg.getBattleId());
        data.put("isOnlineBattle",     true);
        data.put("opponentAvatarPath", msg.getOpponentAvatarPath());

        Platform.runLater(() -> {
            stopAnimations();
            SceneManager.switchSceneWithData("online_battle.fxml",
                    "Pokemon Battle - Online", 1200, 700, data);
        });
    }

    private void onError(ErrorMessage msg) {
        String code = msg.getErrorCode();
        // "NO_OPPONENT" just means we're queued — not a real error
        if ("NO_OPPONENT".equals(code)) {
            Platform.runLater(() -> updateStatus(
                    "Waiting for opponent...",
                    "You're in the queue. Another player will join soon."));
            return;
        }
        Platform.runLater(() -> {
            stopAnimations();
            statusLabel.setText("Error: " + code);
            subStatusLabel.setText(msg.getDescription());
            cancelButton.setText("Back");
        });
    }

    @FXML
    private void onCancel() {
        cancelled = true;
        stopAnimations();
        if (serverConnection != null) {
            serverConnection.disconnect();
        }
        SceneManager.switchSceneWithLoading("new_game.fxml", "Battle Setup", 1200, 700);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void updateStatus(String main, String sub) {
        Platform.runLater(() -> {
            statusLabel.setText(main);
            subStatusLabel.setText(sub);
        });
    }

    private void stopAnimations() {
        if (dotsTimeline != null) dotsTimeline.stop();
    }
}
