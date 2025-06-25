package de.morihofi.acmeserver.types.events;

import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Dispatched after a provisioner's intermediate certificate has been renewed
 * and stored back in the KeyStore. Listeners may reload cached certificates
 * or inform administrators that new credentials are in place.
 */
@AllArgsConstructor
@Getter
public class ProvisionerCertificateRenewedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
