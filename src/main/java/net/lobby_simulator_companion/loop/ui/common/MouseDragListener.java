package net.lobby_simulator_companion.loop.ui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author NickyRamone
 */
public class MouseDragListener extends MouseAdapter {

    private final Component frame;
    private Point dragStartPoint;

    public MouseDragListener(JFrame frame) {
        this.frame = frame;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragStartPoint = e.getPoint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartPoint = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point currCoords = e.getLocationOnScreen();
        frame.setLocation(currCoords.x - dragStartPoint.x, currCoords.y - dragStartPoint.y);
    }

}
