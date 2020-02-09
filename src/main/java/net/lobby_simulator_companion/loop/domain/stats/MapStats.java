package net.lobby_simulator_companion.loop.domain.stats;

/**
 * @author NickyRamone
 */
public class MapStats {

    private int matches;
    private int escapes;
    private int deaths;
    private int matchTime;

    public int getMatches() {
        return matches;
    }

    public int getEscapes() {
        return escapes;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getMatchTime() {
        return matchTime;
    }

    public void incrementMatches() {
        matches++;
    }

    public void incrementEscapes() {
        escapes++;
    }

    public void incrementDeaths() {
        deaths++;
    }

    public void incrementMatchTime(int seconds) {
        matchTime += seconds;
    }

    @Override
    protected MapStats clone() {
        MapStats clone = new MapStats();
        clone.matches = this.matches;
        clone.escapes = this.escapes;
        clone.deaths = this.deaths;
        clone.matchTime = this.matchTime;

        return clone;
    }

}
