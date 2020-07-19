package net.lobby_simulator_companion.loop.util.event;

import java.beans.PropertyChangeSupport;

/**
 * @author NickyRamone
 */
public class EventSupport {

    protected final PropertyChangeSupport propertyChangeSupport;

    protected static final Object NULL_OBJECT = new Object();


    public EventSupport() {
        this.propertyChangeSupport = new PropertyChangeSupport(NULL_OBJECT);
    }

    protected EventSupport(PropertyChangeSupport propertyChangeSupport) {
        this.propertyChangeSupport = propertyChangeSupport;
    }


    public void registerListener(EventListener eventListener) {
        propertyChangeSupport.addPropertyChangeListener(
                evt -> eventListener.eventFired(new Event(evt.getPropertyName(), evt.getNewValue())));
    }

    public void registerListener(Object eventType, EventListener eventListener) {

        propertyChangeSupport.addPropertyChangeListener(eventType.toString(),
                evt -> eventListener.eventFired(new Event(evt.getPropertyName(), evt.getNewValue())));
    }


    public void fireEvent(Object eventType) {
        fireEvent(eventType, null);
    }

    public void fireEvent(Object eventType, Object eventValue) {
        propertyChangeSupport.firePropertyChange(eventType.toString(), null, eventValue);
    }

}
