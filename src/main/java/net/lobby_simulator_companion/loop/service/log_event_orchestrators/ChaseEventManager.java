package net.lobby_simulator_companion.loop.service.log_event_orchestrators;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.domain.Survivor;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.service.log_processing.impl.dto.ChaseEvent;
import net.lobby_simulator_companion.loop.service.log_processing.impl.dto.HitEvent;
import net.lobby_simulator_companion.loop.util.event.EventListener;
import net.lobby_simulator_companion.loop.util.event.SwingEventSupport;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static javax.swing.SwingUtilities.invokeLater;

/**
 * @author NickyRamone
 */
@Slf4j
public class ChaseEventManager {

    public enum Event {
        CHASE_START,
        CHASE_END
    }

    @Builder
    @Getter
    @ToString
    public static final class ChaseInfo {
        private Survivor survivor;
        private int survivorTeamIdx;
        private String playerName;
        private long totalChaseMillis;
        private int chaseCount;
    }

    @AllArgsConstructor
    @Data
    private static final class ChaseKey {
        Survivor survivor;
        int survivorTeamIdx;
    }

    private static final class Chase {
        String playerName;
        LocalDateTime chaseStart;
        int totalChaseMillis;
        int chaseCount;

        void endChase(LocalDateTime chaseEnd) {
            if (chaseStart != null) {
                totalChaseMillis += Duration.between(chaseStart, chaseEnd).toMillis();
                chaseStart = null;
            }
        }
    }

    private final DbdLogMonitor dbdLogMonitor;
    private final SwingEventSupport eventSupport = new SwingEventSupport();

    private Map<ChaseKey, Chase> chases = new HashMap<>();
    private EnumSet<Survivor> duplicateCharacters = EnumSet.noneOf(Survivor.class);


    public ChaseEventManager(DbdLogMonitor dbdLogMonitor) {
        this.dbdLogMonitor = dbdLogMonitor;

        initListeners();
    }


    private void initListeners() {
        dbdLogMonitor.registerListener(DbdLogEvent.SERVER_CONNECT,
                evt -> handleLobbyConnect());
        dbdLogMonitor.registerListener(DbdLogEvent.CHASE_START,
                evt -> invokeLater(() -> handleChaseStart((ChaseEvent) evt.getValue())));
        dbdLogMonitor.registerListener(DbdLogEvent.CHASE_END,
                evt -> invokeLater(() -> handleChaseEnd((ChaseEvent) evt.getValue())));
        dbdLogMonitor.registerListener(DbdLogEvent.KILLER_HIT,
                evt -> handleKillerHit((HitEvent) evt.getValue()));
    }


    private void handleLobbyConnect() {
        chases.clear();
    }

    private void handleChaseStart(ChaseEvent chaseEvent) {
        Survivor survivor = chaseEvent.getSurvivor();
        int duplicateIdx = chaseEvent.getIndex();
        boolean duplicateCharacter = false;

        if (duplicateIdx > 0) {
            duplicateCharacter = true;
            duplicateCharacters.add(survivor);
        }

        ChaseKey chaseKey = new ChaseKey(survivor, duplicateIdx);
        Chase chase = updateChase(chaseKey, c -> {
            c.chaseCount++;
            c.chaseStart = chaseEvent.getTimestamp();
        });


        ChaseInfo chaseInfo = ChaseInfo.builder()
                .survivor(survivor)
                .survivorTeamIdx(duplicateCharacter ? duplicateIdx : -1)
                .playerName(chase.playerName)
                .chaseCount(chase.chaseCount)
                .totalChaseMillis(chase.totalChaseMillis)
                .build();

        eventSupport.fireEvent(Event.CHASE_START, chaseInfo);
    }

    private void handleChaseEnd(ChaseEvent chaseEvent) {
        ChaseKey chaseKey = new ChaseKey(chaseEvent.getSurvivor(), chaseEvent.getIndex());
        Chase chase = chases.get(chaseKey);
        if (chase != null) {
            chase.endChase(chaseEvent.getTimestamp());
        }

        eventSupport.fireEvent(Event.CHASE_END);
    }

    private void handleKillerHit(HitEvent hitEvent) {
        ChaseKey chaseKey = new ChaseKey(hitEvent.getSurvivor(), hitEvent.getIndex());
        updateChase(chaseKey, chase -> chase.playerName = hitEvent.getSurvivorPlayerName());
    }

    /**
     * Contrary to @{@link Map#putIfAbsent(Object, Object)} , this one returns the updated value.
     */
    private Chase updateChase(ChaseKey chaseKey, Consumer<Chase> chaseUpdater) {
        Chase chase = chases.get(chaseKey);

        if (chase == null) {
            chase = new Chase();
            chases.put(chaseKey, chase);
        }

        chaseUpdater.accept(chase);
        return chase;
    }


    public List<ChaseInfo> getChaseSummary() {
        return chases.entrySet().stream()
                .map(e -> ChaseInfo.builder()
                        .survivor(e.getKey().survivor)
                        .survivorTeamIdx(duplicateCharacters.contains(e.getKey().survivor) ?
                                e.getKey().survivorTeamIdx : -1)
                        .playerName(e.getValue().playerName)
                        .totalChaseMillis(e.getValue().totalChaseMillis)
                        .chaseCount(e.getValue().chaseCount)
                        .build())
                .sorted((c1, c2) -> Long.compare(c2.totalChaseMillis, c1.totalChaseMillis))
                .collect(Collectors.toList());
    }


    public void registerEventListener(Event eventType, EventListener listener) {
        eventSupport.registerListener(eventType, listener);
    }

}
