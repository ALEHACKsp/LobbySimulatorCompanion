package net.lobby_simulator_companion.loop.service.log_processing.impl.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.lobby_simulator_companion.loop.domain.Survivor;

import java.time.LocalDateTime;

/**
 * @author NickyRamone
 */
@RequiredArgsConstructor
@Data
public class ChaseEvent {

    private final LocalDateTime timestamp;
    private final Survivor survivor;
    private final int index;

}
