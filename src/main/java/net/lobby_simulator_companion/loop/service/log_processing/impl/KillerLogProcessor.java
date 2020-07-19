package net.lobby_simulator_companion.loop.service.log_processing.impl;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.service.PlayerDto;
import net.lobby_simulator_companion.loop.service.log_processing.AbstractDbdLogProcessor;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.util.event.EventSupport;

import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;
import static net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent.KILLER_CHARACTER;
import static net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent.KILLER_PLAYER;

/**
 * @author NickyRamone
 */
@Slf4j
public class KillerLogProcessor extends AbstractDbdLogProcessor {

    private static final String REGEX__LOBBY_ADD_PLAYER = "AddSessionPlayer.*Session:GameSession PlayerId:([0-9a-f\\-]+)\\|([0-9]+)";
    private static final Pattern PATTERN__LOBBY_ADD_PLAYER = Pattern.compile(REGEX__LOBBY_ADD_PLAYER);

    private static final String REGEX__KILLER_OUTFIT = "LogCustomization: --> ([a-zA-Z0-9]+)_[a-zA-Z0-9]+";
    private static final Pattern PATTERN__KILLER_OUTFIT = Pattern.compile(REGEX__KILLER_OUTFIT);

    private static final Map<Killer, String[]> KILLER_TO_OUTFIT_MAPPING = Stream.of(new Object[][]{
            {Killer.CANNIBAL, new String[]{"CA"}},
            {Killer.CLOWN, new String[]{"GK", "Clown"}},
            {Killer.DEATHSLINGER, new String[]{"UkraineKiller", "UK"}},
            {Killer.DEMOGORGON, new String[]{"QK"}},
            {Killer.DOCTOR, new String[]{"DO", "DOW04", "Killer07"}},
            {Killer.EXECUTIONER, new String[]{"K20"}},
            {Killer.GHOSTFACE, new String[]{"OK"}},
            {Killer.HAG, new String[]{"HA", "WI", "Witch"}},
            {Killer.HILLBILLY, new String[]{"HB", "TC", "Hillbilly"}},
            {Killer.HUNTRESS, new String[]{"BE"}},
            {Killer.LEGION, new String[]{"KK", "Legion"}},
            {Killer.NIGHTMARE, new String[]{"SD"}},
            {Killer.NURSE, new String[]{"TN", "Nurse", "NR"}},
            {Killer.ONI, new String[]{"SwedenKiller"}},
            {Killer.PIG, new String[]{"FK"}},
            {Killer.PLAGUE, new String[]{"MK", "Plague"}},
            {Killer.SHAPE, new String[]{"MM"}},
            {Killer.SPIRIT, new String[]{"HK", "Spirit"}},
            {Killer.TRAPPER, new String[]{"TR", "TRW03", "TRW04", "Chuckles", "S01", "Trapper"}},
            {Killer.WRAITH, new String[]{"TW", "WR", "Wraith"}}

    }).collect(Collectors.toMap(e -> (Killer) e[0], e -> (String[]) e[1]));

    private static final Map<String, Killer> OUTFIT_TO_KILLER_MAPPING =
            KILLER_TO_OUTFIT_MAPPING.entrySet().stream()
                    .flatMap(e -> Stream.of(e.getValue()).map(v -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), v)))
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private PlayerDto lastPlayer;
    private PlayerDto lastKillerPlayer;
    private Killer lastKiller;


    public KillerLogProcessor(EventSupport eventSupport) {
        super(eventSupport);
        registerListener(DbdLogEvent.SERVER_DISCONNECT, evt -> resetKiller());
    }

    @Override
    public boolean process(String logLine, StateWrapper stateWrapper) {
        if (checkForKiller(logLine)) {
            return true;
        }

        return checkForPlayer(logLine);
    }

    private boolean checkForKiller(String logLine) {
        Matcher matcher = PATTERN__KILLER_OUTFIT.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        String outfitCode = matcher.group(1);
        Killer killer = OUTFIT_TO_KILLER_MAPPING.get(outfitCode);

        if (killer == null) {
            // it's a survivor
            lastPlayer = null;
        } else if (lastPlayer == null && lastKillerPlayer == null) {
            // no killer player where to assign this outfit
        } else if (lastPlayer == null && !killer.equals(lastKiller)) {
            // change of outfit for current killer
            lastKiller = killer;
            fireEvent(KILLER_CHARACTER, killer);
        } else if (lastPlayer != null && !lastPlayer.equals(lastKillerPlayer)) {
            // new killer player
            lastKillerPlayer = lastPlayer;
            fireEvent(KILLER_PLAYER, lastKillerPlayer);

            lastKiller = killer;
            fireEvent(KILLER_CHARACTER, killer);
        }

        lastPlayer = null;
        return true;
    }

    private boolean checkForPlayer(String logLine) {
        Matcher matcher = PATTERN__LOBBY_ADD_PLAYER.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        String dbdPlayerId = matcher.group(1);
        String steamUserId = matcher.group(2);
        log.trace("Detected user connecting to lobby. dbd-id: {}; steam-id: {}", dbdPlayerId, steamUserId);
        lastPlayer = new PlayerDto(steamUserId, dbdPlayerId);

        return true;
    }

    private void resetKiller() {
        lastPlayer = null;
        lastKillerPlayer = null;
        lastKiller = null;
    }

}
