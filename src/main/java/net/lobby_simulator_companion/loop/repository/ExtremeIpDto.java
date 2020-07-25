package net.lobby_simulator_companion.loop.repository;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class ExtremeIpDto {

    @Key
    private String city;

    @Key
    private String continent;

    @Key
    private String country;

    @Key
    private String countryCode;

    @Key
    private String ipName;

    @Key
    private String lat;

    @Key
    private String lon;

    @Key
    private String region;

    @Key
    private String isp;

}
