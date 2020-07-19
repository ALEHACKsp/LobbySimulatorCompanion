package net.lobby_simulator_companion.loop.service;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.domain.LoopData;
import net.lobby_simulator_companion.loop.domain.MatchLog;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import net.lobby_simulator_companion.loop.domain.stats.Stats;
import net.lobby_simulator_companion.loop.repository.LoopRepository;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toConcurrentMap;

/**
 * Service for managing data related to players and servers.
 *
 * @author NickyRamone
 */
@Slf4j
public class LoopDataService {

    private static final long SAVE_PERIOD_MS = 5000;

    private final LoopRepository repository;
    private Map<String, Player> players = new HashMap<>();

    private LoopData loopData = new LoopData();
    private boolean dirty;


    public LoopDataService(LoopRepository loopRepository) {
        repository = loopRepository;
    }


    public void start() throws IOException {
        loopData = loadData();
        players = loopData.getPlayers().stream()
                .collect(toConcurrentMap(Player::getSteamId64, identity()));

        // schedule thread for saving dirty data
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_PERIOD_MS, SAVE_PERIOD_MS);
    }


    private LoopData loadData() throws IOException {
        LoopData data;

        try {
            data = repository.load();
        } catch (FileNotFoundException e) {
            data = new LoopData();
            repository.save(data);
        }

        return data;
    }

    public Stats getStats() {
        return loopData.getStats();
    }

    public MatchLog getMatchHistory() {
        return loopData.getMatchLog();
    }

    public void addMatch(Match match) {
        Player player = players.get(match.getKillerPlayerSteamId64());

        if (player != null) {
            player.incrementMatchesPlayed();
            player.incrementSecondsPlayed(match.getSecondsPlayed());

            if (match.escaped()) {
                player.incrementEscapes();
            } else if (match.died()) {
                player.incrementDeaths();
            }
        }

        loopData.getStats().addMatchStats(match);
        loopData.getMatchLog().add(match);
        dirty = true;
    }


    public Optional<Player> getPlayerBySteamId(String steamId) {
        return Optional.ofNullable(steamId).filter(StringUtils::isNotBlank).map(players::get);
    }

    public void addPlayer(Player player) {
        players.put(player.getSteamId64(), player);
        dirty = true;
    }

    public void notifyChange() {
        dirty = true;
    }

    public synchronized void save() {
        if (!dirty) {
            return;
        }

        loopData.getPlayers().clear();
        loopData.addPlayers(new ArrayList<>(players.values()));
        try {
            repository.save(loopData);
            dirty = false;
        } catch (IOException e) {
            log.error("Failed to save data.", e);
        }
    }

}
