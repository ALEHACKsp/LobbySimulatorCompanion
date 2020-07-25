package net.lobby_simulator_companion.loop.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.service.log_processing.AbstractDbdLogProcessor;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.util.event.EventListener;
import net.lobby_simulator_companion.loop.util.event.EventSupport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * DBD log parser daemon.
 * Parses logs to extract real-time events occurring during the game.
 *
 * @author NickyRamone
 */
@Slf4j
public class DbdLogMonitor implements Runnable {

    private static final int LOG_POLLING_PERIOD_MS = 1000;

    private static final Path USER_APPDATA_PATH = Paths.get(System.getenv("APPDATA")).getParent();
    private static final String DEFAULT_LOG_PATH = "Local/DeadByDaylight/Saved/Logs/DeadByDaylight.log";
    private static final File DEFAULT_LOG_FILE = USER_APPDATA_PATH.resolve(DEFAULT_LOG_PATH).toFile();

    public enum State {
        IDLE,
        SEARCHING_LOBBY,
        IN_LOBBY,
        GENERATING_MAP,
        IN_MATCH,
        IN_POST_GAME_CHAT,
        MATCH_END;


        public boolean isBefore(State otherState) {
            return this.ordinal() < otherState.ordinal();
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateWrapper {
        public State state = State.IDLE;
    }

    private final StateWrapper stateWrapper = new StateWrapper();
    private final EventSupport eventSupport;
    private final File logFile;
    private final List<DbdLogProcessor> processors = new ArrayList<>();

    private BufferedReader reader;
    private long logSize;


    public DbdLogMonitor(EventSupport eventSupport) {
        this(eventSupport, DEFAULT_LOG_FILE);
    }

    public DbdLogMonitor(EventSupport eventSupport, File logFile) {
        this.eventSupport = eventSupport;
        this.logFile = logFile;
    }


    public void start() throws IOException {
        initReader();

        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }


    private void initReader() throws IOException {
        if (reader != null) {
            reader.close();
        }

        if (!logFile.exists()) {
            logFile.createNewFile();
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
                    eventSupport.fireEvent(DbdLogEvent.SERVER_DISCONNECT);
                }
                logSize = currentLogSize;
                line = reader.readLine();

                if (line != null) {
                    processLine(line);
                } else {
                    // for now, there are no more entries in the file
                    Thread.sleep(LOG_POLLING_PERIOD_MS);
                }
            } catch (IOException e) {
                log.error("Encountered error while processing log file.", e);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void processLine(String line) {
        for (DbdLogProcessor processor : processors) {

            try {
                if (processor.process(line, stateWrapper)) {
                    break;
                }
            } catch (Exception e) {
                log.error("Encountered problem while executing processor '"
                        + processor.getClass().getSimpleName() + "'", e);
            }
        }
    }

    public State getState() {
        return stateWrapper.state;
    }

    public File getLogFile() {
        return logFile;
    }

    public void registerProcessor(AbstractDbdLogProcessor processor) {
        processors.add(processor);
    }


    public void registerListener(Object eventType, EventListener eventListener) {
        eventSupport.registerListener(eventType, eventListener);
    }

}
