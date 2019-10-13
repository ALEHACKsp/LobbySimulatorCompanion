package net.lobby_simulator_companion.loop.domain;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Player implements Serializable {

    public enum Rating {
        @SerializedName("-1") THUMBS_DOWN,
        @SerializedName("0") UNRATED,
        @SerializedName("1") THUMBS_UP
    }

    private static final int MAX_NAMES_STORED = 5;


    /**
     * The UID of this player, which is the user Steam Id.
     */
    private String uid;

    private String dbdPlayerId;

    /**
     * When this player was first discovered.
     */
    private long firstSeen;

    /**
     * When this player was last seen.
     */
    private long lastSeen;

    private int timesEncountered;

    /**
     * Contrary to "encountered", it is considered "played" if the time spent connected to that player
     * is above some minimum threshold (to assume that there was actually a match and not just waiting
     * in the lobby.
     */
    private int matchesPlayed;

    private int secondsPlayed;

    /**
     * The last Steam names used by this player.
     * We will constrain this to a fixed number (for example, the last 5 used names).
     * The last one in the array is the most recent.
     */
    private List<String> names = new ArrayList<>();

    /**
     * This player's rating value.
     */
    private Rating rating = Rating.UNRATED;

    /**
     * A description for this player.
     */
    private String description;


    public Player() {
        long currentTime = System.currentTimeMillis();
        firstSeen = currentTime;
        lastSeen = currentTime;
    }


    public String getUID() {
        return this.uid;
    }

    public void setUID(String uid) {
        this.uid = uid.trim();
    }

    public String getDbdPlayerId() {
        return dbdPlayerId;
    }

    public void setDbdPlayerId(String dbdPlayerId) {
        this.dbdPlayerId = dbdPlayerId;
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        lastSeen = System.currentTimeMillis();
    }

    public int getTimesEncountered() {
        return timesEncountered;
    }

    public void setTimesEncountered(int timesEncountered) {
        this.timesEncountered = timesEncountered;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void incrementMatchesPlayed() {
        matchesPlayed++;
    }

    public int getSecondsPlayed() {
        return secondsPlayed;
    }

    public void incrementSecondsPlayed(int secondsPlayed) {
        this.secondsPlayed += secondsPlayed;
    }

    public List<String> getNames() {
        return names;
    }

    public String getMostRecentName() {
        String name = "";

        if (!names.isEmpty()) {
            name = names.get(names.size() - 1);
        }

        return name;
    }

    public boolean addName(String name) {
        if (name == null) {
            return false;
        }

        name = name.trim();

        if (name.equals(getMostRecentName())) {
            return false;
        }

        if (names.size() == MAX_NAMES_STORED) {
            // remove the oldest name
            names.remove(0);
        }

        names.add(name);

        return true;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null) {
            description = description.trim();

            if (description.isEmpty()) {
                description = null;
            }
        }
        this.description = description;
    }

}
