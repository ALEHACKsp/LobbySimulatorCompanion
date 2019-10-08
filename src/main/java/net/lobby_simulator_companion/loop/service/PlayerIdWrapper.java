package net.lobby_simulator_companion.loop.service;

public class PlayerIdWrapper {

    private final String steamId; // Steam id64
    private final String dbdId;

    public PlayerIdWrapper(String steamId, String dbdId) {
        this.steamId = steamId;
        this.dbdId = dbdId;
    }

    public String getSteamId() {
        return steamId;
    }

    public String getDbdId() {
        return dbdId;
    }
}
