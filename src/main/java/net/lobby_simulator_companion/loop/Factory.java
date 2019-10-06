package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.dao.SteamProfileDao;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.dao.PlayerbaseRepository;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.service.PlayerService;
import net.lobby_simulator_companion.loop.ui.DebugPanel;
import net.lobby_simulator_companion.loop.ui.MainPanel;
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
     * Eagerly create all instances.
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
        Object instance = instances.get(AppProperties.class);

        if (instance == null) {
            try {
                instance = createAppProperties();
            } catch (Exception e) {
                logger.error("Failed to instantiate class.", e);
            }
        }

        return (AppProperties) instance;
    }

    private static AppProperties createAppProperties() throws Exception {
        AppProperties instance = new AppProperties();
        instances.put(AppProperties.class, instance);

        return instance;
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

    public static SteamProfileDao getSteamProfileDao() {
        return (SteamProfileDao) instances.get(SteamProfileDao.class);
    }

    private static void createSteamProfileDao() {
        String steamProfileUrlPrefix = getAppProperties().get("steam.profile_url_prefix");
        Object instance = new SteamProfileDao(steamProfileUrlPrefix);
        instances.put(SteamProfileDao.class, instance);
    }

    public static DbdLogMonitor getDbdLogMonitor() {
        Object instance = instances.get(DbdLogMonitor.class);

        if (instance == null) {
            try {
                instance = createDbdLogMonitor();
            } catch (Exception e) {
                logger.error("Failed to instantiate class.", e);
            }
        }

        return (DbdLogMonitor) instance;
    }

    private static DbdLogMonitor createDbdLogMonitor() throws Exception {
        DbdLogMonitor instance = new DbdLogMonitor(getSteamProfileDao());
        instances.put(DbdLogMonitor.class, instance);

        return instance;
    }

    public static MainPanel getMainPanel() {
        Object instance = instances.get(MainPanel.class);

        if (instance == null) {
            MainPanel mainPanelInstance = new MainPanel();
            instances.put(MainPanel.class, mainPanelInstance);
            instance = mainPanelInstance;
        }

        return (MainPanel) instance;
    }

    public static DebugPanel getDebugPanel() {
        Object instance = instances.get(DebugPanel.class);

        if (instance == null) {
            try {
                instance = createDebugPanel();
            } catch (Exception e) {
                logger.error("Failed to instantiate debug panel.", e);
            }
        }

        return (DebugPanel) instance;
    }

    private static DebugPanel createDebugPanel() throws Exception {
        DebugPanel instance = new DebugPanel(getMainPanel(), getDbdLogMonitor());
        instances.put(DebugPanel.class, instance);

        return instance;
    }


}