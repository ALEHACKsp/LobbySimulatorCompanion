package net.lobby_simulator_companion.loop.repository;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import net.lobby_simulator_companion.loop.domain.Server;

import java.io.IOException;

/**
 * Data access object for "Extreme-Ip-Lookup" queries.
 * https://extreme-ip-lookup.com/
 *
 * @author NickyRamone
 */
public class ExtremeIpDao implements ServerDao {

    private static final JsonObjectParser jsonObjParser = new JsonObjectParser(new JacksonFactory());
    private static final HttpRequestFactory httpReqFactory = new NetHttpTransport().createRequestFactory(
            request -> request.setParser(jsonObjParser));

    private final String serviceUrlPrefix;


    public ExtremeIpDao(String serviceUrlPrefix) {
        this.serviceUrlPrefix = serviceUrlPrefix;
    }


    @Override
    public Server getByIpAddress(String ipAddress) throws IOException {
        ExtremeIpDto dto = getIpAddressInfo(ipAddress);

        return convertDtoToDomain(ipAddress, dto);
    }

    private ExtremeIpDto getIpAddressInfo(String ipAddress) throws IOException {
        String url = serviceUrlPrefix + ipAddress;
        HttpRequest req = httpReqFactory.buildGetRequest(new GenericUrl(url));

        return req.execute().parseAs(ExtremeIpDto.class);
    }


    private Server convertDtoToDomain(String ipAddress, ExtremeIpDto dto) {
        return Server.builder()
                .address(ipAddress)
                .city(dto.getCity())
                .country(dto.getCountry())
                .region(dto.getRegion())
                .hostName(dto.getIpName())
                .latitude(Double.parseDouble(dto.getLat()))
                .longitude(Double.parseDouble(dto.getLon()))
                .isp(dto.getIsp())
                .build();
    }

}
