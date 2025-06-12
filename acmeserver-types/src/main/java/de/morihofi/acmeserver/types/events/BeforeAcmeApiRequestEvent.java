package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired before processing an ACME API request. The event provides the
 * request path and HTTP method so listeners can perform logging or additional
 * security checks.
 */
@AllArgsConstructor
@Getter
public class BeforeAcmeApiRequestEvent extends AbstractEvent {
    private final String path;
    private final String method;
}
