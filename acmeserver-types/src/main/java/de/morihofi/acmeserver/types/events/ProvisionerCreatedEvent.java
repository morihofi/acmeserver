package de.morihofi.acmeserver.types.events;

import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Published whenever a new ACME provisioner is created during initialization or
 * via management interfaces. Useful for automation that must react to newly
 * available provisioners.
 */
@AllArgsConstructor
@Getter
public class ProvisionerCreatedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
