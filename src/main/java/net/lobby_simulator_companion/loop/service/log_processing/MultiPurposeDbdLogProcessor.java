package net.lobby_simulator_companion.loop.service.log_processing;

import net.lobby_simulator_companion.loop.util.event.EventSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;

/**
 * @author NickyRamone
 */
public abstract class MultiPurposeDbdLogProcessor extends AbstractDbdLogProcessor {

    private final List<BiFunction<String, StateWrapper, Boolean>> lineProcessors = new ArrayList<>();


    public MultiPurposeDbdLogProcessor(EventSupport eventSupport) {
        super(eventSupport);
    }

    @Override
    public boolean process(String logLine, StateWrapper stateWrapper) {
        boolean breakTheChain = false;

        for (BiFunction<String, StateWrapper, Boolean> lineProcessor : lineProcessors) {
            if (lineProcessor.apply(logLine, stateWrapper)) {
                breakTheChain = true;
                break;
            }
        }

        return breakTheChain;
    }


    protected void addLineProcessors(List<BiFunction<String, StateWrapper, Boolean>> lineProcessors) {
        this.lineProcessors.addAll(lineProcessors);
    }

}
