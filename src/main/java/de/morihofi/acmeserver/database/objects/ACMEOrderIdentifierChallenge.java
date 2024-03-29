package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.certificate.acme.challenges.AcmeChallengeType;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.HibernateUtil;
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
     * Logger
     */
    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().getClass());

    @Id
    @Column(name = "challengeId", nullable = false)
    private String challengeId;

    @Column(name = "verifiedTime")
    private Timestamp verifiedTime;

    @Column(name = "challengeType", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcmeChallengeType challengeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identifierId", referencedColumnName = "identifierId")
    private ACMEOrderIdentifier identifier;

    @Column(name = "authorizationToken", nullable = false)
    private String authorizationToken;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcmeStatus status;

    public ACMEOrderIdentifierChallenge() {
    }

    public ACMEOrderIdentifierChallenge(AcmeChallengeType challengeType, ACMEOrderIdentifier identifier) {
        this.challengeType = challengeType;
        this.identifier = identifier;

        // random values
        this.challengeId = Crypto.generateRandomId();
        this.authorizationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(Crypto.generateRandomId().getBytes(StandardCharsets.UTF_8));

        //Default status after creation
        this.status = AcmeStatus.PENDING;
    }


    public ACMEOrderIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(ACMEOrderIdentifier identifier) {
        this.identifier = identifier;
    }

    /**
     * Get the challenge ID associated with this ACME order identifier.
     *
     * @return The challenge ID.
     */
    public String getChallengeId() {
        return challengeId;
    }

    /**
     * Set the challenge ID associated with this ACME order identifier.
     *
     * @param challengeId The challenge ID to set.
     */
    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    /**
     * Get the timestamp when the verification of this ACME order identifier occurred.
     *
     * @return The verification timestamp.
     */
    public Timestamp getVerifiedTime() {
        return verifiedTime;
    }

    /**
     * Set the timestamp when the verification of this ACME order identifier occurred.
     *
     * @param verifiedTime The verification timestamp to set.
     */
    public void setVerifiedTime(Timestamp verifiedTime) {
        this.verifiedTime = verifiedTime;
    }

    public AcmeChallengeType getChallengeType() {
        return challengeType;
    }

    public void setChallengeType(AcmeChallengeType challengeType) {
        this.challengeType = challengeType;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    public AcmeStatus getStatus() {
        return status;
    }

    public void setStatus(AcmeStatus status) {
        this.status = status;
    }


    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated challenge ID.
     *
     * @param challengeId The unique identifier of the challenge associated with the ACME identifier.
     * @return The ACME identifier matching the provided challenge ID, or null if not found.
     */
    public static ACMEOrderIdentifierChallenge getACMEIdentifierChallenge(String challengeId) {
        ACMEOrderIdentifierChallenge challenge = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            challenge = session.createQuery("FROM ACMEOrderIdentifierChallenge WHERE challengeId = :challengeId", ACMEOrderIdentifierChallenge.class)
                    .setParameter("challengeId", challengeId)
                    .setMaxResults(1)
                    .uniqueResult();

            if (challenge != null) {
                log.info("(Challenge ID: {}) Got ACME identifier of type {} with value {}",
                        challengeId,
                        challenge.getIdentifier().getType(),
                        challenge.getIdentifier().getDataValue()

                );
            } else {
                log.error("Challenge ID {} returns null for the ACMEOrderIdentifierChallenge, must be something went wrong", challengeId);
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for challenge id {}", challengeId, e);
        }
        return challenge;
    }

    /**
     * This function marks an ACME challenge as passed
     *
     * @param challengeId id of the Challenge, provided in URL
     */
    @Transactional
    public static void passChallenge(String challengeId) {
        Transaction transaction = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
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
}
