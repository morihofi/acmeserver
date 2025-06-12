package de.morihofi.acmeserver.utils.event;

import de.morihofi.acmeserver.types.events.AbstractEvent;
import java.util.List;

/**
 * Listener capable of handling multiple event types.
 */
public interface EventSubscriber extends EventListener<AbstractEvent> {
    /**
     * Returns the list of event classes this subscriber can handle.
     *
     * @return list of event types
     */
    List<Class<? extends AbstractEvent>> canHandle();
}
