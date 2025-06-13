package de.morihofi.acmeserver.types.events;

import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Published when an ACME order requires certificate issuance. This event
 * allows asynchronous workers to pick up the order and generate the
 * certificate.
 */
@AllArgsConstructor
@Getter
public class AcmeCertificateIssuanceRequestedEvent extends AbstractEvent {
    private final AcmeOrder order;
}
