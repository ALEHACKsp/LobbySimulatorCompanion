package net.lobby_simulator_companion.loop.service;

import java.util.Objects;

/**
 * @author NickyRamone
 */
public class PlayerDto {

    private final String steamId; // Steam id64
    private final String dbdId;
    private String character;

    public PlayerDto(String steamId, String dbdId) {
        this.steamId = steamId;
        this.dbdId = dbdId;
    }

    public String getSteamId() {
        return steamId;
    }

    public String getDbdId() {
        return dbdId;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerDto playerDto = (PlayerDto) o;
        return Objects.equals(steamId, playerDto.steamId) &&
                Objects.equals(dbdId, playerDto.dbdId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(steamId, dbdId);
    }

    @Override
    public String toString() {
        return "PlayerDto{" +
                "steamId='" + steamId + '\'' +
                ", dbdId='" + dbdId + '\'' +
                ", character='" + character + '\'' +
                '}';
    }
}
