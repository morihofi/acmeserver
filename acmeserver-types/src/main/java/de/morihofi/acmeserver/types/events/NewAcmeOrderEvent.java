package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when a new ACME order is created.
 */
@AllArgsConstructor
@Getter
public class NewAcmeOrderEvent extends AbstractEvent {
    private final AcmeOrder order;
}
