package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.cert.X509Certificate;

/**
 * Event fired after a certificate for an ACME order was created.
 */
@AllArgsConstructor
@Getter
public class AcmeCertificateCreatedEvent extends AbstractEvent {
    private final AcmeOrder order;
    private final X509Certificate certificate;
}
