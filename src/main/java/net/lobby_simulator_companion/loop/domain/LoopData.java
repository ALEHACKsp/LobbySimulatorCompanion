package net.lobby_simulator_companion.loop.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Container class for the user data.
 *
 * @author NickyRamone
 */
public class LoopData {

    private final int version = 2;
    private final List<Player> players = new ArrayList<>();


    public int getVersion() {
        return version;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayers(Collection<Player> players) {
        this.players.addAll(players);
    }

}
