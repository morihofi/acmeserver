package de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects;

import java.util.List;

public class ACMEAccountRequestBody {
    private List<String> contact; // Assuming the contact field is a list of emails
    private Boolean termsOfServiceAgreed;
    public List<String> getContact() {
        return contact;
    }

    public void setContact(List<String> contact) {
        this.contact = contact;
    }

    public Boolean getTermsOfServiceAgreed() {
        return termsOfServiceAgreed;
    }

    public void setTermsOfServiceAgreed(Boolean termsOfServiceAgreed) {
        this.termsOfServiceAgreed = termsOfServiceAgreed;
    }
}
