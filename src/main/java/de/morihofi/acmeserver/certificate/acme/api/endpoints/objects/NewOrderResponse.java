package de.morihofi.acmeserver.certificate.acme.api.endpoints.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.NewOrderEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents the response for a new certificate order request. This class encapsulates details of the response returned by the
 * {@link NewOrderEndpoint}. It includes information such as the order status, expiration, validity period, identifiers, authorization
 * details, and finalization URL.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class NewOrderResponse {
    private String status;
    private String expires;
    private String notBefore;
    private String notAfter;
    private List<Identifier> identifiers;
    private List<String> authorizations;
    private String finalize;

    /**
     * Gets the current status of the order.
     *
     * @return The order status as a {@code String}.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the order.
     *
     * @param status The new status as a {@code String}.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the expiration date of the order. This method returns the date and time when the order is set to expire.
     *
     * @return The expiration date as a {@code String}.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Sets the expiration date of the order. This method allows setting a new expiration date and time for the order.
     *
     * @param expires The new expiration date as a {@code String}.
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }

    /**
     * Retrieves the 'not before' date of the order. This method returns the date and time before which the order is not valid.
     *
     * @return The 'not before' date as a {@code String}.
     */
    public String getNotBefore() {
        return notBefore;
    }

    /**
     * Sets the 'not before' date of the order. This method allows specifying the date and time before which the order should not be
     * considered valid.
     *
     * @param notBefore The 'not before' date as a {@code String}.
     */
    public void setNotBefore(String notBefore) {
        this.notBefore = notBefore;
    }

    /**
     * Retrieves the 'not after' date of the order. This method returns the date and time after which the order is no longer valid.
     *
     * @return The 'not after' date as a {@code String}.
     */
    public String getNotAfter() {
        return notAfter;
    }

    /**
     * Sets the 'not after' date of the order. This method allows specifying the date and time after which the order should not be
     * considered valid.
     *
     * @param notAfter The 'not after' date as a {@code String}.
     */
    public void setNotAfter(String notAfter) {
        this.notAfter = notAfter;
    }

    /**
     * Retrieves the list of identifiers for the order. These identifiers typically represent the subjects (like domain names) for which the
     * certificate is requested.
     *
     * @return A list of {@link Identifier} objects representing the subjects of the order.
     */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    /**
     * Sets the list of identifiers for the order. This method allows updating the subjects (like domain names) for which the certificate is
     * requested.
     *
     * @param identifiers A list of {@link Identifier} objects to set as the subjects of the order.
     */
    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Retrieves the list of authorization URLs for the order. These URLs are typically used for completing the necessary authorization
     * challenges to prove control over the identifiers.
     *
     * @return A list of strings representing the authorization URLs.
     */
    public List<String> getAuthorizations() {
        return authorizations;
    }

    /**
     * Sets the list of authorization URLs for the order. This method allows specifying the URLs for completing the authorization
     * challenges.
     *
     * @param authorizations A list of strings to set as the authorization URLs.
     */
    public void setAuthorizations(List<String> authorizations) {
        this.authorizations = authorizations;
    }

    /**
     * Retrieves the URL used to finalize the order. This method returns the URL where a certificate order can be finalized, indicating that
     * all requirements for the order have been met.
     *
     * @return The finalize URL as a {@code String}.
     */
    public String getFinalize() {
        return finalize;
    }

    /**
     * Sets the URL used to finalize the order. This method allows specifying a new URL for finalizing the certificate order.
     *
     * @param finalize The finalize URL as a {@code String}.
     */
    public void setFinalize(String finalize) {
        this.finalize = finalize;
    }
}
