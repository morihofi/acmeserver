package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.exception.ACMEException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when an ACME related exception occurs.
 */
@AllArgsConstructor
@Getter
public class AcmeExceptionEvent extends AbstractEvent {
    private final ACMEException exception;
}
