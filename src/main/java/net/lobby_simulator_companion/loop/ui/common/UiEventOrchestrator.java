package net.lobby_simulator_companion.loop.ui.common;

import net.lobby_simulator_companion.loop.util.event.EventListener;
import net.lobby_simulator_companion.loop.util.event.EventSupport;
import net.lobby_simulator_companion.loop.util.event.SwingEventSupport;

/**
 * @author NickyRamone
 */
public class UiEventOrchestrator {

    public enum UiEvent {
        STRUCTURE_RESIZED,
        UPDATE_KILLER_PLAYER,
        UPDATE_KILLER_PLAYER_TITLE_EXTRA,
        UPDATE_KILLER_PLAYER_RATING
    }

    private final EventSupport eventSupport = new SwingEventSupport();


    public void registerListener(EventListener eventListener) {
        eventSupport.registerListener(eventListener);
    }

    public void registerListener(Object eventType, EventListener eventListener) {
        eventSupport.registerListener(eventType, eventListener);
    }

    public void fireEvent(Object eventType) {
        eventSupport.fireEvent(eventType);
    }

    public void fireEvent(Object eventType, Object eventValue) {
        eventSupport.fireEvent(eventType, eventValue);
    }

}
