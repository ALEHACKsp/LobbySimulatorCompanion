package net.lobby_simulator_companion.loop.ui.common;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author NickyRamone
 */
public class NameValueInfoPanel extends JPanel {

    private Map<Object, Component> fieldComponents = new LinkedHashMap<>();

    @Getter
    private JPanel leftPanel;

    @Getter
    private JPanel rightPanel;


    private NameValueInfoPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        setAlignmentX(Component.CENTER_ALIGNMENT);

        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(UiConstants.COLOR__INFO_PANEL__BG);

        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(UiConstants.COLOR__INFO_PANEL__BG);

        add(leftPanel);
        add(Box.createHorizontalStrut(10));
        add(rightPanel);
    }

    public JLabel get(Object field) {
        return (JLabel) fieldComponents.get(field);
    }

    public <V> V get(Object field, Class<V> clazz) {
        return clazz.cast(fieldComponents.get(field));
    }

    public JLabel getNameComponent(int row) {
        return (JLabel) leftPanel.getComponent(row * 2);
    }

    public Component getValueComponent(int row) {
        return rightPanel.getComponent(row * 2);
    }

    public Set<Map.Entry<Object, Component>> entrySet() {
        return fieldComponents.entrySet();
    }


    public void add(Object key, String description, String tooltip) {
        JLabel valueLabel = new JLabel();
        valueLabel.setText("--");
        valueLabel.setForeground(UiConstants.COLOR__INFO_PANEL__VALUE__FG);
        valueLabel.setFont(ResourceFactory.getRobotoFont());

        add(key, description, tooltip, valueLabel);
    }

    public void add(Object key, String description, String tooltip, JComponent valueComponent) {
        add(fieldComponents.size(), key, description, tooltip, valueComponent);
    }

    public void add(int rowIndex, Object key, String description, String tooltip, JComponent valueComponent) {
        int rowComponentIdx;

        if (rowIndex >= 0) {
            if (rowIndex >= fieldComponents.size()) {
                rowComponentIdx = -1;
            }
            else {
                rowComponentIdx = rowIndex * 2;
            }
        }
        else {
            rowComponentIdx = (fieldComponents.size() - 1) * 2;
        }

        JLabel textLabel = new JLabel(description);
        textLabel.setForeground(UiConstants.COLOR__INFO_PANEL__NAME__FG);
        textLabel.setFont(ResourceFactory.getRobotoFont());
        textLabel.setToolTipText(tooltip);
        textLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        valueComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftPanel.add(textLabel, rowComponentIdx);
        leftPanel.add(Box.createVerticalStrut(5), rowComponentIdx + 1);
        rightPanel.add(valueComponent, rowComponentIdx);
        rightPanel.add(Box.createVerticalStrut(5), rowComponentIdx + 1);
        fieldComponents.put(key, valueComponent);
    }

    public void increaseHeight(int pixels) {
        Dimension leftSize = leftPanel.getPreferredSize();
        Dimension rightSize = rightPanel.getPreferredSize();
        leftPanel.setPreferredSize(new Dimension(leftSize.width, leftSize.height + pixels));
        leftPanel.setMaximumSize(new Dimension(leftSize.width, leftSize.height + pixels));
        rightPanel.setPreferredSize(new Dimension(rightSize.width, rightSize.height + pixels));
        rightPanel.setMaximumSize(new Dimension(rightSize.width, rightSize.height + pixels));
    }


    // TODO: this builder doesn't make much sense. Might as well provide these construction options on the panel itself?
    public static final class Builder {

        private NameValueInfoPanel panel = new NameValueInfoPanel();

        public Builder setSizes(int leftPanelWidth, int rightPanelWidth, int height) {
            panel.leftPanel.setPreferredSize(new Dimension(leftPanelWidth, height));
            panel.leftPanel.setMaximumSize(new Dimension(leftPanelWidth, height));
            panel.rightPanel.setPreferredSize(new Dimension(rightPanelWidth, height));
            panel.rightPanel.setMaximumSize(new Dimension(rightPanelWidth, height));

            return this;
        }


        public Builder addField(Object key, String description) {
            return addField(key, description, (String) null);
        }

        public Builder addField(Object key, String description, String tooltip) {
            JLabel valueLabel = new JLabel();
            valueLabel.setText("--");
            valueLabel.setForeground(UiConstants.COLOR__INFO_PANEL__VALUE__FG);
            valueLabel.setFont(ResourceFactory.getRobotoFont());
            valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            return addField(key, description, tooltip, valueLabel);
        }

        public Builder addField(Object key, String description, Component valueComponent) {
            return addField(key, description, null, valueComponent);
        }

        public Builder addField(Object key, String description, String tooltip, Component valueComponent) {
            JLabel textLabel = new JLabel(description);
            textLabel.setForeground(UiConstants.COLOR__INFO_PANEL__NAME__FG);
            textLabel.setFont(ResourceFactory.getRobotoFont());
            textLabel.setToolTipText(tooltip);
            textLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            panel.leftPanel.add(textLabel);
            panel.leftPanel.add(Box.createVerticalStrut(5));
            panel.rightPanel.add(valueComponent);
            panel.rightPanel.add(Box.createVerticalStrut(5));
            panel.fieldComponents.put(key, valueComponent);

            return this;
        }

        public NameValueInfoPanel build() {
            return panel;
        }

    }

}
