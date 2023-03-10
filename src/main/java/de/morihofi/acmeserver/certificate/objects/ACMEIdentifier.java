package de.morihofi.acmeserver.certificate.objects;

import java.util.Date;

public class ACMEIdentifier {

    public enum VERIFIED_TYPE {
        NOT_VERIFIED(0), VERIFIED(1), VERIFY_FAILED(2);

        private int dbValue;
        VERIFIED_TYPE(int i) {
            dbValue = 1;
        }
    }

    public static int verifyTypeToDatabaseValue(VERIFIED_TYPE verifiedType) throws IllegalAccessException {
        switch (verifiedType){
            case VERIFIED:
                return 1;
            case NOT_VERIFIED:
                return 0;
            case VERIFY_FAILED:
                return 2;
            default:
                throw new IllegalAccessException("Unknown verify type");
        }
    }


    private String type;
    private String value;

    private String authorizationId;
    private String authorizationToken;

    private String challengeId;

    private boolean verified;
    private Date verifiedDate;


    private String certificateId;
    private String certificateCSR;
    private Date certificateIssued;
    private Date certificateExpires;

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

    public Date getCertificateIssued() {
        return certificateIssued;
    }

    public void setCertificateIssued(Date certificateIssued) {
        this.certificateIssued = certificateIssued;
    }

    public Date getCertificateExpires() {
        return certificateExpires;
    }

    public void setCertificateExpires(Date certificateExpires) {
        this.certificateExpires = certificateExpires;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Date getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(Date verifiedDate) {
        this.verifiedDate = verifiedDate;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public ACMEIdentifier(String type, String value) {
        this.type = type;
        this.value = value;
    }



    public ACMEIdentifier(String type, String value, String authorizationId, String authorizationToken, String challengeId, boolean verified, Date verifiedDate, String certificateId, String certificateCSR, Date certificateIssued, Date certificateExpires) {
        this.type = type;
        this.value = value;
        this.authorizationId = authorizationId;
        this.authorizationToken = authorizationToken;
        this.challengeId = challengeId;
        this.verified = verified;
        this.verifiedDate = verifiedDate;
        this.certificateId = certificateId;
        this.certificateCSR = certificateCSR;
        this.certificateIssued = certificateIssued;
        this.certificateExpires = certificateExpires;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }
}
