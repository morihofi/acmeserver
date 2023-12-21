package de.morihofi.acmeserver.database.objects;


import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;

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

    public ACMEOrder getOrder() {
        return order;
    }

    public void setOrder(ACMEOrder order) {
        this.order = order;
    }

    public ACMEIdentifier(String type, String dataValue) {
        this.type = type;
        this.dataValue = dataValue;
    }

    public ACMEIdentifier() {
    }

    public String getProvisioner() {
        return provisioner;
    }

    public void setProvisioner(String provisioner) {
        this.provisioner = provisioner;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Timestamp getVerifiedTime() {
        return verifiedTime;
    }

    public void setVerifiedTime(Timestamp verifiedTime) {
        this.verifiedTime = verifiedTime;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public String getCertificateCSR() {
        return certificateCSR;
    }

    public void setCertificateCSR(String certificateCSR) {
        this.certificateCSR = certificateCSR;
    }

    public Timestamp getCertificateIssued() {
        return certificateIssued;
    }

    public void setCertificateIssued(Timestamp certificateIssued) {
        this.certificateIssued = certificateIssued;
    }

    public Timestamp getCertificateExpires() {
        return certificateExpires;
    }

    public void setCertificateExpires(Timestamp certificateExpires) {
        this.certificateExpires = certificateExpires;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    public BigInteger getCertificateSerialNumber() {
        return certificateSerialNumber;
    }

    public void setCertificateSerialNumber(BigInteger certificateSerialNumber) {
        this.certificateSerialNumber = certificateSerialNumber;
    }

    public Integer getRevokeStatusCode() {
        return revokeStatusCode;
    }

    public void setRevokeStatusCode(Integer revokeStatusCode) {
        this.revokeStatusCode = revokeStatusCode;
    }

    public Timestamp getRevokeTimestamp() {
        return revokeTimestamp;
    }

    public void setRevokeTimestamp(Timestamp revokeTimestamp) {
        this.revokeTimestamp = revokeTimestamp;
    }
}
