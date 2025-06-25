package de.morihofi.acmeserver.types.database.entities;

import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrderIdentifierChallenge;
import de.morihofi.acmeserver.types.database.entities.acme.enums.AcmeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AcmeOrderIdentifierChallenge#isChallengeTransitionAllowed(AcmeStatus, AcmeStatus)}.
 */
@DisplayName("ACME challenge-state transitions")
class AcmeOrderIdentifierChallengeStatusTransitionTest {

    /* ---------- transitions that the spec ALLOWS ---------- */
    @ParameterizedTest(name = "{index}: {0} ➜ {1} must be allowed")
    @CsvSource({
            "PENDING, PROCESSING",
            "PENDING, INVALID",
            "PROCESSING, PROCESSING",
            "PROCESSING, VALID",
            "PROCESSING, INVALID"
    })
    void allowedTransitions(AcmeStatus from, AcmeStatus to) {
        assertTrue(AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(from, to));
    }

    /* ---------- transitions that the spec FORBIDS ---------- */
    @ParameterizedTest(name = "{index}: {0} ➜ {1} must be rejected")
    @CsvSource({
            // disallowed transitions from PENDING
            "PENDING, VALID",
            "PENDING, DEACTIVATED",
            "PENDING, REVOKED",
            "PENDING, EXPIRED",

            // disallowed transitions from PROCESSING
            "PROCESSING, DEACTIVATED",
            "PROCESSING, REVOKED",
            "PROCESSING, EXPIRED",
            "PROCESSING, PENDING",

            // terminal states cannot change again
            "VALID, PROCESSING",
            "VALID, PENDING",
            "INVALID, VALID",
            "INVALID, PROCESSING"
    })
    void forbiddenTransitions(AcmeStatus from, AcmeStatus to) {
        assertFalse(AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(from, to));
    }

    /* ---------- null-safety checks ---------- */
    @Nested
    @DisplayName("Null-argument handling")
    class NullArgumentTests {

        @Test
        @DisplayName("currentState == null → NullPointerException")
        @SuppressWarnings("ConstantConditions")
        void nullCurrentState() {
            assertThrows(NullPointerException.class,
                    () -> AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(null, AcmeStatus.PENDING));
        }

        @Test
        @DisplayName("newState == null → NullPointerException")
        @SuppressWarnings("ConstantConditions")
        void nullNewState() {
            assertThrows(NullPointerException.class,
                    () -> AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(AcmeStatus.PENDING, null));
        }

        @Test
        @DisplayName("both arguments null → NullPointerException")
        @SuppressWarnings("ConstantConditions")
        void bothNull() {
            assertThrows(NullPointerException.class,
                    () -> AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(null, null));
        }
    }

}