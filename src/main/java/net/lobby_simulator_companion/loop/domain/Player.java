package net.lobby_simulator_companion.loop.domain;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trim;

@Builder
@AllArgsConstructor
@Data
public class Player implements Serializable, Cloneable {

    public enum Rating {
        @SerializedName("-1") THUMBS_DOWN,
        @SerializedName("0") UNRATED,
        @SerializedName("1") THUMBS_UP
    }

    private static final int MAX_NAMES_STORED = 10;


    private String steamId64;

    private String dbdPlayerId;

    /**
     * When this player was first discovered.
     */
    private LocalDateTime firstSeen;

    /**
     * When this player was last seen.
     */
    private LocalDateTime lastSeen;

    private int timesEncountered;

    /**
     * Matches actually played (not cancelled) against this killer.
     */
    private int matchesPlayed;

    private int secondsPlayed;

    private int escapes;

    private int deaths;

    /**
     * The last Steam names used by this player.
     * We will constrain this to a fixed number (for example, the last 5 used names).
     * The last one in the array is the most recent.
     */
    @Builder.Default
    private List<String> names = new ArrayList<>();

    /**
     * This player's rating value.
     */
    @Builder.Default
    private Rating rating = Rating.UNRATED;

    /**
     * A description for this player.
     */
    private String description;


    public Player() {
        LocalDateTime now = LocalDateTime.now();
        firstSeen = now;
        lastSeen = now;

        /**
         * double initialization on these fields due to a Lombok bug
         * https://github.com/rzwitserloot/lombok/issues/1347
         */
        names = new ArrayList<>(MAX_NAMES_STORED);
        rating = Rating.UNRATED;
    }

    public void updateLastSeen() {
        lastSeen = LocalDateTime.now();
    }

    public void incrementTimesEncountered() {
        timesEncountered++;
    }

    public void incrementMatchesPlayed() {
        matchesPlayed++;
    }

    public void incrementEscapes() {
        escapes++;
    }

    public void incrementDeaths() {
        deaths++;
    }

    public void incrementSecondsPlayed(int secondsPlayed) {
        this.secondsPlayed += secondsPlayed;
    }

    public Optional<String> getMostRecentName() {
        if (!isEmpty(names)) {
            return Optional.of(names.get(names.size() - 1));
        }

        return Optional.empty();
    }

    public void addName(String name) {
        String normalizedName = trim(name);

        if (isBlank(normalizedName)) {
            return;
        }

        int foundIdx = names.indexOf(normalizedName);
        if (foundIdx >= 0) {
            names.remove(foundIdx);
        } else if (names.size() == MAX_NAMES_STORED) {
            names.remove(0);
        }

        names.add(normalizedName);
    }

    public void setDescription(String description) {
        if (isBlank(description)) {
            description = null;
        }

        this.description = trim(description);
    }

}
