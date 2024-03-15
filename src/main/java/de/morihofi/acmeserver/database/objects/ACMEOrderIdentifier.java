package de.morihofi.acmeserver.database.objects;


import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;

/**
 * Represents an ACME identifier entity used for managing order identifiers, challenges, and certificates.
 */
@Entity
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrderIdentifier implements Serializable {

    @Id
    @Column(name = "identifierId", nullable = false)
    private String identifierId;
    @Column(name = "type")
    private String type;

    @Column(name = "dataValue")
    private String dataValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    private ACMEOrder order;



    @Column(name = "hasChallengesGenerated", nullable = false)
    private boolean hasChallengesGenerated = false;

    @OneToMany(mappedBy = "identifier")
    private List<ACMEOrderIdentifierChallenge> challenges;

    @Column(name = "authorizationId", nullable = false)
    private String authorizationId;

    public List<ACMEOrderIdentifierChallenge> getChallenges() {
        return challenges;
    }


    /**
     * Get the associated order for this ACME identifier.
     * @return The ACME order.
     */
    public ACMEOrder getOrder() {
        return order;
    }

    /**
     * Set the associated order for this ACME identifier.
     * @param order The ACME order to set.
     */
    public void setOrder(ACMEOrder order) {
        this.order = order;
    }

    /**
     * Create an instance of ACME identifier with a specified type and data value.
     * @param type The type of the identifier.
     * @param dataValue The data value of the identifier.
     */
    public ACMEOrderIdentifier(String type, String dataValue) {
        this.type = type;
        this.dataValue = dataValue;
    }

    /**
     * Default constructor for ACME identifier.
     */
    public ACMEOrderIdentifier() {
    }



    /**
     * Get the type of this ACME identifier.
     * @return The type of the identifier.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of this ACME identifier.
     * @param type The type of the identifier to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the data value of this ACME identifier.
     * @return The data value of the identifier.
     */
    public String getDataValue() {
        return dataValue;
    }

    /**
     * Set the data value of this ACME identifier.
     * @param dataValue The data value of the identifier to set.
     */
    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }


    public String getIdentifierId() {
        return identifierId;
    }

    public void setIdentifierId(String identifierId) {
        this.identifierId = identifierId;
    }



    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }



    public AcmeStatus getChallengeStatus() {
        // Checks whether there is at least one challenge with the status VALID
        for (ACMEOrderIdentifierChallenge challenge : challenges) {
            if (challenge.getStatus() == AcmeStatus.VALID) {
                return AcmeStatus.VALID;
            }
        }
        // Default return if no challenge has the status VALID
        return AcmeStatus.PENDING;
    }

    public boolean isHasChallengesGenerated() {
        return hasChallengesGenerated;
    }

    public void setHasChallengesGenerated(boolean hasChallengesGenerated) {
        this.hasChallengesGenerated = hasChallengesGenerated;
    }
}
