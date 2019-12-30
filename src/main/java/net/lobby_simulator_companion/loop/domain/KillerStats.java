package net.lobby_simulator_companion.loop.domain;

/**
 * @author NickyRamone
 */
public class KillerStats {

    private int matches;
    private int escapes;
    private int deaths;
    private int matchTime;

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
    protected KillerStats clone() {
        KillerStats clone = new KillerStats();
        clone.matches = this.matches;
        clone.escapes = this.escapes;
        clone.deaths = this.deaths;
        clone.matchTime = this.matchTime;

        return clone;
    }
}
