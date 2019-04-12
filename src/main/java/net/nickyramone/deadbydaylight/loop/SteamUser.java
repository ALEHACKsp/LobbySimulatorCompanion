package net.nickyramone.deadbydaylight.loop;

/**
 * Represents a Steam user.
 *
 * @author NickyRamone
 */
public class SteamUser {

    private final String id;
    private final String name;


    public SteamUser(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " (https://steamcommunity.com/profiles/" + id + ")";
    }
}
