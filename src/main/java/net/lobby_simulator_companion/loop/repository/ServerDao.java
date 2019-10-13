package net.lobby_simulator_companion.loop.repository;

import net.lobby_simulator_companion.loop.domain.Server;

import java.io.IOException;

/**
 * @author NickyRamone
 */
public interface ServerDao {

    Server getByIpAddress(String ipAddress) throws IOException;
}
