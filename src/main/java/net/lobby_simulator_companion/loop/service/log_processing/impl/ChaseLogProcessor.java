package net.lobby_simulator_companion.loop.service.log_processing.impl;

import net.lobby_simulator_companion.loop.domain.Survivor;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.service.log_processing.LogProcessorUtil;
import net.lobby_simulator_companion.loop.service.log_processing.MultiPurposeDbdLogProcessor;
import net.lobby_simulator_companion.loop.service.log_processing.impl.dto.ChaseEvent;
import net.lobby_simulator_companion.loop.service.log_processing.impl.dto.HitEvent;
import net.lobby_simulator_companion.loop.util.event.EventSupport;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;

/**
 * @author NickyRamone
 */
public class ChaseLogProcessor extends MultiPurposeDbdLogProcessor {

    private static final String REGEX__CHASE_START = "Player \\[BP_([^_]+)_(?:Character_)?C_(\\d+)\\] is in chase";
    private static final Pattern PATTERN__CHASE_START = Pattern.compile(REGEX__CHASE_START);

    private static final String REGEX__CHASE_END = "Player \\[BP_([^_]+)_(?:Character_)?C_(\\d+)\\] is not in chase anymore";
    private static final Pattern PATTERN__CHASE_END = Pattern.compile(REGEX__CHASE_END);

    private static final String REGEX__KILLER_HIT = "On Hit Sprint Effect \\[BP_(.+?)_Character_C_(\\d+) - (.+?)\\]: On";
    private static final Pattern PATTERN__KILLER_HIT = Pattern.compile(REGEX__KILLER_HIT);

    public ChaseLogProcessor(EventSupport eventSupport) {
        super(eventSupport);
        addLineProcessors(Arrays.asList(
                this::checkForChaseStart,
                this::checkForChaseEnd,
                this::checkForKillerHit
        ));
    }


    private Boolean checkForChaseStart(String logLine, StateWrapper stateWrapper) {
        Matcher matcher = PATTERN__CHASE_START.matcher(logLine);

        if (matcher.find()) {
            String survivorBlueprintId = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            LocalDateTime timestamp = LogProcessorUtil.extractTimestamp(logLine);
            fireEvent(DbdLogEvent.CHASE_START,
                    new ChaseEvent(timestamp, Survivor.fromBlueprintId(survivorBlueprintId), index));
            return true;
        }

        return false;
    }

    private Boolean checkForChaseEnd(String logLine, StateWrapper stateWrapper) {
        Matcher matcher = PATTERN__CHASE_END.matcher(logLine);

        if (matcher.find()) {
            String survivorBlueprintId = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            LocalDateTime timestamp = LogProcessorUtil.extractTimestamp(logLine);
            fireEvent(DbdLogEvent.CHASE_END,
                    new ChaseEvent(timestamp, Survivor.fromBlueprintId(survivorBlueprintId), index));
            return true;
        }

        return false;
    }


    private Boolean checkForKillerHit(String logLine, StateWrapper stateWrapper) {
        Matcher matcher = PATTERN__KILLER_HIT.matcher(logLine);

        if (matcher.find()) {
            String survivorBlueprintId = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            String playerName = matcher.group(3);
            fireEvent(DbdLogEvent.KILLER_HIT, new HitEvent(Survivor.fromBlueprintId(survivorBlueprintId), index, playerName));
            return true;
        }

        return false;
    }
}
