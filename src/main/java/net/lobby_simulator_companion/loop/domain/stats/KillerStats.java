package net.lobby_simulator_companion.loop.domain.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author NickyRamone
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class KillerStats {

    private int matches;
    private int escapes;
    private int deaths;
    private int matchTime;


    void incrementMatches() {
        matches++;
    }

    void decrementMatches() {
        matches--;
    }

    void incrementEscapes() {
        escapes++;
    }

    void decrementEscapes() {
        escapes--;
    }

    void incrementDeaths() {
        deaths++;
    }

    void decrementDeaths() {
        deaths--;
    }

    void incrementMatchTime(int seconds) {
        matchTime += seconds;
    }

    void decrementMatchTime(int seconds) {
        matchTime -= seconds;
    }

    @Override
    public KillerStats clone() {
        return toBuilder().build();
    }

}
