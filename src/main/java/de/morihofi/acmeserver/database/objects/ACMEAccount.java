package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.database.HibernateUtil;
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
     * Logger
     */
    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().getClass());

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

    @Column(name = "provisioner", nullable = false)
    private String provisioner;

    @OneToMany(mappedBy = "account")
    private List<ACMEOrder> orders;


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

    /**
     * Get the provisioner associated with this ACME identifier.
     * @return The provisioner name.
     */
    public String getProvisioner() {
        return provisioner;
    }

    /**
     * Set the provisioner associated with this ACME identifier.
     * @param provisioner The provisioner name to set.
     */
    public void setProvisioner(String provisioner) {
        this.provisioner = provisioner;
    }

    public List<ACMEOrder> getOrders() {
        return orders;
    }


    /**
     * Retrieves an ACME (Automated Certificate Management Environment) account by its unique account ID.
     *
     * @param accountId The unique identifier of the ACME account to be retrieved.
     * @return The ACME account matching the provided account ID, or null if not found.
     */
    public static ACMEAccount getAccount(String accountId) {
        ACMEAccount acmeAccount = null;

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT a FROM ACMEAccount a WHERE a.accountId = :accountId", ACMEAccount.class);
            query.setParameter("accountId", accountId);
            Object result = query.getSingleResult();

            if (result != null) {
                acmeAccount = (ACMEAccount) result;
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get ACME Account {}", accountId, e);
        }

        return acmeAccount;
    }

    public static List<ACMEAccount> getAllAccounts() {
        List<ACMEAccount> acmeAccounts = null;

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("FROM ACMEAccount", ACMEAccount.class);
            acmeAccounts = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), ACMEAccount.class);

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get all ACME Accounts", e);
        }

        return acmeAccounts;
    }

    /**
     * Retrieves the ACME (Automated Certificate Management Environment) account associated with a specific order ID.
     *
     * @param orderId The unique identifier of the ACME order for which the associated account is to be retrieved.
     * @return The ACME account associated with the provided order ID, or null if not found.
     */
    public static ACMEAccount getAccountByOrderId(String orderId) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT o.account FROM ACMEOrder o WHERE o.orderId = :orderId", ACMEAccount.class);
            query.setParameter("orderId", orderId);
            Object result = query.getSingleResult();

            if (result != null) {
                return (ACMEAccount) result;
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get ACME Account for order {}", orderId, e);
        }

        return null;
    }

    /**
     * Get all ACMEAccounts for an given email
     *
     * @param email email to get accounts from
     * @return list of ACME Accounts
     */
    public static List<ACMEAccount> getAllACMEAccountsForEmail(String email) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            TypedQuery<ACMEAccount> query = session.createQuery(
                    "SELECT a FROM ACMEAccount a JOIN a.emails e WHERE e = :email", ACMEAccount.class);
            query.setParameter("email", email);

            return query.getResultList();
        }
    }
}
