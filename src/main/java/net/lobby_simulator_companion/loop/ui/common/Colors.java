package net.lobby_simulator_companion.loop.ui.common;

import java.awt.*;

public final class Colors {

    public static final Color STATUS_BAR_BACKGROUND = new Color(0x00, 0x88, 0xff);
    public static final Color STATUS_BAR_FOREGROUND = Color.WHITE;

    public static final Color MSG_BAR_BACKGROUND = new Color(00, 0, 0xb0);
    public static final Color MSG_BAR_FOREGROUND = Color.WHITE;

    public static final Color INFO_PANEL_BACKGROUND = new Color(0xf7, 0xe6, 0xff);
    public static final Color INFO_PANEL_NAME_FOREGROUND = new Color(0, 0, 0xff);
    public static final Color INFO_PANEL_VALUE_FOREGOUND = Color.BLACK;

    public static final Color CONNECTION_BAR_WAITING = Color.YELLOW;
    public static final Color CONNECTION_BAR_CONNECTED_BACKGROUND = new Color(0, 0xb0, 0);
    public static final Color CONNECTION_BAR_DISCONNECTED_BACKGROUND = new Color(0xb0, 0, 0);


    private Colors() {
        // this class should not be instantiated
    }

}
