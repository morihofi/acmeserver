package de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.objects;

public class ACMEChallengeResponse {

    /**
     * Status of the ACME challenge
     */
    private String status;

    /**
     * Verified date, formatted as a string
     */
    private String verified;

    /**
     * URL for challenge approval
     */
    private String url;

    /**
     * Token to place
     */
    private String token;

    /**
     * Type of the challenge
     */
    private String type;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerified() {
        return verified;
    }

    public void setVerified(String verified) {
        this.verified = verified;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
