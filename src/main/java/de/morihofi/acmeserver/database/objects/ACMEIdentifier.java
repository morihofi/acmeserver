package de.morihofi.acmeserver.database.objects;


import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;

/**
 * Represents an ACME identifier entity used for managing order identifiers, challenges, and certificates.
 */
@Entity
@Table(name = "orderidentifiers")
public class ACMEIdentifier implements Serializable {

    @Id
    @Column(name = "challengeId")
    private String challengeId;

    @Column(name = "type")
    private String type;

    @Column(name = "dataValue")
    private String dataValue;

    @Column(name = "authorizationToken")
    private String authorizationToken;

    @Column(name = "authorizationId")
    private String authorizationId;

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "verifiedTime")
    private Timestamp verifiedTime;

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
    public ACMEIdentifier(String type, String dataValue) {
        this.type = type;
        this.dataValue = dataValue;
    }

    /**
     * Default constructor for ACME identifier.
     */
    public ACMEIdentifier() {
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
     * Get the challenge ID of this ACME identifier.
     * @return The challenge ID.
     */
    public String getChallengeId() {
        return challengeId;
    }

    /**
     * Set the challenge ID of this ACME identifier.
     * @param challengeId The challenge ID to set.
     */
    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
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
     * Get the authorization token associated with this ACME identifier.
     * @return The authorization token.
     */
    public String getAuthorizationToken() {
        return authorizationToken;
    }

    /**
     * Set the authorization token associated with this ACME identifier.
     * @param authorizationToken The authorization token to set.
     */
    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    /**
     * Get the authorization ID associated with this ACME identifier.
     * @return The authorization ID.
     */
    public String getAuthorizationId() {
        return authorizationId;
    }

    /**
     * Set the authorization ID associated with this ACME identifier.
     * @param authorizationId The authorization ID to set.
     */
    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    /**
     * Check if this ACME identifier is verified.
     * @return True if verified, false otherwise.
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * Set the verification status of this ACME identifier.
     * @param verified True if verified, false otherwise.
     */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    /**
     * Get the timestamp when this ACME identifier was verified.
     * @return The verified timestamp.
     */
    public Timestamp getVerifiedTime() {
        return verifiedTime;
    }

    /**
     * Set the timestamp when this ACME identifier was verified.
     * @param verifiedTime The verified timestamp to set.
     */
    public void setVerifiedTime(Timestamp verifiedTime) {
        this.verifiedTime = verifiedTime;
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

}
