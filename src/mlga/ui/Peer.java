package mlga.ui;

import mlga.io.peer.IOPeer;
import mlga.io.peer.IOPeer.Status;

import java.net.Inet4Address;

/**
 * Simple wrapper to visually represent a Peer that is connected.
 */
public class Peer {
    private Inet4Address ip;
    private long ping;
    private long last_seen;
    private IOPeer ioPeer;

    public Peer(Inet4Address ip, long rtt) {
        this.ioPeer = new IOPeer();
        this.ip = ip;
        this.ping = rtt;
        this.last_seen = System.currentTimeMillis();
    }

    public void setPing(long ping) {
        this.ping = ping;
        this.last_seen = System.currentTimeMillis();
    }

    public Inet4Address getID() {
        return this.ip;
    }

    /**
     * Returns this Peer's ping.
     */
    public long getPing() {
        return this.ping;
    }


    public void rate() {
        if (ioPeer.getStatus().equals(Status.UNRATED)) {
            ioPeer.setStatus(Status.BLOCKED);
        } else if (ioPeer.getStatus().equals(Status.BLOCKED)) {
            ioPeer.setStatus(Status.LOVED);
        } else {
            ioPeer.setStatus(Status.UNRATED);
        }
    }

    /**
     * Returns the time (in milliseconds) since this Peer was last pinged.
     */
    public long age() {
        return System.currentTimeMillis() - this.last_seen;
    }

    public String getDescription() {
        return ioPeer.getDescription();
    }

    public void setDescription(String description) {
        ioPeer.setDescription(description);
    }

    public String getSteamName() {
        return ioPeer.getMostRecentName();
    }

    public IOPeer getPeerData() {
        return ioPeer;
    }

    public void setPeerData(IOPeer ioPeer) {
        this.ioPeer = ioPeer;
    }

}