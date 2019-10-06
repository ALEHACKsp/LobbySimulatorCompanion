package net.lobby_simulator_companion.loop.service;

/**
 *
 * @author Nicky Ramone
 */
public class GeoLocation {

    private final double latitude;
    private final double longitude;

    public GeoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
