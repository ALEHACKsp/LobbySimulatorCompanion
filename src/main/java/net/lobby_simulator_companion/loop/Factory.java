package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.service.DbdSteamLogMonitor;
import net.lobby_simulator_companion.loop.service.PlayerbaseRepository;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.service.PlayerService;
import net.lobby_simulator_companion.loop.ui.DebugPanel;
import net.lobby_simulator_companion.loop.ui.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for all classes.
 * Can eventually be replaced by an IOC container like Guice.
 *
 * @author NickyRamone
 */
public final class Factory {
    private static final Logger logger = LoggerFactory.getLogger(Factory.class);

    private static final Map<Class, Object> instances = new HashMap<>();

    private Factory() {
    }


    public static void init() {
        try {
            createInstances();
        } catch (Exception e) {
            logger.error("Failed to instantiate classes.", e);
            throw new RuntimeException("Failed to create instances.", e);
        }
    }

    private static void createInstances() throws Exception {
        createAppProperties();
        createPlayerbaseRepository();
        createPlayerService();
        createDbdLogMonitor();
    }


    public static AppProperties getAppProperties() {
        return (AppProperties) instances.get(AppProperties.class);
    }

    private static void createAppProperties() throws Exception {
        instances.put(AppProperties.class, new AppProperties());
    }

    public static PlayerbaseRepository getPlayerbaseRepository() {
        Object instance = instances.get(PlayerbaseRepository.class);

        if (instance == null) {
            createPlayerbaseRepository();
            instance = instances.get(PlayerbaseRepository.class);
        }

        return (PlayerbaseRepository) instance;
    }

    private static void createPlayerbaseRepository() {
        instances.put(PlayerbaseRepository.class, new PlayerbaseRepository());
    }

    public static PlayerService getPlayerService() throws Exception {
        Object instance = instances.get(PlayerService.class);

        if (instance == null) {
            createPlayerService();
            instance = instances.get(PlayerService.class);
        }

        return (PlayerService) instance;
    }

    private static void createPlayerService() throws Exception {
        PlayerbaseRepository playerbaseRepository = getPlayerbaseRepository();
        PlayerService instance = new PlayerService(playerbaseRepository);
        instances.put(PlayerService.class, instance);
    }

    private static DbdSteamLogMonitor getDbdLogMonitor() {
        return (DbdSteamLogMonitor) instances.get(DbdSteamLogMonitor.class);
    }

    private static void createDbdLogMonitor() throws Exception {
        Object instance = new DbdSteamLogMonitor();
        instances.put(DbdSteamLogMonitor.class, instance);
    }

    public static Overlay getOverlay() throws Exception {
        Object instance = instances.get(Overlay.class);

        if (instance == null) {
            createOverlay();
            instance = instances.get(Overlay.class);
        }

        return (Overlay) instance;
    }

    private static void createOverlay() throws Exception {
        Overlay instance = new Overlay(getPlayerService(), getDbdLogMonitor());
        instances.put(Overlay.class, instance);
    }


    public static DebugPanel getDebugPanel() throws Exception {
        Object instance = instances.get(DebugPanel.class);

        if (instance == null) {
            createDebugPanel();
            instance = instances.get(DebugPanel.class);
        }

        return (DebugPanel) instance;
    }

    private static void createDebugPanel() throws Exception {
        DebugPanel instance = new DebugPanel(getOverlay(), getDbdLogMonitor());
        instances.put(DebugPanel.class, instance);
    }


}