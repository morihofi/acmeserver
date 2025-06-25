package de.morihofi.acmeserver.types.events;

import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after a certificate has been revoked. Subscribers may refresh CRLs
 * or perform additional cleanup.
 */
@AllArgsConstructor
@Getter
public class AcmeCertificateRevokedEvent extends AbstractEvent {
    private final AcmeOrder order;
}
