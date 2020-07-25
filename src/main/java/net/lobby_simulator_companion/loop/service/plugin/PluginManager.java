package net.lobby_simulator_companion.loop.service.plugin;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.AppProperties;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * @author NickyRamone
 */
@Slf4j
public class PluginManager {

    private final AppProperties appProperties;
    private final Reflections reflections;
    private Object pluginInstance;


    public PluginManager(AppProperties appProperties) throws MalformedURLException {
        this.appProperties = appProperties;
        this.reflections = buildReflections(appProperties);
    }


    public Optional<Class<?>> searchPlugin() {
        try {
            return searchPlugin(reflections);
        } catch (Throwable e) {
            log.error("Failed to search for plugin.", e);
            return Optional.empty();
        }
    }

    public void loadPlugin(Class pluginClass) {
        try {
            pluginInstance = pluginClass.newInstance();
            log.info("Successfully instantiated plugin: {}", pluginInstance.getClass().getName());
        } catch (Exception e) {
            log.error("Failed to instantiate plugin class: " + pluginClass, e);
        }
    }

    private Reflections buildReflections(AppProperties appProperties) throws MalformedURLException {
        Optional<URL> pluginClasspathUrl = getPluginClasspathUrl(appProperties);

        return pluginClasspathUrl.map(
                classpathUrl -> new URLClassLoader(
                        new URL[]{classpathUrl},
                        PluginManager.class.getClassLoader()
                ))
                .map(classLoader -> new Reflections(new ConfigurationBuilder()
                        .setUrls(classLoader.getURLs())
                        .setScanners(
                                new TypeAnnotationsScanner(),
                                new SubTypesScanner()
                        )
                        .addClassLoader(classLoader)
                ))
                .orElse(null);
    }


    private Optional<URL> getPluginClasspathUrl(AppProperties appProperties) throws MalformedURLException {
        String pluginClasspath = System.getProperty("plugin.classpath");
        boolean pluginDev = appProperties.getBoolean("plugin.development");

        if (pluginDev) {
            if (pluginClasspath != null) {
                return getPluginClassesUrl(pluginClasspath);
            }
            return Optional.empty();
        }

        return getPluginJarUrl(appProperties);
    }

    private Optional<URL> getPluginClassesUrl(String rawPluginClasspath) {
        return Optional.ofNullable(rawPluginClasspath)
                .map(classpath -> classpath.endsWith("/") ? classpath : classpath + "/")
                .map(classpath -> {
                    try {
                        return new File(classpath).toURI().toURL();
                    } catch (MalformedURLException e) {
                        log.error("Failed to convert classpath to URL: {}", classpath);
                        return null;
                    }
                });
    }

    private Optional<URL> getPluginJarUrl(AppProperties appProperties) throws MalformedURLException {
        String appHome = appProperties.get("app.home");
        String pluginFilename = appProperties.get("plugin.filename");
        Path pluginFullPath = Paths.get(appHome, pluginFilename);

        if (!pluginFullPath.toFile().exists()) {
            return Optional.empty();
        }

        String uriString = StringUtils.removeStart(pluginFullPath.toString(), pluginFullPath.getRoot().toString());
        uriString = StringUtils.replace(uriString, "\\", "/");
        String pluginUrl = String.format("jar:file:/%s!/", uriString);

        return Optional.of(new URL(pluginUrl));
    }


    private Optional<Class<?>> searchPlugin(Reflections reflections) {
        if (reflections == null) {
            return Optional.empty();
        }

        return reflections.getTypesAnnotatedWith(LoopPlugin.class).stream()
                .filter(clazz -> {
                    LoopPlugin plugin = clazz.getAnnotation(LoopPlugin.class);

                    try {
                        UUID.fromString(plugin.id());
                        return true;
                    } catch (IllegalArgumentException e) {
                        log.warn("Illegal plugin id: {}", plugin.id());
                        return false;
                    }
                })
                .findFirst();
    }

}
