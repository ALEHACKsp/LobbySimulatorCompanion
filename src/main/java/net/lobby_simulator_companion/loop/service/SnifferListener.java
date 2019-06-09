package net.lobby_simulator_companion.loop.service;

import java.net.Inet4Address;

/**
 * @author Nicky Ramone
 */
public interface SnifferListener {

    void updatePing(Inet4Address ip, int ping);

    void handleException(Exception e);

}
