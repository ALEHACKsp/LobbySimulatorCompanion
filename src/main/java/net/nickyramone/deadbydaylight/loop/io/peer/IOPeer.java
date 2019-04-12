package net.nickyramone.deadbydaylight.loop.io.peer;

import com.google.gson.annotations.SerializedName;

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


    public enum Rating {
        @SerializedName("-1") THUMBS_DOWN,
        @SerializedName("0") UNRATED,
        @SerializedName("1") THUMBS_UP
    }

    /**
     * The UID of this peer, which is the user Steam Id.
     */
    private String uid = "";

    /**
     * When this peer was first discovered.
     */
    private long firstSeen;

    /**
     * The last Steam names used by this peer.
     * We will constrain this to a fixed number (for example, the last 5 used names).
     * The last one in the array is the most recent.
     */
    private List<String> names = new ArrayList<>();

    /**
     * This peer's rating value
     */
    private Rating rating = Rating.UNRATED;

    /**
     * A description for this peer
     */
    private String description;


    public IOPeer() {
        this.firstSeen = System.currentTimeMillis();
    }


    public void setDescription(String description) {
        description = description.trim();

        if (description != this.description) {
            this.description = description;
        }
    }

    public void setUID(String uid) {
        this.uid = uid.trim();
    }

    public void addName(String name) {
        name = name.trim();

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


    public void setRating(Rating rating) {
        this.rating = rating;
    }


    public String getUID() {
        return this.uid;
    }


    public Rating getRating() {
        return rating;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IOPeer ioPeer = (IOPeer) o;
        return firstSeen == ioPeer.firstSeen &&
                Objects.equals(uid, ioPeer.uid) &&
                names.equals(ioPeer.names) &&
                rating == ioPeer.rating &&
                Objects.equals(description, ioPeer.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstSeen, uid, names, rating, description);
    }

    @Override
    public String toString() {
        return "IOPeer{" +
                "uid='" + uid + '\'' +
                ", firstSeen=" + firstSeen +
                ", names=" + names +
                ", rating=" + rating +
                ", description='" + description + '\'' +
                '}';
    }
}
