package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.api.acme.challenge.AcmeChallengeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired after a challenge was processed.
 */
@AllArgsConstructor
@Getter
public class AfterChallengeEvent extends AbstractEvent {
    private final AcmeChallengeType method;
    private final String challengeId;
    private final boolean successful;
}
