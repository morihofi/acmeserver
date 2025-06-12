package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event emitted after a nonce has been successfully redeemed and therefore
 * can no longer be used. Useful for audit or rate limiting subscribers.
 */
@AllArgsConstructor
@Getter
public class AcmeNonceRedeemedEvent extends AbstractEvent {
    private final String nonce;
}
