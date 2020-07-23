package net.lobby_simulator_companion.loop.util.event;

import javax.swing.event.SwingPropertyChangeSupport;

/**
 * @author NickyRamone
 */
public class SwingEventSupport extends EventSupport {

    public SwingEventSupport() {
        super(new SwingPropertyChangeSupport(NULL_OBJECT));
    }

}
