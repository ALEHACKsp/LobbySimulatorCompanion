package net.lobby_simulator_companion.loop.domain;

import java.net.InetAddress;

/**
 * @author NickyRamone
 */
public class Connection {

    private final InetAddress localAddr;
    private final int localPort;
    private final InetAddress remoteAddr;
    private final int remotePort;
    private final long created;
    private long lastSeen;

    public Connection(InetAddress localAddr, int localPort, InetAddress remoteAddr, int remotePort) {
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        this.created = System.currentTimeMillis();
        this.lastSeen = created;
    }

    public Connection(InetAddress localAddr, int localPort, InetAddress remoteAddr, int remotePort, long created) {
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        this.created = created;
        this.lastSeen = created;
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

    public long getCreated() {
        return created;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "localAddr=" + localAddr +
                ", localPort=" + localPort +
                ", remoteAddr=" + remoteAddr +
                ", remotePort=" + remotePort +
                '}';
    }
}
