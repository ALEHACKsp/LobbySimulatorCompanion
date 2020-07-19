package net.lobby_simulator_companion.loop.domain;

import lombok.Builder;
import lombok.Value;

/**
 * @author NickyRamone
 */
@Builder
@Value
public class Server {

    private final String address;
    private String hostName;
    private String country;
    private String region;
    private String city;
    private Double latitude;
    private Double longitude;
    private String isp;

}
