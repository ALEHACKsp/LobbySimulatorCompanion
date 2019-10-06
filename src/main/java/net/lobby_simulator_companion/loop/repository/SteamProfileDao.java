package net.lobby_simulator_companion.loop.repository;

import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DAO for retrieving info from public Steam profiles.
 *
 * @author NickyRamone
 */
public class SteamProfileDao {

    private static final Charset HTML_ENCODING_CHARSET = Charset.forName("UTF-8");
    private static final String REGEX__PROFILE_DATA = "g_rgProfileData[ ]*=[ ]*\\{[^;]+?\"personaname\":\"([^\"]+)\"[^;]+?;";
    private static final Pattern PATTERN__PROFILE_DATA = Pattern.compile(REGEX__PROFILE_DATA);

    private String profileUrlPrefix;


    public SteamProfileDao(String profileUrlPrefix) {
        this.profileUrlPrefix = profileUrlPrefix.endsWith("/") ? profileUrlPrefix : profileUrlPrefix + "/";
    }

    public String getPlayerName(String id64) throws IOException {
        String playerName = null;
        URL profileUrl = new URL(profileUrlPrefix + id64);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(profileUrl.openStream(), HTML_ENCODING_CHARSET))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null && playerName == null) {

                Matcher matcher = PATTERN__PROFILE_DATA.matcher(inputLine);
                if (matcher.find()) {
                    // We need to unescape literal unicode. Sometimes we find names like 'Jovem Dihn\u00e2mico'
                    playerName = StringEscapeUtils.unescapeJava(matcher.group(1));

                    // We need to unescape HTML codes. Sometimes we find names like '&lt;&lt;ADuud&gt;&gt;' ('<<Aduud>>')
                    playerName = StringEscapeUtils.unescapeHtml3(playerName);
                }
            }
        }

        return playerName;
    }
}
