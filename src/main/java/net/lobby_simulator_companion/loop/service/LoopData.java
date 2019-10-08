package net.lobby_simulator_companion.loop.service;

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
    private final List<Server> servers = new ArrayList<>();


    public int getVersion() {
        return version;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayers(Collection<Player> players) {
        this.players.addAll(players);
    }

    public List<Server> getServers() {
        return servers;
    }

    public void addServers(Collection<Server> servers) {
        this.servers.addAll(servers);
    }
}
