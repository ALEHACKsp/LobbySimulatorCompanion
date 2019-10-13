package net.lobby_simulator_companion.loop.domain;

/**
 * @author NickyRamone
 */
public class Server {

    private final String address;
    private String hostName;
    private String country;
    private String region;
    private String city;
    private Double latitude;
    private Double longitude;
    private String isp;
    private Integer discoveryNumber;
    private long created;
    private long lastUpdate;


    public Server(String address) {
        this.address = address;
        this.created = System.currentTimeMillis();
        this.lastUpdate = created;
    }


    public String getAddress() {
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public Integer getDiscoveryNumber() {
        return discoveryNumber;
    }

    public void setDiscoveryNumber(Integer discoveryNumber) {
        this.discoveryNumber = discoveryNumber;
    }

}
