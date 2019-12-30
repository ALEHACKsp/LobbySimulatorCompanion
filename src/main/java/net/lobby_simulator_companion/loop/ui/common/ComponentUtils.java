package net.lobby_simulator_companion.loop.ui.common;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;

/**
 * @author NickyRamone
 */
public class ComponentUtils {

    public static final Border DEFAULT_BORDER = new EmptyBorder(5, 5, 5, 5);
    public static final Border NO_BORDER =  new EmptyBorder(0, 0, 0, 0);


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

}
