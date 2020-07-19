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

            AggregateStats aggregateStats = statsByGroup.get(group);

            if (matches.size() >= group.aggregateSize) {
                Match oldestMatch = matches.get(matches.size() - group.aggregateSize);
                aggregateStats.substractMatchStats(oldestMatch);
            }

            aggregateStats.addMatchStats(match);
        }
        matches.add(match);
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
