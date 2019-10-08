package net.lobby_simulator_companion.loop.config;

import net.lobby_simulator_companion.loop.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class AppProperties {
    private static final Logger logger = LoggerFactory.getLogger(AppProperties.class);

    private Properties properties;

    public AppProperties() throws IOException, URISyntaxException {
        properties = new Properties();
        properties.load(AppProperties.class.getClassLoader().getResourceAsStream("app.properties"));

        URI execUri = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        properties.put("app.home", new File(execUri).toPath().getParent().toString());

        logger.info("App home: {}", properties.getProperty("app.home"));
        logger.info("App version: {}", properties.getProperty("app.version"));
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

}
