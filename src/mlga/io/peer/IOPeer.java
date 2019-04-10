package mlga.io.peer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper used to save/load Peer settings.
 * Must be serializable, so GSON can stream-encode them as objects.
 */
public class IOPeer implements Serializable {

    private static final int MAX_NAMES_STORED = 5;

    /**
     * Status is stored locally (and saved to file) as an int, for potential future-proofing. <br>
     * However, all external accessors for Status use these values. <br>
     * This allows for future changes without having to hunt down all calls to getStatus()
     */
    public enum Status {
        UNRATED(-1), BLOCKED(0), LOVED(1);

        public final int val;

        Status(int value) {
            this.val = value;
        }
    }

    /**
     * When this peer was first discovered.
     */
    private long firstSeen;

    /**
     * A version flag for each Peer object, for if something needs changing later.
     */
    public final int version = 2;

    /**
     * List of all IPs this peer has been known under.
     */
//    private CopyOnWriteArrayList<Integer> ips = new CopyOnWriteArrayList<Integer>();
    private List<Integer> ips = new ArrayList<>();

    /**
     * The UID of this peer, which is the user Steam Id.
     */
    private String uid = "";

    /**
     * The last Steam names used by this peer.
     * We will constrain this to a fixed number (for example, the last 5 used names).
     * The last one in the array is the most recent.
     */
    private List<String> names = new ArrayList<>();

    /**
     * This peer's status value, stored as an integer for any future updates/refactoring.
     */
    private int status = Status.UNRATED.val;

    /**
     * A description for this peer
     */
    private String description = "";


    public IOPeer() {
        this.firstSeen = System.currentTimeMillis();
    }


    public void setDescription(String description) {
        description = description.trim();

        if (description != this.description) {
            this.description = description;
        }
    }

    /**
     * Sets the UID of this peer, once we find it in the logs.
     */
    public void setUID(String uid) {
        this.uid = uid.trim();
    }

    public void addName(String name) {
        if (name.equals(getMostRecentName())) {
            return;
        }

        if (names.size() == MAX_NAMES_STORED) {
            // remove the oldest name
            names.remove(0);
        }

        names.add(name);
    }

    public String getMostRecentName() {
        String name = "";

        if (!names.isEmpty()) {
            name = names.get(names.size() - 1);
        }

        return name;
    }


    /**
     * Sets this Peer's status to the int supplied. Check {@link IOPeer.Status} for values.
     */
    public void setStatus(Status stat) {
        this.status = stat.val;
    }


    /**
     * Get this peer's UID. <br>
     */
    public String getUID() {
        return this.uid;
    }

    /**
     * Get the {@link IOPeer.Status} of this Peer, as set by the user.
     */
    public Status getStatus() {
        for (Status s : Status.values()) {
            if (s.val == this.status) {
                return s;
            }
        }
        System.err.println("Unknown status value: " + this.status);
        return Status.UNRATED;
    }

    public String getDescription() {
        return description;
    }

    public boolean equals(Object o) {
        if (!(o instanceof IOPeer)) {
            return false;
        }
        IOPeer p2 = (IOPeer) o;
        if ((getUID() == null && p2.getUID() != null) || getUID() != null && !getUID().equals(p2.getUID())) {
            return false;
        }
        if (ips.size() != p2.ips.size()) {
            return false;
        }
        for (int i = 0; i < ips.size(); i++) {
            int v = ips.get(i).compareTo(p2.ips.get(i));
            if (v != 0) {
                return false;
            }
        }
        return true;
    }

}
