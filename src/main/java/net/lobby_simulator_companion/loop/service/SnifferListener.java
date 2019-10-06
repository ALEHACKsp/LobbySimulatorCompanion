package net.lobby_simulator_companion.loop.service;

/**
 * @author NickyRamone
 */
public interface SnifferListener {

    void notifyNewConnection(Connection connection);

    void notifyDisconnect();

    void handleException(Exception e);

}
