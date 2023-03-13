package de.morihofi.acmeserver.certificate.objects;

import java.util.ArrayList;

public class ACMEAccount {

    private String accountId;
    private ArrayList<String> emails;
    private boolean deactivated;
    private String jwt;

    public ACMEAccount(String accountId, ArrayList<String> email, boolean deactivated, String jwt) {
        this.accountId = accountId;
        this.emails = email;
        this.deactivated = deactivated;
        this.jwt = jwt;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public ArrayList<String> getEmails() {
        return emails;
    }

    public void setEmails(ArrayList<String> email) {
        this.emails = email;
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
