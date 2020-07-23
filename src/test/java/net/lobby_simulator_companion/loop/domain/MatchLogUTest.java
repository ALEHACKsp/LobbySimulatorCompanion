package net.lobby_simulator_companion.loop.domain;

import net.lobby_simulator_companion.loop.domain.stats.AggregateStats;
import net.lobby_simulator_companion.loop.domain.stats.KillerStats;
import net.lobby_simulator_companion.loop.domain.stats.MapStats;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import org.junit.Test;

import static net.lobby_simulator_companion.loop.domain.MatchLog.RollingGroup;
import static net.lobby_simulator_companion.loop.domain.MatchLog.RollingGroup.LAST_100_MATCHES;
import static net.lobby_simulator_companion.loop.domain.MatchLog.RollingGroup.LAST_50_MATCHES;
import static net.lobby_simulator_companion.loop.util.MathUtils.arithmeticSeriesSum;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author NickyRamone
 */
public class MatchLogUTest {

    private final MatchLog matchLog = new MatchLog();


    @Test
    public void rollingAggregation() {
        int n = 200;

        for (int i = 1; i <= n; i++) {
            Match matchStats = generateMatchStats(i);
            matchLog.add(matchStats);
        }

        verifyRollingAggregation(LAST_50_MATCHES, n);
        verifyRollingAggregation(LAST_100_MATCHES, n);
    }


    /**
     * Generate deterministic match data dependent of the match number.
     * Deterministic aspects:
     * - The lobby count will always be equal to the match number.
     * - The queue time will be equal to the match number + 1.
     * - The wait time will be equal to the match number + 2.
     * - The play time will be equal to the match number + 3.
     * - The killer will be Trapper for odd match number and Hillbilly for even ones.
     * - The map will be Coal Tower for odd match number and Thompson House for even ones.
     * - We always escape against Trapper and always die against Hillbilly.
     *
     * @param matchNumber This is a virtual number for identifying the match data and it's independent of the capacity
     *                    of the aggregator. If you aggregate 1000 matches, last match number is going to be 1000.
     */
    private Match generateMatchStats(int matchNumber) {
        boolean oddMatchNumber = matchNumber % 2 != 0;

        return Match.builder()
                .lobbiesFound(matchNumber)
                .secondsQueued(matchNumber + 1)
                .secondsWaited(matchNumber + 2)
                .secondsPlayed(matchNumber + 3)
                .killer(oddMatchNumber ? Killer.TRAPPER : Killer.HILLBILLY)
                .realmMap(oddMatchNumber ? RealmMap.COAL_TOWER : RealmMap.THOMPSON_HOUSE)
                .escaped(oddMatchNumber)
                .build();
    }


    private void verifyRollingAggregation(RollingGroup group, int numMatchesAggregated) {

        AggregateStats actualStats = matchLog.getStats(group);
        int n = group.aggregateSize;
        int oldestMatchNum = numMatchesAggregated - n + 1;

        AggregateStats expectedStats = AggregateStats.builder()
                .matchesPlayed(n)
                .lobbiesFound(arithmeticSeriesSum(n, oldestMatchNum, 1))
                .secondsQueued(arithmeticSeriesSum(n, oldestMatchNum + 1, 1))
                .secondsWaited(arithmeticSeriesSum(n, oldestMatchNum + 2, 1))
                .secondsPlayed(arithmeticSeriesSum(n, oldestMatchNum + 3, 1))
                .escapes(n / 2)
                .deaths(n / 2)
                .escapesInARow(numMatchesAggregated % 2 == 0 ? 0 : 1)
                .deathsInARow(numMatchesAggregated % 2 == 0 ? 1 : 0)
                .maxEscapesInARow(1)
                .maxDeathsInARow(1)
                .build();

        expectedStats.getKillersStats().put(Killer.TRAPPER, KillerStats.builder()
                .escapes(n / 2)
                .deaths(0)
                .matches(n / 2)
                .matchTime(arithmeticSeriesSum(n / 2, oldestMatchNum + 3, 2))
                .build());

        expectedStats.getKillersStats().put(Killer.HILLBILLY, KillerStats.builder()
                .escapes(n / 2)
                .escapes(0)
                .deaths(n / 2)
                .matches(n / 2)
                .matchTime(arithmeticSeriesSum(n / 2, oldestMatchNum + 1 + 3, 2))
                .build());

        expectedStats.getMapStats().put(RealmMap.COAL_TOWER, MapStats.builder()
                .matches(n / 2)
                .escapes(n / 2)
                .deaths(0)
                .matchTime(arithmeticSeriesSum(n / 2, oldestMatchNum + 3, 2))
                .build());
        expectedStats.getMapStats().put(RealmMap.THOMPSON_HOUSE, MapStats.builder()
                .matches(n / 2)
                .escapes(0)
                .deaths(n / 2)
                .matchTime(arithmeticSeriesSum(n / 2, oldestMatchNum + 1 + 3, 2))
                .build());

        assertThat(actualStats, equalTo(expectedStats));
    }

}
