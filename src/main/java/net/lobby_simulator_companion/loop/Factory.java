package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.repository.LoopRepository;
import net.lobby_simulator_companion.loop.repository.SteamProfileDao;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.ui.DebugPanel;
import net.lobby_simulator_companion.loop.ui.KillerPanel;
import net.lobby_simulator_companion.loop.ui.MainWindow;
import net.lobby_simulator_companion.loop.ui.ServerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for all classes.
 * Can eventually be replaced by an IOC container like Guice or Spring.
 *
 * @author NickyRamone
 */
public final class Factory {
    private static final Logger logger = LoggerFactory.getLogger(Factory.class);

    private static final Map<Class, Object> instances = new HashMap<>();

    private Factory() {
    }


    public interface ThrowingFunction<T, R> {
        R apply() throws Exception;

        @SuppressWarnings("unchecked")
        static <T extends Exception, R> R sneakyThrow(Exception t) throws T {
            throw (T) t;
        }
    }

    static Supplier unchecked(ThrowingFunction f) {
        return () -> {
            try {
                return f.apply();
            } catch (Exception ex) {
                return ThrowingFunction.sneakyThrow(ex);
            }
        };
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
        getSettings();
        getAppProperties();
        getLoopRepository();
        getLoopDataService();
        getSteamProfileDao();
        getDbdLogMonitor();
    }

    private static <T> T getInstance(Class<T> clazz, Supplier<T> objFactory) {
        T instance = clazz.cast(instances.get(clazz));

        if (instance == null) {
            try {
                instance = objFactory.get();
                instances.put(clazz, instance);
            } catch (Exception e) {
                logger.error("Failed to instantiate class.", e);
            }
        }

        return instance;
    }

    public static AppProperties getAppProperties() {
        return getInstance(AppProperties.class, unchecked(AppProperties::new));
    }

    public static Settings getSettings() {
        return getInstance(Settings.class, unchecked(Settings::new));
    }

    public static LoopRepository getLoopRepository() {
        return getInstance(LoopRepository.class,
                () -> new LoopRepository(getSettings(), getAppProperties()));
    }

    public static LoopDataService getLoopDataService() {
        return getInstance(LoopDataService.class, unchecked(
                () -> new LoopDataService(getLoopRepository())));
    }

    public static SteamProfileDao getSteamProfileDao() {
        return getInstance(SteamProfileDao.class, () -> {
            String steamProfileUrlPrefix = getAppProperties().get("steam.profile_url_prefix");
            return new SteamProfileDao(steamProfileUrlPrefix);
        });
    }


    public static DbdLogMonitor getDbdLogMonitor() {
        return getInstance(DbdLogMonitor.class, unchecked(() ->
                new DbdLogMonitor(getSteamProfileDao())));
    }

    public static MainWindow getMainWindow() {
        return getInstance(MainWindow.class, () ->
                new MainWindow(getSettings(), getServerPanel(), getKillerPanel()));
    }

    public static ServerPanel getServerPanel() {
        return getInstance(ServerPanel.class, () -> new ServerPanel(getSettings()));
    }

    public static KillerPanel getKillerPanel() {
        return getInstance(KillerPanel.class, () ->
                new KillerPanel(getSettings(), getAppProperties(), getLoopDataService(), getSteamProfileDao()));
    }

    public static DebugPanel getDebugPanel() {
        return getInstance(DebugPanel.class, unchecked(() ->
                new DebugPanel(getMainWindow(), getDbdLogMonitor())));
    }


}