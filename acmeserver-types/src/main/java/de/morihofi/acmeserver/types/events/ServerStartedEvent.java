package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired when the web server has started and is ready to handle incoming
 * connections.
 */
@AllArgsConstructor
@Getter
public class ServerStartedEvent extends AbstractEvent {
    private final IServerInstance serverInstance;
}
