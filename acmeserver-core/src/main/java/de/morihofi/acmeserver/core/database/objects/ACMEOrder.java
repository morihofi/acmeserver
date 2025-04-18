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

package de.morihofi.acmeserver.core.database.objects;

import de.morihofi.acmeserver.core.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.core.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.core.database.AcmeOrderState;
import de.morihofi.acmeserver.core.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import de.morihofi.acmeserver.core.tools.certificate.PemUtil;
import de.morihofi.acmeserver.core.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.core.tools.safety.TypeSafetyHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Represents an ACME order entity used for managing certificate orders.
 */
@Entity
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrder implements Serializable {


    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated certificate serial number.
     *
     * @param serialNumber   The serial number of the certificate associated with the ACME identifier.
     * @param serverInstance The server instance for database connection.
     * @return The ACME identifier matching the provided certificate serial number, or null if not found.
     */
    public static ACMEOrder getACMEOrderCertificateSerialNumber(BigInteger serialNumber, ServerInstance serverInstance) {
        ACMEOrder order = null;
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
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
     * Retrieves an ACME order by its unique order ID.
     *
     * @param orderId        The unique identifier of the ACME order.
     * @param serverInstance The server instance for database connection.
     * @return The ACME order matching the provided order ID, or null if not found.
     */
    public static ACMEOrder getACMEOrder(String orderId, ServerInstance serverInstance) {
        ACMEOrder order;
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            order = session.createQuery("FROM ACMEOrder a WHERE a.orderId = :orderId", ACMEOrder.class)
                    .setParameter("orderId", orderId)
                    .getSingleResult();
        }
        return order;
    }

    /**
     * Retrieves all ACME orders with a specific state.
     *
     * @param orderState     The state of the ACME orders to retrieve.
     * @param serverInstance The server instance for database connection.
     * @return A list of ACME orders with the specified state.
     */
    public static List<ACMEOrder> getAllACMEOrdersWithState(AcmeOrderState orderState, ServerInstance serverInstance) {
        List<ACMEOrder> orders;
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            orders = session.createQuery("FROM ACMEOrder a WHERE a.orderState = :orderState", ACMEOrder.class)
                    .setParameter("orderState", orderState)
                    .getResultList();
        }
        return orders;
    }

    /**
     * Retrieves the PEM-encoded certificate chain of an ACME entity by its certificate ID. This method fetches the issued certificate from
     * a database using Hibernate, appends the intermediate certificate, and then appends each certificate in the CA certificate chain. If
     * the issued certificate is not found, it throws an IllegalArgumentException.
     *
     * @param certificateId  The authorization ID associated with the ACME entity.
     * @param provisioner    The provisioner instance used for cryptographic operations.
     * @param serverInstance The server instance for database connection.
     * @return A string representation of the certificate chain in PEM format.
     * @throws CertificateEncodingException if an error occurs during the encoding of certificates.
     * @throws IOException                  if an I/O error occurs during certificate processing.
     * @throws KeyStoreException            if an error occurs while accessing the keystore.
     */
    public static String getCertificateChainPEMofACMEbyCertificateId(String certificateId, Provisioner provisioner, ServerInstance serverInstance)
            throws CertificateEncodingException, IOException, KeyStoreException {
        StringBuilder pemBuilder = new StringBuilder();

        // Get Issued certificate
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT a FROM ACMEOrder a WHERE a.certificateId = :certificateId", ACMEOrder.class);
            query.setParameter("certificateId", certificateId);
            Object result = query.getSingleResult();

            if (result instanceof ACMEOrder acmeOrder) {

                String certificatePEM = acmeOrder.getCertificatePem();
                Date certificateExpires = acmeOrder.getCertificateExpires();

                if (certificatePEM == null && acmeOrder.getCertificateCSR() == null) {
                    throw new ACMEServerInternalException(
                            "No CSR was found in database. Have you already submitted a CSR? You cannot get a certificate without "
                                    + "submitting a CSR.");
                } else if (certificatePEM == null) {
                    return null; // Returning null if it looks like that the server is generating in background
                }

                log.info("Getting Certificate for authorization Id {} -> Expires at {}", certificateId, certificateExpires);

                pemBuilder.append(certificatePEM);
            }

            transaction.commit();
        }

        // Certificate chain
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
     * Retrieves a list of revoked certificates from the database. Revoked certificates are identified by having both a revoke status code
     * and a revoke timestamp in their associated ACME identifiers.
     *
     * @param provisionerName Provisioner to get revoked certificates for.
     * @param serverInstance  The server instance for database connection.
     * @return A list of {@link RevokedCertificate} objects representing the revoked certificates.
     */
    public static List<RevokedCertificate> getRevokedCertificates(String provisionerName, ServerInstance serverInstance) {
        List<RevokedCertificate> certificates = new ArrayList<>();

        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            // Certificates are revoked when they have a statusCode and a timestamp
            Query query = session.createQuery(
                    "FROM ACMEOrder a WHERE revokeStatusCode IS NOT NULL AND revokeTimestamp IS NOT NULL AND a.account.provisioner = "
                            + ":provisionerName",
                    ACMEOrder.class);
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
     * @param order          The ACME order for which the certificate is to be revoked.
     * @param reason         The reason code for revoking the certificate.
     * @param serverInstance The server instance for database connection.
     * @throws ACMEServerInternalException If an error occurs while revoking the certificate.
     */
    public static void revokeCertificate(ACMEOrder order, int reason, ServerInstance serverInstance) {
        order.setRevokeTimestamp(Timestamp.from(Instant.now()));
        order.setRevokeStatusCode(reason);

        Transaction transaction;
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            transaction = session.beginTransaction();

            session.merge(order);

            transaction.commit();
            log.info("Revoked certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(),
                    order.getAccount().getProvisioner());
        } catch (Exception e) {
            log.error("Unable to revoke certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(),
                    order.getAccount().getProvisioner(), e);
            throw new ACMEServerInternalException("Unable to revoke certificate");
        }
    }

    /**
     * Internal ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ACME Order ID
     */
    @Column(name = "orderId", unique = true)
    private String orderId;

    /**
     * ACME Account ID where the order belongs to
     */
    @ManyToOne
    @JoinColumn(name = "accountId", referencedColumnName = "accountId")
    private ACMEAccount account;

    /**
     * Creation of the order
     */
    @Column(name = "created")
    private Timestamp created;

    /**
     * Expiring of the order
     */
    @Column(name = "expires")
    private Timestamp expires;

    /**
     * Not before for the generated certificate
     */
    @Column(name = "notBefore")
    private Timestamp notBefore;
    /**
     * Not after for the generated certificate
     */
    @Column(name = "notAfter")
    private Timestamp notAfter;
    /**
     * Order Identifiers (Domains, IPs) of this Order
     */
    @OneToMany(mappedBy = "order")
    private List<ACMEOrderIdentifier> orderIdentifiers;

    /**
     * Order state, used for background certificate generation
     */
    @Column(name = "orderState", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcmeOrderState orderState = AcmeOrderState.IDLE;

    /**
     * Certificate Id for downloading the certificate after generation
     */
    @Column(name = "certificateId", columnDefinition = "TEXT", unique = true)
    private String certificateId;
    /**
     * Certificate signing request containing the public key for signing and domains/ips
     */
    @Column(name = "certificateCSR", columnDefinition = "TEXT")
    private String certificateCSR;
    /**
     * Timestamp when the certificate was issued
     */
    @Column(name = "certificateIssued")
    private Timestamp certificateIssued;
    /**
     * Time when the certificate will expire
     */
    @Column(name = "certificateExpires")
    private Timestamp certificateExpires;
    /**
     * The certificate without the full chain
     */
    @Column(name = "certificatePem", columnDefinition = "TEXT")
    private String certificatePem;
    /**
     * Serial number of the certificate
     */
    @Column(name = "certificateSerialNumber", precision = 50, scale = 0)
    private BigInteger certificateSerialNumber;
    /**
     * Revokation status of the certificate. Defaults to null if not revoked
     */
    @Column(name = "revokeStatusCode", nullable = true)
    private Integer revokeStatusCode;
    /**
     * Revokation timestamp of the certificate. Defaults to null if not revoked
     */
    @Column(name = "revokeTimestamp", nullable = true)
    private Timestamp revokeTimestamp;

}
