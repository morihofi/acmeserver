package de.morihofi.acmeserver.database;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.safety.TypeSafetyHelper;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class Database {

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(Database.class);

    /**
     * Creates a new ACME (Automated Certificate Management Environment) account in the database with the provided parameters.
     *
     * @param accountId The unique identifier for the ACME account.
     * @param jwk       The JSON Web Key (JWK) representing the account's public key.
     * @param emails    A list of email addresses associated with the account.
     * @throws ACMEServerInternalException If an error occurs while creating the ACME account.
     */
    public static void createAccount(String accountId, String jwk, List<String> emails) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();
            ACMEAccount account = new ACMEAccount();
            account.setAccountId(accountId);
            account.setPublicKeyPEM(PemUtil.convertToPem(SignatureCheck.convertJWKToPublicKey(new JSONObject(jwk))));
            account.setEmails(emails);
            account.setDeactivated(false);
            session.persist(account);
            transaction.commit();
            log.info("New ACME account created with account id {}", accountId);
        } catch (Exception e) {
            log.error("Unable to create new ACME account", e);
            throw new ACMEServerInternalException(e.getMessage());
        }
    }

    /**
     * Stores a certificate and related information in the database for a specific ACME order and DNS value.
     *
     * @param orderId        The unique identifier of the ACME order associated with the certificate.
     * @param dnsValue       The DNS value for which the certificate is issued.
     * @param csr            The Certificate Signing Request (CSR) used to request the certificate.
     * @param pemCertificate The PEM-encoded certificate to be stored.
     * @param issueDate      The timestamp when the certificate was issued.
     * @param expireDate     The timestamp when the certificate will expire.
     * @param serialNumber   The serial number of the certificate.
     */
    public static void storeCertificateInDatabase(String orderId, String dnsValue, String csr, String pemCertificate, Timestamp issueDate, Timestamp expireDate, BigInteger serialNumber) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();

            // Using ACMEIdentifier class
            Query query = session.createQuery("FROM ACMEIdentifier a WHERE a.order.orderId = :orderId AND a.dataValue = :dnsValue", ACMEIdentifier.class);
            query.setParameter("orderId", orderId);
            query.setParameter("dnsValue", dnsValue);

            ACMEIdentifier acmeIdentifier = (ACMEIdentifier) query.getSingleResult();

            if (acmeIdentifier != null) {
                acmeIdentifier.setCertificateCSR(csr);
                acmeIdentifier.setCertificateIssued(issueDate);
                acmeIdentifier.setCertificateExpires(expireDate);
                acmeIdentifier.setCertificatePem(pemCertificate);
                acmeIdentifier.setCertificateSerialNumber(serialNumber);

                session.merge(acmeIdentifier);
                transaction.commit();

                log.info("Stored certificate successful");
            } else {
                log.error("Unable to find ACME Identifier with Order ID {} and DNS Value {}", orderId, dnsValue);
            }
        } catch (Exception e) {
            log.error("Unable to store certificate", e);
        }
    }


    /**
     * This function marks an ACME challenge as passed
     *
     * @param challengeId id of the Challenge, provided in URL
     */
    @Transactional
    public static void passChallenge(String challengeId) {
        Transaction transaction = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            transaction = session.beginTransaction();

            ACMEOrderIdentifier orderIdentifier = session.get(ACMEOrderIdentifier.class, challengeId);
            if (orderIdentifier != null) {
                orderIdentifier.setVerified(true);
                orderIdentifier.setVerifiedTime(Timestamp.from(Instant.now()));
                session.merge(orderIdentifier);
                transaction.commit();
                log.info("ACME challenge {} was marked as passed", challengeId);
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
    public static ACMEIdentifier getACMEIdentifierByChallengeId(String challengeId) {
        ACMEIdentifier identifier = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();
            identifier = session.createQuery("FROM ACMEIdentifier WHERE challengeId = :challengeId", ACMEIdentifier.class)
                    .setParameter("challengeId", challengeId)
                    .setMaxResults(1)
                    .uniqueResult();

            if (identifier != null) {
                log.info("(Challenge ID: {}) Got ACME identifier of type {} with value {}",
                        challengeId,
                        identifier.getType(),
                        identifier.getDataValue()

                );
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for challenge id {}", challengeId, e);
        }
        return identifier;
    }

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated authorization ID.
     *
     * @param authorizationId The unique identifier of the authorization associated with the ACME identifier.
     * @return The ACME identifier matching the provided authorization ID, or null if not found.
     */
    public static ACMEIdentifier getACMEIdentifierByAuthorizationId(String authorizationId) {
        ACMEIdentifier identifier = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();
            identifier = session.createQuery("FROM ACMEIdentifier WHERE authorizationId = :authorizationId", ACMEIdentifier.class)
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
    public static ACMEIdentifier getACMEIdentifierCertificateSerialNumber(BigInteger serialNumber) {
        ACMEIdentifier identifier = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();
            identifier = session.createQuery("FROM ACMEIdentifier WHERE certificateSerialNumber = :certificateSerialNumber", ACMEIdentifier.class)
                    .setParameter("certificateSerialNumber", serialNumber)
                    .setMaxResults(1)
                    .getSingleResult();

            if (identifier != null) {
                log.info("(Certificate Serial Number: {}) Got ACME identifier of type {} with value {}",
                        serialNumber,
                        identifier.getType(),
                        identifier.getDataValue()
                );
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for certificate serial number id {}", serialNumber, e);
        }
        return identifier;
    }

    /**
     * Retrieves a list of ACME (Automated Certificate Management Environment) identifiers associated with a specific order ID.
     *
     * @param orderId The unique identifier of the ACME order for which identifiers are to be retrieved.
     * @return A list of ACME identifiers associated with the provided order ID.
     */
    public static List<ACMEIdentifier> getACMEIdentifiersByOrderId(String orderId) {
        List<ACMEIdentifier> identifiers = new ArrayList<>();
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();
            identifiers = session.createQuery("FROM ACMEIdentifier ai WHERE ai.order.orderId = :orderId", ACMEIdentifier.class)
                    .setParameter("orderId", orderId)
                    .getResultList();

            identifiers.forEach(identifier ->
                    log.info("(Order ID: {}) Got ACME identifier of type {} with value {}",
                            orderId,
                            identifier.getType(),
                            identifier.getDataValue()
                    )
            );

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for order id {}", orderId, e);
        }
        return identifiers;
    }

    /**
     * Creates a new ACME (Automated Certificate Management Environment) order and associates it with an ACME account.
     *
     * @param account        The ACME account for which the order is created.
     * @param orderId        The unique identifier for the new ACME order.
     * @param identifierList A list of ACME identifiers to be associated with the order.
     * @param provisioner    The provisioner responsible for creating the order.
     * @throws ACMEServerInternalException If an error occurs while creating the ACME order.
     */
    @Transactional
    public static void createOrder(ACMEAccount account, String orderId, List<ACMEIdentifier> identifierList, String provisioner) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();

            // Create order
            ACMEOrder order = new ACMEOrder();
            order.setOrderId(orderId);
            order.setAccount(account);
            order.setCreated(Timestamp.from(Instant.now()));
            session.persist(order);

            log.info("Created new order {}", orderId);

            // Create order identifiers
            for (ACMEIdentifier identifier : identifierList) {

                if (identifier.getAuthorizationId() == null || identifier.getAuthorizationToken() == null) {
                    throw new RuntimeException("Authorization Id or Token is null");
                }

                identifier.setProvisioner(provisioner);
                identifier.setOrder(order);
                identifier.setVerified(false);
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
            assert session != null;
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
     * Retrieves the PEM-encoded certificate chain of an ACME entity by its authorization ID.
     * This method fetches the issued certificate from a database using Hibernate, appends the intermediate certificate,
     * and then appends each certificate in the CA certificate chain. If the issued certificate is not found,
     * it throws an IllegalArgumentException.
     *
     * @param authorizationId              The authorization ID associated with the ACME entity.
     * @param intermediateCertificateBytes The byte array of the intermediate certificate.
     * @param provisioner                  The provisioner instance used for cryptographic operations.
     * @return A string representation of the certificate chain in PEM format.
     * @throws CertificateEncodingException if an error occurs during the encoding of certificates.
     * @throws IOException                  if an I/O error occurs during certificate processing.
     * @throws KeyStoreException            if an error occurs while accessing the keystore.
     */
    public static String getCertificateChainPEMofACMEbyAuthorizationId(String authorizationId, byte[] intermediateCertificateBytes, Provisioner provisioner) throws CertificateEncodingException, IOException, KeyStoreException {
        StringBuilder pemBuilder = new StringBuilder();
        boolean certFound = false;

        // Get Issued certificate
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT ai FROM ACMEIdentifier ai WHERE ai.authorizationId = :authorizationId", ACMEIdentifier.class);
            query.setParameter("authorizationId", authorizationId);
            Object result = query.getSingleResult();

            if (result instanceof ACMEIdentifier acmeIdentifier) {
                certFound = true;

                String certificatePEM = acmeIdentifier.getCertificatePem();
                Date certificateExpires = acmeIdentifier.getCertificateExpires();

                log.info("Getting Certificate for authorization Id {} -> Expires at {}", authorizationId, certificateExpires);

                pemBuilder.append(certificatePEM);
            }

            transaction.commit();
        }

        if (!certFound) {
            throw new IllegalArgumentException("No certificate was found for authorization id \"" + authorizationId + "\"");
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


        log.info("Returning certificate chain {}", certificateChain);
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
            assert session != null;
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

    /**
     * Retrieves the ACME (Automated Certificate Management Environment) account associated with a specific order ID.
     *
     * @param orderId The unique identifier of the ACME order for which the associated account is to be retrieved.
     * @return The ACME account associated with the provided order ID, or null if not found.
     */
    public static ACMEAccount getAccountByOrderId(String orderId) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
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
            assert session != null;
            Transaction transaction = session.beginTransaction();

            //Certificates are revoked when they have a statusCode and a timestamp
            Query query = session.createQuery("FROM ACMEIdentifier WHERE revokeStatusCode IS NOT NULL AND revokeTimestamp IS NOT NULL AND provisioner = :provisionerName", ACMEIdentifier.class);
            query.setParameter("provisionerName", provisionerName);
            List<ACMEIdentifier> result = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), ACMEIdentifier.class);

            if (!result.isEmpty()) {
                for (ACMEIdentifier revokedIdentifier : result) {
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
     * @param identifier The ACME identifier for which the certificate is to be revoked.
     * @param reason     The reason code for revoking the certificate.
     * @throws ACMEServerInternalException If an error occurs while revoking the certificate.
     */
    public static void revokeCertificate(ACMEIdentifier identifier, int reason) {
        identifier.setRevokeTimestamp(Timestamp.from(Instant.now()));
        identifier.setRevokeStatusCode(reason);

        Transaction transaction;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            transaction = session.beginTransaction();

            session.merge(identifier);

            transaction.commit();
            log.info("Revoked certificate with serial number {} (Provisioner {})", identifier.getCertificateSerialNumber(), identifier.getProvisioner());
        } catch (Exception e) {
            log.error("Unable to revoke certificate with serial number {} (Provisioner {})", identifier.getCertificateSerialNumber(), identifier.getProvisioner(), e);
            throw new ACMEServerInternalException("Unable to revoke certificate");
        }
    }
}
