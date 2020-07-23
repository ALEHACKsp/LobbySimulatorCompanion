package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.domain.Server;
import net.lobby_simulator_companion.loop.repository.ServerDao;
import net.lobby_simulator_companion.loop.repository.SteamProfileDao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author NickyRamone
 */
public class DevModeConfigurer {

    private static final Map<String, String> steamPlayerNamesById = new HashMap<>();


    public static void init() throws IOException {
        Factory.setInstance(SteamProfileDao.class, mockSteamProfileDao());
        Factory.setInstance(ServerDao.class, mockServerDao());
        Factory.gameStateManager().setMinMatchSeconds(5);
    }

    public static void configureMockSteamProfileDaoResponse(String id64, String steamPlayerName) {
        steamPlayerNamesById.put(id64, steamPlayerName);
    }


    private static SteamProfileDao mockSteamProfileDao() throws IOException {
        SteamProfileDao dao = mock(SteamProfileDao.class);
        when(dao.getPlayerName(any())).thenAnswer(
                invocationOnMock -> steamPlayerNamesById.get(invocationOnMock.getArgument(0)));

        return dao;
    }

    private static ServerDao mockServerDao() throws IOException {
        ServerDao dao = mock(ServerDao.class);
        when(dao.getByIpAddress(any())).thenReturn(Server.builder()
                .country("Argentina")
                .city("Buenos Aires")
                .isp("Argento-Server")
                .build());

        return dao;
    }

}
