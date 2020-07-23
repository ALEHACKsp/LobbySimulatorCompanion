package net.lobby_simulator_companion.loop.service.log_processing.impl;

import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.service.log_processing.EventCaptor;
import net.lobby_simulator_companion.loop.util.event.EventSupport;
import org.junit.Test;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.State;
import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author NickyRamone
 */
public class MainLogProcessorUTest {

    private MainLogProcessor processor = new MainLogProcessor(new EventSupport());


    @Test
    public void matchEnd_normal() {
        // arrange
        String logLine = "[2020.07.13-19.59.09:349][644]GameFlow: ADBDGameState::SetGameLevelEnded "
                + "- Game marked as ended with reason 'Normal'";

        final EventCaptor eventCaptor = new EventCaptor();
        processor.registerListener(eventCaptor::copyFrom);

        // act
        processor.process(logLine, new StateWrapper(State.IN_MATCH));

        // assert
        assertThat(eventCaptor, equalTo(EventCaptor.builder()
                .key(DbdLogEvent.MATCH_END.name())
                .value(true)
                .build()));
    }


    @Test
    public void matchEnd_killerQuit() {
        // arrange
        String logLine = "[2020.07.13-19.59.09:349][644]GameFlow: ADBDGameState::SetGameLevelEnded "
                + "- Game marked as ended with reason 'KillerLeft'";

        final EventCaptor eventCaptor = new EventCaptor();
        processor.registerListener(eventCaptor::copyFrom);

        // act
        processor.process(logLine, new StateWrapper(State.IN_MATCH));

        // assert
        assertThat(eventCaptor, equalTo(EventCaptor.builder()
                .key(DbdLogEvent.MATCH_END.name())
                .value(false)
                .build()));
    }

}
