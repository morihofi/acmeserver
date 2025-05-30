/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.types.database.entities;

import de.morihofi.acmeserver.types.api.acme.challenge.AcmeChallengeType;
import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Represents an ACME order identifier entity used for managing challenge verification.
 */
@Entity
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
@NoArgsConstructor
public class AcmeOrderIdentifierChallenge implements Serializable {


    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated challenge ID.
     *
     * @param challengeId    The unique identifier of the challenge associated with the ACME identifier.
     * @param serverInstance The server instance for database connection.
     * @return The ACME identifier matching the provided challenge ID, or null if not found.
     */
    public static AcmeOrderIdentifierChallenge getACMEIdentifierChallenge(@NonNull String challengeId, @NonNull IServerInstance serverInstance) {
        AcmeOrderIdentifierChallenge challenge = null;
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();
            challenge = session.createQuery("FROM ACMEOrderIdentifierChallenge WHERE challengeId = :challengeId",
                            AcmeOrderIdentifierChallenge.class)
                    .setParameter("challengeId", challengeId)
                    .setMaxResults(1)
                    .uniqueResult();

            if (challenge != null) {
                log.info("Got ACME identifier of type {} with value {} for challenge ID: {}",
                        challenge.getIdentifier().getType(),
                        challenge.getIdentifier().getDataValue(),
                        challengeId
                );
            } else {
                log.error("Challenge ID {} returns null for the ACMEOrderIdentifierChallenge, must be something went wrong", challengeId);
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get ACME identifiers for challenge id {}", challengeId, e);
        }
        return challenge;
    }


    public static void markChallenge(AcmeStatus newState, String challengeId, IServerInstance serverInstance) {
        Transaction transaction = null;
        try (Session session = serverInstance.getDatabaseSession()) {
            transaction = session.beginTransaction();

            AcmeOrderIdentifierChallenge orderIdentifierChallenge = session.get(AcmeOrderIdentifierChallenge.class, challengeId);
            if (orderIdentifierChallenge != null) {

                if(
                        !isChallengeTransitionAllowed(orderIdentifierChallenge.getStatus(), newState)
                ){
                    throw new IllegalStateException("The challenge transition from " + orderIdentifierChallenge.getStatus() + " to " + newState + " is not allowed");
                }

                orderIdentifierChallenge.setStatus(newState);

                if(newState.equals(AcmeStatus.VALID)){
                    orderIdentifierChallenge.setVerifiedTime(Timestamp.from(Instant.now()));
                }

                session.merge(orderIdentifierChallenge);

                log.info("ACME challenge {} was marked as passed", challengeId);

                transaction.commit();
            } else {
                log.warn("No ACME challenge found with id {}", challengeId);
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Unable to mark ACME challenge as passed", e);
        }
    }

    /**
     * Returns {@code true} iff the requested state change is permitted by the ACME
     *  authorization-status state machine (RFC 8555 §7.1.6).
     *
     * <pre>
     *              pending ──┬─────────► valid ──┬────────► revoked   (server)
     *                        │                   │
     *                        │                   ├────────► deactivated (client or server)
     *                        │                   │
     *                        │                   └────────► expired    (clock)
     *                        │
     *                        └─────────► invalid   (challenge failure / error)
     * </pre>
     *
     * Once an authorization is in {@code invalid}, {@code revoked}, {@code
     * deactivated}, or {@code expired}, it is a terminal state and can no longer
     * transition.
     */
    static boolean isChallengeTransitionAllowed(@NonNull AcmeStatus currentState,
                                                @NonNull AcmeStatus newState) {

        return switch (currentState) {
            /* -------------------------------- pending --------------------------- */
            case PENDING -> newState == AcmeStatus.VALID
                    || newState == AcmeStatus.INVALID
                    || newState == AcmeStatus.DEACTIVATED;

            /* -------------------------------- valid ----------------------------- */
            case VALID -> newState == AcmeStatus.DEACTIVATED
                    || newState == AcmeStatus.REVOKED
                    || newState == AcmeStatus.EXPIRED;

            /* ------------- terminal states: nothing may change them ------------- */
            case INVALID, DEACTIVATED, REVOKED, EXPIRED -> false;

            /* ------------- unknown enum constant (defensive fallback) ----------- */
            default -> {
                log.warn("Unknown authorization state '{}'; refusing transition to '{}'",
                        currentState, newState);
                yield false;
            }
        };
    }


    public static void failChallenge(String challengeId, IServerInstance serverInstance) {
        markChallenge(AcmeStatus.INVALID, challengeId, serverInstance);
    }

    /**
     * This function marks an ACME challenge as passed.
     *
     * @param challengeId    The ID of the Challenge, provided in URL.
     * @param serverInstance The server instance for database connection.
     */
    @Transactional
    public static void passChallenge(String challengeId, IServerInstance serverInstance) {
        markChallenge(AcmeStatus.VALID, challengeId, serverInstance);
    }

    /**
     * Unique identifier for the ACME order identifier challenge.
     */
    @Id
    @Column(name = "challengeId", nullable = false)
    private String challengeId;

    /**
     * The timestamp when the verification of this ACME order identifier occurred.
     */
    @Column(name = "verifiedTime")
    private Timestamp verifiedTime;

    /**
     * The type of the challenge (e.g., "http-01", "dns-01").
     */
    @Column(name = "challengeType", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcmeChallengeType challengeType;

    /**
     * The ACME order identifier associated with this challenge.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identifierId", referencedColumnName = "identifierId")
    private AcmeOrderIdentifier identifier;

    /**
     * The authorization token for this challenge.
     */
    @Column(name = "authorizationToken", nullable = false)
    private String authorizationToken;

    /**
     * The status of this challenge (e.g., "pending", "valid").
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcmeStatus status = AcmeStatus.PENDING;

    /**
     * Creates an instance of ACME order identifier challenge with a specified challenge type and identifier.
     *
     * @param challengeType The type of the challenge.
     * @param identifier    The ACME order identifier associated with this challenge.
     */
    public AcmeOrderIdentifierChallenge(AcmeChallengeType challengeType, AcmeOrderIdentifier identifier, String challengeId, String authorizationTokenBase64Url) {
        this.challengeType = challengeType;
        this.identifier = identifier;

        // random values
        this.challengeId = challengeId;
        this.authorizationToken = authorizationTokenBase64Url;

    }



}
