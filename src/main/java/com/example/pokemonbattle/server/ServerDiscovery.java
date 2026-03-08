package com.example.pokemonbattle.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;

/**
 * UDP-based LAN server discovery.
 *
 * <p>Server side: call {@link #startBroadcasting(int)} — the server will broadcast
 * its LAN IP and TCP port every 2 seconds on UDP port {@value #DISCOVERY_PORT}.
 *
 * <p>Client side: call {@link #discoverServer(int)} — listens for the broadcast
 * and returns the server's IP address. Works on any network automatically.
 */
public class ServerDiscovery {

    /** UDP port used for discovery beacons. */
    public static final int DISCOVERY_PORT = 5556;

    private static final String BEACON_PREFIX        = "POKEMON_BATTLE_SERVER:";
    private static final int    BROADCAST_INTERVAL_MS = 2000;

    private static volatile boolean broadcasting = false;
    private static Thread broadcastThread;

    // ─────────────────────────────────────────────────────────────
    //  SERVER SIDE
    // ─────────────────────────────────────────────────────────────

    /**
     * Start broadcasting the server's presence on the LAN.
     * Safe to call multiple times — only one broadcast thread runs at a time.
     *
     * @param tcpPort the TCP port the battle server is listening on
     */
    public static synchronized void startBroadcasting(int tcpPort) {
        if (broadcasting) return;
        broadcasting = true;

        broadcastThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                String localIp = getLocalIpAddress();
                String payload = BEACON_PREFIX + localIp + ":" + tcpPort;
                byte[] data    = payload.getBytes("UTF-8");

                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddr, DISCOVERY_PORT);

                System.out.println("[Discovery] Broadcasting: " + payload);

                while (broadcasting) {
                    socket.send(packet);
                    Thread.sleep(BROADCAST_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[Discovery] Broadcast error: " + e.getMessage());
            }
        }, "DiscoveryBroadcastThread");

        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    /**
     * Stop broadcasting. Call from {@code BattleServer.shutdown()}.
     */
    public static synchronized void stopBroadcasting() {
        broadcasting = false;
        if (broadcastThread != null) {
            broadcastThread.interrupt();
            broadcastThread = null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  CLIENT SIDE
    // ─────────────────────────────────────────────────────────────

    /**
     * Listen for a server broadcast on the LAN and return the server's host address.
     * Blocks until a beacon is received or the timeout expires.
     *
     * @param timeoutMs maximum time to wait in milliseconds (e.g. 5000)
     * @return the server's IP address string, or {@code null} if not found in time
     * @throws IOException if the discovery socket cannot be opened
     */
    public static String discoverServer(int timeoutMs) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            socket.setSoTimeout(timeoutMs);
            byte[] buf    = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            socket.receive(packet);  // blocks until data or timeout

            String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
            if (message.startsWith(BEACON_PREFIX)) {
                // payload is  <ip>:<tcpPort>  — return only the IP
                String hostPort = message.substring(BEACON_PREFIX.length());
                return hostPort.split(":")[0];
            }
        } catch (SocketTimeoutException e) {
            // nothing found within timeout — caller will fall back to localhost
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns the machine's active LAN IPv4 address (non-loopback).
     * Uses the OS routing table to pick the correct interface — this works
     * reliably even when the machine has VirtualBox, Hyper-V, WSL, etc.
     * Falls back to {@code 127.0.0.1} if none is found.
     */
    private static String getLocalIpAddress() throws SocketException {
        // Preferred method: ask the OS which interface it would use to reach
        // an external IP. No packet is actually sent.
        try (DatagramSocket probe = new DatagramSocket()) {
            probe.connect(InetAddress.getByName("8.8.8.8"), 80);
            String ip = probe.getLocalAddress().getHostAddress();
            if (ip != null && !ip.startsWith("0.") && !ip.equals("127.0.0.1")) {
                return ip;
            }
        } catch (Exception ignored) {
            // fall through to manual scan
        }

        // Fallback: scan interfaces manually
        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
            for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                if (addr instanceof Inet4Address) {
                    return addr.getHostAddress();
                }
            }
        }
        return "127.0.0.1";
    }
}
