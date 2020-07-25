package net.lobby_simulator_companion.loop.ui.common;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @author NickyRamone
 */
public class NameValueInfoPanel extends JPanel {


    private Map<Object, Pair<JComponent, JComponent>> fieldComponents = new LinkedHashMap<>();

    @Getter
    private JPanel leftPanel;

    @Getter
    private JPanel rightPanel;


    public NameValueInfoPanel() {
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

    public JLabel getLeft(Object field) {
        return (JLabel) fieldComponents.get(field).getLeft();
    }

    public JLabel getRight(Object field) {
        return getRight(field, JLabel.class);
    }

    public <V> V getRight(Object field, Class<V> clazz) {
        Pair<JComponent, JComponent> value = fieldComponents.get(field);

        return value != null ? clazz.cast(fieldComponents.get(field).getRight()) : null;
    }

    public JLabel getLeftByRow(int row) {
        return (JLabel) leftPanel.getComponent(row * 2);
    }

    public Component getRightByRow(int row) {
        return rightPanel.getComponent(row * 2);
    }

    public Set<Map.Entry<Object, Pair<JComponent, JComponent>>> entrySet() {
        return fieldComponents.entrySet();
    }


    public void addFields(Class<? extends Enum<?>> enumClass) {
        for (Object enumConstant : enumClass.getEnumConstants()) {
            addField(enumConstant);
        }
    }

    public void addField(Object key) {
        addField(key, null);
    }


    public void addField(Object key, String tooltip) {
        JLabel valueLabel = new JLabel();
        valueLabel.setText("--");
        valueLabel.setForeground(UiConstants.COLOR__INFO_PANEL__VALUE__FG);
        valueLabel.setFont(ResourceFactory.getRobotoFont());
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        addField(key, tooltip, valueLabel);
    }

    public void addField(Object key, String tooltip, JComponent valueComponent) {
        addField(fieldComponents.size(), key, tooltip, valueComponent);
    }


    public void addField(int rowIndex, Object key, String tooltip, JComponent valueComponent) {
        int rowComponentIdx;

        if (rowIndex >= 0) {
            if (rowIndex >= fieldComponents.size()) {
                rowComponentIdx = -1;
            } else {
                rowComponentIdx = rowIndex * 2;
            }
        } else {
            rowComponentIdx = (fieldComponents.size() - 1) * 2;
        }

        JLabel textLabel = new JLabel(key.toString());
        textLabel.setForeground(UiConstants.COLOR__INFO_PANEL__NAME__FG);
        textLabel.setFont(ResourceFactory.getRobotoFont());
        textLabel.setToolTipText(tooltip);
        textLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        valueComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

        int strutIdx = rowComponentIdx == -1 ? -1 : rowComponentIdx + 1;

        leftPanel.add(textLabel, rowComponentIdx);
        leftPanel.add(Box.createVerticalStrut(5), strutIdx);
        rightPanel.add(valueComponent, rowComponentIdx);
        rightPanel.add(Box.createVerticalStrut(5), strutIdx);
        fieldComponents.put(key, Pair.of(textLabel, valueComponent));
    }

    public void setRight(Object key, JComponent right) {
        Pair<JComponent, JComponent> currentValue = fieldComponents.get(key);
        int i = getComponentIndex(currentValue.getRight());
        rightPanel.remove(i);
        rightPanel.add(right, i);
        fieldComponents.put(key, Pair.of(currentValue.getLeft(), currentValue.getRight()));
    }


    private int getComponentIndex(Component component) {
        Container container = component.getParent();

        return IntStream.range(0, container.getComponentCount())
                .filter(i -> container.getComponent(i) == component)
                .findAny()
                .orElse(-1);
    }

    public void remove(Object fieldKey) {
        Pair<JComponent, JComponent> value = fieldComponents.remove(fieldKey);
        int idx = getComponentIndex(value.getLeft());
        // we need to remove the field components and their struts as well
        leftPanel.remove(leftPanel.getComponent(idx));
        leftPanel.remove(leftPanel.getComponent(idx));
        rightPanel.remove(rightPanel.getComponent(idx));
        rightPanel.remove(rightPanel.getComponent(idx));
    }

    public void setSizes(int leftPanelWidth, int rightPanelWidth, int height) {
        leftPanel.setPreferredSize(new Dimension(leftPanelWidth, height));
        leftPanel.setMaximumSize(new Dimension(leftPanelWidth, height));
        rightPanel.setPreferredSize(new Dimension(rightPanelWidth, height));
        rightPanel.setMaximumSize(new Dimension(rightPanelWidth, height));
    }


    public void increaseHeight(int pixels) {
        Dimension leftSize = leftPanel.getPreferredSize();
        Dimension rightSize = rightPanel.getPreferredSize();
        leftPanel.setPreferredSize(new Dimension(leftSize.width, leftSize.height + pixels));
        leftPanel.setMaximumSize(new Dimension(leftSize.width, leftSize.height + pixels));
        rightPanel.setPreferredSize(new Dimension(rightSize.width, rightSize.height + pixels));
        rightPanel.setMaximumSize(new Dimension(rightSize.width, rightSize.height + pixels));
    }

}
