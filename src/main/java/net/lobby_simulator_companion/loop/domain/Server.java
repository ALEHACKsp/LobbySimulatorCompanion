package net.lobby_simulator_companion.loop.domain;

/**
 * @author NickyRamone
 */
public class Server {

    private final int address; // 32-bit IPv4 address.
    private String hostName;
    private String country;
    private String region;
    private String city;
    private Long latitude;
    private Long longitude;
    private Integer discoveryNumber;
    private long lastUpdate;


    public Server(int address) {
        this.address = address;
        this.lastUpdate = System.currentTimeMillis();
    }


    public int getAddress() {
        return address;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

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

    public Long getLatitude() {
        return latitude;
    }

    public void setLatitude(Long latitude) {
        this.latitude = latitude;
    }

    public Long getLongitude() {
        return longitude;
    }

    public void setLongitude(Long longitude) {
        this.longitude = longitude;
    }

    public Integer getDiscoveryNumber() {
        return discoveryNumber;
    }

    public void setDiscoveryNumber(Integer discoveryNumber) {
        this.discoveryNumber = discoveryNumber;
    }

}
