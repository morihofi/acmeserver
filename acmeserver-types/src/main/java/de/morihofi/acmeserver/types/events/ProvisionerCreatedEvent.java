package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when a new ACME provisioner is created.
 */
@AllArgsConstructor
@Getter
public class ProvisionerCreatedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
