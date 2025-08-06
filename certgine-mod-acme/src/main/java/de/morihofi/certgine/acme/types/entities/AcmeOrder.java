/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.entities;


import de.morihofi.certgine.acme.types.entities.enums.AcmeOrderState;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.types.exception.exceptions.ACMEServerInternalException;
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
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an ACME order entity used for managing certificate orders.
 */
@Entity
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class AcmeOrder implements Serializable {


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
    @Column(name = "created", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant created;
    /**
     * Expiring of the order
     */
    @Column(name = "expires", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant expires;
    /**
     * Not before for the generated certificate
     */
    @Column(name = "notBefore", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant notBefore;
    /**
     * Not after for the generated certificate
     */
    @Column(name = "notAfter", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant notAfter;
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
    @Column(name = "certificateIssued", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant certificateIssued;
    /**
     * Time when the certificate will expire
     */
    @Column(name = "certificateExpires", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant certificateExpires;
    /**
     * The certificate without the full chain
     */
    @Column(name = "certificatePem", columnDefinition = "TEXT")
    private String certificatePem;
    /**
     * Serial number of the certificate
     */
    @Column(name = "certificateSerialNumber", precision = 50)
    private BigInteger certificateSerialNumber;
    /**
     * Revokation status of the certificate. Defaults to null if not revoked
     */
    @Column(name = "revokeStatusCode")
    private Integer revokeStatusCode;
    /**
     * Revokation timestamp of the certificate. Defaults to null if not revoked
     */
    @Column(name = "revokeTimestamp", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant revokeTimestamp;

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated certificate serial number.
     *
     * @param serialNumber   The serial number of the certificate associated with the ACME identifier.
     * @param serverInstance The server instance for database connection.
     * @return The ACME identifier matching the provided certificate serial number, or null if not found.
     */
    public static AcmeOrder getAcmeOrderCertificateSerialNumber(@NonNull BigInteger serialNumber, @NonNull IServerInstance serverInstance) {
        AcmeOrder order = null;
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();
            order = session.createQuery("FROM AcmeOrder WHERE certificateSerialNumber = :certificateSerialNumber", AcmeOrder.class)
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
    public static AcmeOrder getAcmeOrder(@NonNull String orderId, @NonNull IServerInstance serverInstance) {
        AcmeOrder order;
        try (Session session = serverInstance.getDatabaseSession()) {
            order = session.createQuery("FROM AcmeOrder a WHERE a.orderId = :orderId", AcmeOrder.class)
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
    public static List<AcmeOrder> getAllAcmeOrdersWithState(@NonNull AcmeOrderState orderState, @NonNull IServerInstance serverInstance) {
        List<AcmeOrder> orders;
        try (Session session = serverInstance.getDatabaseSession()) {
            orders = session.createQuery("FROM AcmeOrder a WHERE a.orderState = :orderState", AcmeOrder.class)
                    .setParameter("orderState", orderState)
                    .getResultList();
        }
        return orders;
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
                    "FROM AcmeOrder a WHERE revokeStatusCode IS NOT NULL AND revokeTimestamp IS NOT NULL "
                            + "AND a.account.acmeProvisioner.name = :provisionerName",
                    AcmeOrder.class);
            query.setParameter("provisionerName", provisionerName);
            List<AcmeOrder> result = query.getResultList();

            if (!result.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
                for (AcmeOrder revokedIdentifier : result) {
                    certificates.add(new RevokedCertificate(
                            revokedIdentifier.getCertificateSerialNumber(),
                            revokedIdentifier.getRevokeTimestamp(),
                            revokedIdentifier.getRevokeStatusCode()
                    ));
                    log.debug("Loaded revoked certificate {} at {}", revokedIdentifier.getCertificateSerialNumber(),
                            formatter.format(revokedIdentifier.getRevokeTimestamp()));
                }
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get revoked certificates", e);
        }

        return certificates;
    }

    /**
     * Retrieves the revocation information for a specific certificate serial
     * number. If the certificate is revoked, a {@link RevokedCertificate}
     * instance containing the revocation date and reason is returned. Otherwise
     * {@code null} is returned.
     *
     * @param serialNumber    Serial number of the certificate.
     * @param provisionerName Name of the provisioner issuing the certificate.
     * @param serverInstance  Server instance for database access.
     * @return Revocation data or {@code null} if the certificate is not revoked
     * or could not be found.
     */
    public static RevokedCertificate getRevokedCertificate(BigInteger serialNumber,
                                                           String provisionerName,
                                                           IServerInstance serverInstance) {
        RevokedCertificate rc = null;

        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            Query<AcmeOrder> query = session.createQuery(
                    "FROM AcmeOrder a WHERE a.certificateSerialNumber = :serialNumber "
                            + "AND a.account.acmeProvisioner.name = :provisionerName",
                    AcmeOrder.class);
            query.setParameter("serialNumber", serialNumber);
            query.setParameter("provisionerName", provisionerName);
            AcmeOrder result = query.setMaxResults(1).uniqueResult();

            if (result != null
                    && result.getRevokeStatusCode() != null
                    && result.getRevokeTimestamp() != null) {
                rc = new RevokedCertificate(
                        result.getCertificateSerialNumber(),
                        result.getRevokeTimestamp(),
                        result.getRevokeStatusCode());
                DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
                log.debug("Loaded revoked certificate {} at {}", serialNumber,
                        formatter.format(result.getRevokeTimestamp()));
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get revoked certificate for serial number {}", serialNumber, e);
        }

        return rc;
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
        order.setRevokeTimestamp(Instant.now());
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

}
