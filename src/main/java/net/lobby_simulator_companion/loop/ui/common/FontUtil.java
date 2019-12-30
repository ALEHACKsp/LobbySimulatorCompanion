package net.lobby_simulator_companion.loop.ui.common;

import java.awt.*;

/**
 * @author NickyRamone
 */
public final class FontUtil {

    private FontUtil() {
        // this class should not be instantiated
    }

    public static String replaceNonDisplayableChars(Font font, String text, char wildcardChar) {
        if (text == null) {
            return null;
        }
        char[] result = text.toCharArray();

        for (int i = 0; i < result.length; i++) {
            char c = result[i];
            if (!font.canDisplay(c)) {
                result[i] = wildcardChar;
            }
        }

        return String.valueOf(result);
    }

}
