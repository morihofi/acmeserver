package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired once the HTTP server is ready to accept requests.
 */
@AllArgsConstructor
@Getter
public class ServerStartedEvent extends AbstractEvent {
    private final IServerInstance serverInstance;
}
