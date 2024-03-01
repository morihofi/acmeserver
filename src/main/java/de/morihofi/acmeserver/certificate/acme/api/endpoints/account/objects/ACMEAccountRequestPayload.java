package de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents the HTTP request body for creating or managing an account in the {@link AccountEndpoint} and {@link NewAccountEndpoint}.
 * This class encapsulates the details required for ACME account operations, such as contact information and agreement to terms of service.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEAccountRequestPayload {

    /**
     * List of E-Mail contacts in the ACME account.
     * Each contact is specified with a 'mailto:' prefix.
     */
    private List<String> contact;

    /**
     * Indicates whether the user has agreed to the Terms of Service of the ACME Provisioner.
     */
    private boolean termsOfServiceAgreed;

    /**
     * Retrieves the list of email contacts associated with the ACME account.
     * Each email in the list is prefixed with 'mailto:'.
     *
     * @return A list of {@code String} representing the email contacts.
     */
    public List<String> getContact() {
        return contact;
    }

    /**
     * Sets the list of email contacts for the ACME account.
     * Each email should be prefixed with 'mailto:'.
     *
     * @param contact A list of {@code String} to set as the email contacts.
     */
    public void setContact(List<String> contact) {
        this.contact = contact;
    }

    /**
     * Checks if the user has agreed to the Terms of Service.
     * This boolean flag indicates the user's consent to the terms.
     *
     * @return {@code true} if the Terms of Service are agreed, otherwise {@code false}.
     */
    public boolean getTermsOfServiceAgreed() {
        return termsOfServiceAgreed;
    }

    /**
     * Sets the user's agreement status to the Terms of Service.
     * This method updates the user's consent to the terms.
     *
     * @param termsOfServiceAgreed The agreement status to set, {@code true} if agreed, otherwise {@code false}.
     */
    public void setTermsOfServiceAgreed(boolean termsOfServiceAgreed) {
        this.termsOfServiceAgreed = termsOfServiceAgreed;
    }
}

