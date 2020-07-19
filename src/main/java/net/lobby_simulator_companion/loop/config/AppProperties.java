package net.lobby_simulator_companion.loop.config;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.Boot;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Manage application properties.
 *
 * @author NickyRamone
 */
@Slf4j
public class AppProperties {

    private Properties properties;

    public AppProperties() throws IOException, URISyntaxException {
        properties = new Properties();
        properties.load(AppProperties.class.getClassLoader().getResourceAsStream("app.properties"));

        URI execUri = Boot.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        String appHomePath = new File(execUri).toPath().getParent().toString();
        System.setProperty("app.home", appHomePath);

        properties.put("app.home", appHomePath);

        log.info("App home: {}", properties.getProperty("app.home"));
        log.info("App version: {}", properties.getProperty("app.version"));
    }

    public String get(String key) {
        return (String) properties.get(key);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);

        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    public int getInt(String key) {
        String value = properties.getProperty(key);

        return value == null ? 0 : Integer.parseInt(value);
    }

}
