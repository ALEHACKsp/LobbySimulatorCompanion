package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.repository.SteamProfileDao;

import java.util.HashMap;
import java.util.Map;

public class DevModeConfigurer {

    private static final Map<String, String> steamPlayerNamesById = new HashMap<>();


    public static void init() {
        mockSteamProfileDao(steamPlayerNamesById);
    }

    public static void configureMockSteamProfileDaoResponse(String id64, String steamPlayerName) {
        steamPlayerNamesById.put(id64, steamPlayerName);
    }


    private static void mockSteamProfileDao(Map<String, String> playerNamesById) {
        Factory.setInstance(SteamProfileDao.class, new MockSteamProfileDao(playerNamesById));
    }


    private static class MockSteamProfileDao extends SteamProfileDao {

        private final Map<String, String> namesById;


        public MockSteamProfileDao(Map<String, String> namesById) {
            super("");
            this.namesById = namesById;
        }

        @Override
        public String getPlayerName(String id64) {
            return namesById.get(id64);
        }

    }
}
