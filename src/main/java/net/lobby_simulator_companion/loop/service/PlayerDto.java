package net.lobby_simulator_companion.loop.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author NickyRamone
 */
@RequiredArgsConstructor
@Data
public class PlayerDto {

    private final String steamId; // Steam id64
    private final String dbdId;

}
