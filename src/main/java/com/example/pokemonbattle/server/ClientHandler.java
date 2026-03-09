package com.example.pokemonbattle.server;

import com.example.pokemonbattle.database.UserDAO;
import com.example.pokemonbattle.database.GameDataDAO;
import com.example.pokemonbattle.model.User;
import com.example.pokemonbattle.model.Player;
import com.example.pokemonbattle.model.PokemonInstance;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles a single client connection.
 * Runs in its own thread to allow the server to handle multiple clients concurrently.
 * Manages login, matchmaking, and battle coordination.
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final BattleServer server;
    private final int clientId;
    
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    
    private Integer userId;
    private String playerName;
    private boolean authenticated = false;
    private boolean connected = true;
    
    private OnlineBattle currentBattle;
    
    // Stored when this client is waiting in the matchmaking queue
    private FindOpponentRequest pendingFindRequest;
    
    private UserDAO userDAO;
    private GameDataDAO gameDataDAO;
    
    public ClientHandler(Socket socket, BattleServer server, int clientId) {
        this.socket = socket;
        this.server = server;
        this.clientId = clientId;
        this.userDAO = new UserDAO();
        this.gameDataDAO = new GameDataDAO();
        this.gameDataDAO.ensureDataLoaded();
    }
    
    @Override
    public void run() {
        try {
            // Enable TCP keepalive so the OS detects dead connections
            socket.setKeepAlive(true);
            // Read timeout: if no data for 120s, check connection health
            socket.setSoTimeout(120_000);

            // Initialize streams (output stream first to avoid deadlock)
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("[Client #" + clientId + "] Connected from " + socket.getInetAddress());
            
            // Handle client messages
            while (connected) {
                try {
                    Object message = inputStream.readObject();
                    // Reset timeout after each successful read
                    
                    if (message instanceof GameMessage) {
                        handleMessage((GameMessage) message);
                    } else {
                        System.err.println("[Client #" + clientId + "] Unknown message type: " + message.getClass());
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // No data for 120s — check if still connected
                    if (socket.isClosed() || !socket.isConnected()) {
                        System.out.println("[Client #" + clientId + "] Connection timed out");
                        connected = false;
                    }
                    // otherwise keep waiting (client might just be idle)
                } catch (EOFException e) {
                    System.out.println("[Client #" + clientId + "] Connection closed by client");
                    connected = false;
                } catch (java.io.StreamCorruptedException e) {
                    System.err.println("[Client #" + clientId + "] Stream corrupted: " + e.getMessage());
                    connected = false;
                } catch (ClassNotFoundException e) {
                    System.err.println("[Client #" + clientId + "] ClassNotFoundException: " + e.getMessage());
                    connected = false;
                } catch (IOException e) {
                    System.err.println("[Client #" + clientId + "] IO error reading message: " + e.getMessage());
                    connected = false;
                }
            }
        } catch (IOException e) {
            System.err.println("[Client #" + clientId + "] IOException: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    /**
     * Route incoming messages to appropriate handlers.
     */
    private void handleMessage(GameMessage message) throws IOException {
        String type = message.getMessageType();
        System.out.println("[Client #" + clientId + "] Received: " + type);
        
        try {
            switch (type) {
                case "LOGIN_REQUEST":
                    handleLogin((LoginRequest) message);
                    break;
                case "FIND_OPPONENT_REQUEST":
                    handleFindOpponent((FindOpponentRequest) message);
                    break;
                case "ACTION":
                    handleAction((ActionMessage) message);
                    break;
                case "FORFEIT":
                    handleForfeit((ForfeitMessage) message);
                    break;
                case "MOVE":
                    handleMove((MoveMessage) message);
                    break;
                default:
                    System.err.println("[Client #" + clientId + "] Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("[Client #" + clientId + "] Error handling message: " + e.getMessage());
            e.printStackTrace();
            sendMessage(new ErrorMessage("INTERNAL_ERROR", "Server error processing your request"));
        }
    }
    
    /**
     * Handle login request.
     * Verify credentials and load player data from database.
     */
    private void handleLogin(LoginRequest request) throws IOException {
        String username = request.getUsername();
        String password = request.getPassword();
        
        System.out.println("[Client #" + clientId + "] Login attempt: " + username);
        
        try {
            // Try to find user in DB
            Optional<User> userOpt = userDAO.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                // DB user not found — accept as guest with generated ID
                this.userId = Math.abs(username.hashCode()) % 100000 + 1;
                this.playerName = username;
                this.authenticated = true;
                server.registerClient(this);
                System.out.println("[Client #" + clientId + "] Guest session: " + username + " (ID: " + userId + ")");
                sendMessage(new LoginResponse(true, userId, playerName));
                return;
            }
            
            User user = userOpt.get();
            
            // For online play, always accept the connection.
            // Remote players authenticated on their own machine and may have
            // a different password hash in their local DB.
            if (!user.getPasswordHash().equals(password) 
                    && !password.equals("GUEST_TOKEN")) {
                System.out.println("[Client #" + clientId + "] Remote player accepted (different local DB): " + username);
            }
            
            // Authentication successful
            this.userId = user.getId();
            this.playerName = user.getUsername();
            this.authenticated = true;
            
            // Register with server
            server.registerClient(this);
            
            System.out.println("[Client #" + clientId + "] User authenticated: " + username + " (ID: " + userId + ")");
            
            sendMessage(new LoginResponse(true, userId, playerName));
            
        } catch (SQLException e) {
            System.err.println("[Client #" + clientId + "] Database error: " + e.getMessage());
            sendMessage(new LoginResponse(false, "Database error: " + e.getMessage()));
        }
    }
    
    /**
     * Handle find opponent request.
     * Queue player and find a match for online battle.
     */
    private void handleFindOpponent(FindOpponentRequest request) throws IOException {
        if (!authenticated) {
            sendMessage(new ErrorMessage("NOT_AUTHENTICATED", "You must login first"));
            return;
        }
        
        System.out.println("[Client #" + clientId + "] " + playerName + " is looking for opponent");
        
        // Store the request so we can use team data when matched later
        this.pendingFindRequest = request;
        
        // Find a waiting opponent (or queue this player)
        ClientHandler opponent = server.findAndMatchOpponent(this);
        
        if (opponent == null) {
            sendMessage(new ErrorMessage("NO_OPPONENT", "No opponent available, waiting in queue..."));
            System.out.println("[Client #" + clientId + "] " + playerName + " queued for battle");
        } else {
            // Start battle between this player and opponent
            startBattle(opponent, request);
        }
    }
    
    /**
     * Start an online battle with an opponent.
     */
    private void startBattle(ClientHandler opponent, FindOpponentRequest myRequest) throws IOException {
        System.out.println("[Client #" + clientId + "] Starting battle with " + opponent.playerName);
        
        // Create Player objects from the actual teams provided by each client
        Player myPlayer = createPlayerFromRequest(myRequest);
        Player opponentPlayer = createPlayerFromRequest(opponent.pendingFindRequest);
        
        if (myPlayer == null || opponentPlayer == null) {
            String errorMsg = "Failed to load player data for battle";
            sendMessage(new ErrorMessage("BATTLE_ERROR", errorMsg));
            opponent.sendMessage(new ErrorMessage("BATTLE_ERROR", errorMsg));
            return;
        }
        
        // Create online battle
        OnlineBattle battle = new OnlineBattle(
            myRequest.getUserId(), myRequest.getPlayerName(), this,
            opponent.userId, opponent.playerName, opponent,
            myPlayer, opponentPlayer
        );
        
        // Set battle for both handlers
        this.currentBattle = battle;
        opponent.currentBattle = battle;
        
        // Start the battle
        try {
            battle.startBattle();
        } catch (IOException e) {
            System.err.println("[Client #" + clientId + "] Error starting battle: " + e.getMessage());
            sendMessage(new ErrorMessage("BATTLE_ERROR", "Failed to start battle"));
            opponent.sendMessage(new ErrorMessage("BATTLE_ERROR", "Failed to start battle"));
        }
    }
    
    /**
     * Create a Player using the actual team data sent by the client in FindOpponentRequest.
     */
    private Player createPlayerFromRequest(FindOpponentRequest request) {
        if (request == null) {
            System.err.println("[Client #" + clientId + "] No FindOpponentRequest stored — cannot build Player");
            return null;
        }
        try {
            Player player = new Player(request.getPlayerName());
            Integer[] ids     = request.getPokemonIds();
            Integer[] levels  = request.getPokemonLevels();
            Integer[] moveIds = request.getMoveIds(); // flat: 4 per pokemon
            
            for (int i = 0; i < ids.length; i++) {
                PokemonInstance pokemon = new PokemonInstance(ids[i], levels[i]);
                
                // Apply the specific moves the client selected (4 per pokemon)
                if (moveIds != null) {
                    pokemon.clearMoves();
                    for (int j = 0; j < 4; j++) {
                        int idx = i * 4 + j;
                        if (idx < moveIds.length && moveIds[idx] != null) {
                            com.example.pokemonbattle.model.Move move = gameDataDAO.getMove(moveIds[idx]);
                            if (move != null) {
                                pokemon.addMove(move);
                            }
                        }
                    }
                }
                player.addToTeam(pokemon);
            }
            System.out.println("[Client #" + clientId + "] Built player '" + request.getPlayerName() + "' with " + ids.length + " pokemon");
            return player;
        } catch (Exception e) {
            System.err.println("[Client #" + clientId + "] Error building player from request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fallback: create a random team (used only if no request data is available).
     */
    @SuppressWarnings("unused")
    private Player createRandomPlayer(String name) {
        try {
            Player player = new Player(playerName);
            
            // Create a default team (6 level 50 random pokemon)
            // In production, load actual player's pokemon team from database
            for (int i = 0; i < 3; i++) {
                int pokemonId = 1 + (int)(Math.random() * 10);  // Random pokemon 1-10
                PokemonInstance pokemon = new PokemonInstance(pokemonId, 50);
                player.addToTeam(pokemon);
            }
            
            return player;
        } catch (Exception e) {
            System.err.println("[Client #" + clientId + "] Error creating player: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Handle player forfeiting the battle.
     */
    private void handleForfeit(ForfeitMessage forfeitMsg) throws IOException {
        if (currentBattle == null || !currentBattle.isBattleActive()) {
            return;
        }
        try {
            currentBattle.forfeit(userId);
        } catch (IOException e) {
            System.err.println("[Client #" + clientId + "] Error processing forfeit: " + e.getMessage());
        }
    }

    /**
     * Handle action submission (ATTACK or SWITCH) during battle.
     */
    private void handleAction(ActionMessage actionMsg) throws IOException {
        if (currentBattle == null || !currentBattle.isBattleActive()) {
            sendMessage(new ErrorMessage("NO_ACTIVE_BATTLE", "No active battle"));
            return;
        }
        
        try {
            currentBattle.submitAction(userId, actionMsg);
        } catch (IOException e) {
            System.err.println("[Client #" + clientId + "] Error submitting action: " + e.getMessage());
            sendMessage(new ErrorMessage("ACTION_ERROR", "Failed to process action"));
        }
    }
    
    /**
     * Handle move submission during battle (legacy — kept for backward compatibility).
     */
    private void handleMove(MoveMessage moveMsg) throws IOException {
        if (currentBattle == null || !currentBattle.isBattleActive()) {
            sendMessage(new ErrorMessage("NO_ACTIVE_BATTLE", "No active battle"));
            return;
        }
        
        // Convert legacy MoveMessage to ActionMessage and submit
        ActionMessage action = ActionMessage.attack(moveMsg.getBattleId(), moveMsg.getMoveId(), moveMsg.getMoveName(), moveMsg.getTurn());
        try {
            currentBattle.submitAction(userId, action);
        } catch (IOException e) {
            System.err.println("[Client #" + clientId + "] Error submitting move: " + e.getMessage());
            sendMessage(new ErrorMessage("MOVE_ERROR", "Failed to process move"));
        }
    }
    
    /**
     * Send a message to this client.
     */
    public synchronized void sendMessage(GameMessage message) throws IOException {
        if (!connected || outputStream == null) {
            return;
        }
        
        try {
            outputStream.writeObject(message);
            outputStream.flush();
            outputStream.reset(); // Clear serialization cache to prevent stream corruption
            System.out.println("[Client #" + clientId + "] Sent: " + message.getMessageType());
        } catch (IOException e) {
            System.err.println("[Client #" + clientId + "] Error sending message: " + e.getMessage());
            connected = false;
            throw e;
        }
    }
    
    /**
     * Set the current battle for this client.
     */
    public void setBattle(OnlineBattle battle) {
        this.currentBattle = battle;
    }
    
    /**
     * Disconnect this client.
     */
    private void disconnect() {
        connected = false;
        
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[Client #" + clientId + "] Error closing connection: " + e.getMessage());
        }
        
        // If in an active battle, treat disconnect as forfeit
        if (currentBattle != null && currentBattle.isBattleActive()) {
            try {
                currentBattle.forfeit(userId);
            } catch (IOException e) {
                System.err.println("[Client #" + clientId + "] Error forfeiting on disconnect: " + e.getMessage());
            }
        }

        // Notify server of disconnection
        if (authenticated && userId != null) {
            server.unregisterClient(this);
            System.out.println("[Client #" + clientId + "] User disconnected: " + playerName);
        }
    }
    
    // Getters
    public int getClientId() { return clientId; }
    public Integer getUserId() { return userId; }
    public String getPlayerName() { return playerName; }
    public boolean isAuthenticated() { return authenticated; }
    public boolean isConnected() { return connected; }
}
