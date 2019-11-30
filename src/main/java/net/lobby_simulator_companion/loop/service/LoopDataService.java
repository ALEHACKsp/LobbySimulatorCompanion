package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.domain.LoopData;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.repository.LoopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing data related to players and servers.
 *
 * @author NickyRamone
 */
public class LoopDataService {

    private static final Logger logger = LoggerFactory.getLogger(LoopDataService.class);
    private static final long SAVE_PERIOD_MS = 5000;

    private final LoopRepository repository;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private boolean dirty;


    public LoopDataService(LoopRepository loopRepository) throws IOException {
        repository = loopRepository;
        LoopData loopData;
        try {
            loopData = repository.load();
        } catch (FileNotFoundException e1) {
            loopData = new LoopData();
            repository.save(loopData);
        }

        for (Player player : loopData.getPlayers()) {
            players.put(player.getUID(), player);
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_PERIOD_MS, SAVE_PERIOD_MS);
    }

    public Player getPlayerBySteamId(String steamId) {
        return players.get(steamId);
    }

    public void addPlayer(String steamId, Player player) {
        players.put(steamId, player);
        dirty = true;
    }

    public void notifyChange() {
        dirty = true;
    }

    public synchronized void save() {
        if (!dirty) {
            return;
        }

        LoopData loopData = new LoopData();
        loopData.addPlayers(players.values().stream().collect(Collectors.toList()));
        try {
            repository.save(loopData);
            dirty = false;
            logger.debug("Saved Loop data.");
        } catch (IOException e) {
            logger.error("Failed to save data.", e);
        }
    }


}
