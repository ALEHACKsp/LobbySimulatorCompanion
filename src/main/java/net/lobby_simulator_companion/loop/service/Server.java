package net.lobby_simulator_companion.loop.service;

/**
 *
 * @author NickyRamone
 */
public class Server {

    private String country;
    private String region;
    private String city;
    private GeoLocation geoLocation;
    private Integer discoveryNumber;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public Integer getDiscoveryNumber() {
        return discoveryNumber;
    }

    public void setDiscoveryNumber(Integer discoveryNumber) {
        this.discoveryNumber = discoveryNumber;
    }

}
