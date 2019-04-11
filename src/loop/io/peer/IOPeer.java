package loop.io.peer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public void clearNames() {
        names.clear();
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IOPeer ioPeer = (IOPeer) o;
        return version == ioPeer.version &&
                status == ioPeer.status &&
                Objects.equals(uid, ioPeer.uid) &&
                Objects.equals(names, ioPeer.names) &&
                Objects.equals(description, ioPeer.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, uid, names, status, description);
    }

    @Override
    public String toString() {
        return "IOPeer{" +
                "firstSeen=" + firstSeen +
                ", version=" + version +
                ", uid='" + uid + '\'' +
                ", names=" + names +
                ", status=" + status +
                ", description='" + description + '\'' +
                '}';
    }
}
