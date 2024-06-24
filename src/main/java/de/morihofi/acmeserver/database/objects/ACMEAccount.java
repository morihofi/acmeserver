/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.safety.TypeSafetyHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

/**
 * Represents an ACME account entity, which is used for managing ACME accounts.
 */
@Entity
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEAccount implements Serializable {
    /**
     * Logger instance for logging ACME account activities.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) account by its unique account ID.
     *
     * @param accountId The unique identifier of the ACME account to be retrieved.
     * @param serverInstance The server instance for database connection.
     * @return The ACME account matching the provided account ID, or null if not found.
     */
    public static ACMEAccount getAccount(String accountId, ServerInstance serverInstance) {
        ACMEAccount acmeAccount = null;

        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT a FROM ACMEAccount a WHERE a.accountId = :accountId", ACMEAccount.class);
            query.setParameter("accountId", accountId);
            Object result = query.getSingleResult();

            if (result != null) {
                acmeAccount = (ACMEAccount) result;
            }

            transaction.commit();
        } catch (Exception e) {
            LOG.error("Unable to get ACME Account {}", accountId, e);
        }

        return acmeAccount;
    }

    /**
     * Retrieves all ACME accounts.
     *
     * @param serverInstance The server instance for database connection.
     * @return A list of all ACME accounts.
     */
    public static List<ACMEAccount> getAllAccounts(ServerInstance serverInstance) {
        List<ACMEAccount> acmeAccounts = null;

        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("FROM ACMEAccount", ACMEAccount.class);
            acmeAccounts = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), ACMEAccount.class);

            transaction.commit();
        } catch (Exception e) {
            LOG.error("Unable to get all ACME Accounts", e);
        }

        return acmeAccounts;
    }

    /**
     * Retrieves the ACME account associated with a specific order ID.
     *
     * @param orderId The unique identifier of the ACME order for which the associated account is to be retrieved.
     * @param serverInstance The server instance for database connection.
     * @return The ACME account associated with the provided order ID, or null if not found.
     */
    public static ACMEAccount getAccountByOrderId(String orderId, ServerInstance serverInstance) {
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT o.account FROM ACMEOrder o WHERE o.orderId = :orderId", ACMEAccount.class);
            query.setParameter("orderId", orderId);
            Object result = query.getSingleResult();

            if (result != null) {
                return (ACMEAccount) result;
            }

            transaction.commit();
        } catch (Exception e) {
            LOG.error("Unable to get ACME Account for order {}", orderId, e);
        }

        return null;
    }

    /**
     * Retrieves all ACME accounts associated with a specific email.
     *
     * @param email The email address to search for associated ACME accounts.
     * @param serverInstance The server instance for database connection.
     * @return A list of ACME accounts associated with the provided email address.
     */
    public static List<ACMEAccount> getAllACMEAccountsForEmail(String email, ServerInstance serverInstance) {
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            TypedQuery<ACMEAccount> query = session.createQuery(
                    "SELECT a FROM ACMEAccount a JOIN a.emails e WHERE e = :email", ACMEAccount.class);
            query.setParameter("email", email);

            return query.getResultList();
        }
    }

    /**
     * Internal Id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique ACME Account Id
     */
    @Column(name = "accountId", unique = true)
    private String accountId;

    /**
     * Public Key of the ACME Account Client
     */
    @Column(name = "publicKeyPEM", columnDefinition = "TEXT")
    private String publicKeyPEM;

    /**
     * E-Mails associated to this ACME Account
     */
    @ElementCollection
    @CollectionTable(name = "account_emails", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "email")
    private List<String> emails;

    /**
     * Deactivated status
     */
    @Column(name = "deactivated")
    private Boolean deactivated;

    /**
     * Provisioner where this ACME account was registered in.
     */
    @Column(name = "provisioner", nullable = false)
    private String provisioner;

    /**
     * Orders for this ACME Account
     */
    @OneToMany(mappedBy = "account")
    private List<ACMEOrder> orders;

    /**
     * Get the unique identifier of the ACME account.
     *
     * @return The account ID.
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the unique identifier of the ACME account.
     *
     * @param id The account ID to set.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the account ID associated with the ACME account.
     *
     * @return The account ID.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Set the account ID associated with the ACME account.
     *
     * @param accountId The account ID to set.
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Check if the ACME account is deactivated.
     *
     * @return True if deactivated, false otherwise.
     */
    public Boolean isDeactivated() {
        return deactivated;
    }

    /**
     * Get the list of email addresses associated with the ACME account.
     *
     * @return The list of email addresses.
     */
    public List<String> getEmails() {
        return emails;
    }

    /**
     * Set the list of email addresses associated with the ACME account.
     *
     * @param emails The list of email addresses to set.
     */
    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    /**
     * Get the public key in PEM format associated with the ACME account.
     *
     * @return The public key in PEM format.
     */
    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    /**
     * Set the public key in PEM format associated with the ACME account.
     *
     * @param publicKeyPEM The public key in PEM format to set.
     */
    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    /**
     * Get the deactivated status of the ACME account.
     *
     * @return True if deactivated, false otherwise.
     */
    public Boolean getDeactivated() {
        return deactivated;
    }

    /**
     * Set the deactivated status of the ACME account.
     *
     * @param deactivated True if deactivated, false otherwise.
     */
    public void setDeactivated(Boolean deactivated) {
        this.deactivated = deactivated;
    }

    /**
     * Get the provisioner associated with this ACME identifier.
     *
     * @return The provisioner name.
     */
    public String getProvisioner() {
        return provisioner;
    }

    /**
     * Set the provisioner associated with this ACME identifier.
     *
     * @param provisioner The provisioner name to set.
     */
    public void setProvisioner(String provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Get the list of ACME orders associated with this account.
     *
     * @return The list of ACME orders.
     */
    public List<ACMEOrder> getOrders() {
        return orders;
    }

    /**
     * Set the list of ACME orders associated with this account.
     *
     * @param orders The list of ACME orders to set.
     */
    public void setOrders(List<ACMEOrder> orders) {
        this.orders = orders;
    }
}
