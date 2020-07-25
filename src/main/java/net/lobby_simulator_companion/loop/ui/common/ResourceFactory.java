package net.lobby_simulator_companion.loop.ui.common;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for obtaining singleton instances of different resources.
 *
 * @author NickyRamone
 */
@Slf4j
@UtilityClass
public final class ResourceFactory {

    private static final String ROBOTO_FONT_PATH = "/Roboto-Medium.ttf";
    private static final Map<Icon, ImageIcon> iconCache = new HashMap<>();
    private static Font roboto;
    private static Font robotoBig;

    public enum Icon {
        COLLAPSE("/collapse_icon.png"),
        EXPAND("/expand_icon.png"),
        LEFT("/left_icon.png"),
        RIGHT("/right_icon.png"),
        SHOW("/show_icon.png"),
        HIDE("/hide_icon.png"),
        THUMBS_UP("/thumbs-up_icon.png"),
        THUMBS_DOWN("/thumbs-down_icon.png"),
        ESCAPED_BUTTON_PRESSED("/escaped_button_pressed.png"),
        ESCAPED_BUTTON("/escaped_button.png"),
        DIED_BUTTON_PRESSED("/died_button_pressed.png"),
        DIED_BUTTON("/died_button.png"),
        IGNORE_BUTTON("/ignore_button.png"),
        IGNORE_BUTTON_PRESSED("/ignore_button_pressed.png"),
        EDIT("/edit_icon.png"),
        COPY_TO_CLIPBOARD("/clipboard_icon.png"),
        GEO_LOCATION("/geo-location_icon.png"),
        RATE("/rate_icon.png"),
        STEAM("/steam_icon.png"),
        SKULL_WHITE("/skull-white_icon.png"),
        SKULL_BLACK("/skull-black_icon.png"),
        SKULL_BLUE("/skull-blue_icon.png"),
        SKULL_RED("/skull-red_icon.png"),
        DISCONNECT("/broken-link_icon.png"),
        SWITCH_OFF("/switch-off_icon.png");

        final String imagePath;

        Icon(String imagePath) {
            this.imagePath = imagePath;
        }
    }


    public Font getRobotoFont() {
        if (roboto == null) {
            InputStream is = ResourceFactory.class.getResourceAsStream(ROBOTO_FONT_PATH);
            try {
                roboto = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
            } catch (FontFormatException e) {
                log.error("Font file is corrupt.", e);
            } catch (IOException e) {
                log.error("Failed to load Roboto font.", e);
            }
        }
        return roboto;
    }

    public ImageIcon getIcon(Icon icon) {
        ImageIcon imageIcon = iconCache.get(icon);

        if (imageIcon == null) {
            URL imageUrl = ResourceFactory.class.getResource(icon.imagePath);

            if (imageUrl != null) {
                imageIcon = new ImageIcon(imageUrl);
                iconCache.put(icon, imageIcon);
            } else {
                log.error("Failed to load '{}' icon.", icon.name());
            }
        }

        return imageIcon;
    }

}
