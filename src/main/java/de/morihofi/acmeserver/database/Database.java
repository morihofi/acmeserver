package de.morihofi.acmeserver.database;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifierChallenge;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.safety.TypeSafetyHelper;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class Database {


    private Database() {
    }

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(Database.class);


    /**
     * This function marks an ACME challenge as passed
     *
     * @param challengeId id of the Challenge, provided in URL
     */
    @Transactional
    public static void passChallenge(String challengeId) {
        Transaction transaction = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            transaction = session.beginTransaction();

            ACMEOrderIdentifierChallenge orderIdentifierChallenge = session.get(ACMEOrderIdentifierChallenge.class, challengeId);
            if (orderIdentifierChallenge != null) {
                orderIdentifierChallenge.setStatus(AcmeStatus.VALID);
                orderIdentifierChallenge.setVerifiedTime(Timestamp.from(Instant.now()));
                session.merge(orderIdentifierChallenge);

                log.info("ACME challenge {} was marked as passed", challengeId);


                transaction.commit();


            } else {
                log.warn("No ACME challenge found with id {}", challengeId);
            }


        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Unable to mark ACME challenge as passed", e);
        }
    }

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated challenge ID.
     *
     * @param challengeId The unique identifier of the challenge associated with the ACME identifier.
     * @return The ACME identifier matching the provided challenge ID, or null if not found.
     */
    public static ACMEOrderIdentifierChallenge getACMEIdentifierChallenge(String challengeId) {
        ACMEOrderIdentifierChallenge challenge = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            challenge = session.createQuery("FROM ACMEOrderIdentifierChallenge WHERE challengeId = :challengeId", ACMEOrderIdentifierChallenge.class)
                    .setParameter("challengeId", challengeId)
                    .setMaxResults(1)
                    .uniqueResult();

            if (challenge != null) {
                log.info("(Challenge ID: {}) Got ACME identifier of type {} with value {}",
                        challengeId,
                        challenge.getIdentifier().getType(),
                        challenge.getIdentifier().getDataValue()

                );
            } else {
                log.error("Challenge ID {} returns null for the ACMEOrderIdentifierChallenge, must be something went wrong", challengeId);
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for challenge id {}", challengeId, e);
        }
        return challenge;
    }

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated authorization ID.
     *
     * @param authorizationId The unique identifier of the authorization associated with the ACME identifier.
     * @return The ACME identifier matching the provided authorization ID, or null if not found.
     */
    public static ACMEOrderIdentifier getACMEIdentifierByAuthorizationId(String authorizationId) {
        ACMEOrderIdentifier identifier = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            identifier = session.createQuery("FROM ACMEOrderIdentifier WHERE authorizationId = :authorizationId", ACMEOrderIdentifier.class)
                    .setParameter("authorizationId", authorizationId)
                    .setMaxResults(1)
                    .getSingleResult();

            if (identifier != null) {
                log.info("(Authorization ID: {}) Got ACME identifier of type {} with value {}",
                        authorizationId,
                        identifier.getType(),
                        identifier.getDataValue()
                );
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for authorization id {}", authorizationId, e);
        }
        return identifier;
    }

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated certificate serial number.
     *
     * @param serialNumber The serial number of the certificate associated with the ACME identifier.
     * @return The ACME identifier matching the provided certificate serial number, or null if not found.
     */
    public static ACMEOrder getACMEOrderCertificateSerialNumber(BigInteger serialNumber) {
        ACMEOrder order = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            order = session.createQuery("FROM ACMEOrder WHERE certificateSerialNumber = :certificateSerialNumber", ACMEOrder.class)
                    .setParameter("certificateSerialNumber", serialNumber)
                    .setMaxResults(1)
                    .getSingleResult();

            if (order != null) {
                log.info("Got ACME certificate with serial number {} in database", serialNumber);
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME order for certificate serial number id {}", serialNumber, e);
        }
        return order;
    }

    /**
     * Retrieves a ACME ORder (Automated Certificate Management Environment) with a specific order ID.
     *
     * @param orderId The unique identifier of the ACME order for which identifiers are to be retrieved.
     * @return A list of ACME identifiers associated with the provided order ID.
     */
    public static ACMEOrder getACMEOrder(String orderId) {
        ACMEOrder order = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            order = session.createQuery("FROM ACMEOrder a WHERE a.orderId = :orderId", ACMEOrder.class)
                    .setParameter("orderId", orderId)
                    .getSingleResult();

        } catch (Exception e) {
            log.error("Unable get ACMEOrder for order id {}", orderId, e);
        }
        return order;
    }

    public static List<ACMEOrder> getAllACMEOrdersWithState(AcmeOrderState orderState) {
        List<ACMEOrder> orders = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            orders = session.createQuery("FROM ACMEOrder a WHERE a.orderState = :orderState", ACMEOrder.class)
                    .setParameter("orderState", orderState)
                    .getResultList();

        } catch (Exception e) {
            log.error("Unable get ACMEOrders for with oder State {}", orderState, e);
        }
        return orders;
    }

    /**
     * Creates a new ACME (Automated Certificate Management Environment) order and associates it with an ACME account.
     *
     * @param account        The ACME account for which the order is created.
     * @param orderId        The unique identifier for the new ACME order.
     * @param identifierList A list of ACME identifiers to be associated with the order.
     * @param certificateId
     * @throws ACMEServerInternalException If an error occurs while creating the ACME order.
     */
    @Transactional
    public static void createOrder(ACMEAccount account, String orderId, List<ACMEOrderIdentifier> identifierList, String certificateId) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            // Create order
            ACMEOrder order = new ACMEOrder();
            order.setOrderId(orderId);
            order.setAccount(account);
            order.setCreated(Timestamp.from(Instant.now()));
            order.setCertificateId(certificateId);
            session.persist(order);

            log.info("Created new order {}", orderId);

            // Create order identifiers
            for (ACMEOrderIdentifier identifier : identifierList) {
                identifier.setIdentifierId(Crypto.generateRandomId());
                identifier.setOrder(order);
                session.persist(identifier);

                log.info("Added identifier {} of type {} to order {}",
                        identifier.getDataValue(),
                        identifier.getType(),
                        orderId
                );
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to create new ACME Order with id {} for account {}", orderId, account.getAccountId(), e);
            throw new ACMEServerInternalException("Unable to create new ACME Order");
        }
    }

    /**
     * Updates the email addresses associated with an ACME (Automated Certificate Management Environment) account.
     *
     * @param account The ACME account for which email addresses are to be updated.
     * @param emails  A list of new email addresses to be associated with the account.
     * @throws ACMEInvalidContactException If an error occurs while updating the email addresses.
     */
    @Transactional
    public static void updateAccountEmail(ACMEAccount account, List<String> emails) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            // Convert emails list to JSON array string
            JSONArray emailArr = new JSONArray();
            for (String email : emails) {
                emailArr.put(email);
            }


            // Update email
            account.getEmails().clear();
            account.getEmails().addAll(emails);
            session.merge(account);

            transaction.commit();

            log.info("ACME account {} updated emails to {}", account.getAccountId(), String.join(",", emails));

        } catch (Exception e) {
            log.error("Unable to update emails for account {}", account.getAccountId(), e);
            throw new ACMEInvalidContactException("Unable to update emails");
        }
    }

    /**
     * Retrieves the PEM-encoded certificate chain of an ACME entity by its certificate ID.
     * This method fetches the issued certificate from a database using Hibernate, appends the intermediate certificate,
     * and then appends each certificate in the CA certificate chain. If the issued certificate is not found,
     * it throws an IllegalArgumentException.
     *
     * @param certificateId The authorization ID associated with the ACME entity.
     * @param provisioner   The provisioner instance used for cryptographic operations.
     * @return A string representation of the certificate chain in PEM format.
     * @throws CertificateEncodingException if an error occurs during the encoding of certificates.
     * @throws IOException                  if an I/O error occurs during certificate processing.
     * @throws KeyStoreException            if an error occurs while accessing the keystore.
     */
    public static String getCertificateChainPEMofACMEbyCertificateId(String certificateId, Provisioner provisioner) throws CertificateEncodingException, IOException, KeyStoreException {
        StringBuilder pemBuilder = new StringBuilder();

        // Get Issued certificate
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT a FROM ACMEOrder a WHERE a.certificateId = :certificateId", ACMEOrder.class);
            query.setParameter("certificateId", certificateId);
            Object result = query.getSingleResult();

            if (result instanceof ACMEOrder acmeOrder) {


                String certificatePEM = acmeOrder.getCertificatePem();
                Date certificateExpires = acmeOrder.getCertificateExpires();

                if (certificatePEM == null) {
                    throw new IllegalArgumentException("No certificate was found for authorization id \"" + certificateId + "\". Have you already submitted a CSR? There is no certificate without a CSR.");
                }

                log.info("Getting Certificate for authorization Id {} -> Expires at {}", certificateId, certificateExpires);

                pemBuilder.append(certificatePEM);
            }

            transaction.commit();
        }


        //Certificate chain
        log.info("Adding Intermediate and CA certificate");
        pemBuilder.append("\n");

        List<X509Certificate> certificateChain = TypeSafetyHelper.safeCastToClassOfType(
                Arrays.stream(
                        provisioner.getCryptoStoreManager().getKeyStore().getCertificateChain(
                                CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(
                                        provisioner.getProvisionerName()
                                )
                        )
                ).toList(),
                X509Certificate.class);

        for (X509Certificate certificate : certificateChain) {
            pemBuilder.append(PemUtil.certificateToPEM(certificate.getEncoded()));
            pemBuilder.append("\n");
        }


        log.info("Returning certificate chain of intermediate and root-ca {}", certificateChain);

        return pemBuilder.toString();
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
     * Retrieves a list of revoked certificates from the database. Revoked certificates are identified by having
     * both a revoke status code and a revoke timestamp in their associated ACME identifiers.
     *
     * @param provisionerName Provisioner to get revoked certificates for
     * @return A list of {@link RevokedCertificate} objects representing the revoked certificates.
     */
    public static List<RevokedCertificate> getRevokedCertificates(String provisionerName) {
        List<RevokedCertificate> certificates = new ArrayList<>();

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            //Certificates are revoked when they have a statusCode and a timestamp
            Query query = session.createQuery("FROM ACMEOrder a WHERE revokeStatusCode IS NOT NULL AND revokeTimestamp IS NOT NULL AND a.account.provisioner = :provisionerName", ACMEOrder.class);
            query.setParameter("provisionerName", provisionerName);
            List<ACMEOrder> result = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), ACMEOrder.class);

            if (!result.isEmpty()) {
                for (ACMEOrder revokedIdentifier : result) {
                    certificates.add(new RevokedCertificate(
                            revokedIdentifier.getCertificateSerialNumber(),
                            revokedIdentifier.getRevokeTimestamp(),
                            revokedIdentifier.getRevokeStatusCode()
                    ));
                }
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get revoked certificates", e);
        }

        return certificates;
    }

    /**
     * Revokes an ACME (Automated Certificate Management Environment) certificate associated with an ACME identifier.
     *
     * @param order  The ACME order for which the certificate is to be revoked.
     * @param reason The reason code for revoking the certificate.
     * @throws ACMEServerInternalException If an error occurs while revoking the certificate.
     */
    public static void revokeCertificate(ACMEOrder order, int reason) {
        order.setRevokeTimestamp(Timestamp.from(Instant.now()));
        order.setRevokeStatusCode(reason);

        Transaction transaction;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            transaction = session.beginTransaction();

            session.merge(order);

            transaction.commit();
            log.info("Revoked certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(), order.getAccount().getProvisioner());
        } catch (Exception e) {
            log.error("Unable to revoke certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(), order.getAccount().getProvisioner(), e);
            throw new ACMEServerInternalException("Unable to revoke certificate");
        }
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
