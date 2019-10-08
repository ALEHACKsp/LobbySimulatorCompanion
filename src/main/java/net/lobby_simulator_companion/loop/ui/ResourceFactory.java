package net.lobby_simulator_companion.loop.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A factory for obtaining singleton instances of different resources.
 *
 * @author NickyRamone
 */
public final class ResourceFactory {

    private static final Logger logger = LoggerFactory.getLogger(ResourceFactory.class);

    private static final String ROBOTO_FONT_PATH = "/Roboto-Medium.ttf";

    private static final String CLEAR_ICON_PATH = "/clear_icon.png";
    private static final String COLLAPSE_ICON_PATH = "/collapse_icon.png";
    private static final String EDIT_ICON_PATH = "/edit_icon.png";
    private static final String EXPAND_ICON_PATH = "/expand_icon.png";
    private static final String RATE_ICON_PATH = "/rate_icon.png";
    private static final String STEAM_ICON_PATH = "/steam_icon.png";
    private static final String SWITCH_OFF_ICON_PATH = "/switch-off_icon.png";
    private static final String THUMBS_DOWN_ICON_PATH = "/thumbs-down_icon.png";
    private static final String THUMBS_UP_ICON_PATH = "/thumbs-up_icon.png";

    private static Font roboto;
    private static ImageIcon collapseIcon;
    private static ImageIcon editIcon;
    private static ImageIcon expandIcon;
    private static ImageIcon rateIcon;
    private static ImageIcon clearIcon;
    private static ImageIcon steamIcon;
    private static ImageIcon switchOffIcon;
    private static ImageIcon thumbsUpIcon;
    private static ImageIcon thumbsDownIcon;


    private ResourceFactory() {
    }


    public static synchronized Font getRobotoFont() {
        if (roboto == null) {
            InputStream is = ResourceFactory.class.getResourceAsStream(ROBOTO_FONT_PATH);
            try {
                roboto = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
            } catch (FontFormatException e) {
                logger.error("Font file is corrupt.", e);
            } catch (IOException e) {
                logger.error("Failed to load Roboto font.", e);
            }
        }
        return roboto;
    }

    private static synchronized ImageIcon getIcon(ImageIcon icon, String path, String description) {

        if (icon == null) {
            URL imageUrl = ResourceFactory.class.getResource(path);

            if (imageUrl != null) {
                icon = new ImageIcon(imageUrl, description);
            }
            else {
                logger.error("Failed to load '{}' icon.", description);
            }
        }

        return icon;
    }


    public static synchronized ImageIcon getClearIcon() {
        return getIcon(clearIcon, CLEAR_ICON_PATH, "clear");
    }

    public static synchronized ImageIcon getCollapseIcon() {
        return getIcon(collapseIcon, COLLAPSE_ICON_PATH, "collapse");
    }

    public static synchronized ImageIcon getEditIcon() {
        return getIcon(editIcon, EDIT_ICON_PATH, "edit");
    }

    public static synchronized ImageIcon getExpandIcon() {
        return getIcon(expandIcon, EXPAND_ICON_PATH, "expand");
    }

    public static synchronized ImageIcon getSteamIcon() {
        return getIcon(steamIcon, STEAM_ICON_PATH, "Steam");
    }

    public static synchronized ImageIcon getThumbsUpIcon() {
        return getIcon(thumbsUpIcon, THUMBS_UP_ICON_PATH, "thumbs up");
    }

    public static synchronized ImageIcon getThumbsDownIcon() {
        return getIcon(thumbsUpIcon, THUMBS_DOWN_ICON_PATH, "thumbs down");
    }

    public static synchronized ImageIcon getRateIcon() {
        return getIcon(rateIcon, RATE_ICON_PATH, "rate");
    }

    public static synchronized ImageIcon getSwitchOffIcon() {
        return getIcon(switchOffIcon, SWITCH_OFF_ICON_PATH, "switch-off");
    }

}
