package net.lobby_simulator_companion.loop.util;

import java.util.function.Supplier;

/**
 * @author NickyRamone
 */
public class LangUtil {

    public interface ThrowingFunction<T, R> {
        R apply() throws Exception;

        @SuppressWarnings("unchecked")
        static <T extends Exception, R> R sneakyThrow(Exception t) throws T {
            throw (T) t;
        }
    }

    public static Supplier unchecked(ThrowingFunction f) {
        return () -> {
            try {
                return f.apply();
            } catch (Exception ex) {
                return ThrowingFunction.sneakyThrow(ex);
            }
        };
    }

}
