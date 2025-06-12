package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Published when the runtime shutdown hook is executed. Use this to cleanly
 * close resources before the JVM exits.
 */
@AllArgsConstructor
@Getter
public class ServerShutdownEvent extends AbstractEvent {
    private final IServerInstance serverInstance;
}
