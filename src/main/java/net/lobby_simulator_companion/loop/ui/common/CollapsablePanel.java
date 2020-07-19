package net.lobby_simulator_companion.loop.ui.common;

import net.lobby_simulator_companion.loop.config.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author NickyRamone
 */
public class CollapsablePanel extends JPanel {

    public static final String EVENT_COLLAPSED_OR_EXPANDED = "CollapsablePanel.collapsedOrExpanded";

    private JLabel collapseButton;
    private JComponent contentPanel;


    public CollapsablePanel(JComponent titlePanel, JComponent contentPanel, Settings settings, String collapseSettingKey) {
        this.contentPanel = contentPanel;

        collapseButton = createCollapseButton();
        titlePanel.add(collapseButton);

        setBackground(Color.BLACK);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(titlePanel);
        add(contentPanel);

        contentPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                collapseButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.COLLAPSE));
                super.componentShown(e);
                settings.set(collapseSettingKey, false);
                firePropertyChange(EVENT_COLLAPSED_OR_EXPANDED, false, true);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                collapseButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.EXPAND));
                super.componentHidden(e);
                settings.set(collapseSettingKey, true);
                firePropertyChange(EVENT_COLLAPSED_OR_EXPANDED, false, true);
            }
        });

        contentPanel.setVisible(!settings.getBoolean(collapseSettingKey));
    }

    private JLabel createCollapseButton() {
        JLabel button = new JLabel();
        button.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.COLLAPSE));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(ComponentUtils.DEFAULT_BORDER);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                contentPanel.setVisible(!contentPanel.isVisible());
            }
        });

        return button;
    }

    public JLabel getCollapseButton() {
        return collapseButton;
    }
}