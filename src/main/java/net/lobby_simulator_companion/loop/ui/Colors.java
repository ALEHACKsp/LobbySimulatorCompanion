package net.lobby_simulator_companion.loop.ui;

import java.awt.*;

// TODO: Define nicer colors.
public final class Colors {

    public static final Color DEFAULT_CONTAINER_BACKGROUND = Color.BLACK;

    public static final Color STATUS_BAR_BACKGROUND = new Color(0x00, 0x88, 0xff);
    public static final Color STATUS_BAR_FOREGROUND = Color.WHITE;

    public static final Color MSG_BAR_BACKGROUND = new Color(00, 0, 0xb0);
    public static final Color MSG_BAR_FOREGROUND = Color.WHITE;

//    public static final Color INFO_PANEL_BACKGROUND = new Color(0x80, 0x80, 0x80);
//    public static final Color INFO_PANEL_BACKGROUND = new Color(0xff, 0x66, 0x00);
//    public static final Color INFO_PANEL_BACKGROUND = new Color(0xff, 0x94, 0x4d);
//    public static final Color INFO_PANEL_BACKGROUND = new Color(0xcc, 0x99, 0xff);
//    public static final Color INFO_PANEL_BACKGROUND = new Color(0xee, 0xcc, 0xff);
    public static final Color INFO_PANEL_BACKGROUND = new Color(0xf7, 0xe6, 0xff);
//    public static final Color INFO_PANEL_NAME_FOREGROUND = Color.WHITE;
    public static final Color INFO_PANEL_NAME_FOREGROUND = new Color(0, 0, 0xff);
//    public static final Color INFO_PANEL_VALUE_FOREGOUND = new Color(0x01, 0x83, 0x20);
    public static final Color INFO_PANEL_VALUE_FOREGOUND = Color.BLACK;

    public static final Color CONNECTION_BAR_CONNECTED_BACKGROUND = new Color(0, 0xb0, 0);
    public static final Color CONNECTION_BAR_DISCONNECTED_BACKGROUND = new Color(0xb0, 0, 0);


    private Colors() {
        // this class should not be instantiated
    }

}
