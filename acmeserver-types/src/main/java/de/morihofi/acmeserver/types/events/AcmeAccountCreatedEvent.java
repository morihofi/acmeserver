package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when an ACME account is created.
 */
@AllArgsConstructor
@Getter
public class AcmeAccountCreatedEvent extends AbstractEvent {
    private final AcmeAccount account;
}
