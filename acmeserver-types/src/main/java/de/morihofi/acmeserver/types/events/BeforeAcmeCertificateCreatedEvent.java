package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired before a certificate for an ACME order is generated.
 */
@AllArgsConstructor
@Getter
public class BeforeAcmeCertificateCreatedEvent extends AbstractEvent {
    private final AcmeOrder order;
}
