package net.lobby_simulator_companion.loop.ui.startup;

import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.service.plugin.LoopPlugin;
import net.lobby_simulator_companion.loop.service.plugin.PluginManager;

import javax.swing.*;
import java.util.Optional;

/**
 * @author NickyRamone
 */
public class PluginLoadUi {

    private static final String SETTING__AUTHORIZE_PLUGIN = "loop.plugin.";

    private final AppProperties appProperties;
    private final Settings settings;
    private final PluginManager pluginManager;


    public PluginLoadUi(AppProperties appProperties, Settings settings, PluginManager pluginManager) {
        this.appProperties = appProperties;
        this.settings = settings;
        this.pluginManager = pluginManager;
    }

    public void loadPlugin() {
        Optional<Class<?>> pluginClass = pluginManager.searchPlugin();

        if (!pluginClass.isPresent()) {
            return;
        }

        if (requestAuthorizationForPlugin(pluginClass.get())) {
            pluginManager.loadPlugin(pluginClass.get());
        }
    }

    private boolean requestAuthorizationForPlugin(Class clazz) {
        LoopPlugin plugin = (LoopPlugin) clazz.getAnnotation(LoopPlugin.class);
        String authSettingKey = SETTING__AUTHORIZE_PLUGIN + plugin.id();
        Boolean savedChoice = Optional.ofNullable(settings.get(authSettingKey))
                .map(v -> settings.getBoolean(authSettingKey)).orElse(null);

        if (savedChoice != null) {
            return savedChoice;
        }

        String modalTitle = appProperties.get("app.name.short") + " - Plugin detected";
        JCheckBox rememberChoiceCheckBox = new JCheckBox("Remember my choice.");

        String message = "We found a non-official plugin installed.\n\n"
                + "id: " + plugin.id() + "\n"
                + "author: " + plugin.author() + "\n"
                + "description: " + plugin.description() + "\n"
                + "\n"
                + "For security reasons, it is advised that you only accept plugins whose source you can trust.\n"
                + "If you decide to not authorize, the plugin will not be loaded.\n"
                + "\n"
                + "Authorize?\n\n";
        Object[] params = {message, rememberChoiceCheckBox};

        boolean authorized = JOptionPane.showConfirmDialog(
                new JFrame(), params, modalTitle, JOptionPane.YES_NO_OPTION) == 0;

        boolean rememberChoice = rememberChoiceCheckBox.isSelected();

        if (rememberChoice) {
            settings.set(authSettingKey, authorized);
        }

        return authorized;
    }

}
