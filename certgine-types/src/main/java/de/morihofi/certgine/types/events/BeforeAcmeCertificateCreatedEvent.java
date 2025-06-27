package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.database.entities.acme.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Triggered right before the certificate for an ACME order is generated. This
 * allows subscribers to prepare any external systems that need to be aware of
 * upcoming certificate creation.
 */
@AllArgsConstructor
@Getter
public class BeforeAcmeCertificateCreatedEvent extends AbstractEvent {
    private final AcmeOrder order;
}
