package net.lobby_simulator_companion.loop.ui.common;

import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * @author NickyRamone
 */
@UtilityClass
public final class FontUtil {

    private static final char DEFAULT_NON_DISPLAYABLE_CHAR = '\u0387';
    private static final Font DEFAULT_FONT = ResourceFactory.getRobotoFont();


    public String replaceNonDisplayableChars(String text) {
        return replaceNonDisplayableChars(DEFAULT_FONT, text, DEFAULT_NON_DISPLAYABLE_CHAR);
    }

    private String replaceNonDisplayableChars(Font font, String text, char wildcardChar) {
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
