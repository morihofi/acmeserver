package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired after a new ACME account was created and stored in the database.
 * Subscribers can inspect the {@link AcmeAccount} instance to perform
 * additional tasks such as audit logging.
 */
@AllArgsConstructor
@Getter
public class AcmeAccountCreatedEvent extends AbstractEvent {
    private final AcmeAccount account;
}
