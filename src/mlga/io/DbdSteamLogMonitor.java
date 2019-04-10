package mlga.io;

import mlga.SteamUser;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;
import java.util.Observer;
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
public class DbdSteamLogMonitor extends Observable implements Runnable {

    private static final int LOG_POLLING_PERIOD_MS = 1000;

    private static final Path USER_APPDATA_PATH = Paths.get(System.getenv("APPDATA")).getParent();
    private static final String LOG_PATH = "Local/DeadByDaylight/Saved/Logs/DeadByDaylight.log";
    private static final File LOG_FILE = USER_APPDATA_PATH.resolve(LOG_PATH).toFile();

    private static final String REGEX__QUEUE_JOIN_LOBBY = "MirrorsMatchmaking: OnJoinSessionLogDelegate \\(Platform\\): QueueJoinLobby (.+)";
    private static final Pattern PATTERN__QUEUE_JOIN_LOBBY = Pattern.compile(REGEX__QUEUE_JOIN_LOBBY);
    private static final String REGEX__LOBBY_PARAMS = "steam.([0-9]+)//Game/Maps/OfflineLobby";
    private static final Pattern PATTERN__LOBBY_PARAMS = Pattern.compile(REGEX__LOBBY_PARAMS);

    private BufferedReader reader;
    private String lastSteamIdFound;
    private String lastSteamNameFound;
    private long logSize;


    public DbdSteamLogMonitor(Observer observer) throws IOException {
        initReader();
        addObserver(observer);
    }


    private void initReader() throws IOException {
        if (reader != null) {
            reader.close();
        }
        clearUser();
        reader = new BufferedReader(new FileReader(LOG_FILE));

        // consume all entries in the log file, since they are old and cannot be related to any active connection.
        while (reader.readLine() != null) ;

        if (Settings.SIMULATE_TRAFFIC) {
            lastSteamNameFound = "DummyUser";
            lastSteamIdFound = "12345";
            notifyObservers();
        }
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
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void processLine(String line) {
        Matcher matcher = PATTERN__QUEUE_JOIN_LOBBY.matcher(line);
        if (matcher.find()) {
            String steamUserName = matcher.group(1).trim();
            lastSteamNameFound = steamUserName;
            lastSteamIdFound = null;
            System.out.println("joined lobby of user: " + steamUserName);
            return;
        }

        // any of the below entries depend on having found the steam user name first
        if (lastSteamNameFound == null) {
            return;
        }

        matcher = PATTERN__LOBBY_PARAMS.matcher(line);
        if (matcher.find()) {
            String steamUserId = matcher.group(1).trim();
            lastSteamIdFound = steamUserId;
            System.out.println("Found user id: " + steamUserId);
            setChanged();
            notifyObservers();
        }
    }

    public SteamUser getLastSteamUserFound() {
        if (lastSteamNameFound == null || lastSteamIdFound == null) {
            return null;
        }

        return new SteamUser(lastSteamIdFound, lastSteamNameFound);
    }

    private void clearUser() {
        lastSteamIdFound = null;
        lastSteamNameFound = null;
    }
}
