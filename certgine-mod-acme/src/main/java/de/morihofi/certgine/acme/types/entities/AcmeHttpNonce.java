/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.entities;

import de.morihofi.certgine.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Entity storing ACME HTTP nonces used to prevent replay attacks.
 */
@Entity
@Table(name = "httpnonces")
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
@NoArgsConstructor
public class AcmeHttpNonce {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Id
    @Column(name = "nonce", nullable = false)
    private String nonce;

    @Column(name = "redeemed")
    private LocalDateTime redeemTimestamp;

    @Column(name = "generated")
    private LocalDateTime generationTimestamp = LocalDateTime.now();

    public AcmeHttpNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Generates and stores a new nonce.
     *
     * @param serverInstance server instance for database access
     * @return generated nonce
     */
    public static String createNonce(@NonNull IServerInstance serverInstance) {
        log.info("Generating nonce");

        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String base64Nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(new AcmeHttpNonce(base64Nonce));
            log.info("Nonce {} stored", base64Nonce);
            tx.commit();
        }

        return base64Nonce;
    }
}

