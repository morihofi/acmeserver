package de.morihofi.acmeserver.database.objects;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.List;

/**
 * Represents an ACME account entity, which is used for managing ACME accounts.
 */
@Entity
@Table(name = "accounts")
public class ACMEAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accountId", unique = true)
    private String accountId;

    @Column(name = "publicKeyPEM", columnDefinition = "TEXT")
    private String publicKeyPEM;


    @ElementCollection
    @CollectionTable(name = "account_emails", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "email")
    private List<String> emails;

    @Column(name = "deactivated")
    private Boolean deactivated;

    /**
     * Get the unique identifier of the ACME account.
     * @return The account ID.
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the unique identifier of the ACME account.
     * @param id The account ID to set.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the account ID associated with the ACME account.
     * @return The account ID.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Set the account ID associated with the ACME account.
     * @param accountId The account ID to set.
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Check if the ACME account is deactivated.
     * @return True if deactivated, false otherwise.
     */
    public Boolean isDeactivated() {
        return deactivated;
    }

    /**
     * Set the deactivated status of the ACME account.
     * @param deactivated True if deactivated, false otherwise.
     */
    public void setDeactivated(Boolean deactivated) {
        this.deactivated = deactivated;
    }

    /**
     * Get the list of email addresses associated with the ACME account.
     * @return The list of email addresses.
     */
    public List<String> getEmails() {
        return emails;
    }

    /**
     * Set the list of email addresses associated with the ACME account.
     * @param emails The list of email addresses to set.
     */
    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    /**
     * Get the public key in PEM format associated with the ACME account.
     * @return The public key in PEM format.
     */
    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    /**
     * Set the public key in PEM format associated with the ACME account.
     * @param publicKeyPEM The public key in PEM format to set.
     */
    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    /**
     * Get the deactivated status of the ACME account.
     * @return True if deactivated, false otherwise.
     */
    public Boolean getDeactivated() {
        return deactivated;
    }
}
