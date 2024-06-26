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

package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.certificate.acme.challenges.AcmeChallengeType;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Represents an ACME order identifier entity used for managing challenge verification.
 */
@Entity
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrderIdentifierChallenge implements Serializable {

    /**
     * Logger instance for logging ACME challenge activities.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated challenge ID.
     *
     * @param challengeId    The unique identifier of the challenge associated with the ACME identifier.
     * @param serverInstance The server instance for database connection.
     * @return The ACME identifier matching the provided challenge ID, or null if not found.
     */
    public static ACMEOrderIdentifierChallenge getACMEIdentifierChallenge(String challengeId, ServerInstance serverInstance) {
        ACMEOrderIdentifierChallenge challenge = null;
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            challenge = session.createQuery("FROM ACMEOrderIdentifierChallenge WHERE challengeId = :challengeId",
                            ACMEOrderIdentifierChallenge.class)
                    .setParameter("challengeId", challengeId)
                    .setMaxResults(1)
                    .uniqueResult();

            if (challenge != null) {
                LOG.info("(Challenge ID: {}) Got ACME identifier of type {} with value {}",
                        challengeId,
                        challenge.getIdentifier().getType(),
                        challenge.getIdentifier().getDataValue()

                );
            } else {
                LOG.error("Challenge ID {} returns null for the ACMEOrderIdentifierChallenge, must be something went wrong", challengeId);
            }
            transaction.commit();
        } catch (Exception e) {
            LOG.error("Unable to get ACME identifiers for challenge id {}", challengeId, e);
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
    public static void passChallenge(String challengeId, ServerInstance serverInstance) {
        Transaction transaction = null;
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            transaction = session.beginTransaction();

            ACMEOrderIdentifierChallenge orderIdentifierChallenge = session.get(ACMEOrderIdentifierChallenge.class, challengeId);
            if (orderIdentifierChallenge != null) {
                orderIdentifierChallenge.setStatus(AcmeStatus.VALID);
                orderIdentifierChallenge.setVerifiedTime(Timestamp.from(Instant.now()));
                session.merge(orderIdentifierChallenge);

                LOG.info("ACME challenge {} was marked as passed", challengeId);

                transaction.commit();
            } else {
                LOG.warn("No ACME challenge found with id {}", challengeId);
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            LOG.error("Unable to mark ACME challenge as passed", e);
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
    private AcmeStatus status;

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
    public ACMEOrderIdentifierChallenge(AcmeChallengeType challengeType, ACMEOrderIdentifier identifier) {
        this.challengeType = challengeType;
        this.identifier = identifier;

        // random values
        this.challengeId = Crypto.generateRandomId();
        this.authorizationToken =
                Base64.getUrlEncoder().withoutPadding().encodeToString(Crypto.generateRandomId().getBytes(StandardCharsets.UTF_8));

        // Default status after creation
        this.status = AcmeStatus.PENDING;
    }

    /**
     * Retrieves the ACME order identifier associated with this challenge.
     *
     * @return The ACME order identifier.
     */
    public ACMEOrderIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Sets the ACME order identifier associated with this challenge.
     *
     * @param identifier The ACME order identifier to set.
     */
    public void setIdentifier(ACMEOrderIdentifier identifier) {
        this.identifier = identifier;
    }

    /**
     * Retrieves the unique identifier for the ACME order identifier challenge.
     *
     * @return The challenge ID.
     */
    public String getChallengeId() {
        return challengeId;
    }

    /**
     * Sets the unique identifier for the ACME order identifier challenge.
     *
     * @param challengeId The challenge ID to set.
     */
    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    /**
     * Retrieves the timestamp when the verification of this ACME order identifier occurred.
     *
     * @return The verification timestamp.
     */
    public Timestamp getVerifiedTime() {
        return verifiedTime;
    }

    /**
     * Sets the timestamp when the verification of this ACME order identifier occurred.
     *
     * @param verifiedTime The verification timestamp to set.
     */
    public void setVerifiedTime(Timestamp verifiedTime) {
        this.verifiedTime = verifiedTime;
    }

    /**
     * Retrieves the type of the challenge.
     *
     * @return The challenge type.
     */
    public AcmeChallengeType getChallengeType() {
        return challengeType;
    }

    /**
     * Sets the type of the challenge.
     *
     * @param challengeType The challenge type to set.
     */
    public void setChallengeType(AcmeChallengeType challengeType) {
        this.challengeType = challengeType;
    }

    /**
     * Retrieves the authorization token for this challenge.
     *
     * @return The authorization token.
     */
    public String getAuthorizationToken() {
        return authorizationToken;
    }

    /**
     * Sets the authorization token for this challenge.
     *
     * @param authorizationToken The authorization token to set.
     */
    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    /**
     * Retrieves the status of this challenge.
     *
     * @return The status of this challenge.
     */
    public AcmeStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of this challenge.
     *
     * @param status The status to set.
     */
    public void setStatus(AcmeStatus status) {
        this.status = status;
    }
}
