package com.example.pokemonbattle.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * LAN server discovery with two strategies:
 *
 * <p><b>Strategy 1 — UDP Broadcast</b>: Server broadcasts its IP every 2 s on
 * UDP port {@value #DISCOVERY_PORT}. Works on most home routers.
 *
 * <p><b>Strategy 2 — TCP Ping</b>: Server listens on TCP port
 * {@value #PING_PORT}. Client scans the /24 subnet in parallel. Handles routers
 * that have AP-isolation (block UDP broadcasts between clients).
 */
public class ServerDiscovery {

    /** UDP port used for discovery beacons. */
    public static final int DISCOVERY_PORT = 5556;
    /** TCP port used for the subnet-scan ping handshake. */
    public static final int PING_PORT      = 5557;

    private static final String BEACON_PREFIX         = "POKEMON_BATTLE_SERVER:";
    private static final String PING_CHALLENGE        = "PKMN_PING";
    private static final String PING_RESPONSE         = "PKMN_PONG";
    private static final int    BROADCAST_INTERVAL_MS = 2000;

    private static volatile boolean broadcasting = false;
    private static Thread broadcastThread;
    private static Thread pingListenerThread;

    // ─────────────────────────────────────────────────────────────
    //  SERVER SIDE
    // ─────────────────────────────────────────────────────────────

    /**
     * Start broadcasting (UDP) and listening for ping probes (TCP).
     * Both run as daemon threads. Safe to call multiple times.
     */
    public static synchronized void startBroadcasting(int tcpBattlePort) {
        if (broadcasting) return;
        broadcasting = true;

        // ── UDP broadcast thread ──────────────────────────────────
        broadcastThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                String localIp = getLocalIpAddress();
                String payload = BEACON_PREFIX + localIp + ":" + tcpBattlePort;
                byte[] data    = payload.getBytes("UTF-8");

                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddr, DISCOVERY_PORT);

                System.out.println("[Discovery] Broadcasting on UDP " + DISCOVERY_PORT + ": " + payload);

                while (broadcasting) {
                    socket.send(packet);
                    Thread.sleep(BROADCAST_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[Discovery] UDP broadcast error: " + e.getMessage());
            }
        }, "DiscoveryBroadcastThread");
        broadcastThread.setDaemon(true);
        broadcastThread.start();

        // ── TCP ping listener thread (fallback for AP-isolated routers) ──
        pingListenerThread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(PING_PORT)) {
                ss.setSoTimeout(500); // short accept timeout so we can check broadcasting flag
                System.out.println("[Discovery] TCP ping listener on port " + PING_PORT);
                while (broadcasting) {
                    try {
                        Socket client = ss.accept();
                        // Handle in a tiny inline thread so we don't block accept loop
                        new Thread(() -> {
                            try {
                                byte[] buf = new byte[16];
                                int n = client.getInputStream().read(buf);
                                if (n > 0 && new String(buf, 0, n).startsWith(PING_CHALLENGE)) {
                                    client.getOutputStream().write(
                                        (PING_RESPONSE + ":" + getLocalIpAddress() + ":" + tcpBattlePort)
                                        .getBytes("UTF-8"));
                                }
                                client.close();
                            } catch (Exception ignored) {}
                        }).start();
                    } catch (SocketTimeoutException ignored) {
                        // loop again to check broadcasting flag
                    }
                }
            } catch (Exception e) {
                if (broadcasting) System.err.println("[Discovery] TCP ping error: " + e.getMessage());
            }
        }, "DiscoveryPingListenerThread");
        pingListenerThread.setDaemon(true);
        pingListenerThread.start();
    }

    /**
     * Stop both discovery services. Call from {@code BattleServer.shutdown()}.
     */
    public static synchronized void stopBroadcasting() {
        broadcasting = false;
        if (broadcastThread != null)    { broadcastThread.interrupt();    broadcastThread    = null; }
        if (pingListenerThread != null) { pingListenerThread.interrupt(); pingListenerThread = null; }
    }

    // ─────────────────────────────────────────────────────────────
    //  CLIENT SIDE
    // ─────────────────────────────────────────────────────────────

    /**
     * Discover the server on the LAN using both strategies in parallel.
     *
     * <ol>
     *   <li>UDP listen — waits {@code timeoutMs / 2} ms for a broadcast beacon.</li>
     *   <li>TCP subnet scan — probes every host on the local /24 in parallel
     *       if UDP finds nothing (handles AP-isolation routers).</li>
     * </ol>
     *
     * @param timeoutMs total time budget in milliseconds (e.g. 8000)
     * @param onStatus  optional callback to report progress to the UI (may be null)
     * @return the server's IP address string, or {@code null} if not found
     */
    public static String discoverServer(int timeoutMs, Consumer<String> onStatus) throws IOException {
        // ── Strategy 1: UDP broadcast ─────────────────────────────
        if (onStatus != null) onStatus.accept("Listening for server broadcast...");
        String udpResult = tryUdpDiscover(timeoutMs / 2);
        if (udpResult != null) {
            System.out.println("[Discovery] Found via UDP: " + udpResult);
            return udpResult;
        }

        // ── Strategy 2: TCP subnet scan ───────────────────────────
        if (onStatus != null) onStatus.accept("Scanning network for server...");
        String localIp = getLocalIpAddress();
        if (!localIp.equals("127.0.0.1")) {
            String tcpResult = tcpSubnetScan(localIp, timeoutMs / 2);
            if (tcpResult != null) {
                System.out.println("[Discovery] Found via TCP scan: " + tcpResult);
                return tcpResult;
            }
        }

        return null;
    }

    /** Convenience overload without status callback. */
    public static String discoverServer(int timeoutMs) throws IOException {
        return discoverServer(timeoutMs, null);
    }

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE — discovery implementations
    // ─────────────────────────────────────────────────────────────

    private static String tryUdpDiscover(int timeoutMs) {
        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            socket.setSoTimeout(timeoutMs);
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
            if (message.startsWith(BEACON_PREFIX)) {
                return message.substring(BEACON_PREFIX.length()).split(":")[0];
            }
        } catch (SocketTimeoutException ignored) {
        } catch (Exception e) {
            System.err.println("[Discovery] UDP listen error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Scan every .1–.254 address on the same /24 subnet in parallel,
     * sending a TCP ping to {@value #PING_PORT}. Returns first responding IP.
     */
    private static String tcpSubnetScan(String localIp, int timeoutMs) {
        String subnet = localIp.substring(0, localIp.lastIndexOf('.') + 1); // "192.168.0."
        AtomicReference<String> found = new AtomicReference<>(null);
        int perHostTimeout = Math.min(400, timeoutMs / 4);

        Thread[] threads = new Thread[254];
        for (int i = 1; i <= 254; i++) {
            final String candidate = subnet + i;
            threads[i - 1] = new Thread(() -> {
                if (found.get() != null) return;
                try (Socket s = new Socket()) {
                    s.connect(new java.net.InetSocketAddress(candidate, PING_PORT), perHostTimeout);
                    s.setSoTimeout(perHostTimeout);
                    s.getOutputStream().write(PING_CHALLENGE.getBytes("UTF-8"));
                    byte[] buf = new byte[64];
                    int n = s.getInputStream().read(buf);
                    if (n > 0) {
                        String resp = new String(buf, 0, n);
                        if (resp.startsWith(PING_RESPONSE)) {
                            found.compareAndSet(null, candidate);
                        }
                    }
                } catch (Exception ignored) {}
            }, "SubnetScan-" + candidate);
            threads[i - 1].setDaemon(true);
            threads[i - 1].start();
        }

        // Wait up to timeoutMs for any thread to find the server
        long deadline = System.currentTimeMillis() + timeoutMs;
        for (Thread t : threads) {
            if (found.get() != null) break;
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) break;
            try { t.join(remaining); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return found.get();
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns the machine's active WiFi/LAN IPv4 address.
     * Uses OS routing to pick the correct interface even when VirtualBox,
     * Hyper-V, WSL adapters are present.
     */
    static String getLocalIpAddress() throws SocketException {
        try (DatagramSocket probe = new DatagramSocket()) {
            probe.connect(InetAddress.getByName("8.8.8.8"), 80);
            String ip = probe.getLocalAddress().getHostAddress();
            if (ip != null && !ip.startsWith("0.") && !ip.equals("127.0.0.1")) {
                return ip;
            }
        } catch (Exception ignored) {}

        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
            for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                if (addr instanceof Inet4Address) return addr.getHostAddress();
            }
        }
        return "127.0.0.1";
    }
}


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
