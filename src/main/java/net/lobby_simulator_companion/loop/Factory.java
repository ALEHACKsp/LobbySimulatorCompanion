package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.dao.SteamProfileDao;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.dao.PlayerbaseRepository;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.service.PlayerService;
import net.lobby_simulator_companion.loop.ui.DebugPanel;
import net.lobby_simulator_companion.loop.ui.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Eagerly create alle instances.
     */
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
        createSteamProfileDao();
        createDbdLogMonitor();
    }


    public static AppProperties getAppProperties() {
        return (AppProperties) instances.get(AppProperties.class);
    }

    private static void createAppProperties() throws Exception {
        instances.put(AppProperties.class, new AppProperties());
    }

    public static PlayerbaseRepository getPlayerbaseRepository() {
        return (PlayerbaseRepository) instances.get(PlayerbaseRepository.class);
    }

    private static void createPlayerbaseRepository() {
        instances.put(PlayerbaseRepository.class, new PlayerbaseRepository());
    }

    public static PlayerService getPlayerService() throws Exception {
        return (PlayerService) instances.get(PlayerService.class);
    }

    private static void createPlayerService() throws Exception {
        PlayerbaseRepository playerbaseRepository = getPlayerbaseRepository();
        PlayerService instance = new PlayerService(playerbaseRepository);
        instances.put(PlayerService.class, instance);
    }

    private static SteamProfileDao getSteamProfileDao() {
        return (SteamProfileDao) instances.get(SteamProfileDao.class);
    }

    private static void createSteamProfileDao() {
        String steamProfileUrlPrefix = getAppProperties().get("steam.profile_url_prefix");
        Object instance = new SteamProfileDao(steamProfileUrlPrefix);
        instances.put(SteamProfileDao.class, instance);
    }

    private static DbdLogMonitor getDbdLogMonitor() {
        return (DbdLogMonitor) instances.get(DbdLogMonitor.class);
    }

    private static void createDbdLogMonitor() throws Exception {
        Object instance = new DbdLogMonitor(getSteamProfileDao());
        instances.put(DbdLogMonitor.class, instance);
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