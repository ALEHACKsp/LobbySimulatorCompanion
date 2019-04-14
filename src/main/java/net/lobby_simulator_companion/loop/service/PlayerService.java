package net.lobby_simulator_companion.loop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing players data provided by user.
 *
 * @author NickyRamone
 */
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private final PlayerbaseRepository repository;

    private final Map<String, Player> players = new HashMap<>();


    public PlayerService(PlayerbaseRepository playerbaseRepository) throws IOException {
        repository = playerbaseRepository;
        Playerbase playerbase;
        try {
            playerbase = repository.load();
        } catch (FileNotFoundException e1) {
            playerbase = new Playerbase(new ArrayList<>());
            repository.save(playerbase);
        }

        List<Player> storedPlayers = playerbase.getPlayers();

        for (Player player : storedPlayers) {
            if (!player.getRating().equals(Player.Rating.UNRATED) || !player.getDescription().isEmpty()) {
                players.put(player.getUID(), player);
            }
        }
    }

    public Player getPlayerBySteamId(String steamId) {
        return players.get(steamId);
    }

    public void addPlayer(String steamId, Player player) {
        players.put(steamId, player);
    }

    public synchronized void save() {
        List<Player> playersToSave = players.values().stream().filter(p -> shouldSavePeer(p)).collect(Collectors.toList());
        Playerbase playerbase = new Playerbase(playersToSave);
        try {
            repository.save(playerbase);
        } catch (IOException e) {
            logger.error("Failed to save players data.", e);
        }
    }

    private boolean shouldSavePeer(Player peer) {
        return !peer.getUID().isEmpty() &&
                (!peer.getRating().equals(Player.Rating.UNRATED) || !peer.getDescription().isEmpty());
    }


}
