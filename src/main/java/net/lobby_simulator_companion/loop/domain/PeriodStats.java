package net.lobby_simulator_companion.loop.domain;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author NickyRamone
 */
public abstract class PeriodStats implements Cloneable {

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private int lobbiesFound = 0;
    private int secondsQueued = 0;
    private int secondsWaited = 0;
    private int secondsPlayed = 0;
    private int matchesPlayed = 0;
    private int escapes = 0;
    private int escapesInARow = 0;
    private int maxEscapesInARow = 0;
    private int deaths = 0;
    private int deathsInARow = 0;
    private int maxDeathsInARow = 0;
    private Map<Killer, KillerStats> killersStats = new HashMap<>();


    public PeriodStats() {
        this(LocalDateTime.now());
    }

    public PeriodStats(PeriodStats other) {
        copyFrom(other);
    }

    public PeriodStats(LocalDateTime now) {
        periodStart = getPeriodStart(now);
        periodEnd = getPeriodEnd(now);
    }

    public void copyFrom(PeriodStats other) {
        this.periodStart = other.periodStart;
        this.periodEnd = other.periodEnd;
        this.lobbiesFound = other.lobbiesFound;
        this.secondsQueued = other.secondsQueued;
        this.matchesPlayed = other.matchesPlayed;
        this.secondsPlayed = other.secondsPlayed;
        this.secondsWaited = other.secondsWaited;
        this.escapes = other.escapes;
        this.escapesInARow = other.escapesInARow;
        this.maxEscapesInARow = other.maxEscapesInARow;
        this.deaths = other.deaths;
        this.deathsInARow = other.deathsInARow;
        this.maxDeathsInARow = other.maxDeathsInARow;
        this.killersStats = other.killersStats.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().clone()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void reset() {
        LocalDateTime now = LocalDateTime.now();
        periodStart = getPeriodStart(now);
        periodEnd = getPeriodEnd(now);
        lobbiesFound = 0;
        secondsQueued = 0;
        matchesPlayed = 0;
        secondsPlayed = 0;
        secondsWaited = 0;
        escapes = 0;
        deaths = 0;
        escapesInARow = 0;
        maxEscapesInARow = 0;
        deathsInARow = 0;
        maxDeathsInARow = 0;
        killersStats.clear();
    }

    protected abstract LocalDateTime getPeriodStart(LocalDateTime now);

    protected abstract LocalDateTime getPeriodEnd(LocalDateTime now);

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public int getLobbiesFound() {
        return lobbiesFound;
    }

    public void setLobbiesFound(int lobbiesFound) {
        this.lobbiesFound = lobbiesFound;
    }

    public int getSecondsQueued() {
        return secondsQueued;
    }

    public void setSecondsQueued(int seconds) {
        secondsQueued = seconds;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public int getSecondsPlayed() {
        return secondsPlayed;
    }

    public int getSecondsWaited() {
        return secondsWaited;
    }

    public void setSecondsWaited(int seconds) {
        secondsWaited = seconds;
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

    public int getEscapes() {
        return escapes;
    }

    public void incrementLobbiesFound() {
        lobbiesFound++;
    }

    public void incrementSecondsQueued(int seconds) {
        secondsQueued += seconds;
    }

    public void incrementMatchesPlayed(Killer killer) {
        matchesPlayed++;
        getOrCreateKillerStats(killer).incrementMatches();
    }

    public void incrementSecondsPlayed(Killer killer, int secondsPlayed) {
        this.secondsPlayed += secondsPlayed;
        getOrCreateKillerStats(killer).incrementMatchTime(secondsPlayed);
    }

    public void incrementSecondsWaited(int secondsWaited) {
        this.secondsWaited += secondsWaited;
    }

    public void incrementEscapes(Killer killer) {
        escapes++;
        escapesInARow++;
        maxEscapesInARow = Math.max(escapesInARow, maxEscapesInARow);
        deathsInARow = 0;
        getOrCreateKillerStats(killer).incrementEscapes();
    }

    public void incrementDeaths(Killer killer) {
        deaths++;
        deathsInARow++;
        maxDeathsInARow = Math.max(deathsInARow, maxDeathsInARow);
        escapesInARow = 0;
        getOrCreateKillerStats(killer).incrementDeaths();
    }

    public int getDeaths() {
        return deaths;
    }

    public float getSurvivalProbability() {
        int matches = getMatchesSubmitted();

        if (matches == 0) {
            return 0;
        }

        return (float) escapes / matches * 100;
    }

    public int getEscapesInARow() {
        return escapesInARow;
    }

    public int getMaxEscapesInARow() {
        return maxEscapesInARow;
    }

    public int getDeathsInARow() {
        return deathsInARow;
    }

    public int getMaxDeathsInARow() {
        return maxDeathsInARow;
    }

    private KillerStats getOrCreateKillerStats(Killer killer) {
        KillerStats killerStats = this.killersStats.get(killer);

        if (killerStats == null) {
            killerStats = new KillerStats();
            this.killersStats.put(killer, killerStats);
        }

        return killerStats;
    }


    @Override
    public abstract PeriodStats clone();

}
