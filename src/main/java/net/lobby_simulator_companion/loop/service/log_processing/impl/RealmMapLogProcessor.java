package net.lobby_simulator_companion.loop.service.log_processing.impl;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.domain.RealmMap;
import net.lobby_simulator_companion.loop.service.log_processing.AbstractDbdLogProcessor;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.util.event.EventSupport;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;

/**
 * @author NickyRamone
 */
@Slf4j
public class RealmMapLogProcessor extends AbstractDbdLogProcessor {

    private static final String REGEX__MAP_GENERATION = "ProceduralLevelGeneration: InitLevel: Theme: .* Map: ([^\\s]+)";
    private static final Pattern PATTERN__MAP_GENERATION = Pattern.compile(REGEX__MAP_GENERATION);
    private static final Map<String, RealmMap> REALM_MAP_BY_ID = Stream.of(RealmMap.values())
            .collect(toMap(RealmMap::getId, identity()));


    public RealmMapLogProcessor(EventSupport eventSupport) {
        super(eventSupport);
    }


    @Override
    public boolean process(String logLine, StateWrapper stateWrapper) {
        Matcher matcher = PATTERN__MAP_GENERATION.matcher(logLine);

        if (!matcher.find()) {
            return false;
        }

        String mapId = matcher.group(1);
        RealmMap realmMap = Optional.ofNullable(REALM_MAP_BY_ID.get(mapId)).orElse(RealmMap.UNIDENTIFIED);

        if (!realmMap.isIdentified()) {
            log.warn("Unable to identify realm map: {}", mapId);
        }

        fireEvent(DbdLogEvent.MAP_GENERATE, realmMap);

        return true;
    }

}
