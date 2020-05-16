package net.lobby_simulator_companion.loop.config;

import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.util.FileUtil;
import org.ini4j.Profile;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Handles user preferences storing them in memory and in disk.
 * A thread will periodically check to see if there are changes in memory that need to be stored in disk.
 * This thread will only perform a save to disk if no properties have been changed during a predefined interval.
 *
 * @author NickyRamone
 */
public class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);
    private static final File SETTINGS_FILE = FileUtil.getLoopPath().resolve("loop.ini").toFile();
    private static final long SAVE_INTERVAL_SECONDS = 10;

    private final Wini ini;
    private final Profile.Section globalSection;
    private final Random r = new Random();
    private volatile boolean dirty;
    private long lastChange;
    private Set<Integer> featuresEnabled = new HashSet<>();
    private Map<Integer, Double> featureChances = new HashMap<>();


    public Settings() throws IOException {
        if (!SETTINGS_FILE.exists()) {
            if (!SETTINGS_FILE.createNewFile()) {
                throw new IOException("Failed to initialize settings manager. File does not exist "
                        + "and it could not be created.");
            }
        }
        ini = new Wini(SETTINGS_FILE);

        if (ini.isEmpty()) {
            ini.add("?");
        }
        globalSection = ini.get("?");

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_INTERVAL_SECONDS * 1000, SAVE_INTERVAL_SECONDS * 1000);

        initSwitches();
    }


    public String get(String key) {
        return globalSection.get(key);
    }

    public String get(String key, String defaultValue) {
        String value = globalSection.get(key);

        return value != null ? value : defaultValue;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        String val = globalSection.get(key);

        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = globalSection.get(key);

        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    private void initSwitches() {
        LocalDate expFeaturesLastUpdate = Instant.ofEpochSecond(
                Long.parseLong(Factory.getAppProperties().get("app.feature.experimental.lastUpdate")))
                .atZone(ZoneId.systemDefault()).toLocalDate();
        int days = (int) DAYS.between(expFeaturesLastUpdate, LocalDate.now());
        double chance = 1 - days / 20.0;
        int i = 1;
        String featureCodeList;

        while (true) {
            featureCodeList = Factory.getAppProperties().get("app.feature.experimental." + i);
            if (featureCodeList == null) {
                break;
            }
            int[] codes = Arrays.stream(featureCodeList.split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();

            for (int j = 0; j < codes.length; j++) {
                int code = codes[j];
                boolean switchVal = getBoolean(String.format("loop.feature.experimental.%s",
                        Integer.toHexString((code >>> 4) | (code << (Integer.SIZE - 4)))), false);

                if (switchVal) {
                    featuresEnabled.add(i);
                    featureChances.put(i, (j == codes.length - 1 ? 1 : chance));
                    break;
                } else {
                    featureChances.put(i, 0.0);
                }
            }
            i++;
        }
    }

    public boolean getExperimentalSwitch(int featureNum) {
        return featuresEnabled.contains(featureNum);
    }

    public boolean getExperimentalSwitchWithChance(int featureNum) {
        return r.nextDouble() <= featureChances.get(featureNum);
    }

    public void set(String key, Object value) {
        String oldValue = get(key);
        String newValue = (value == null || value instanceof String) ? (String) value : String.valueOf(value);

        // only set it if the new value differs from the old one
        if (!Objects.equals(oldValue, newValue)) {
            globalSection.put(key, value);
            dirty = true;
            lastChange = System.currentTimeMillis();
        }
    }

    public void save() {
        int secondsElapsedSinceLastChange = (int) (System.currentTimeMillis() - lastChange) / 1000;

        if (secondsElapsedSinceLastChange > SAVE_INTERVAL_SECONDS) {
            forceSave();
        }
    }

    public void forceSave() {
        if (dirty) {
            try {
                logger.debug("Saving settings.");
                ini.store();
                dirty = false;
            } catch (IOException e) {
                logger.error("Failed to save settings.", e);
            }
        }
    }

}
