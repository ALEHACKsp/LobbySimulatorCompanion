package net.lobby_simulator_companion.loop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import net.lobby_simulator_companion.loop.domain.MatchLog;
import net.lobby_simulator_companion.loop.domain.stats.Stats;
import net.lobby_simulator_companion.loop.util.gson.LocalDateTimeTypeAdapter;
import net.lobby_simulator_companion.loop.util.gson.LowercaseEnumTypeAdapterFactory;

import java.time.LocalDateTime;

/**
 * @author NickyRamone
 */
@UtilityClass
public class LoopGsonFactory {


    public Gson gson(boolean prettyJson) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        if (prettyJson) {
            gsonBuilder.setPrettyPrinting();
        }
        gsonBuilder.enableComplexMapKeySerialization();
        configureTypeAdapters(gsonBuilder);

        return gsonBuilder.create();
    }

    private void configureTypeAdapters(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
        gsonBuilder.registerTypeAdapter(Stats.class, new Stats.Serializer());
        gsonBuilder.registerTypeAdapter(Stats.class, new Stats.Deserializer());
        gsonBuilder.registerTypeAdapter(MatchLog.class, new MatchLog.Deserializer());
        gsonBuilder.registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory());
    }


}
