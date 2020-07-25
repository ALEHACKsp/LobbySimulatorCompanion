package net.lobby_simulator_companion.loop;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.LoopGsonFactory;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.repository.ExtremeIpDao;
import net.lobby_simulator_companion.loop.repository.LoopRepository;
import net.lobby_simulator_companion.loop.repository.ServerDao;
import net.lobby_simulator_companion.loop.repository.SteamProfileDao;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.service.log_event_orchestrators.ChaseEventManager;
import net.lobby_simulator_companion.loop.service.log_processing.impl.ChaseLogProcessor;
import net.lobby_simulator_companion.loop.service.log_processing.impl.KillerLogProcessor;
import net.lobby_simulator_companion.loop.service.log_processing.impl.MainLogProcessor;
import net.lobby_simulator_companion.loop.service.log_processing.impl.RealmMapLogProcessor;
import net.lobby_simulator_companion.loop.service.plugin.PluginManager;
import net.lobby_simulator_companion.loop.ui.KillerPanel;
import net.lobby_simulator_companion.loop.ui.MainWindow;
import net.lobby_simulator_companion.loop.ui.MatchPanel;
import net.lobby_simulator_companion.loop.ui.PeriodAggregateStatsPanel;
import net.lobby_simulator_companion.loop.ui.RollingAggregateStatsPanel;
import net.lobby_simulator_companion.loop.ui.ServerPanel;
import net.lobby_simulator_companion.loop.ui.StatsPanel;
import net.lobby_simulator_companion.loop.ui.SurvivalInputPanel;
import net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator;
import net.lobby_simulator_companion.loop.ui.startup.PluginLoadUi;
import net.lobby_simulator_companion.loop.util.event.EventSupport;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.lobby_simulator_companion.loop.util.LangUtil.unchecked;


/**
 * Factory for components.
 * Can eventually be replaced by an IOC container like Guice or Spring.
 *
 * @author NickyRamone
 */
@Slf4j
public final class Factory {

    private static final String PROPERTY__WRITE_ENCRYPTED = "storage.write.encrypted";
    private static final Map<Class, Object> instances = new HashMap<>();

    private Factory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }


    private static <T> T getInstance(Class<T> clazz, Supplier<T> objFactory) {
        T instance = clazz.cast(instances.get(clazz));

        if (instance == null) {
            try {
                instance = objFactory.get();
                instances.put(clazz, instance);
            } catch (Exception e) {
                log.error("Failed to instantiate class.", e);
                throw new RuntimeException("Failed to instantiate class.");
            }
        }

        return instance;
    }

    public static AppProperties appProperties() {
        return getInstance(AppProperties.class, unchecked(AppProperties::new));
    }

    public static Settings settings() {
        return getInstance(Settings.class, unchecked(Settings::new));
    }

    public static LoopRepository loopRepository() {
        return getInstance(LoopRepository.class,
                () -> new LoopRepository(appProperties(), gson()));
    }

    public static LoopDataService loopDataService() {
        return getInstance(LoopDataService.class, unchecked(
                () -> new LoopDataService(loopRepository())));
    }

    public static PluginLoadUi pluginLoadUi() {
        return getInstance(PluginLoadUi.class,
                () -> new PluginLoadUi(appProperties(), settings(), pluginManager()));
    }

    public static UiEventOrchestrator uiEventOrchestrator() {
        return getInstance(UiEventOrchestrator.class, () -> new UiEventOrchestrator());
    }

    public static SteamProfileDao steamProfileDao() {
        return getInstance(SteamProfileDao.class, () -> {
            String steamProfileUrlPrefix = appProperties().get("steam.profile_url_prefix");
            return new SteamProfileDao(steamProfileUrlPrefix);
        });
    }

    public static ServerDao serverDao() {
        return getInstance(ServerDao.class, () -> {
            String serviceUrlPrefix = appProperties().get("dao.server.extreme_ip.url_prefix");
            return new ExtremeIpDao(serviceUrlPrefix);
        });
    }


    public static DbdLogMonitor dbdLogMonitor() {
        return getInstance(DbdLogMonitor.class, unchecked(() -> {
                    DbdLogMonitor obj = appProperties().getBoolean("debug.panel") ?
                            new DbdLogMonitor(dbdLogEventSupport(), File.createTempFile("dbd-mock-log_", ".log"))
                            : new DbdLogMonitor(dbdLogEventSupport());

                    obj.registerProcessor(mainLogProcessor());
                    obj.registerProcessor(killerLogProcessor());
                    obj.registerProcessor(realmMapLogProcessor());
                    obj.registerProcessor(chaseLogProcessor());

                    return obj;
                })
        );
    }

    private static EventSupport dbdLogEventSupport() {
        return getInstance(EventSupport.class, EventSupport::new);
    }


    private static MainLogProcessor mainLogProcessor() {
        return getInstance(MainLogProcessor.class, () -> new MainLogProcessor(dbdLogEventSupport()));
    }

    private static KillerLogProcessor killerLogProcessor() {
        return getInstance(KillerLogProcessor.class, () -> new KillerLogProcessor(dbdLogEventSupport()));
    }

    private static RealmMapLogProcessor realmMapLogProcessor() {
        return getInstance(RealmMapLogProcessor.class, () -> new RealmMapLogProcessor(dbdLogEventSupport()));
    }

    private static ChaseLogProcessor chaseLogProcessor() {
        return getInstance(ChaseLogProcessor.class, () -> new ChaseLogProcessor(dbdLogEventSupport()));
    }

    public static GameStateManager gameStateManager() {
        return getInstance(GameStateManager.class,
                () -> new GameStateManager(
                        appProperties(),
                        dbdLogMonitor(),
                        loopDataService(),
                        steamProfileDao(),
                        chaseEventManager()
                ));
    }

    private static ChaseEventManager chaseEventManager() {
        return getInstance(ChaseEventManager.class,
                () -> new ChaseEventManager(dbdLogMonitor()));
    }

    public static MainWindow mainWindow() {
        return getInstance(MainWindow.class, () ->
                new MainWindow(settings(), appProperties(), loopDataService(),
                        gameStateManager(), uiEventOrchestrator(),
                        serverPanel(), matchPanel(), killerPanel(), statsPanel(), survivalInputPanel()));
    }

    public static SurvivalInputPanel survivalInputPanel() {
        return getInstance(SurvivalInputPanel.class, () ->
                new SurvivalInputPanel(loopDataService(), gameStateManager(), uiEventOrchestrator()));
    }

    public static ServerPanel serverPanel() {
        return getInstance(ServerPanel.class, () -> new ServerPanel(
                settings(), appProperties(), gameStateManager(), uiEventOrchestrator(), serverDao()));
    }

    public static KillerPanel killerPanel() {
        return getInstance(KillerPanel.class, () ->
                new KillerPanel(settings(), loopDataService(), gameStateManager(),
                        uiEventOrchestrator()));
    }

    public static MatchPanel matchPanel() {
        return getInstance(MatchPanel.class, () -> new MatchPanel(settings(), gameStateManager(), uiEventOrchestrator()));
    }

    public static StatsPanel statsPanel() {
        return getInstance(StatsPanel.class, () ->
                new StatsPanel(settings(), loopDataService(), gameStateManager(), uiEventOrchestrator(),
                        periodAggregateStatsPanel(), rollingAggregateStatsPanel()));
    }

    public static PeriodAggregateStatsPanel periodAggregateStatsPanel() {
        return getInstance(PeriodAggregateStatsPanel.class, () ->
                new PeriodAggregateStatsPanel(settings(), loopDataService(), gameStateManager()));
    }

    public static RollingAggregateStatsPanel rollingAggregateStatsPanel() {
        return getInstance(RollingAggregateStatsPanel.class, () ->
                new RollingAggregateStatsPanel(settings(), loopDataService(), gameStateManager()));
    }


    public static PluginManager pluginManager() {
        return getInstance(PluginManager.class, unchecked(
                () -> new PluginManager(appProperties())));
    }

    public static Gson gson() {
        return getInstance(Gson.class, () -> LoopGsonFactory.gson(appProperties().getBoolean(PROPERTY__WRITE_ENCRYPTED)));
    }

    public static void setInstance(Class type, Object instance) {
        instances.put(type, instance);
    }

}
