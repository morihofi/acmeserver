package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when an ACME provisioner is removed.
 */
@AllArgsConstructor
@Getter
public class ProvisionerDeletedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
