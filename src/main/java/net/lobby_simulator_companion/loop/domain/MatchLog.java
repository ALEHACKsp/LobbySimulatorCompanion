package net.lobby_simulator_companion.loop.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.RequiredArgsConstructor;
import net.lobby_simulator_companion.loop.domain.stats.AggregateStats;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;

/**
 * @author NickyRamone
 */
public class MatchLog {

    @RequiredArgsConstructor
    public enum RollingGroup {
        LAST_50_MATCHES("Last 50 matches", 50),
        LAST_100_MATCHES("Last 100 matches", 100),
        LAST_250_MATCHES("Last 250 matches", 250),
        LAST_500_MATCHES("Last 500 matches", 500),
        LAST_1000_MATCHES("Last 1000 matches", 1000);

        private final String description;
        public final int aggregateSize;


        @Override
        public String toString() {
            return description;
        }
    }

    private final CircularFifoQueue<Match> matches;
    private final transient Map<RollingGroup, AggregateStats> statsByGroup = new HashMap<>();


    public MatchLog() {
        int maxMatchesSupported = Arrays.stream(RollingGroup.values())
                .mapToInt(g -> g.aggregateSize)
                .reduce(0, (result, groupSize) -> max(groupSize, result));

        matches = new CircularFifoQueue<>(maxMatchesSupported);

        for (RollingGroup group : RollingGroup.values()) {
            statsByGroup.put(group, new AggregateStats());
        }
    }


    public void add(Match match) {

        for (RollingGroup group : RollingGroup.values()) {
            recalculateGroupStatsForNewMatch(group, match);
        }

        matches.add(match);
    }


    /**
     * When the queue is full we cannot calculate the escape streaks/records in O(1) with no additional structure.
     * As a simplification, we will recalculate the stats for every group.
     */
    private void recalculateGroupStatsForNewMatch(RollingGroup group, Match match) {

        AggregateStats stats = statsByGroup.get(group);
        int startIdx = matches.size() - group.aggregateSize + 1;

        if (startIdx >= 0) {
            stats.reset();
            for (int i = startIdx; i < matches.size(); i++) {
                stats.addMatchStats(matches.get(i));
            }
        }

        stats.addMatchStats(match);
    }

    private int getGroupOldestMatchIndex(RollingGroup group) {
        if (matches.isEmpty()) {
            return -1;
        }

        if (matches.size() <= group.aggregateSize) {
            return 0;
        }

        return matches.size() - group.aggregateSize + 1;
    }

    public Match getOldestMatchForGroup(RollingGroup group) {
        int idx = getGroupOldestMatchIndex(group);

        return idx >= 0? matches.get(idx): null;
    }


    public AggregateStats getStats(RollingGroup group) {
        return statsByGroup.get(group);
    }

    public int matchCount() {
        return matches.size();
    }


    public static class Deserializer implements JsonDeserializer<MatchLog> {

        @Override
        public MatchLog deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MatchLog result = new MatchLog();
            JsonObject matchLog = (JsonObject) json;
            JsonArray matches = matchLog.getAsJsonArray("matches");

            for (JsonElement e : matches) {
                Match match = context.deserialize(e, Match.class);
                result.add(match);
            }

            return result;
        }
    }

}
