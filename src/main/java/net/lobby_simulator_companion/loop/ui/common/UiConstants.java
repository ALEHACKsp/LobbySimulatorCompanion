package net.lobby_simulator_companion.loop.ui.common;


import java.awt.*;

/**
 * @author NickyRamone
 */
public final class UiConstants {

    public static final Color COLOR__TITLE_BAR__BG = new Color(0x00, 0x88, 0xff);
    public static final Color COLOR__TITLE_BAR__FG = Color.WHITE;

    public static final Color COLOR__MSG_BAR__BG = new Color(00, 0, 0xb0);
    public static final Color COLOR__MSG_BAR__FG = Color.WHITE;

    public static final Color COLOR__INFO_PANEL__BG = new Color(0xf7, 0xe6, 0xff);
    public static final Color COLOR__INFO_PANEL__NAME__FG = new Color(0, 0, 0xff);
    public static final Color COLOR__INFO_PANEL__VALUE__FG = Color.BLACK;

    public static final Color COLOR__STATUS_BAR__SEARCHING_LOBBY__BG = Color.YELLOW;
    public static final Color COLOR__STATUS_BAR__CONNECTED__BG = new Color(0, 0xb0, 0);
    public static final Color COLOR__STATUS_BAR__DISCONNECTED__BG = new Color(0xb0, 0, 0);

    public static final int WIDTH__LOOP_MAIN = 700;
    public static final int WIDTH__INFO_PANEL__NAME_COLUMN = 200;
    public static final int WIDTH__INFO_PANEL__VALUE_COLUMN = 200;


    private UiConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
