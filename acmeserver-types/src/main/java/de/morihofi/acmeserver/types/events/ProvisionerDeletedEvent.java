package de.morihofi.acmeserver.types.events;

import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired when an existing ACME provisioner is removed from the system. This
 * allows cleanup of resources that were tied to that provisioner.
 */
@AllArgsConstructor
@Getter
public class ProvisionerDeletedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
