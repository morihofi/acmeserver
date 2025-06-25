package de.morihofi.acmeserver.types.events;

import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired once a new ACME order has been persisted. The {@link AcmeOrder}
 * entity can be used by listeners to trigger further processing or notifications.
 */
@AllArgsConstructor
@Getter
public class NewAcmeOrderEvent extends AbstractEvent {
    private final AcmeOrder order;
}
