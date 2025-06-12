package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when the server configuration and dependencies are initialized.
 */
@AllArgsConstructor
@Getter
public class ServerInitializedEvent extends AbstractEvent {
    private final IServerInstance serverInstance;
}
