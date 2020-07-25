package net.lobby_simulator_companion.loop.domain.stats;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.RequiredArgsConstructor;
import net.lobby_simulator_companion.loop.domain.stats.periodic.DailyStats;
import net.lobby_simulator_companion.loop.domain.stats.periodic.GlobalStats;
import net.lobby_simulator_companion.loop.domain.stats.periodic.MonthlyStats;
import net.lobby_simulator_companion.loop.domain.stats.periodic.PeriodStats;
import net.lobby_simulator_companion.loop.domain.stats.periodic.WeeklyStats;
import net.lobby_simulator_companion.loop.domain.stats.periodic.YearlyStats;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author NickyRamone
 */
public class Stats {

    @RequiredArgsConstructor
    public enum Period {
        DAILY("Daily"),
        WEEKLY("Weekly"),
        MONTHLY("Monthly"),
        YEARLY("Yearly"),
        GLOBAL("Global");

        private final String description;

        @Override
        public String toString() {
            return description;
        }
    }

    private final PeriodStats[] periodsStats = new PeriodStats[Period.values().length];


    public Stats() {
        LocalDateTime now = LocalDateTime.now();
        periodsStats[Period.DAILY.ordinal()] = new DailyStats(now);
        periodsStats[Period.WEEKLY.ordinal()] = new WeeklyStats(now);
        periodsStats[Period.MONTHLY.ordinal()] = new MonthlyStats(now);
        periodsStats[Period.YEARLY.ordinal()] = new YearlyStats(now);
        periodsStats[Period.GLOBAL.ordinal()] = new GlobalStats(now);
    }

    public PeriodStats get(Period period) {
        return periodsStats[period.ordinal()];
    }

    void set(Period period, PeriodStats periodStats) {
        periodsStats[period.ordinal()] = periodStats;
    }


    public void addMatchStats(Match matchStats) {
        for (PeriodStats p : periodsStats) {
            p.addMatchStats(matchStats);
        }
    }

    public Stream<PeriodStats> asStream() {
        return Arrays.stream(periodsStats);
    }


    public static final class Serializer implements JsonSerializer<Stats> {
        @Override
        public JsonElement serialize(Stats src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObj = new JsonObject();

            for (Stats.Period period : Stats.Period.values()) {
                jsonObj.add(period.name().toLowerCase(), context.serialize(src.get(period)));
            }

            return jsonObj;
        }
    }


    public static final class Deserializer implements JsonDeserializer<Stats> {

        @Override
        public Stats deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Stats result = new Stats();

            JsonObject jsonObj = json.getAsJsonObject();
            jsonObj.entrySet().forEach(e -> {
                Stats.Period period = Stats.Period.valueOf(e.getKey().toUpperCase());
                PeriodStats periodStats = null;

                switch (period) {
                    case DAILY:
                        periodStats = context.deserialize(e.getValue(), DailyStats.class);
                        break;
                    case WEEKLY:
                        periodStats = context.deserialize(e.getValue(), WeeklyStats.class);
                        break;
                    case MONTHLY:
                        periodStats = context.deserialize(e.getValue(), MonthlyStats.class);
                        break;
                    case YEARLY:
                        periodStats = context.deserialize(e.getValue(), YearlyStats.class);
                        break;
                    case GLOBAL:
                        periodStats = context.deserialize(e.getValue(), GlobalStats.class);
                        break;
                }

                result.set(period, periodStats);
            });

            return result;
        }
    }

}
