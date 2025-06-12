package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when an ACME nonce was consumed.
 */
@AllArgsConstructor
@Getter
public class AcmeNonceRedeemedEvent extends AbstractEvent {
    private final String nonce;
}
