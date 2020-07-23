package net.lobby_simulator_companion.loop.service.log_processing.impl;

import net.lobby_simulator_companion.loop.domain.Survivor;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.service.log_processing.EventCaptor;
import net.lobby_simulator_companion.loop.service.log_processing.impl.dto.ChaseEvent;
import net.lobby_simulator_companion.loop.service.log_processing.impl.dto.HitEvent;
import net.lobby_simulator_companion.loop.util.event.EventSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author NickyRamone
 */
public class ChaseLogProcessorUTest {

    private ChaseLogProcessor processor;


    @Before
    public void setUp() {
        processor = new ChaseLogProcessor(new EventSupport());
    }


    @Test
    public void chaseStart() {
        String logLine = "[2020.07.10-23.09.36:685][979]Interaction: Player [BP_CamperFemale08_Character_C_0] is in chase.";
        assertLogEvent(logLine, DbdLogEvent.CHASE_START, new ChaseEvent(
                LocalDateTime.of(2020, 7, 10, 23, 9, 36, 685000000),
                Survivor.NANCY_WHEELER,
                0
        ));

        logLine = "[2020.07.14-21.52.14:710][801]Interaction: Player [BP_CamperMale01_C_0] is in chase.";
        assertLogEvent(logLine, DbdLogEvent.CHASE_START, new ChaseEvent(
                LocalDateTime.of(2020, 7, 14, 21, 52, 14, 710000000),
                Survivor.DWIGHT_FAIRFIELD,
                0
        ));
    }

    @Test
    public void chaseEnd() {
        // arrange
        String logLine = "[2020.07.10-23.29.00:123][900]Interaction: Player [BP_CamperFemale08_Character_C_0] is not in chase anymore.";
        assertLogEvent(logLine, DbdLogEvent.CHASE_END, new ChaseEvent(
                LocalDateTime.of(2020, 7, 10, 23, 29, 0, 123000000),
                Survivor.NANCY_WHEELER,
                0
        ));

        logLine = "[2020.07.10-23.29.00:123][900]Interaction: Player [BP_CamperFemale08_C_1] is not in chase anymore.";
        assertLogEvent(logLine, DbdLogEvent.CHASE_END, new ChaseEvent(
                LocalDateTime.of(2020, 7, 10, 23, 29, 0, 123000000),
                Survivor.NANCY_WHEELER,
                1
        ));
    }

    @Test
    public void killerHit() {
        // arrange
        String logLine = "[2020.07.11-19.34.04:470][882]LogDBDGeneral: On Hit Sprint Effect [BP_CamperMale04_Character_C_0 - Hoogar33]: On";
        assertLogEvent(logLine, DbdLogEvent.KILLER_HIT, new HitEvent(Survivor.BILL_OVERBECK, 0, "Hoogar33"));
    }


    private void assertLogEvent(String logLine, DbdLogEvent expectedLogEvent, Object expectedEventValue) {
        // arrange
        final EventCaptor eventCaptor = new EventCaptor();
        processor.registerListener(eventCaptor::copyFrom);

        // act
        processor.process(logLine, new StateWrapper());

        // assert
        assertThat(eventCaptor.getKey(), equalTo(expectedLogEvent.name()));
        assertThat(eventCaptor.getValue(), equalTo(expectedEventValue));
    }
}
