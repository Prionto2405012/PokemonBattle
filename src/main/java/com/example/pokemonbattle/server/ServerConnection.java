package com.example.pokemonbattle.server;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Client-side TCP connector for connecting to Pokemon Battle Server.
 * This class should be used by the JavaFX client to communicate with the server.
 * 
 * Usage example:
 * 
 *  ServerConnection conn = new ServerConnection("localhost", 5555);
 *  conn.connect();
 *  
 *  // Login
 *  LoginRequest loginRequest = new LoginRequest("username", "password");
 *  conn.sendMessage(loginRequest);
 *  
 *  // Set up listener for incoming messages
 *  conn.setMessageListener(message -> {
 *      if (message instanceof LoginResponse) {
 *          LoginResponse response = (LoginResponse) message;
 *          if (response.isSuccess()) {
 *              System.out.println("Logged in as: " + response.getPlayerName());
 *          }
 *      }
 *  });
 *  
 *  // Find opponent
 *  conn.sendMessage(new FindOpponentRequest(userId, playerName));
 *  
 *  // When battle starts, listen for BattleStartMessage
 *  // Then send moves with MoveMessage
 */
public class ServerConnection {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    
    private boolean connected = false;
    private Consumer<GameMessage> messageListener;
    
    private Thread receiverThread;
    
    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Connect to the server.
     */
    public void connect() throws IOException {
        System.out.println("[Client] Connecting to " + host + ":" + port + "...");
        
        socket = new Socket(host, port);
        
        // Initialize streams (output first to prevent deadlock)
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(socket.getInputStream());
        
        connected = true;
        
        // Start receiver thread
        receiverThread = new Thread(this::receiveMessages, "MessageReceiverThread");
        receiverThread.setDaemon(true);
        receiverThread.start();
        
        System.out.println("[Client] Connected to server");
    }
    
    /**
     * Send a message to the server.
     */
    public synchronized void sendMessage(GameMessage message) throws IOException {
        if (!connected || outputStream == null) {
            throw new IOException("Not connected to server");
        }
        
        try {
            outputStream.writeObject(message);
            outputStream.flush();
            System.out.println("[Client] Sent: " + message.getMessageType());
        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }
    
    /**
     * Listen for incoming messages from server.
     * This runs in a separate thread.
     */
    private void receiveMessages() {
        while (connected) {
            try {
                Object obj = inputStream.readObject();
                
                if (obj instanceof GameMessage) {
                    GameMessage message = (GameMessage) obj;
                    System.out.println("[Client] Received: " + message.getMessageType());
                    
                    // Call message listener if set
                    if (messageListener != null) {
                        messageListener.accept(message);
                    }
                }
            } catch (EOFException e) {
                System.out.println("[Client] Server disconnected");
                connected = false;
            } catch (IOException e) {
                if (connected) {
                    System.err.println("[Client] Connection error: " + e.getMessage());
                }
                connected = false;
            } catch (ClassNotFoundException e) {
                System.err.println("[Client] ClassNotFoundException: " + e.getMessage());
            }
        }
    }
    
    /**
     * Set a listener for incoming messages.
     */
    public void setMessageListener(Consumer<GameMessage> listener) {
        this.messageListener = listener;
    }
    
    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        connected = false;
        
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[Client] Error disconnecting: " + e.getMessage());
        }
        
        System.out.println("[Client] Disconnected from server");
    }
    
    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected();
    }
    
    // Example usage in JavaFX Controller
    /*
    public class BattleController {
        private ServerConnection serverConnection;
        
        @FXML
        public void initialize() {
            try {
                serverConnection = new ServerConnection("localhost", 5555);
                serverConnection.connect();
                
                // Set up message listener
                serverConnection.setMessageListener(message -> {
                    if (message instanceof BattleStartMessage) {
                        handleBattleStart((BattleStartMessage) message);
                    } else if (message instanceof DamageMessage) {
                        handleDamage((DamageMessage) message);
                    } else if (message instanceof BattleEndMessage) {
                        handleBattleEnd((BattleEndMessage) message);
                    }
                });
                
                // Login
                LoginRequest loginRequest = new LoginRequest("player1", "password");
                serverConnection.sendMessage(loginRequest);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private void handleBattleStart(BattleStartMessage msg) {
            Platform.runLater(() -> {
                // Update UI with opponent's pokemon
                displayOpponentTeam(msg.getOpponentPokemonIds(), msg.getOpponentPokemonNames());
            });
        }
        
        private void handleDamage(DamageMessage msg) {
            Platform.runLater(() -> {
                // Update HP bars
                updateBattleUI(msg);
            });
        }
        
        private void handleBattleEnd(BattleEndMessage msg) {
            Platform.runLater(() -> {
                // Show winner screen
                showWinnerDialog(msg.getWinnerName());
            });
        }
        
        @FXML
        public void onMoveClick(ActionEvent event) {
            try {
                MoveMessage moveMsg = new MoveMessage(
                    currentBattleId,
                    selectedMove.getId(),
                    selectedMove.getName(),
                    currentTurn
                );
                serverConnection.sendMessage(moveMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @FXML
        public void onSearchOpponent() {
            try {
                FindOpponentRequest request = new FindOpponentRequest(userId, playerName);
                serverConnection.sendMessage(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onDestroy() {
            if (serverConnection != null) {
                serverConnection.disconnect();
            }
        }
    }
    */
}
