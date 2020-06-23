package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.domain.Killer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DBD log parser daemon.
 * Parses logs to extract information about the killer we are playing against.
 * <p>
 * TODO: Stop using {@link Observer} and opt for {@link java.beans.PropertyChangeListener} and {@link java.beans.PropertyChangeSupport}.
 *
 * @author NickyRamone
 */
public class DbdLogMonitor extends Observable implements Runnable {

    private static final int LOG_POLLING_PERIOD_MS = 1000;

    private static final Logger logger = LoggerFactory.getLogger(DbdLogMonitor.class);

    private static final Path USER_APPDATA_PATH = Paths.get(System.getenv("APPDATA")).getParent();
    private static final String DEFAULT_LOG_PATH = "Local/DeadByDaylight/Saved/Logs/DeadByDaylight.log";
    private static final File DEFAULT_LOG_FILE = USER_APPDATA_PATH.resolve(DEFAULT_LOG_PATH).toFile();

    private static final String REGEX__LOBBY_ADD_PLAYER = "AddSessionPlayer.*Session:GameSession PlayerId:([0-9a-f\\-]+)\\|([0-9]+)";
    private static final Pattern PATTERN__LOBBY_ADD_PLAYER = Pattern.compile(REGEX__LOBBY_ADD_PLAYER);

    private static final String REGEX__KILLER_OUTFIT = "LogCustomization: --> ([a-zA-Z0-9]+)_[a-zA-Z0-9]+";
    private static final Pattern PATTERN__KILLER_OUTFIT = Pattern.compile(REGEX__KILLER_OUTFIT);

    private static final String REGEX__SERVER_CONNECT = "UPendingNetGame::SendInitialJoin.+RemoteAddr: "
            + "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})(?::([0-9]{1,5}))?";
    private static final Pattern PATTERN__SERVER_CONNECT = Pattern.compile(REGEX__SERVER_CONNECT);

    private static final String REGEX__MATCH_WAIT = "(POST https://.+?/api/v1/queue\\])|"
            + "(\\[PartyContextComponent::UpdateReadyButtonStateInfo\\] Ready button updated : 1)";
    private static final Pattern PATTERN__MATCH_WAIT = Pattern.compile(REGEX__MATCH_WAIT);

    private static final String REGEX__MATCH_WAIT_CANCEL = "RESPONSE: code 200.+?POST https://.+?/api/v1/queue/cancel\\]";
    private static final Pattern PATTERN__MATCH_WAIT_CANCEL = Pattern.compile(REGEX__MATCH_WAIT_CANCEL);

    private static final String REGEX__MAP_GENERATION = "ProceduralLevelGeneration: InitLevel: Theme: .* Map: ([^\\s]+)";
    private static final Pattern PATTERN__MAP_GENERATION = Pattern.compile(REGEX__MAP_GENERATION);


    private static final Map<Killer, String[]> KILLER_TO_OUTFIT_MAPPING = Stream.of(new Object[][]{
            {Killer.CANNIBAL, new String[]{"CA"}},
            {Killer.CLOWN, new String[]{"GK", "Clown"}},
            {Killer.DEATHSLINGER, new String[]{"UkraineKiller", "UK"}},
            {Killer.DEMOGORGON, new String[]{"QK"}},
            {Killer.DOCTOR, new String[]{"DO", "DOW04", "Killer07"}},
            {Killer.GHOSTFACE, new String[]{"OK"}},
            {Killer.HAG, new String[]{"HA", "WI", "Witch"}},
            {Killer.HILLBILLY, new String[]{"HB", "TC", "Hillbilly"}},
            {Killer.HUNTRESS, new String[]{"BE"}},
            {Killer.LEGION, new String[]{"KK", "Legion"}},
            {Killer.NIGHTMARE, new String[]{"SD"}},
            {Killer.NURSE, new String[]{"TN", "Nurse", "NR"}},
            {Killer.ONI, new String[]{"SwedenKiller"}},
            {Killer.PIG, new String[]{"FK"}},
            {Killer.PLAGUE, new String[]{"MK", "Plague"}},
            {Killer.SHAPE, new String[]{"MM"}},
            {Killer.SPIRIT, new String[]{"HK", "Spirit"}},
            {Killer.TRAPPER, new String[]{"TR", "TRW03", "TRW04", "Chuckles", "S01", "Trapper"}},
            {Killer.WRAITH, new String[]{"TW", "WR", "Wraith"}}

    }).collect(Collectors.toMap(e -> (Killer) e[0], e -> (String[]) e[1]));

    private static final Map<String, Killer> OUTFIT_TO_KILLER_MAPPING =
            KILLER_TO_OUTFIT_MAPPING.entrySet().stream()
                    .flatMap(e -> Stream.of(e.getValue()).map(v -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), v)))
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    public static final class Event {
        public enum Type {
            MATCH_WAIT,
            MATCH_WAIT_CANCEL,
            SERVER_CONNECT,
            MATCH_START,
            MATCH_END,
            KILLER_PLAYER,
            KILLER_CHARACTER,
            MAP_GENERATE,
            SERVER_DISCONNECT
        }

        public final Type type;
        public final Object argument;

        public Event(Type type, Object argument) {
            this.type = type;
            this.argument = argument;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "type=" + type +
                    ", argument=" + argument +
                    '}';
        }
    }

    private boolean connected;
    private boolean notifiedDisconnect; // to avoid reporting disconnection repeatedly
    private File logFile;
    private List<Function<String, Boolean>> lineProcessors;
    private BufferedReader reader;
    private long logSize;
    private PlayerDto lastPlayer;
    private PlayerDto lastKillerPlayer;
    private Killer lastKiller;


    public DbdLogMonitor() throws IOException {
        this(DEFAULT_LOG_FILE);
    }

    public DbdLogMonitor(File logFile) throws IOException {
        this.logFile = logFile;

        lineProcessors = Arrays.asList(
                this::checkForServerConnect,
                this::checkForKiller,
                this::checkForPlayer,
                this::checkForMatchWait,
                this::checkForMatchWaitCancel,
                this::checkForMatchStart,
                this::checkForMapGeneration,
                this::checkForMatchEnd,
                this::checkForServerDisconnect
        );

        initReader();
    }


    private void initReader() throws IOException {
        if (reader != null) {
            reader.close();
        }
        reader = new BufferedReader(new FileReader(logFile));

        // consume all entries in the log file, since they are old and cannot be related to any active connection.
        while (reader.readLine() != null) ;
    }


    @Override
    public void run() {
        String line;

        while (true) {
            try {
                long currentLogSize = logFile.length();

                if (currentLogSize < logSize) {
                    // the log file has been recreated (probably due to DBD being restarted),
                    // so we need to re-instantiate the reader
                    initReader();
                }
                logSize = currentLogSize;
                line = reader.readLine();

                if (line != null) {
                    connected = true;
                    notifiedDisconnect = false;
                    processLine(line);
                } else {
                    // for now, there are no more entries in the file
                    Thread.sleep(LOG_POLLING_PERIOD_MS);
                }
            } catch (IOException e) {
                logger.error("Encountered error while processing log file.", e);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void processLine(String line) {
        for (Function<String, Boolean> lineProcessor : lineProcessors) {
            if (lineProcessor.apply(line)) {
                break;
            }
        }
    }


    private Boolean checkForServerConnect(String logLine) {
        Matcher matcher = PATTERN__SERVER_CONNECT.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        String serverAddress = matcher.group(1);
        int serverPort = matcher.group(2) != null ? Integer.valueOf(matcher.group(2)) : 0;
        Event event = new Event(Event.Type.SERVER_CONNECT,
                InetSocketAddress.createUnresolved(serverAddress, serverPort));
        setChanged();
        notifyObservers(event);

        return true;
    }

    private Boolean checkForKiller(String logLine) {
        Matcher matcher = PATTERN__KILLER_OUTFIT.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        String outfitCode = matcher.group(1);
        Killer killer = OUTFIT_TO_KILLER_MAPPING.get(outfitCode);

        if (killer == null) {
            // it's a survivor
            lastPlayer = null;
        } else if (lastPlayer == null && lastKillerPlayer == null) {
            // no killer player where to assign this outfit
        } else if (lastPlayer == null && !killer.equals(lastKiller)) {
            // change of outfit for current killer
            lastKiller = killer;
            Event event = new Event(Event.Type.KILLER_CHARACTER, killer);
            setChanged();
            notifyObservers(event);
        } else if (lastPlayer != null && !lastPlayer.equals(lastKillerPlayer)) {
            // new killer player
            logger.debug("Detected new killer player: {}", lastPlayer);
            lastKillerPlayer = lastPlayer;
            Event event = new Event(Event.Type.KILLER_PLAYER, lastKillerPlayer);
            setChanged();
            notifyObservers(event);

            lastKiller = killer;
            event = new Event(Event.Type.KILLER_CHARACTER, killer);
            setChanged();
            notifyObservers(event);
        }

        lastPlayer = null;
        return true;
    }

    private Boolean checkForPlayer(String logLine) {
        Matcher matcher = PATTERN__LOBBY_ADD_PLAYER.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        String dbdPlayerId = matcher.group(1);
        String steamUserId = matcher.group(2);
        logger.debug("Detected user connecting to lobby. dbd-id: {}; steam-id: {}", dbdPlayerId, steamUserId);
        lastPlayer = new PlayerDto(steamUserId, dbdPlayerId);

        return true;
    }


    private Boolean checkForMatchWait(String logLine) {
        Matcher matcher = PATTERN__MATCH_WAIT.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        Event event = new Event(Event.Type.MATCH_WAIT, null);
        setChanged();
        notifyObservers(event);

        return true;
    }

    private Boolean checkForMatchWaitCancel(String logLine) {
        boolean result = false;
        Matcher matcher = PATTERN__MATCH_WAIT_CANCEL.matcher(logLine);

        if (matcher.find()
                || logLine.contains("[MirrorsSocialPresence::DestroyParty]")
                || logLine.contains("[PartyContextComponent::OnQuickmatchComplete] result : UnknownError")
                || logLine.contains("[UDBDGameInstance::RegisterDisconnectError]") // NAT error?
        ) {
            Event event = new Event(Event.Type.MATCH_WAIT_CANCEL, null);
            setChanged();
            notifyObservers(event);
            result = true;
        }

        return result;
    }

    private Boolean checkForMatchStart(String logLine) {
        if (logLine.contains("GameFlow: ACollectable::BeginPlay")) {
            Event event = new Event(Event.Type.MATCH_START, null);
            setChanged();
            notifyObservers(event);
            return true;
        }
        return false;
    }

    private Boolean checkForMapGeneration(String logLine) {
        Matcher matcher = PATTERN__MAP_GENERATION.matcher(logLine);

        if (!matcher.find()) {
            return false;
        }

        String mapId = matcher.group(1);
        logger.debug("Detected map generation for '{}'", mapId);
        Event event = new Event(Event.Type.MAP_GENERATE, mapId);
        setChanged();
        notifyObservers(event);

        return true;
    }

    private Boolean checkForMatchEnd(String logLine) {
        if (logLine.contains("/api/v1/softWallet/put/analytics")) {
            Event event = new Event(Event.Type.MATCH_END, null);
            setChanged();
            notifyObservers(event);
            return true;
        }
        return false;
    }

    private Boolean checkForServerDisconnect(String logLine) {
        // the first check detects disconnection while the second detects leaving the post-game chat screen
        if (logLine.contains("SetIsDisconnected from: false to: true") ||
                logLine.contains("FOnlineAsyncTaskMirrorsDestroyMatch")) {

            Event event = new Event(Event.Type.SERVER_DISCONNECT, null);
            setChanged();
            notifiedDisconnect = true;
            notifyObservers(event);
            return true;
        }
        return false;
    }

    public void resetKiller() {
        lastPlayer = null;
        lastKillerPlayer = null;
        lastKiller = null;
    }

    public File getLogFile() {
        return logFile;
    }
}
