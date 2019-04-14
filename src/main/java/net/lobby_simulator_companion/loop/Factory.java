package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.service.PlayerbaseRepository;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class Factory {
    private static final Logger logger = LoggerFactory.getLogger(Factory.class);

    private static final Map<Class, Object> instances = new HashMap<>();

    private Factory() {
    }


    public static void init() {
        try {
            createAppProperties();
            // TODO: we should create them here, but right now we can't because Security uses Boot.nif
//            createPlayerbaseRepository();
//            createPlayerService();
        }
        catch (Exception e) {
            logger.error("Failed to instantiate classes." , e);
            throw new RuntimeException("Failed to create instances.", e);
        }
    }


    public static AppProperties getAppProperties() {
        return (AppProperties) instances.get(AppProperties.class);
    }

    private static void createAppProperties() throws Exception {
        instances.put(AppProperties.class, new AppProperties());
    }

    public static PlayerbaseRepository getPlayerbaseRepository() {
        PlayerbaseRepository instance = (PlayerbaseRepository) instances.get(PlayerbaseRepository.class);

        if (instance == null) {
            createPlayerbaseRepository();
            instance = (PlayerbaseRepository) instances.get(PlayerbaseRepository.class);
        }

        return instance;
    }

    private static void createPlayerbaseRepository() {
        instances.put(PlayerbaseRepository.class, new PlayerbaseRepository());
    }

    public static PlayerService getPlayerService() throws Exception {
        PlayerService instance = (PlayerService) instances.get(PlayerService.class);

        if (instance == null) {
            createPlayerService();
            instance = (PlayerService) instances.get(PlayerService.class);
        }

        return instance;
    }

    private static void createPlayerService() throws Exception {
        PlayerbaseRepository playerbaseRepository = getPlayerbaseRepository();
        PlayerService instance = new PlayerService(playerbaseRepository);
        instances.put(PlayerService.class, instance);
    }


}