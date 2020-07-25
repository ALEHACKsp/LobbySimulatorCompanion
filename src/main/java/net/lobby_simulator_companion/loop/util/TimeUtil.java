package net.lobby_simulator_companion.loop.util;

import lombok.experimental.UtilityClass;

/**
 * @author NickyRamone
 */
@UtilityClass
public class TimeUtil {

    private static final int SECONDS_IN_A_MINUTE = 60;
    private static final int SECONDS_IN_AN_HOUR = 60 * SECONDS_IN_A_MINUTE;
    private static final int SECONDS_IN_A_DAY = 24 * SECONDS_IN_AN_HOUR;
    private static final int SECONDS_IN_A_WEEK = 7 * SECONDS_IN_A_DAY;
    private static final int SECONDS_IN_A_MONTH = 30 * SECONDS_IN_A_DAY;
    private static final int SECONDS_IN_A_YEAR = 365 * SECONDS_IN_A_DAY;


    public static String formatTimeUpToHours(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int mod = totalSeconds % 3600;
        int minutes = mod / 60;
        int seconds = mod % 60;

        return hours == 0 ? String.format("%02d:%02d", minutes, seconds) :
                String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }


    public static String formatTimeUpToYearsVerbose(int totalSeconds) {
        int years = totalSeconds / SECONDS_IN_A_YEAR;
        int mod = totalSeconds % SECONDS_IN_A_YEAR;
        int months = mod / SECONDS_IN_A_MONTH;
        mod = mod % SECONDS_IN_A_MONTH;
        int weeks = mod / SECONDS_IN_A_WEEK;
        mod = mod % SECONDS_IN_A_WEEK;
        int days = mod / SECONDS_IN_A_DAY;
        mod = mod % SECONDS_IN_A_DAY;
        int hours = mod / SECONDS_IN_AN_HOUR;
        mod = mod % SECONDS_IN_AN_HOUR;
        int minutes = Math.round((float) mod / SECONDS_IN_A_MINUTE);

        StringBuilder builder = new StringBuilder();
        if (years == 1) {
            builder.append(String.format(", %d year", years));
        } else if (years > 1) {
            builder.append(String.format(", %d years", years));
        }

        if (months == 1) {
            builder.append(String.format(", %d month", months));
        } else if (months > 1) {
            builder.append(String.format(", %d months", months));
        }

        if (weeks == 1) {
            builder.append(String.format(", %d week", weeks));
        } else if (months > 1) {
            builder.append(String.format(", %d weeks", weeks));
        }

        if (days == 1) {
            builder.append(String.format(", %d day", days));
        } else if (days > 1) {
            builder.append(String.format(", %d days", days));
        }

        if (hours == 1) {
            builder.append(String.format(", %d hour", hours));
        } else if (hours > 1) {
            builder.append(String.format(", %d hours", hours));
        }

        if (minutes == 1) {
            builder.append(String.format(", %d minute", minutes));
        } else if (minutes > 1) {
            builder.append(String.format(", %d minutes", minutes));
        }

        String result = builder.toString();

        if (result.isEmpty()) {
            result = "0 seconds";
        } else {
            // remove the comma and space at the beginning of the string
            result = builder.toString().substring(2);
        }

        return result;
    }

    public static String formatTimeUpToYears(Integer totalSeconds) {
        if (totalSeconds == null) {
            return null;
        }

        int years = totalSeconds / SECONDS_IN_A_YEAR;
        int mod = totalSeconds % SECONDS_IN_A_YEAR;
        int months = mod / SECONDS_IN_A_MONTH;
        mod = mod % SECONDS_IN_A_MONTH;
        int weeks = mod / SECONDS_IN_A_WEEK;
        mod = mod % SECONDS_IN_A_WEEK;
        int days = mod / SECONDS_IN_A_DAY;
        mod = mod % SECONDS_IN_A_DAY;
        int hours = mod / SECONDS_IN_AN_HOUR;
        mod = mod % SECONDS_IN_AN_HOUR;
        int minutes = mod / SECONDS_IN_A_MINUTE;
        mod = mod % SECONDS_IN_A_MINUTE;
        int seconds = mod;

        StringBuilder builder = new StringBuilder();
        if (years > 0) {
            builder.append(String.format(" %dy", years));
        }
        if (months > 0) {
            builder.append(String.format(" %dmo", months));
        }
        if (weeks > 0) {
            builder.append(String.format(" %dw", weeks));
        }
        if (days > 0) {
            builder.append(String.format(" %dd", days));
        }
        if (hours > 0) {
            builder.append(String.format(" %dh", hours));
        }
        if (minutes > 0) {
            builder.append(String.format(" %dm", minutes));
        }
        if (seconds > 0) {
            builder.append(String.format(" %ds", seconds));
        }

        String result = builder.toString();

        if (result.isEmpty()) {
            result = "0s";
        } else {
            // remove the space at the beginning of the string
            result = builder.toString().substring(1);
        }

        return result;
    }

}
