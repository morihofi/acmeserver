package de.morihofi.acmeserver.types.events;

import de.morihofi.acmeserver.types.database.entities.TsaAuthority;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Event published when a new TSA authority is created. */
@AllArgsConstructor
@Getter
public class TsaAuthorityCreatedEvent extends AbstractEvent {
    private final TsaAuthority authority;
}
