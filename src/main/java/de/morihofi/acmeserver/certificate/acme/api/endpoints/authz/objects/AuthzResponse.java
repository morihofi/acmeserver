package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.Identifier;

import java.util.List;

public class AuthzResponse {
    private String status;
    private String expires;
    private Identifier identifier;
    private List<Challenge> challenges;

    // Constructor
    public AuthzResponse() {
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    public List<Challenge> getChallenges() {
        return challenges;
    }

    public void setChallenges(List<Challenge> challenges) {
        this.challenges = challenges;
    }
}
