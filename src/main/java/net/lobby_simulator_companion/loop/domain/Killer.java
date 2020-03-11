package net.lobby_simulator_companion.loop.domain;

public enum Killer {
    UNIDENTIFIED(null),
    CANNIBAL("Cannibal"),
    CLOWN("Clown"),
    DEATHSLINGER("Deathslinger"),
    DEMOGORGON("Demogorgon"),
    DOCTOR("Doctor"),
    GHOSTFACE("Ghost Face"),
    HAG("Hag"),
    HILLBILLY("Hillbilly"),
    HUNTRESS("Huntress"),
    LEGION("Legion"),
    NIGHTMARE("Nightmare"),
    NURSE("Nurse"),
    ONI("Oni"),
    PIG("Pig"),
    PLAGUE("Plague"),
    SHAPE("Shape"),
    SPIRIT("Spirit"),
    TRAPPER("Trapper"),
    WRAITH("Wraith");


    private final String alias;


    Killer(String alias) {
        this.alias = alias;
    }

    public String alias() {
        return alias;
    }

    public boolean isIdentified() {
        return this != UNIDENTIFIED;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
