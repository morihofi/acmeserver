package de.morihofi.acmeserver.types.database.entities;

import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AcmeOrderIdentifierChallenge#isChallengeTransitionAllowed(AcmeStatus, AcmeStatus)}.
 */
@DisplayName("ACME authorization-state transitions")
class ACMEOrderIdentifierChallengeStatusTransitionTest {

    /* ---------- transitions that the spec ALLOWS ---------- */
    @ParameterizedTest(name = "{index}: {0} ➜ {1} must be allowed")
    @CsvSource({
            "PENDING, VALID",
            "PENDING, INVALID",
            "PENDING, DEACTIVATED",
            "VALID,   DEACTIVATED",
            "VALID,   REVOKED",
            "VALID,   EXPIRED"
    })
    void allowedTransitions(AcmeStatus from, AcmeStatus to) {
        assertTrue(AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(from, to));
    }

    /* ---------- transitions that the spec FORBIDS ---------- */
    @ParameterizedTest(name = "{index}: {0} ➜ {1} must be rejected")
    @CsvSource({
            // anything other than VALID / INVALID / DEACTIVATED out of PENDING
            "PENDING, REVOKED",
            "PENDING, EXPIRED",

            // terminal states cannot change again
            "INVALID, PENDING",
            "INVALID, VALID",
            "DEACTIVATED, VALID",
            "DEACTIVATED, PENDING",
            "REVOKED,   VALID",
            "REVOKED,   PENDING",
            "EXPIRED,   VALID",
            "EXPIRED,   PENDING"
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
        void nullCurrentState() {
            assertThrows(NullPointerException.class,
                    () -> AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(null, AcmeStatus.PENDING));
        }

        @Test
        @DisplayName("newState == null → NullPointerException")
        void nullNewState() {
            assertThrows(NullPointerException.class,
                    () -> AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(AcmeStatus.PENDING, null));
        }

        @Test
        @DisplayName("both arguments null → NullPointerException")
        void bothNull() {
            assertThrows(NullPointerException.class,
                    () -> AcmeOrderIdentifierChallenge.isChallengeTransitionAllowed(null, null));
        }
    }

}