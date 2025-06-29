/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.database.entities.acme;

import de.morihofi.certgine.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Represents an ACME account entity, which is used for managing ACME accounts.
 */
@Entity
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class AcmeAccount implements Serializable {

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) account by its unique account ID.
     *
     * @param accountId The unique identifier of the ACME account to be retrieved.
     * @param serverInstance The server instance for database connection.
     * @return The ACME account matching the provided account ID, or null if not found.
     */
    public static AcmeAccount getAccount(@NonNull String accountId, @NonNull IServerInstance serverInstance) {
        AcmeAccount acmeAccount = null;

        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            Query<AcmeAccount> query = session.createQuery("SELECT a FROM AcmeAccount a WHERE a.accountId = :accountId", AcmeAccount.class);
            query.setParameter("accountId", accountId);
            AcmeAccount result = query.uniqueResult();

            if (result != null) {
                acmeAccount = result;
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get ACME Account {}", accountId, e);
        }

        return acmeAccount;
    }

    /**
     * Retrieves all ACME accounts.
     *
     * @param serverInstance The server instance for database connection.
     * @return A list of all ACME accounts.
     */
    @NonNull
    public static List<AcmeAccount> getAllAccounts(@NonNull IServerInstance serverInstance) {
        List<AcmeAccount> acmeAccounts = null;

        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            Query<AcmeAccount> query = session.createQuery("FROM ACMEAccount", AcmeAccount.class);
            acmeAccounts = query.getResultList();

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get all ACME Accounts", e);
        }

        return acmeAccounts != null ? acmeAccounts : Collections.emptyList();
    }

    /**
     * Retrieves the ACME account associated with a specific order ID.
     *
     * @param orderId The unique identifier of the ACME order for which the associated account is to be retrieved.
     * @param serverInstance The server instance for database connection.
     * @return The ACME account associated with the provided order ID, or null if not found.
     */
    public static AcmeAccount getAccountByOrderId(@NonNull String orderId, @NonNull IServerInstance serverInstance) {
        try (Session session = serverInstance.getDatabaseSession()) {
            Query<AcmeAccount> query = session.createQuery("SELECT o.account FROM AcmeOrder o WHERE o.orderId = :orderId", AcmeAccount.class);
            query.setParameter("orderId", orderId);

            return query.uniqueResult();

        } catch (Exception e) {
            log.error("Unable to get ACME Account for order {}", orderId, e);
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
    @NonNull
    public static List<AcmeAccount> getAllACMEAccountsForEmail(@NonNull String email, @NonNull IServerInstance serverInstance) {
        try (Session session = serverInstance.getDatabaseSession()) {
            Query<AcmeAccount> query = session.createQuery(
                    "SELECT a FROM AcmeAccount a JOIN a.emails e WHERE e = :email", AcmeAccount.class);
            query.setParameter("email", email);

            return query.getResultList();
        }
    }

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
    private boolean deactivated;

    /**
     * Provisioner where this ACME account was registered in.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "provisioner_id", nullable = false)
    private AcmeProvisioner acmeProvisioner;

    /**
     * External Account Binding used during account creation.
     */
    @ManyToOne
    @JoinColumn(name = "eab_id")
    private AcmeExternalAccountBinding externalAccountBinding;

    /**
     * Orders for this ACME Account
     */
    @OneToMany(mappedBy = "account")
    private List<AcmeOrder> orders;
}
