package net.lobby_simulator_companion.loop.service;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;

/**
 * @author NickyRamone
 */
public interface DbdLogProcessor {

    /**
     * @return true if the processor chain should stop here, so that no other processors analyze this log line.
     */
    boolean process(String logLine, StateWrapper gameState);

}
