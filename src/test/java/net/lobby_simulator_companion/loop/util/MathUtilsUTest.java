package net.lobby_simulator_companion.loop.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author NickyRamone
 */
public class MathUtilsUTest {


    @Test
    public void arithmeticSeriesSum__nIsEven_fromIsEven_stepIsEven() {

        // 0 + 2 + 4 + 6 + 8 + 10 = 30
        assertArithmeticSeriesSum(6, 0, 2, 30);

        // 6 + 10 + 14 + 18 + 22 + 26 + 30 + 34 = 160
        assertArithmeticSeriesSum(8, 6, 4, 160);

        // 4 + 10 + 16 + 22 = 52
        assertArithmeticSeriesSum(4, 4, 6, 52);
    }

    @Test
    public void arithmeticSeriesSum__nIsEven_fromIsEven_stepIsOdd() {

        // 0 + 1 + 2 + 3 + 4 + 5 = 15
        assertArithmeticSeriesSum(6, 0, 1, 15);

        // 6 + 7 + 8 + 9 = 30
        assertArithmeticSeriesSum(4, 6, 1, 30);

        // 6 + 9 + 12 + 15 = 42
        assertArithmeticSeriesSum(4, 6, 3, 42);
    }

    @Test
    public void arithmeticSeriesSum__nIsEven_fromIsOdd_stepIsEven() {

        // 1 + 3 + 5 + 7 + 9 + 11 = 36
        assertArithmeticSeriesSum(6, 1, 2, 36);

        // 9 + 13 + 17 + 21 + 25 + 29 = 114
        assertArithmeticSeriesSum(6, 9, 4, 114);
    }

    @Test
    public void arithmeticSeriesSum__nIsEven_fromIsOdd_stepIsOdd() {

        // 1 + 2 + 3 + 4 + 5 + 6 = 21
        assertArithmeticSeriesSum(6, 1, 1, 21);

        // 9 + 13 + 17 + 21 + 25 + 29 = 114
        assertArithmeticSeriesSum(6, 9, 4, 114);
    }

    @Test
    public void arithmeticSeriesSum__nIsOdd_fromIsEven_stepIsEven() {

        // 0 + 2 + 4 + 6 + 8 = 20
        assertArithmeticSeriesSum(5, 0, 2, 20);

        // 4 + 10 + 16 + 22 + 28 = 80
        assertArithmeticSeriesSum(5, 4, 6, 80);
    }

    @Test
    public void arithmeticSeriesSum__nIsOdd_fromIsEven_stepIsOdd() {

        // 0 + 1 + 2 + 3 + 4 = 10
        assertArithmeticSeriesSum(5, 0, 1, 10);

        // 4 + 11 + 18 + 25 + 32 = 80
        assertArithmeticSeriesSum(5, 4, 7, 90);
    }

    @Test
    public void arithmeticSeriesSum__nIsOdd_fromIsOdd_stepIsEven() {

        // 1 + 3 + 5 + 7 + 9 = 25
        assertArithmeticSeriesSum(5, 1, 2, 25);

        // 7 + 17 + 27 + 37 + 47 = 135
        assertArithmeticSeriesSum(5, 7, 10, 135);
    }

    @Test
    public void arithmeticSeriesSum__nIsOdd_fromIsOdd_stepIsOdd() {

        // 1 + 2 + 3 + 4 + 5 = 15
        assertArithmeticSeriesSum(5, 1, 1, 15);

        // 7 + 18 + 29 + 40 + 51 = 145
        assertArithmeticSeriesSum(5, 7, 11, 145);
    }


    private void assertArithmeticSeriesSum(int n, int from, int step, int expectedResult) {
        int result = MathUtils.arithmeticSeriesSum(n, from, step);
        assertThat(result, equalTo(expectedResult));
    }
}
