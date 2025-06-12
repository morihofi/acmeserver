package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired after a provisioner's intermediate certificate was renewed.
 */
@AllArgsConstructor
@Getter
public class ProvisionerCertificateRenewedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
