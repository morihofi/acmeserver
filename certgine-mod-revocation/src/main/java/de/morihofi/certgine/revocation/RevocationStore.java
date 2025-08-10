/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation;

import de.morihofi.certgine.revocation.entities.RevokedCertificateEntity;
import de.morihofi.certgine.types.cryptography.revoke.RevocationReason;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

/**
 * Central store for revoked certificates.
 * <p>
 * This class persists and retrieves revocation information using the
 * {@link IServerInstance}'s database connection. Certificates can only be
 * added to the store; there is no removal operation.
 */
@Slf4j
@UtilityClass
public class RevocationStore {

    /**
     * Returns all revoked certificates known to the system.
     *
     * @param serverInstance current server instance
     * @return list of revoked certificates
     */
    public static @NonNull List<RevokedCertificate> getRevokedCertificates(@NonNull IServerInstance serverInstance) {
        try (Session s = serverInstance.getDatabaseSession()) {
            return s.createQuery("FROM RevokedCertificateEntity", RevokedCertificateEntity.class)
                    .list()
                    .stream()
                    .map(e -> new RevokedCertificate(e.getSerialNumber(), e.getRevocationDate(),
                            RevocationReason.fromCode(e.getRevocationReasonCode())))
                    .toList();
        }
    }

    /**
     * Returns a revoked certificate by serial number if present, otherwise {@code null}.
     *
     * @param serialNumber  serial number to lookup
     * @param serverInstance current server instance
     * @return revoked certificate or {@code null}
     */
    public static RevokedCertificate getRevokedCertificate(@NonNull BigInteger serialNumber,
                                                           @NonNull IServerInstance serverInstance) {
        try (Session s = serverInstance.getDatabaseSession()) {
            RevokedCertificateEntity entity = s.createQuery(
                            "FROM RevokedCertificateEntity r WHERE r.serialNumber = :sn",
                            RevokedCertificateEntity.class)
                    .setParameter("sn", serialNumber)
                    .uniqueResult();
            if (entity == null) {
                return null;
            }
            return new RevokedCertificate(entity.getSerialNumber(), entity.getRevocationDate(),
                    RevocationReason.fromCode(entity.getRevocationReasonCode()));
        }
    }

    /**
     * Stores revocation information for a certificate.
     *
     * @param serialNumber     serial number of the certificate to revoke
     * @param revocationReason reason code as defined in RFC 5280
     * @param serverInstance   current server instance
     */
    public static void revokeCertificate(@NonNull BigInteger serialNumber,
                                         @NonNull RevocationReason revocationReason,
                                         @NonNull IServerInstance serverInstance) {
        try (Session s = serverInstance.getDatabaseSession()) {
            Transaction tx = s.beginTransaction();
            RevokedCertificateEntity existing = s.createQuery(
                            "FROM RevokedCertificateEntity r WHERE r.serialNumber = :sn",
                            RevokedCertificateEntity.class)
                    .setParameter("sn", serialNumber)
                    .uniqueResult();
            if (existing == null) {
                RevokedCertificateEntity entity = new RevokedCertificateEntity();
                entity.setSerialNumber(serialNumber);
                entity.setRevocationDate(Instant.now());
                entity.setRevocationReasonCode(revocationReason.getCode());
                s.persist(entity);
                log.info("Revoked certificate {} with reason {}", serialNumber, revocationReason);
            }
            tx.commit();
        } catch (Exception e) {
            log.error("Unable to persist revocation for certificate {}", serialNumber, e);
            throw new RuntimeException("Unable to persist revocation", e);
        }
    }
}

