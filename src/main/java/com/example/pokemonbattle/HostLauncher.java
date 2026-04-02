package com.example.pokemonbattle;

import java.io.IOException;
import java.net.BindException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.pokemonbattle.server.BattleServer;

import javafx.application.Application;

/**
 * Host mode launcher.
 * Starts the battle server first, then launches the JavaFX client in the same process.
 */
public final class HostLauncher {

    private HostLauncher() {
    }

    public static void main(String[] args) {
        int port = 5555;

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

        BattleServer server = new BattleServer(port);
        AtomicBoolean shutdownStarted = new AtomicBoolean(false);

        Runnable shutdownServer = () -> {
            if (shutdownStarted.compareAndSet(false, true) && server.isRunning()) {
                server.shutdown();
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(shutdownServer, "HostLauncherShutdownHook"));

        try {
            server.start();
        } catch (BindException e) {
            System.err.println("BattleServer is already running on port " + port + ".");
            System.err.println("Stop the existing server first, or start host mode with another port.");
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("Fatal error: Could not start server");
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        try {
            Application.launch(HelloApplication.class, args);
        } finally {
            shutdownServer.run();
        }
    }
}
