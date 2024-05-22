package de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents a response from the ACME server for an order request.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrderResponse {
    private String status;
    private String expires;
    private String issued;
    private String finalize;
    private String certificate;
    private List<Identifier> identifiers;
    private List<String> authorizations;

    /**
     * Get the status of the ACME order.
     *
     * @return The status string.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the status of the ACME order.
     *
     * @param status The status string to set.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Get the expiration date of the ACME order.
     *
     * @return The expiration date string.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Set the expiration date of the ACME order.
     *
     * @param expires The expiration date string to set.
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }

    /**
     * Get the finalize URL for the ACME order.
     *
     * @return The finalize URL string.
     */
    public String getFinalize() {
        return finalize;
    }

    /**
     * Set the finalize URL for the ACME order.
     *
     * @param finalize The finalize URL string to set.
     */
    public void setFinalize(String finalize) {
        this.finalize = finalize;
    }

    /**
     * Get the certificate URL for the ACME order.
     *
     * @return The certificate URL string.
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * Set the certificate URL for the ACME order.
     *
     * @param certificate The certificate URL string to set.
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * Get the list of identifiers associated with the ACME order.
     *
     * @return The list of Identifier objects.
     */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    /**
     * Set the list of identifiers associated with the ACME order.
     *
     * @param identifiers The list of Identifier objects to set.
     */
    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Get the list of authorizations associated with the ACME order.
     *
     * @return The list of authorization URL strings.
     */
    public List<String> getAuthorizations() {
        return authorizations;
    }

    /**
     * Set the list of authorizations associated with the ACME order.
     *
     * @param authorizations The list of authorization URL strings to set.
     */
    public void setAuthorizations(List<String> authorizations) {
        this.authorizations = authorizations;
    }

    /**
     * Get the issuance date of the ACME order's certificate.
     *
     * @return The issuance date string.
     */
    public String getIssued() {
        return issued;
    }

    /**
     * Set the issuance date of the ACME order's certificate.
     *
     * @param issued The issuance date string to set.
     */
    public void setIssued(String issued) {
        this.issued = issued;
    }
}
