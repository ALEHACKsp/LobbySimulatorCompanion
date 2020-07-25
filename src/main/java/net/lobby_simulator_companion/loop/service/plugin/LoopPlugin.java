package net.lobby_simulator_companion.loop.service.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author NickyRamone
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoopPlugin {

    String id();

    String author() default "";

    String description() default "";

}
