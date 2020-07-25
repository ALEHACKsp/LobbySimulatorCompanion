package net.lobby_simulator_companion.loop.service;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author NickyRamone
 */
public class StatsUtilsUTest {

    private static final class DistroRate implements Comparable<DistroRate> {
        float rate;
        List<Integer> distro;

        DistroRate(float rate, List<Integer> distro) {
            this.rate = rate;
            this.distro = distro;
        }

        @Override
        public int compareTo(DistroRate o) {
            float diff = this.rate - o.rate;

            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            }

            return 0;
        }
    }

    @Test
    public void whenDistrosAreSortedByRating_thenTheyShouldBeInExpectedOrder() {
        int[][] distros = {
                {8, 0, 0, 0, 0},
                {7, 1, 0, 0, 0},
                {6, 2, 0, 0, 0},
                {5, 3, 0, 0, 0},
                {4, 4, 0, 0, 0},
                {6, 1, 1, 0, 0},
                {5, 2, 1, 0, 0},
                {4, 3, 1, 0, 0},
                {4, 2, 2, 0, 0},
                {3, 3, 2, 0, 0},
                {5, 1, 1, 1, 0},
                {4, 2, 1, 1, 0},
                {3, 3, 1, 1, 0},
                {3, 2, 2, 1, 0},
                {2, 2, 2, 2, 0},
                {4, 1, 1, 1, 1},
                {3, 2, 1, 1, 1},
                {2, 2, 2, 1, 1}
        };

        List<List<Integer>> sorted = distrosArrayToList(distros).stream()
                .map(d -> new DistroRate(StatsUtils.rateDistribution(d), d))
                .sorted()
                .map(dr -> dr.distro)
                .collect(Collectors.toList());

        assertThat(sorted, equalTo(distrosArrayToList(distros)));
    }

    private List<List<Integer>> distrosArrayToList(int[][] distros) {
        return Arrays.stream(distros)
                .map(d -> Arrays.stream(d).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

}
