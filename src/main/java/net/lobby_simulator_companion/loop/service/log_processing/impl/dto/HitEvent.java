package net.lobby_simulator_companion.loop.service.log_processing.impl.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.lobby_simulator_companion.loop.domain.Survivor;

/**
 * @author NickyRamone
 */
@RequiredArgsConstructor
@Data
public class HitEvent {

    private final Survivor survivor;
    private final int index;
    private final String survivorPlayerName;

}
