package de.morihofi.acmeserver.database.objects;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents an ACME order identifier entity used for managing challenge verification.
 */
@Entity
@Table(name = "orderidentifiers")
public class ACMEOrderIdentifier implements Serializable {

    @Id
    @Column(name = "challengeId")
    private String challengeId;

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "verifiedTime")
    private Timestamp verifiedTime;

    /**
     * Get the challenge ID associated with this ACME order identifier.
     * @return The challenge ID.
     */
    public String getChallengeId() {
        return challengeId;
    }

    /**
     * Set the challenge ID associated with this ACME order identifier.
     * @param challengeId The challenge ID to set.
     */
    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    /**
     * Check if this ACME order identifier is verified.
     * @return True if verified, false otherwise.
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * Set the verification status of this ACME order identifier.
     * @param verified True if verified, false otherwise.
     */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    /**
     * Get the timestamp when the verification of this ACME order identifier occurred.
     * @return The verification timestamp.
     */
    public Timestamp getVerifiedTime() {
        return verifiedTime;
    }

    /**
     * Set the timestamp when the verification of this ACME order identifier occurred.
     * @param verifiedTime The verification timestamp to set.
     */
    public void setVerifiedTime(Timestamp verifiedTime) {
        this.verifiedTime = verifiedTime;
    }

}
