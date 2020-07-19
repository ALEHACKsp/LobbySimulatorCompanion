package net.lobby_simulator_companion.loop.service.log_processing;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author NickyRamone
 */
@UtilityClass
public class LogProcessorUtil {

    private final String REGEX__TIMESTAMP = "^\\[([^\\[\\]]+)\\].+";
    private final Pattern PATTERN__TIMESTAMP = Pattern.compile(REGEX__TIMESTAMP);

    private final String DBD_LOG_DATE_FORMAT = "u.MM.dd-HH.mm.ss:SSS";
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DBD_LOG_DATE_FORMAT);


    /**
     * @return timestamp in UTC
     */
    public LocalDateTime extractTimestamp(String logLine) {

        Matcher matcher = PATTERN__TIMESTAMP.matcher(logLine);

        if (matcher.find()) {
            String timestampString = matcher.group(1);
            return LocalDateTime.parse(timestampString, DATE_TIME_FORMATTER);
        }

        return null;
    }

}
