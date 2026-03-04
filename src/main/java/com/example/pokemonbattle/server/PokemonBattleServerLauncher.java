package com.example.pokemonbattle.server;

import java.io.IOException;

/**
 * Server Launcher Console Application
 * Simple entry point to start the Pokemon Battle Online Server.
 * 
 * Usage: java PokemonBattleServerLauncher [port]
 * 
 * Examples:
 *   java PokemonBattleServerLauncher          # Uses default port 5555
 *   java PokemonBattleServerLauncher 7777    # Uses port 7777
 */
public class PokemonBattleServerLauncher {
    
    public static void main(String[] args) {
        int port = 5555;  // Default port
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.err.println("Error: Port must be between 1024 and 65535");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        // Start the server
        BattleServer server = new BattleServer(port);
        
        try {
            // Start the server
            server.start();
            
            // Server is now running and accepting connections
            // The main thread now handles CLI commands via BattleServer.main()
            BattleServer.main(new String[]{ String.valueOf(port) });
            
        } catch (IOException e) {
            System.err.println("Fatal error: Could not start server");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
