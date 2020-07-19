package net.lobby_simulator_companion.loop.domain;

import lombok.Getter;
import net.lobby_simulator_companion.loop.domain.stats.Stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Container class for the app stored data.
 *
 * @author NickyRamone
 */
@Getter
public class LoopData {

    private final int version = 3;
    private final List<Player> players = new ArrayList<>();
    private final Stats stats = new Stats();
    private final MatchLog matchLog = new MatchLog();


    public void addPlayers(Collection<Player> players) {
        this.players.addAll(players);
    }

}
