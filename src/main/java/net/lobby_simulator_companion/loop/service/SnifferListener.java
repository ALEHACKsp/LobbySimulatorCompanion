package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.domain.Connection;

/**
 * @author NickyRamone
 */
public interface SnifferListener {

    void notifyNewConnection(Connection connection);

    void notifyDisconnect();

    void handleException(Exception e);

}
