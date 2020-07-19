package net.lobby_simulator_companion.loop.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.domain.RealmMap;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import net.lobby_simulator_companion.loop.domain.stats.periodic.PeriodStats;
import net.lobby_simulator_companion.loop.repository.SteamProfileDao;
import net.lobby_simulator_companion.loop.service.log_event_orchestrators.ChaseEventManager;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.util.Stopwatch;
import net.lobby_simulator_companion.loop.util.event.EventListener;
import net.lobby_simulator_companion.loop.util.event.EventSupport;
import net.lobby_simulator_companion.loop.util.event.SwingEventSupport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import static javax.swing.SwingUtilities.invokeLater;

/**
 * The purpose of this class is to store state about game events (e.g., information about current killer player,
 * current match time or queue time).
 * <p>
 * Measures different times (such as queue or match times) and updates stats based on the game state.
 *
 * @author NickyRamone
 */
@Slf4j
public class GameStateManager {

    /**
     * Minimum time connected from which we can assume that a match has taken place.
     */
    private static final int DEFAULT_MIN_MATCH_SECONDS = 60;

    private final AppProperties appProperties;
    private final DbdLogMonitor dbdLogMonitor;
    private final LoopDataService dataService;
    private final SteamProfileDao steamProfileDao;
    private final ChaseEventManager chaseEventManager;
    private final EventSupport eventSupport = new SwingEventSupport();
    private final Stopwatch queueStopwatch = new Stopwatch();
    private final Stopwatch matchWaitStopwatch = new Stopwatch();
    private final Stopwatch matchStopwatch = new Stopwatch();
    private final int minMatchSeconds;


    @Getter
    private Match currentMatch;

    private boolean resetMatchWait;


    public GameStateManager(AppProperties appProperties, DbdLogMonitor dbdLogMonitor, LoopDataService dataService,
                            SteamProfileDao steamProfileDao, ChaseEventManager chaseEventManager) {
        this.appProperties = appProperties;
        this.dbdLogMonitor = dbdLogMonitor;
        this.dataService = dataService;
        this.steamProfileDao = steamProfileDao;
        this.chaseEventManager = chaseEventManager;
        this.minMatchSeconds = appProperties.getBoolean("debug.panel") ?
                appProperties.getInt("debug.minMatchSeconds") : DEFAULT_MIN_MATCH_SECONDS;

        init();
    }


    private void init() {
        dbdLogMonitor.registerListener(DbdLogEvent.MATCH_WAIT, evt -> handleMatchWaitStart());
        dbdLogMonitor.registerListener(DbdLogEvent.MATCH_WAIT_CANCEL, evt -> handleMatchWaitCancel());
        dbdLogMonitor.registerListener(DbdLogEvent.SERVER_CONNECT, evt -> handleServerConnect((InetSocketAddress) evt.getValue()));
        dbdLogMonitor.registerListener(DbdLogEvent.KILLER_PLAYER, evt -> handleNewKillerPlayer((PlayerDto) evt.getValue()));
        dbdLogMonitor.registerListener(DbdLogEvent.KILLER_CHARACTER, evt -> handleNewKillerCharacter((Killer) evt.getValue()));
        dbdLogMonitor.registerListener(DbdLogEvent.MAP_GENERATE, evt -> handleMapGeneration((RealmMap) evt.getValue()));
        dbdLogMonitor.registerListener(DbdLogEvent.REALM_ENTER, evt -> handleRealmEnter());
        dbdLogMonitor.registerListener(DbdLogEvent.MATCH_START, evt -> handleMatchStart());
        dbdLogMonitor.registerListener(DbdLogEvent.USER_LEFT_REALM, evt -> handleRealmLeave());
        dbdLogMonitor.registerListener(DbdLogEvent.SURVIVED, evt -> handleCurrentPlayerSurvival());
        dbdLogMonitor.registerListener(DbdLogEvent.SERVER_DISCONNECT, evt -> handleServerDisconnect());

        chaseEventManager.registerEventListener(ChaseEventManager.Event.CHASE_START, evt -> fireEvent(GameEvent.CHASE_STARTED, evt.getValue()));
        chaseEventManager.registerEventListener(ChaseEventManager.Event.CHASE_END, evt -> fireEvent(GameEvent.CHASE_ENDED));

        initStatResetTimers();
    }

    private void initStatResetTimers() {
        dataService.getStats().asStream().forEach(this::initStatResetTimer);
    }

    private void initStatResetTimer(PeriodStats periodStats) {
        if (periodStats.getPeriodEnd() == null) {
            return;
        }
        Date statsResetDate = Date.from(periodStats.getPeriodEnd().atZone(ZoneId.systemDefault()).toInstant()
                .plusSeconds(5L));

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.debug("Resetting stats timer for {}", periodStats.getClass());
                periodStats.reset();
                dataService.notifyChange();
                timer.cancel();
                initStatResetTimer(periodStats);
                fireEvent(GameEvent.UPDATED_STATS);
            }
        }, statsResetDate);
    }


    public void forceDisconnect() {
        turnToIdle();
    }

    private void handleMatchWaitStart() {
        log.debug("Game event: searching for lobby");
        queueStopwatch.reset();
        queueStopwatch.start();

        if (resetMatchWait) {
            matchWaitStopwatch.reset();
        }

        matchWaitStopwatch.start();
        fireEvent(GameEvent.START_LOBBY_SEARCH);
    }


    private void handleMatchWaitCancel() {
        log.debug("Game event: lobby search cancelled");
        turnToIdle();
    }


    private void handleServerConnect(InetSocketAddress inetSocketAddress) {
        log.debug("Game event: connected to lobby");
        queueStopwatch.stop();

        currentMatch = new Match();
        currentMatch.incrementLobbiesFound();
        currentMatch.incrementSecondsQueued(getQueueTimeInSeconds());
        fireEvent(GameEvent.CONNECTED_TO_LOBBY, inetSocketAddress.getHostName());
    }


    private void handleMatchStart() {
        log.debug("Game event: match start");
        matchWaitStopwatch.stop();
        resetMatchWait = true;
        matchStopwatch.reset();
        matchStopwatch.start();
        currentMatch.setMatchStartTime(LocalDateTime.now());
        currentMatch.incrementSecondsWaited(getMatchWaitTimeInSeconds());
        fireEvent(GameEvent.MATCH_STARTED);
    }

    private void handleRealmLeave() {
        log.debug("Game event: user left the entity's realm");
        matchStopwatch.stop();
        currentMatch.setSecondsPlayed(getMatchDurationInSeconds());
        currentMatch.setEscaped(Optional.ofNullable(currentMatch.getEscaped()).orElse(false));

        // TODO: can we detect match cancel automatically?
        boolean matchCancelled = getMatchDurationInSeconds() < minMatchSeconds;

        if (!matchCancelled) {
            dataService.addMatch(currentMatch);
        } else {
            currentMatch.setCancelled(true);
        }

        fireEvent(GameEvent.MATCH_ENDED, currentMatch);
        fireEvent(GameEvent.UPDATED_STATS);
        fireEvent(GameEvent.UPDATED_CHASE_SUMMARY, chaseEventManager.getChaseSummary());
    }

    private void handleCurrentPlayerSurvival() {
        currentMatch.setEscaped(true);
    }


    private void handleServerDisconnect() {
        log.debug("Game event: server disconnect");
        turnToIdle();
    }

    private void turnToIdle() {
        queueStopwatch.stop();
        matchWaitStopwatch.stop();
        fireEvent(GameEvent.DISCONNECTED);
    }

    private void handleNewKillerPlayer(PlayerDto playerDto) {
        new Thread(() -> {
            String playerName;
            try {
                playerName = steamProfileDao.getPlayerName(playerDto.getSteamId());
            } catch (IOException e) {
                log.error("Failed to retrieve player's name for steam id#{}.", playerDto.getSteamId());
                playerName = "";
            }
            String steamId = playerDto.getSteamId();
            Optional<Player> storedPlayer = dataService.getPlayerBySteamId(steamId);
            final Player player;

            if (!storedPlayer.isPresent()) {
                log.debug("User #{} not found in the storage. Creating new entry...", steamId);
                player = new Player();
                player.setSteamId64(steamId);
                player.setDbdPlayerId(playerDto.getDbdId());
                player.addName(playerName);
                player.incrementTimesEncountered();
                dataService.addPlayer(player);
            } else {
                log.debug("User '{}' (#{}) found in the storage. Updating entry...", playerName, steamId);
                player = storedPlayer.get();
                player.updateLastSeen();
                player.addName(playerName);
                player.incrementTimesEncountered();
                dataService.notifyChange();
            }

            currentMatch.setKillerPlayerSteamId64(player.getSteamId64());
            currentMatch.setKillerPlayerDbdId(player.getDbdPlayerId());
            invokeLater(() -> fireEvent(GameEvent.NEW_KILLER_PLAYER, player));

        }).start();
    }

    private void handleNewKillerCharacter(Killer killerCharacter) {
        currentMatch.setKiller(killerCharacter);
        fireEvent(GameEvent.NEW_KILLER_CHARACTER, killerCharacter);
    }

    private void handleRealmEnter() {
        fireEvent(GameEvent.ENTERING_REALM);
    }

    private void handleMapGeneration(RealmMap realmMap) {
        currentMatch.setRealmMap(realmMap);
        fireEvent(GameEvent.START_MAP_GENERATION, realmMap);
    }

    @Deprecated
    public void notifySurvivalUserInput(Boolean survived) {
        currentMatch.setEscaped(survived);
        dataService.addMatch(currentMatch);
        fireEvent(GameEvent.UPDATED_STATS);
    }

    public Optional<Player> getKillerPlayer() {
        return dataService.getPlayerBySteamId(currentMatch.getKillerPlayerSteamId64());
    }

    public int getMatchDurationInSeconds() {
        return matchStopwatch.getSeconds();
    }

    public int getQueueTimeInSeconds() {
        return queueStopwatch.getSeconds();
    }

    public int getMatchWaitTimeInSeconds() {
        return matchWaitStopwatch.getSeconds();
    }

    public void registerListener(EventListener eventListener) {
        eventSupport.registerListener(eventListener);
    }

    public void registerListener(Object eventType, EventListener eventListener) {
        eventSupport.registerListener(eventType, eventListener);
    }

    public void fireEvent(Object eventType) {
        eventSupport.fireEvent(eventType);
    }

    public void fireEvent(Object eventType, Object eventValue) {
        eventSupport.fireEvent(eventType, eventValue);
    }

}
