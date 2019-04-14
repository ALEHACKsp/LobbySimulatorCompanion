package net.lobby_simulator_companion.loop.service;

import java.util.List;

/**
 * Container class for the user data.
 *
 * @author NickyRamone
 */
public class Playerbase {

    private final int version = 1;

    private final List<Player> players;


    public Playerbase(List<Player> players) {
        this.players = players;
    }


    public int getVersion() {
        return version;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
