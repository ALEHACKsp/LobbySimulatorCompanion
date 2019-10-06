package net.lobby_simulator_companion.loop.service;

/**
 * @author Nicky Ramone
 */
public interface SnifferListener {

    void notifyNewConnection(Connection connection);

    void notifyDisconnect();

    void handleException(Exception e);

}
