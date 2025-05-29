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
public class ACMEOrderIdentifierChallenge implements Serializable {


    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated challenge ID.
     *
     * @param challengeId    The unique identifier of the challenge associated with the ACME identifier.
     * @param serverInstance The server instance for database connection.
     * @return The ACME identifier matching the provided challenge ID, or null if not found.
     */
    public static ACMEOrderIdentifierChallenge getACMEIdentifierChallenge(@NonNull String challengeId, IServerInstance serverInstance) {
        ACMEOrderIdentifierChallenge challenge = null;
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();
            challenge = session.createQuery("FROM ACMEOrderIdentifierChallenge WHERE challengeId = :challengeId",
                            ACMEOrderIdentifierChallenge.class)
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

    /**
     * This function marks an ACME challenge as passed.
     *
     * @param challengeId    The ID of the Challenge, provided in URL.
     * @param serverInstance The server instance for database connection.
     */
    @Transactional
    public static void passChallenge(String challengeId, IServerInstance serverInstance) {
        Transaction transaction = null;
        try (Session session = serverInstance.getDatabaseSession()) {
            transaction = session.beginTransaction();

            ACMEOrderIdentifierChallenge orderIdentifierChallenge = session.get(ACMEOrderIdentifierChallenge.class, challengeId);
            if (orderIdentifierChallenge != null) {
                orderIdentifierChallenge.setStatus(AcmeStatus.VALID);
                orderIdentifierChallenge.setVerifiedTime(Timestamp.from(Instant.now()));
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
    private ACMEOrderIdentifier identifier;

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
     * Default constructor for ACME order identifier challenge.
     */
    public ACMEOrderIdentifierChallenge() {
    }

    /**
     * Creates an instance of ACME order identifier challenge with a specified challenge type and identifier.
     *
     * @param challengeType The type of the challenge.
     * @param identifier    The ACME order identifier associated with this challenge.
     */
    public ACMEOrderIdentifierChallenge(AcmeChallengeType challengeType, ACMEOrderIdentifier identifier, String challengeId, String authorizationTokenBase64Url) {
        this.challengeType = challengeType;
        this.identifier = identifier;

        // random values
        this.challengeId = challengeId;
        this.authorizationToken = authorizationTokenBase64Url;

    }


}
