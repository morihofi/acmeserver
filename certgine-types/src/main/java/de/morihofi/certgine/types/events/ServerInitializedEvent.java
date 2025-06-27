package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Emitted after the server instance has been created and configuration loaded
 * but before the HTTP server starts accepting requests.
 */
@AllArgsConstructor
@Getter
public class ServerInitializedEvent extends AbstractEvent {
    private final IServerInstance serverInstance;
}
