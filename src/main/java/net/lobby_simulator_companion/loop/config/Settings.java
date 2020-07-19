package net.lobby_simulator_companion.loop.config;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.util.FileUtil;
import org.ini4j.Profile;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Handles user preferences storing them in memory and in disk.
 * A thread will periodically check to see if there are changes in memory that need to be stored in disk.
 * This thread will only perform a save to disk if no properties have been changed during a predefined interval
 * (this is to avoid saving unnecessarily frequently).
 * This means that as long as properties keep being updated within the interval, we will not save and wait until
 * there's no activity.
 *
 * @author NickyRamone
 */
@Slf4j
public class Settings {

    private static final File SETTINGS_FILE = FileUtil.getLoopPath().resolve("loop.ini").toFile();
    private static final long SAVE_INTERVAL_SECONDS = 10;

    private final Wini ini;
    private final Profile.Section globalSection;
    private final Random r = new Random();
    private final Set<Integer> featuresEnabled = new HashSet<>();
    private final Map<Integer, Double> featureChances = new HashMap<>();

    private volatile boolean dirty;
    private Instant lastChange = Instant.now();


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

    public <E extends Enum<E>> E get(String key, Class<E> enumClass, E defaultValue) {
        String value = get(key);

        if (value != null) {
            for (E enumValue : EnumSet.allOf(enumClass)) {
                if (enumValue.name().equalsIgnoreCase(value)) {
                    return enumValue;
                }
            }
        }

        return defaultValue;
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
        LocalDate expFeaturesLastUpdate = Optional.ofNullable(Factory.appProperties().get("app.feature.experimental.lastUpdate"))
                .map(timestamp -> Instant.ofEpochSecond(Long.parseLong(timestamp))
                        .atZone(ZoneId.systemDefault()).toLocalDate())
                .orElse(LocalDate.now());

        int days = (int) DAYS.between(expFeaturesLastUpdate, LocalDate.now());
        double chance = 1 - days / 20.0;
        int i = 1;
        String featureCodeList;

        while (true) {
            featureCodeList = Factory.appProperties().get("app.feature.experimental." + i);
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

    public void set(String key, Enum value) {
        set(key, value.name().toLowerCase());
    }

    public void set(String key, Object value) {
        String oldValue = get(key);
        String newValue = (value == null || value instanceof String) ? (String) value : String.valueOf(value);

        // only set it if the new value differs from the old one
        if (!Objects.equals(oldValue, newValue)) {
            globalSection.put(key, value);
            dirty = true;
            lastChange = Instant.now();
        }
    }

    private void save() {
        int secondsElapsedSinceLastChange = (int) Duration.between(lastChange, Instant.now()).toMillis() / 1000;

        if (secondsElapsedSinceLastChange > SAVE_INTERVAL_SECONDS) {
            forceSave();
        }
    }

    public void forceSave() {
        if (dirty) {
            try {
                log.debug("Saving settings.");
                ini.store();
                dirty = false;
            } catch (IOException e) {
                log.error("Failed to save settings.", e);
            }
        }
    }

}
