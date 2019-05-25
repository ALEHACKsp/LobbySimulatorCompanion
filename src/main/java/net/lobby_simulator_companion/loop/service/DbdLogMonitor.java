package net.lobby_simulator_companion.loop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DBD log parser daemon.
 * Parses logs to extract information about the Steam users that we are connecting to.
 * <p>
 * Note: This approach is quite hacky and not a great solution to the problem, but it does its job fairly easily.
 * A better way would be to obtain the information from the UDP packets, if possible.
 *
 * @author NickyRamone
 */
public class DbdLogMonitor extends Observable implements Runnable {

    private static final int LOG_POLLING_PERIOD_MS = 1000;

    private static final Logger logger = LoggerFactory.getLogger(DbdLogMonitor.class);

    private static final Path USER_APPDATA_PATH = Paths.get(System.getenv("APPDATA")).getParent();
    private static final String LOG_PATH = "Local/DeadByDaylight/Saved/Logs/DeadByDaylight.log";
    private static final File LOG_FILE = USER_APPDATA_PATH.resolve(LOG_PATH).toFile();

    private static final String REGEX__USERNAME = "STEAM: - Id: (.+?) \\[0x[0-9A-F]+\\]";
    private static final Pattern PATTERN__USERNAME = Pattern.compile(REGEX__USERNAME);
    private static final String REGEX__LOBBY_PARAMS = "steam.([0-9]+)//Game/Maps/OfflineLobby";
    private static final Pattern PATTERN__LOBBY_PARAMS = Pattern.compile(REGEX__LOBBY_PARAMS);

    private BufferedReader reader;
    private String lastSteamIdFound;
    private String lastSteamNameFound;
    private long logSize;


    public DbdLogMonitor() throws IOException {
        initReader();
    }


    private void initReader() throws IOException {
        if (reader != null) {
            reader.close();
        }
        clearUser();
        reader = new BufferedReader(new FileReader(LOG_FILE));

        // consume all entries in the log file, since they are old and cannot be related to any active connection.
        while (reader.readLine() != null) ;
    }


    @Override
    public void run() {
        String line;

        while (true) {
            try {
                long currentLogSize = LOG_FILE.length();

                if (currentLogSize < logSize) {
                    // the log file has been recreated (probably due to DBD being restarted),
                    // so we need to re-instantiate the reader
                    initReader();
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
                logger.error("Encountered error while reading from the log file.", e);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void processLine(String line) {
        Matcher matcher = PATTERN__LOBBY_PARAMS.matcher(line);
        if (matcher.find()) {
            String steamUserId = matcher.group(1).trim();

            if (!steamUserId.equals(lastSteamIdFound)) {
                lastSteamIdFound = steamUserId;
                lastSteamNameFound = null;
                logger.debug("Detected host user id: {}", steamUserId);
            }
            return;
        }

        // any of the below entries depend on having found the steam user id first
        if (lastSteamIdFound == null || lastSteamNameFound != null) {
            return;
        }

        matcher = PATTERN__USERNAME.matcher(line);
        if (matcher.find()) {
            String steamUserName = matcher.group(1).trim();

            if (!steamUserName.equals(lastSteamNameFound)) {
                lastSteamNameFound = steamUserName;
                logger.debug("Detected host user name: {}", steamUserName);
                setChanged();
                notifyObservers(new SteamUser(lastSteamIdFound, lastSteamNameFound));
                lastSteamIdFound = null;
                lastSteamNameFound = null;
            }
        }
    }

    private void clearUser() {
        lastSteamIdFound = null;
        lastSteamNameFound = null;
    }
}
