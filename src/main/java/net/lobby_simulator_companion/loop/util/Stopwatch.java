package net.lobby_simulator_companion.loop.util;

import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/**
 * Simplified @{@link StopWatch}. Just a wrapper around it to avoid having to deal with states.
 * Guaranteed to avoid exceptions related to the state of the stopwatch.
 *
 * @author NickyRamone
 */
public class Stopwatch {

    private final StopWatch delegate = new StopWatch();


    public void start() {

        if (delegate.isSuspended()) {
            delegate.resume();
        } else if (delegate.isStopped()) {
            delegate.start();
        }
    }

    public void stop() {
        if (delegate.isStopped() || delegate.isSuspended()) {
            return;
        }
        delegate.suspend();
    }

    public void reset() {
        delegate.reset();
    }

    public int getSeconds() {
        return (int) delegate.getTime(TimeUnit.SECONDS);
    }
}
