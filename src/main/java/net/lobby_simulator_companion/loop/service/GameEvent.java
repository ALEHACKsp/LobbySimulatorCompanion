package net.lobby_simulator_companion.loop.service;

/**
 * @author NickyRamone
 */
public enum GameEvent {
    DISCONNECTED,
    START_LOBBY_SEARCH,
    CONNECTED_TO_LOBBY,
    NEW_KILLER_PLAYER,
    NEW_KILLER_CHARACTER,
    START_MAP_GENERATION,
    ENTERING_REALM,
    MATCH_STARTED,
    CHASE_STARTED,
    CHASE_ENDED,
    LEFT_REALM,
    MATCH_ENDED,
    UPDATED_CHASE_SUMMARY,
    UPDATED_STATS,
}
