package de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents the response structure for an ACME account, used primarily in the {@link NewAccountEndpoint}.
 * This class encapsulates the account details such as its status, contact information, and a link to the order endpoint.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class AccountResponse {

    /**
     * The status of the ACME account.
     * This status indicates the current state or condition of the account, such as 'active' or 'disabled'.
     */
    private String status;

    /**
     * The list of email contacts associated with the ACME account.
     * Each contact is specified with a 'mailto:' prefix.
     */
    private List<String> contact;

    /**
     * The URL link to the Order Endpoint.
     * This link is used to access the endpoint for order-related operations in the ACME protocol.
     */
    private String orders;

    /**
     * Retrieves the status of the ACME account.
     *
     * @return The status of the account as a {@code String}.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the ACME account.
     *
     * @param status The new status of the account as a {@code String}.
     */
    public void setStatus(String status) {
        this.status = status;
    }

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
     * Retrieves the URL link to the Order Endpoint.
     * This link is used for accessing order-related operations in the ACME protocol.
     *
     * @return The URL link to the Order Endpoint as a {@code String}.
     */
    public String getOrders() {
        return orders;
    }

    /**
     * Sets the URL link to the Order Endpoint.
     * This method allows updating the link for order-related operations in the ACME protocol.
     *
     * @param orders The new URL link to the Order Endpoint as a {@code String}.
     */
    public void setOrders(String orders) {
        this.orders = orders;
    }
}
