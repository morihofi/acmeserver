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

    @Column(name = "certificateId", columnDefinition = "TEXT")
    private String certificateId;

    @Column(name = "certificateCSR", columnDefinition = "TEXT")
    private String certificateCSR;

    @Column(name = "certificateIssued")
    private Timestamp certificateIssued;

    @Column(name = "certificateExpires")
    private Timestamp certificateExpires;

    @Column(name = "certificatePem", columnDefinition = "TEXT")
    private String certificatePem;

    @Column(name = "certificateSerialNumber", precision = 50, scale = 0)
    private BigInteger certificateSerialNumber;

    @Column(name = "revokeStatusCode", nullable = true)
    private Integer revokeStatusCode;

    @Column(name = "revokeTimestamp", nullable = true)
    private Timestamp revokeTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    private ACMEOrder order;

    @Column(name = "provisioner", nullable = false)
    private String provisioner;

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
     * Get the provisioner associated with this ACME identifier.
     * @return The provisioner name.
     */
    public String getProvisioner() {
        return provisioner;
    }

    /**
     * Set the provisioner associated with this ACME identifier.
     * @param provisioner The provisioner name to set.
     */
    public void setProvisioner(String provisioner) {
        this.provisioner = provisioner;
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

    /**
     * Get the certificate ID associated with this ACME identifier.
     * @return The certificate ID.
     */
    public String getCertificateId() {
        return certificateId;
    }

    /**
     * Set the certificate ID associated with this ACME identifier.
     * @param certificateId The certificate ID to set.
     */
    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    /**
     * Get the certificate CSR (Certificate Signing Request) associated with this ACME identifier.
     * @return The certificate CSR.
     */
    public String getCertificateCSR() {
        return certificateCSR;
    }

    /**
     * Set the certificate CSR (Certificate Signing Request) associated with this ACME identifier.
     * @param certificateCSR The certificate CSR to set.
     */
    public void setCertificateCSR(String certificateCSR) {
        this.certificateCSR = certificateCSR;
    }

    /**
     * Get the timestamp when the certificate associated with this ACME identifier was issued.
     * @return The timestamp when the certificate was issued.
     */
    public Timestamp getCertificateIssued() {
        return certificateIssued;
    }

    /**
     * Set the timestamp when the certificate associated with this ACME identifier was issued.
     * @param certificateIssued The timestamp when the certificate was issued to set.
     */
    public void setCertificateIssued(Timestamp certificateIssued) {
        this.certificateIssued = certificateIssued;
    }

    /**
     * Get the timestamp when the certificate associated with this ACME identifier expires.
     * @return The timestamp when the certificate expires.
     */
    public Timestamp getCertificateExpires() {
        return certificateExpires;
    }

    /**
     * Set the timestamp when the certificate associated with this ACME identifier expires.
     * @param certificateExpires The timestamp when the certificate expires to set.
     */
    public void setCertificateExpires(Timestamp certificateExpires) {
        this.certificateExpires = certificateExpires;
    }

    /**
     * Get the PEM-encoded certificate associated with this ACME identifier.
     * @return The PEM-encoded certificate.
     */
    public String getCertificatePem() {
        return certificatePem;
    }

    /**
     * Set the PEM-encoded certificate associated with this ACME identifier.
     * @param certificatePem The PEM-encoded certificate to set.
     */
    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    /**
     * Get the serial number of the certificate associated with this ACME identifier.
     * @return The certificate's serial number.
     */
    public BigInteger getCertificateSerialNumber() {
        return certificateSerialNumber;
    }

    /**
     * Set the serial number of the certificate associated with this ACME identifier.
     * @param certificateSerialNumber The certificate's serial number to set.
     */
    public void setCertificateSerialNumber(BigInteger certificateSerialNumber) {
        this.certificateSerialNumber = certificateSerialNumber;
    }

    /**
     * Get the revoke status code associated with this ACME identifier.
     * @return The revoke status code.
     */
    public Integer getRevokeStatusCode() {
        return revokeStatusCode;
    }

    /**
     * Set the revoke status code associated with this ACME identifier.
     * @param revokeStatusCode The revoke status code to set.
     */
    public void setRevokeStatusCode(Integer revokeStatusCode) {
        this.revokeStatusCode = revokeStatusCode;
    }

    /**
     * Get the timestamp when this ACME identifier was revoked.
     * @return The timestamp when the identifier was revoked.
     */
    public Timestamp getRevokeTimestamp() {
        return revokeTimestamp;
    }

    /**
     * Set the timestamp when this ACME identifier was revoked.
     * @param revokeTimestamp The timestamp when the identifier was revoked to set.
     */
    public void setRevokeTimestamp(Timestamp revokeTimestamp) {
        this.revokeTimestamp = revokeTimestamp;
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
