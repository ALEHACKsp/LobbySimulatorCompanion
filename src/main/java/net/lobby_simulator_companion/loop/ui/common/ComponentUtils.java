package net.lobby_simulator_companion.loop.ui.common;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;

/**
 * @author NickyRamone
 */
public final class ComponentUtils {

    public static final Border DEFAULT_BORDER = new EmptyBorder(5, 5, 5, 5);
    public static final Border NO_BORDER = new EmptyBorder(0, 0, 0, 0);


    private ComponentUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }


    public static JLabel createButtonLabel(Color textColor, String tooltip, ResourceFactory.Icon icon, MouseListener mouseListener) {
        JLabel button = new JLabel();
        button.setForeground(textColor);
        button.setToolTipText(tooltip);
        button.setIcon(ResourceFactory.getIcon(icon));
        button.setFont(ResourceFactory.getRobotoFont());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(mouseListener);

        return button;
    }


    public static JLabel createInfoPanelNameLabel() {
        return createInfoPanelLabel(null, JLabel.RIGHT, UiConstants.COLOR__INFO_PANEL__NAME__FG);
    }

    public static JLabel createInfoPanelValueLabel() {
        return createInfoPanelLabel(null, SwingConstants.LEADING, UiConstants.COLOR__INFO_PANEL__VALUE__FG);
    }

    private static JLabel createInfoPanelLabel(String text, int horizontalAlignment, Color fgColor) {
        JLabel label = new JLabel(text, horizontalAlignment);
        label.setFont(ResourceFactory.getRobotoFont());
        label.setForeground(fgColor);
        label.setBackground(UiConstants.COLOR__INFO_PANEL__BG);

        return label;
    }


}
