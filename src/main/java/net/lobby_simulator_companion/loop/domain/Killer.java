package net.lobby_simulator_companion.loop.domain;

public enum Killer {
    UNIDENTIFIED(null),
    CANNIBAL("The Cannibal"),
    CLOWN("The Clown"),
    DEMOGORGON("the Demogorgon"),
    DOCTOR("The Doctor"),
    GHOSTFACE("The Ghost Face"),
    HAG("The Hag"),
    HILLBILLY("The Hillbilly"),
    HUNTRESS("The Huntress"),
    LEGION("The Legion"),
    NIGHTMARE("The Nightmare"),
    NURSE("The Nurse"),
    ONI("The Oni"),
    PIG("The Pig"),
    PLAGUE("The Plague"),
    SHAPE("The Shape"),
    SPIRIT("The Spirit"),
    TRAPPER("The Trapper"),
    WRAITH("The Wraith");


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
