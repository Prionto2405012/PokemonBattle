package com.example.pokemonbattle.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.pokemonbattle.database.GameDataDAO;
import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.model.PokemonInstance;
import com.example.pokemonbattle.model.PokemonSpecies;

/**
 * Main TCP Server for Pokemon Online Battles.
 * Listens for client connections and manages multiple concurrent battles.
 * Handles player authentication, matchmaking, and battle coordination.
 */
public class BattleServer {
    private final int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    
    // Thread-safe list of connected clients
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    
    // Queue of clients waiting for a battle opponent
    private final Queue<ClientHandler> matchmakingQueue = new LinkedList<>();
    
    // Map of active battles
    private final Map<Integer, OnlineBattle> activeBattles = new HashMap<>();
    
    private int nextClientId = 1;
    private Thread serverThread;
    
    public BattleServer(int port) {
        this.port = port;
    }
    
    /**
     * Start the server and begin accepting client connections.
     */
    public void start() throws IOException {
        if (running) {
            System.out.println("Server is already running");
            return;
        }
        
        // ── Load game data (Pokemon species & moves) into PokemonInstance statics ──
        System.out.println("Loading game data...");
        GameDataDAO dao = new GameDataDAO();
        dao.ensureDataLoaded();
        Map<Integer, Move> allMoves = dao.loadAllMoves();
        List<PokemonSpecies> allPokemon = dao.loadAllPokemon();
        for (PokemonSpecies species : allPokemon) {
            species.selectRandomMoves(allMoves);
        }
        PokemonInstance.setAllPokemonSpecies(allPokemon);
        PokemonInstance.setAllMoves(allMoves);
        System.out.println("Loaded " + allPokemon.size() + " Pokemon + " + allMoves.size() + " moves.");

        serverSocket = new ServerSocket(port);
        running = true;
        
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║         Pokemon Battle Online Server Started              ║");
        System.out.println("║              Listening on port " + port + "               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Start accepting connections in a separate thread
        serverThread = new Thread(this::acceptConnections, "ServerAcceptThread");
        serverThread.setDaemon(false);
        serverThread.start();
        
        System.out.println("Server thread started. Waiting for client connections...");
    }
    
    /**
     * Accept incoming client connections and spawn handler threads.
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                int clientId = nextClientId++;
                ClientHandler handler = new ClientHandler(clientSocket, this, clientId);
                
                // Start handler thread
                handler.start();
                
                System.out.println("[Server] Connection accepted. New handler thread created (ID: " + clientId + ")");
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Server] Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Register an authenticated client with the server.
     */
    public synchronized void registerClient(ClientHandler client) {
        connectedClients.add(client);
        System.out.println("[Server] Client registered: " + client.getPlayerName() + " (Total: " + connectedClients.size() + ")");
    }
    
    /**
     * Unregister a disconnected client.
     */
    public synchronized void unregisterClient(ClientHandler client) {
        connectedClients.remove(client);
        matchmakingQueue.remove(client);
        System.out.println("[Server] Client unregistered: " + (client.getPlayerName() != null ? client.getPlayerName() : "Unknown") + 
                          " (Total: " + connectedClients.size() + ")");
    }
    
    /**
     * Find and match an opponent for a client.
     * Returns another waiting client or null if no match available.
     */
    public synchronized ClientHandler findAndMatchOpponent(ClientHandler client) {
        // Remove client from queue if already there
        matchmakingQueue.remove(client);
        
        // Check if there's someone already waiting
        if (!matchmakingQueue.isEmpty()) {
            ClientHandler opponent = matchmakingQueue.poll();
            System.out.println("[Server] Matched " + client.getPlayerName() + " with " + opponent.getPlayerName());
            return opponent;
        }
        
        // No opponent available, add to queue
        matchmakingQueue.add(client);
        System.out.println("[Server] Added " + client.getPlayerName() + " to matchmaking queue. Queue size: " + matchmakingQueue.size());
        
        return null;
    }
    
    /**
     * Register an active battle with the server.
     */
    public synchronized void registerBattle(OnlineBattle battle) {
        activeBattles.put(battle.getBattleId(), battle);
        System.out.println("[Server] Battle registered (ID: " + battle.getBattleId() + ", Total: " + activeBattles.size() + ")");
    }
    
    /**
     * Unregister a completed battle.
     */
    public synchronized void unregisterBattle(Integer battleId) {
        activeBattles.remove(battleId);
        System.out.println("[Server] Battle unregistered (ID: " + battleId + ", Total: " + activeBattles.size() + ")");
    }
    
    /**
     * Get server statistics.
     */
    public synchronized String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════════════════════╗\n");
        sb.append("║              Server Statistics                            ║\n");
        sb.append("║────────────────────────────────────────────────────────────║\n");
        sb.append(String.format("║  Connected Clients:      %-41d║\n", connectedClients.size()));
        sb.append(String.format("║  Waiting for Match:      %-41d║\n", matchmakingQueue.size()));
        sb.append(String.format("║  Active Battles:         %-41d║\n", activeBattles.size()));
        sb.append("╚════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }
    
    /**
     * Gracefully shutdown the server.
     */
    public synchronized void shutdown() {
        System.out.println("\n[Server] Shutting down server...");
        running = false;
        
        // Close all client connections
        for (ClientHandler client : connectedClients) {
            try {
                // Send shutdown message (optional)
                client.interrupt();
            } catch (Exception e) {
                // Ignore
            }
        }
        connectedClients.clear();
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error closing server socket: " + e.getMessage());
        }
        
        System.out.println("[Server] Server shutdown complete.");
    }
    
    /**
     * Check if server is running.
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Main method to run the server.
     */
    public static void main(String[] args) {
        int port = 5555;  // Default port
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        BattleServer server = new BattleServer(port);
        
        try {
            server.start();
            
            // Keep server running and listen for commands
            Scanner scanner = new Scanner(System.in);
            while (server.isRunning()) {
                System.out.print("> ");
                String command = scanner.nextLine().trim().toLowerCase();
                
                switch (command) {
                    case "status":
                    case "stats":
                        System.out.println(server.getStats());
                        break;
                    case "clients":
                        System.out.println("\nConnected Clients:");
                        for (ClientHandler client : server.connectedClients) {
                            System.out.println("  - " + client.getPlayerName() + " (ID: " + client.getUserId() + ")");
                        }
                        if (server.connectedClients.isEmpty()) {
                            System.out.println("  No connected clients");
                        }
                        break;
                    case "battles":
                        System.out.println("\nActive Battles:");
                        for (OnlineBattle battle : server.activeBattles.values()) {
                            System.out.println("  - Battle #" + battle.getBattleId() + " (Active: " + battle.isBattleActive() + ")");
                        }
                        if (server.activeBattles.isEmpty()) {
                            System.out.println("  No active battles");
                        }
                        break;
                    case "queue":
                        System.out.println("\nMatchmaking Queue:");
                        int queueIdx = 1;
                        for (ClientHandler client : server.matchmakingQueue) {
                            System.out.println("  " + queueIdx + ". " + client.getPlayerName());
                            queueIdx++;
                        }
                        if (server.matchmakingQueue.isEmpty()) {
                            System.out.println("  Queue is empty");
                        }
                        break;
                    case "help":
                        System.out.println("\nAvailable Commands:");
                        System.out.println("  status / stats  - Show server statistics");
                        System.out.println("  clients        - List connected clients");
                        System.out.println("  battles        - List active battles");
                        System.out.println("  queue          - Show matchmaking queue");
                        System.out.println("  help           - Show this help message");
                        System.out.println("  exit / quit    - Shutdown the server");
                        break;
                    case "exit":
                    case "quit":
                        server.shutdown();
                        scanner.close();
                        System.exit(0);
                        break;
                    case "":
                        // Empty input, show prompt again
                        break;
                    default:
                        System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
                }
            }
            
            scanner.close();
        } catch (IOException e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
