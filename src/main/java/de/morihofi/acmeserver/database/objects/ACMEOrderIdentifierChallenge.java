package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.certificate.acme.challenges.AcmeChallengeType;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents an ACME order identifier entity used for managing challenge verification.
 */
@Entity
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrderIdentifierChallenge implements Serializable {

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
        this.authorizationToken = Crypto.generateRandomId();

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
}
