package de.morihofi.acmeserver.certificate.objects;

public class ACMEAccount {

    private String accountId;
    private String email;
    private boolean deactivated;
    private String jwt;

    public ACMEAccount(String accountId, String email, boolean deactivated, String jwt) {
        this.accountId = accountId;
        this.email = email;
        this.deactivated = deactivated;
        this.jwt = jwt;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
