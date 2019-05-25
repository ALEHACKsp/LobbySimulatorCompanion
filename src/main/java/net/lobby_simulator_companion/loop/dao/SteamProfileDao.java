package net.lobby_simulator_companion.loop.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DAO for retrieving info from public Steam profiles.
 *
 * @author NickyRamone
 */
public class SteamProfileDao {

    private static final String REGEX__PROFILE_DATA = "g_rgProfileData[ ]*=[ ]*\\{[^;]+?\"personaname\":\"([^\"]+)\"[^;]+?;";
    private static final Pattern PATTERN__PROFILE_DATA = Pattern.compile(REGEX__PROFILE_DATA);

    private String profileUrlPrefix;


    public SteamProfileDao(String profileUrlPrefix) {
        this.profileUrlPrefix = profileUrlPrefix.endsWith("/") ? profileUrlPrefix : profileUrlPrefix + "/";
    }

    public String getPlayerName(String id64) throws IOException {
        String playerName = null;
        URL oracle = new URL(profileUrlPrefix + id64);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null && playerName == null) {

                Matcher matcher = PATTERN__PROFILE_DATA.matcher(inputLine);
                if (matcher.find()) {
                    playerName = matcher.group(1);
                }
            }
        }

        return playerName;
    }
}
