package de.morihofi.acmeserver.database;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.persistence.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.CertificateEncodingException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Database {

    public static final Logger log = LogManager.getLogger(Database.class);


    public static void createAccount(String accountId, String jwk, List<String> emails) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();
            ACMEAccount account = new ACMEAccount();
            account.setAccountId(accountId);
            account.setPublicKeyPEM(CertTools.convertToPem(SignatureCheck.convertJWKToPublicKey(new JSONObject(jwk))));
            account.setEmails(emails);
            account.setDeactivated(false);
            session.persist(account);
            transaction.commit();
            log.info("New ACME account created with account id \"" + accountId + "\"");
        } catch (Exception e) {
            log.error("Unable to create new ACME account", e);
            throw new ACMEServerInternalException(e.getMessage());
        }
    }


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
                log.error("Unable to find ACME Identifier with Order ID \"" + orderId + "\" and DNS Value \"" + dnsValue + "\"");
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
                log.info("ACME challenge with id \"" + challengeId + "\" was marked as passed");
            } else {
                log.warn("No ACME challenge found with id \"" + challengeId + "\"");
            }

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Unable to mark ACME challenge as passed", e);
        }
    }

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
                log.info("(Challenge ID: \"" + challengeId + "\") Got ACME identifier of type \"" +
                        identifier.getType() + "\" with value \"" + identifier.getDataValue() + "\"");
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for challenge id \"" + challengeId + "\"", e);
        }
        return identifier;
    }

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
                log.info("(Authorization ID: \"" + authorizationId + "\") Got ACME identifier of type \"" +
                        identifier.getType() + "\" with value \"" + identifier.getDataValue() + "\"");
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for authorization id \"" + authorizationId + "\"", e);
        }
        return identifier;
    }


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
                log.info("(Certificate Serial Number: \"" + serialNumber + "\") Got ACME identifier of type \"" +
                        identifier.getType() + "\" with value \"" + identifier.getDataValue() + "\"");
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for certificate serial number id \"" + serialNumber + "\"", e);
        }
        return identifier;
    }


    public static List<ACMEIdentifier> getACMEIdentifiersByOrderId(String orderId) {
        List<ACMEIdentifier> identifiers = new ArrayList<>();
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();
            identifiers = session.createQuery("FROM ACMEIdentifier ai WHERE ai.order.orderId = :orderId", ACMEIdentifier.class)
                    .setParameter("orderId", orderId)
                    .getResultList();

            identifiers.forEach(identifier ->
                    log.info("(Order ID: \"" + orderId + "\") Got ACME identifier of type \"" +
                            identifier.getType() + "\" with value \"" + identifier.getDataValue() + "\"")
            );

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME identifiers for order id \"" + orderId + "\"", e);
        }
        return identifiers;
    }


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

            log.info("Created new order \"" + orderId + "\"");

            // Create order identifiers
            for (ACMEIdentifier identifier : identifierList) {

                if (identifier.getAuthorizationId() == null || identifier.getAuthorizationToken() == null) {
                    throw new RuntimeException("Authorization Id or Token is null");
                }

                identifier.setProvisioner(provisioner);
                identifier.setOrder(order);
                identifier.setVerified(false);
                session.persist(identifier);

                log.info("Added identifier \"" + identifier.getDataValue() + "\" of type \"" + identifier.getType() + "\" to order \"" + orderId + "\"");
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to create new ACME Order with id \"" + orderId + "\" for account \"" + account.getAccountId() + "\"", e);
            throw new ACMEServerInternalException("Unable to create new ACME Order");
        }
    }

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

            log.info("ACME account \"" + account.getAccountId() + "\" updated emails to \"" + String.join(",", emails) + "\"");

        } catch (Exception e) {
            log.error("Unable to update emails for account \"" + account.getAccountId() + "\"", e);
            throw new ACMEInvalidContactException("Unable to update emails");
        }
    }

    /*
    public static String getCertificateChainPEMofACMEbyAuthorizationId(String authorizationId) throws CertificateEncodingException, IOException {
        StringBuilder pemBuilder = new StringBuilder();
        boolean certFound = false;

        // Get Issued certificate
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("FROM ACMEIdentifier WHERE authorizationId = :authorizationId", ACMEIdentifier.class);
            query.setParameter("authorizationId", authorizationId);
            ACMEIdentifier identifier = (ACMEIdentifier) query.getSingleResult();

            if (identifier != null) {
                certFound = true;

                String certificatePEM = identifier.getCertificatePem();
                Date certificateExpires = identifier.getCertificateExpires();

                log.info("Getting Certificate for authorization Id \"" + authorizationId + "\" -> Expires at " + certificateExpires.toString());

                pemBuilder.append(certificatePEM);
            }

            transaction.commit();

        } catch (Exception e) {
            log.error("Unable get Certificate for authorization id \"" + authorizationId + "\"", e);
        }

        if (!certFound) {
            throw new IllegalArgumentException("No certificate was found for authorization id \"" + authorizationId + "\"");
        }

        //Intermediate Certificate
        log.info("Adding Intermediate certificate");
        pemBuilder.append(CertTools.certificateToPEM(Main.intermediateCertificate.getEncoded()));
        pemBuilder.append("\n");

        //CA Certificate
        log.info("Adding CA certificate");
        try (Stream<String> stream = Files.lines(Main.caPath, StandardCharsets.UTF_8)) {
            stream.forEach(s -> pemBuilder.append(s).append("\n"));
        } catch (IOException e) {
            //handle exception
            log.error("Unable to load CA Certificate", e);
        }

        return pemBuilder.toString();
    }

 */

    public static String getCertificateChainPEMofACMEbyAuthorizationId(String authorizationId, byte[] intermediateCertificateBytes) throws CertificateEncodingException, IOException {
        StringBuilder pemBuilder = new StringBuilder();
        boolean certFound = false;

        // Get Issued certificate
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT ai FROM ACMEIdentifier ai WHERE ai.authorizationId = :authorizationId", ACMEIdentifier.class);
            query.setParameter("authorizationId", authorizationId);
            Object result = query.getSingleResult();

            if (result != null) {
                certFound = true;

                // Assuming you have a class OrderIdentifier with these fields
                ACMEIdentifier oi = (ACMEIdentifier) result;
                String certificatePEM = oi.getCertificatePem();
                Date certificateExpires = oi.getCertificateExpires();

                log.info("Getting Certificate for authorization Id \"" + authorizationId + "\" -> Expires at " + certificateExpires.toString());

                pemBuilder.append(certificatePEM);
            }

            transaction.commit();
        }

        if (!certFound) {
            throw new IllegalArgumentException("No certificate was found for authorization id \"" + authorizationId + "\"");
        }

        //Intermediate Certificate
        log.info("Adding Intermediate certificate");
        pemBuilder.append(CertTools.certificateToPEM(intermediateCertificateBytes));
        pemBuilder.append("\n");

        //CA Certificate
        log.info("Adding CA certificate");
        try (Stream<String> stream = Files.lines(Main.caCertificatePath, StandardCharsets.UTF_8)) {
            assert stream != null;
            stream.forEach(s -> pemBuilder.append(s).append("\n"));
        } catch (IOException e) {
            //handle exception
            log.error("Unable to load CA Certificate", e);
        }

        log.info("Returning certificate chain");
        return pemBuilder.toString();
    }


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
            log.error("Unable to get ACME Account \"" + accountId + "\"", e);
        }

        return acmeAccount;
    }

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
            log.error("Unable to get ACME Account for order \"" + orderId + "\"", e);
        }

        return null;
    }

    public static List<RevokedCertificate> getRevokedCertificates() {
        List<RevokedCertificate> certificates = new ArrayList<>();

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            Transaction transaction = session.beginTransaction();

            //Certificates are revoked when they have a statusCode and a timestamp
            Query query = session.createQuery("FROM ACMEIdentifier WHERE revokeStatusCode IS NOT NULL AND revokeTimestamp IS NOT NULL", ACMEIdentifier.class);
            List<ACMEIdentifier> result = query.getResultList();

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


    public static void revokeCertificate(ACMEIdentifier identifier, int reason) {
        identifier.setRevokeTimestamp(Timestamp.from(Instant.now()));
        identifier.setRevokeStatusCode(reason);

        Transaction transaction;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            transaction = session.beginTransaction();

            session.merge(identifier);

            transaction.commit();
            log.info("Revoked certificate with serial number " + identifier.getCertificateSerialNumber() + " (Provisioner " + identifier.getProvisioner() + ")");
        } catch (Exception e) {
            log.error("Unable to revoke certificate with serial number " + identifier.getCertificateSerialNumber() + " (Provisioner " + identifier.getProvisioner() + ")", e);
            throw new ACMEServerInternalException("Unable to revoke certificate");
        }
    }
}
