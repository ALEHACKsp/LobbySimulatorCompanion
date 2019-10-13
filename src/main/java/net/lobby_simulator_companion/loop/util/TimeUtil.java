package net.lobby_simulator_companion.loop.util;

/**
 * @author NickyRamone
 */
public final class TimeUtil {

    private TimeUtil() {
        // this class should not be instantiated
    }

    public static String formatTimeElapsed(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int mod = totalSeconds % 3600;
        int minutes = mod / 60;
        int seconds = mod % 60;

        return hours == 0 ? String.format("%02d:%02d", minutes, seconds) :
                String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
