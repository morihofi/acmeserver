package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.database.entities.acme.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.cert.X509Certificate;

/**
 * Published once a certificate for an ACME order has been generated and stored.
 * The corresponding order and the resulting {@link java.security.cert.X509Certificate}
 * are provided for logging or integration with other systems.
 */
@AllArgsConstructor
@Getter
public class AcmeCertificateCreatedEvent extends AbstractEvent {
    private final AcmeOrder order;
    private final X509Certificate certificate;
}
