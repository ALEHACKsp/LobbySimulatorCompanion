package net.lobby_simulator_companion.loop.service.log_processing;

import net.lobby_simulator_companion.loop.service.DbdLogProcessor;
import net.lobby_simulator_companion.loop.util.event.EventListener;
import net.lobby_simulator_companion.loop.util.event.EventSupport;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;

/**
 * @author NickyRamone
 */
public abstract class AbstractDbdLogProcessor implements DbdLogProcessor {

    private EventSupport eventSupport;


    public AbstractDbdLogProcessor(EventSupport eventSupport) {
        this.eventSupport = eventSupport;
    }


    @Override
    public abstract boolean process(String logLine, StateWrapper stateWrapper);


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
