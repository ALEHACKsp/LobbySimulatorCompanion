package net.lobby_simulator_companion.loop.util.event;

import javax.swing.event.SwingPropertyChangeSupport;

import static javax.swing.SwingUtilities.invokeLater;

/**
 * @author NickyRamone
 */
public class SwingEventSupport extends EventSupport {

    public SwingEventSupport() {
        super(new SwingPropertyChangeSupport(NULL_OBJECT));
    }

    @Override
    public void fireEvent(Object eventType, Object eventValue) {
        invokeLater(() -> propertyChangeSupport.firePropertyChange(eventType.toString(), null, eventValue));
    }

}
