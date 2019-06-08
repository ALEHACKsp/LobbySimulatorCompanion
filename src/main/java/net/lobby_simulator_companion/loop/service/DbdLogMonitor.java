package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.dao.SteamProfileDao;
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

    private static final String REGEX__LOBBY_PARAMS = "steam.([0-9]+)//Game/Maps/OfflineLobby";
    private static final Pattern PATTERN__LOBBY_PARAMS = Pattern.compile(REGEX__LOBBY_PARAMS);

    private SteamProfileDao steamProfileDao;
    private BufferedReader reader;
    private long logSize;


    public DbdLogMonitor(SteamProfileDao steamProfileDao) throws IOException {
        this.steamProfileDao = steamProfileDao;
        initReader();
    }


    private void initReader() throws IOException {
        if (reader != null) {
            reader.close();
        }
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
                logger.error("Encountered error while processing log file.", e);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void processLine(String line) throws IOException {
        Matcher matcher = PATTERN__LOBBY_PARAMS.matcher(line);
        if (matcher.find()) {
            String steamUserId = matcher.group(1).trim();
            logger.debug("Detected host user id: {}", steamUserId);
            logger.debug("Fetching user name...");
            String playerName = steamProfileDao.getPlayerName(steamUserId);
            logger.debug("Retrieved Steam user name: {}", playerName);
            setChanged();
            notifyObservers(new SteamUser(steamUserId, playerName));
        }
    }

}
