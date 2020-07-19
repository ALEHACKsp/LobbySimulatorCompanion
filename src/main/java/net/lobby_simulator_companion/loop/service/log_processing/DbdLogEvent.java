package net.lobby_simulator_companion.loop.service.log_processing;

/**
 * @author NickyRamone
 */
public enum DbdLogEvent {

    MATCH_WAIT,
    MATCH_WAIT_CANCEL,
    SERVER_CONNECT,
    KILLER_PLAYER,
    KILLER_CHARACTER,
    MAP_GENERATE,
    REALM_ENTER,
    MATCH_START,
    CHASE_START,
    KILLER_HIT,
    CHASE_END,
    USER_LEFT_REALM,
    SURVIVED,
    MATCH_END,
    SERVER_DISCONNECT

}
