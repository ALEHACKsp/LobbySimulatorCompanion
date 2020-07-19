package net.lobby_simulator_companion.loop.ui.common;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * @author NickyRamone
 */
public class UiEventOrchestrator {

    public enum UiEvent {
        STRUCTURE_RESIZED,
        UPDATE_KILLER_PLAYER,
        UPDATE_KILLER_PLAYER_TITLE_EXTRA,
        UPDATE_KILLER_PLAYER_RATING
    }

    // TODO: replace with SwingEventSupport
    private final SwingPropertyChangeSupport eventSupport = new SwingPropertyChangeSupport(this);


    public void addEventListener(UiEvent uiEvent, PropertyChangeListener listener) {
        eventSupport.addPropertyChangeListener(uiEvent.name(), listener);
    }

    public void fireEvent(UiEvent uiEvent) {
        fireEvent(uiEvent, null);
    }

    public void fireEvent(UiEvent uiEvent, Object value) {
        eventSupport.firePropertyChange(uiEvent.name(), null, value);
    }

}
