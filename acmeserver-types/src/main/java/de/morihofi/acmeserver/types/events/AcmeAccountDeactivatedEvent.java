package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event emitted after an account has been deactivated via the ACME API.
 * It contains the updated {@link AcmeAccount} so listeners can react to
 * the deactivation (e.g. revoke certificates or disable services).
 */
@AllArgsConstructor
@Getter
public class AcmeAccountDeactivatedEvent extends AbstractEvent {
    private final AcmeAccount account;
}
