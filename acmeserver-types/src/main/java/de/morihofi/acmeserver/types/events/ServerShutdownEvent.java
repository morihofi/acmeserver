package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when the server is shutting down.
 */
@AllArgsConstructor
@Getter
public class ServerShutdownEvent extends AbstractEvent {
    private final IServerInstance serverInstance;
}
