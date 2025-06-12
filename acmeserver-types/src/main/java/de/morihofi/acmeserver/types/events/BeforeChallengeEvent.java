package de.morihofi.acmeserver.types.events;
import de.morihofi.acmeserver.types.events.AbstractEvent;

import de.morihofi.acmeserver.types.api.acme.challenge.AcmeChallengeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Emitted directly before a domain ownership challenge is validated. It
 * includes the challenge method and identifier so subscribers can prepare
 * resources or log the attempt.
 */
@AllArgsConstructor
@Getter
public class BeforeChallengeEvent extends AbstractEvent {
    private final AcmeChallengeType method;
    private final String challengeId;
}
