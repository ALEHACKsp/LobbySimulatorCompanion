package net.lobby_simulator_companion.loop.util;

import lombok.experimental.UtilityClass;

/**
 * @author NickyRamone
 */
@UtilityClass
public class MathUtils {


    public static int arithmeticSeriesSum(int n, int from, int step) {
        int to = from + step * (n - 1);

        return n * (from + to) / 2;
    }

}
