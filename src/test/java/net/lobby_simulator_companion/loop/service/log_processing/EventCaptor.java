package net.lobby_simulator_companion.loop.service.log_processing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lobby_simulator_companion.loop.util.event.Event;

/**
 * @author NickyRamone
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EventCaptor {
    Object key;
    Object value;

    public void copyFrom(Event event) {
        key = event.getType();
        value = event.getValue();
    }

}
