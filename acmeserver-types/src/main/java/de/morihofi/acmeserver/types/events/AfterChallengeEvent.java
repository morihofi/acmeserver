package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.api.acme.challenge.AcmeChallengeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after the ownership challenge has been processed. Contains the result
 * indicating whether validation succeeded.
 */
@AllArgsConstructor
@Getter
public class AfterChallengeEvent extends AbstractEvent {
    private final AcmeChallengeType method;
    private final String challengeId;
    private final boolean successful;
}
