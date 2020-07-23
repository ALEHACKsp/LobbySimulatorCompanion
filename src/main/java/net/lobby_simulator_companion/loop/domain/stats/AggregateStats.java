package net.lobby_simulator_companion.loop.domain.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.RealmMap;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.lang.Math.max;

/**
 * @author NickyRamone
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class AggregateStats {

    private int lobbiesFound;
    private int secondsQueued;
    private int secondsWaited;
    private int secondsPlayed;
    private int matchesPlayed;
    private int escapes;
    private int escapesInARow;
    private int maxEscapesInARow;
    private int deaths;
    private int deathsInARow;
    private int maxDeathsInARow;
    private final Map<Killer, KillerStats> killersStats = new TreeMap<>();
    private final Map<RealmMap, MapStats> mapStats = new TreeMap<>();


    public void addMatchStats(Match matchStats) {
        lobbiesFound += matchStats.getLobbiesFound();
        secondsQueued += matchStats.getSecondsQueued();
        secondsWaited += matchStats.getSecondsWaited();
        secondsPlayed += matchStats.getSecondsPlayed();
        matchesPlayed++;
        escapes += matchStats.escaped() ? 1 : 0;
        deaths += matchStats.escaped() ? 0 : 1;

        if (matchStats.escaped()) {
            deathsInARow = 0;
            escapesInARow++;
            maxEscapesInARow = max(escapesInARow, maxEscapesInARow);
        } else if (matchStats.died()) {
            escapesInARow = 0;
            deathsInARow++;
            maxDeathsInARow = max(deathsInARow, maxDeathsInARow);
        }

        aggregateKillerStats(matchStats);
        aggregateMapStats(matchStats);
    }

    private void aggregateKillerStats(Match matchStats) {
        Killer killer = Optional.ofNullable(matchStats.getKiller()).orElse(Killer.UNIDENTIFIED);

        KillerStats killerStats = getKillerStats(killer);
        killerStats.incrementMatches();
        killerStats.incrementMatchTime(matchStats.getSecondsPlayed());

        if (matchStats.escaped()) {
            killerStats.incrementEscapes();
        } else if (matchStats.died()) {
            killerStats.incrementDeaths();
        }
    }

    private void aggregateMapStats(Match matchStats) {
        RealmMap realmMap = Optional.ofNullable(matchStats.getRealmMap()).orElse(RealmMap.UNIDENTIFIED);
        MapStats mapStats = getMapStats(realmMap);
        mapStats.incrementMatches();
        mapStats.incrementMatchTime(matchStats.getSecondsPlayed());

        if (matchStats.escaped()) {
            mapStats.incrementEscapes();
        } else if (matchStats.died()) {
            mapStats.incrementDeaths();
        }
    }

    public int getAverageSecondsInQueue() {
        return lobbiesFound == 0 ? 0 : secondsQueued / lobbiesFound;
    }

    public int getAverageSecondsWaitedPerMatch() {
        return matchesPlayed == 0 ? secondsWaited : secondsWaited / matchesPlayed;
    }

    public int getAverageSecondsPerMatch() {
        return matchesPlayed == 0 ? 0 : (secondsPlayed / matchesPlayed);
    }

    public int getMatchesSubmitted() {
        return escapes + deaths;
    }

    public float getSurvivalProbability() {
        int matches = getMatchesSubmitted();

        if (matches == 0) {
            return 0;
        }

        return (float) escapes / matches * 100;
    }


    private KillerStats getKillerStats(Killer killer) {
        return killersStats.computeIfAbsent(killer, k -> new KillerStats());
    }

    private MapStats getMapStats(RealmMap realmMap) {
        return mapStats.computeIfAbsent(realmMap, m -> new MapStats());
    }


    public void reset() {
        lobbiesFound = 0;
        secondsQueued = 0;
        secondsWaited = 0;
        secondsPlayed = 0;
        matchesPlayed = 0;
        escapes = 0;
        escapesInARow = 0;
        maxEscapesInARow = 0;
        deaths = 0;
        deathsInARow = 0;
        maxDeathsInARow = 0;
        killersStats.clear();
        mapStats.clear();
    }

}
