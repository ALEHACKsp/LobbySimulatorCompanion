package net.lobby_simulator_companion.loop.util.event;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author NickyRamone
 */
@RequiredArgsConstructor
@Data
public class Event {

    private final Object type;
    private final Object value;

}
