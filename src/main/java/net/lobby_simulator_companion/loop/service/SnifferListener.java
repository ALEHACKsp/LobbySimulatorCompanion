package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.domain.Connection;

/**
 * @author NickyRamone
 */
public interface SnifferListener {

    void notifyMatchConnect(Connection connection);

    void notifyMatchDisconnect();

    void handleException(Exception e);

}
