package net.lobby_simulator_companion.loop.service;

import java.net.InetAddress;

/**
 *
 * @author Nicky Ramone
 *
 */
public class Connection {

    private final InetAddress localAddr;
    private final int localPort;
    private final InetAddress remoteAddr;
    private final int remotePort;
    private long lastSeen;

    public Connection(InetAddress localAddr, int localPort, InetAddress remoteAddr, int remotePort, long lastSeen) {
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        this.lastSeen = lastSeen;
    }



    public InetAddress getLocalAddr() {
        return localAddr;
    }

    public int getLocalPort() {
        return localPort;
    }

    public InetAddress getRemoteAddr() {
        return remoteAddr;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

}
