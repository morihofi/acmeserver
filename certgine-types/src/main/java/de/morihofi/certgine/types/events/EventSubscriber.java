package de.morihofi.certgine.types.events;

import java.util.List;

/**
 * Listener capable of handling multiple event types. Implementations return a
 * list of event classes from {@link #canHandle()} indicating which events they
 * want to receive.
 */
public interface EventSubscriber extends EventListener<AbstractEvent> {
    /**
     * Returns the list of event classes this subscriber can handle.
     *
     * @return list of event types
     */
    List<Class<? extends AbstractEvent>> canHandle();
}
