/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.entities;

import de.morihofi.certgine.acme.types.api.AcmeChallengeType;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.types.intf.IServerInstance;
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
     * Unique identifier for the ACME order identifier challenge.
     */
    @Id
    @Column(name = "challengeId", nullable = false)
    private String challengeId;
    /**
     * The timestamp when the verification of this ACME order identifier occurred.
     */
    @Column(name = "verifiedTime", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant verifiedTime;
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
            challenge = session.createQuery("FROM AcmeOrderIdentifierChallenge WHERE challengeId = :challengeId",
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
                log.error("Challenge ID {} returns null for the AcmeOrderIdentifierChallenge, must be something went wrong", challengeId);
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

                if (
                        !isChallengeTransitionAllowed(orderIdentifierChallenge.getStatus(), newState)
                ) {
                    throw new IllegalStateException("The challenge transition from " + orderIdentifierChallenge.getStatus() + " to " + newState + " is not allowed");
                }

                orderIdentifierChallenge.setStatus(newState);

                if (newState.equals(AcmeStatus.VALID)) {
                    orderIdentifierChallenge.setVerifiedTime(Instant.now());
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
     * challenge-status state machine (RFC 8555 §7.1.5).
     *
     * <pre>
     *              pending ──► processing ──┬─────► valid
     *                        │             └─────► invalid
     *                        └─────────────► invalid
     * </pre>
     * <p>
     * Once a challenge is in {@code valid} or {@code invalid}, it is a terminal state and can no longer transition.
     */
    public static boolean isChallengeTransitionAllowed(@NonNull AcmeStatus currentState,
                                                       @NonNull AcmeStatus newState) {

        return switch (currentState) {
            /* -------------------------------- pending --------------------------- */
            case PENDING -> newState == AcmeStatus.PROCESSING
                    || newState == AcmeStatus.INVALID;

            /* -------------------------------- processing --------------------- */
            case PROCESSING -> newState == AcmeStatus.PROCESSING
                    || newState == AcmeStatus.VALID
                    || newState == AcmeStatus.INVALID;

            case VALID, INVALID -> false;

            /* ------------- unknown enum constant (defensive fallback) ----------- */
            default -> {
                log.warn("Unknown challenge state '{}'; refusing transition to '{}'",
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


}
