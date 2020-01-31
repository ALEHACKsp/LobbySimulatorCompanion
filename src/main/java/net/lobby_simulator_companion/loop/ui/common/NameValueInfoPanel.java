package net.lobby_simulator_companion.loop.ui.common;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author NickyRamone
 */
public class NameValueInfoPanel<K> extends JPanel {

    private Map<K, Component> fieldComponents = new LinkedHashMap<>();


    private NameValueInfoPanel() {
        setLayout(new GridLayout(0, 2, 10, 5));
        setBackground(Colors.INFO_PANEL_BACKGROUND);
    }

    public JLabel get(K field) {
        return (JLabel) fieldComponents.get(field);
    }

    public <V> V get(K field, Class<V> clazz) {
        return clazz.cast(fieldComponents.get(field));
    }

    public Set<Map.Entry<K, Component>> entrySet() {
        return fieldComponents.entrySet();
    }

    public static final class Builder<K> {

        private NameValueInfoPanel<K> panel = new NameValueInfoPanel<>();

        public Builder<K> addField(K key, String description) {
            return addField(key, description, (String) null);
        }

        public Builder<K> addField(K key, String description, String tooltip) {
            JLabel valueLabel = new JLabel();
            valueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
            valueLabel.setFont(ResourceFactory.getRobotoFont());

            return addField(key, description, tooltip, valueLabel);
        }

        public Builder<K> addField(K key, String description, Component valueComponent) {
            return addField(key, description, null, valueComponent);
        }

        public Builder<K> addField(K key, String description, String tooltip, Component valueComponent) {
            JLabel textLabel = new JLabel(description, JLabel.RIGHT);
            textLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
            textLabel.setFont(ResourceFactory.getRobotoFont());
            textLabel.setToolTipText(tooltip);

            panel.add(textLabel);
            panel.add(valueComponent);
            panel.fieldComponents.put(key, valueComponent);

            return this;
        }

        public NameValueInfoPanel<K> build() {
            return panel;
        }

    }

}
