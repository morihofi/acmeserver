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

package de.morihofi.acmeserver.types.database.entities;


import de.morihofi.acmeserver.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.acmeserver.types.database.enums.AcmeOrderState;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
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
public class AcmeOrder implements Serializable {


    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated certificate serial number.
     *
     * @param serialNumber   The serial number of the certificate associated with the ACME identifier.
     * @param serverInstance The server instance for database connection.
     * @return The ACME identifier matching the provided certificate serial number, or null if not found.
     */
    public static AcmeOrder getACMEOrderCertificateSerialNumber(@NonNull BigInteger serialNumber, @NonNull IServerInstance serverInstance) {
        AcmeOrder order = null;
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();
            order = session.createQuery("FROM ACMEOrder WHERE certificateSerialNumber = :certificateSerialNumber", AcmeOrder.class)
                    .setParameter("certificateSerialNumber", serialNumber)
                    .setMaxResults(1)
                    .uniqueResult();

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
    public static AcmeOrder getACMEOrder(@NonNull String orderId, @NonNull IServerInstance serverInstance) {
        AcmeOrder order;
        try (Session session = serverInstance.getDatabaseSession()) {
            order = session.createQuery("FROM ACMEOrder a WHERE a.orderId = :orderId", AcmeOrder.class)
                    .setParameter("orderId", orderId)
                    .uniqueResult();
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
    public static List<AcmeOrder> getAllACMEOrdersWithState(@NonNull AcmeOrderState orderState, @NonNull IServerInstance serverInstance) {
        List<AcmeOrder> orders;
        try (Session session = serverInstance.getDatabaseSession()) {
            orders = session.createQuery("FROM ACMEOrder a WHERE a.orderState = :orderState", AcmeOrder.class)
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
     * @throws KeyStoreException if an error occurs while accessing the keystore.
     */
    public static List<X509Certificate> getCertificateChainOfACMEbyCertificateId(@NonNull String certificateId, @NonNull AcmeProvisioner provisioner, @NonNull IServerInstance serverInstance)
            throws KeyStoreException {

        // Get Issued certificate
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            Query<AcmeOrder> query = session.createQuery("SELECT a FROM ACMEOrder a WHERE a.certificateId = :certificateId", AcmeOrder.class);
            query.setParameter("certificateId", certificateId);
            Object result = query.uniqueResult();

            if (result instanceof AcmeOrder acmeOrder) {

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

            }

            transaction.commit();
        }

        // Certificate chain
        log.info("Adding Intermediate and CA certificate");

        KeyStore keyStore = serverInstance.getCryptoStoreManager().getKeyStore();
        String alias = serverInstance.getCryptoStoreManager().getKeyStoreAliasForProvisionerIntermediate(provisioner.getName());

        //FIXME: Check if we return the full chain (root ca until server cert)
        // I think we are missing the last one, but we will see until testing
        return Arrays.stream(keyStore.getCertificateChain(alias))
                .map(X509Certificate.class::cast)
                .toList();
    }

    /**
     * Retrieves a list of revoked certificates from the database. Revoked certificates are identified by having both a revoke status code
     * and a revoke timestamp in their associated ACME identifiers.
     *
     * @param provisionerName Provisioner to get revoked certificates for.
     * @param serverInstance  The server instance for database connection.
     * @return A list of {@link RevokedCertificate} objects representing the revoked certificates.
     */
    public static List<RevokedCertificate> getRevokedCertificates(String provisionerName, IServerInstance serverInstance) {
        List<RevokedCertificate> certificates = new ArrayList<>();

        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            // Certificates are revoked when they have a statusCode and a timestamp
            Query<AcmeOrder> query = session.createQuery(
                    "FROM ACMEOrder a WHERE revokeStatusCode IS NOT NULL AND revokeTimestamp IS NOT NULL AND a.account.provisioner = "
                            + ":provisionerName",
                    AcmeOrder.class);
            query.setParameter("provisionerName", provisionerName);
            List<AcmeOrder> result = query.getResultList();

            if (!result.isEmpty()) {
                for (AcmeOrder revokedIdentifier : result) {
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
    public static void revokeCertificate(AcmeOrder order, int reason, IServerInstance serverInstance) {
        order.setRevokeTimestamp(Timestamp.from(Instant.now()));
        order.setRevokeStatusCode(reason);

        Transaction transaction;
        try (Session session = serverInstance.getDatabaseSession()) {
            transaction = session.beginTransaction();

            session.merge(order);

            transaction.commit();
            log.info("Revoked certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(),
                    order.getAccount().getAcmeProvisioner());
        } catch (Exception e) {
            log.error("Unable to revoke certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(),
                    order.getAccount().getAcmeProvisioner(), e);
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
    private AcmeAccount account;

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
    private List<AcmeOrderIdentifier> orderIdentifiers;

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
