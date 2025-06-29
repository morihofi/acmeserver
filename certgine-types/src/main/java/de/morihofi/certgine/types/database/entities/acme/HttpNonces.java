/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.database.entities.acme;

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

@Entity
@Table(name = "httpnonces")
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
@NoArgsConstructor
public class HttpNonces {

    public HttpNonces(String nonce) {
        this.nonce = nonce;
    }

    @Id
    @Column(name = "nonce", nullable = false)
    private String nonce;

    @Column(name = "redeemed")
    private LocalDateTime redeemTimestamp;

    @Column(name = "generated")
    private LocalDateTime generationTimestamp = LocalDateTime.now();

    private final static SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a nonce (number used once) for security purposes.
     *
     * @return A randomly generated nonce as a hexadecimal string.
     * @throws IllegalArgumentException If there is an issue creating the nonce.
     */
    public static String createNonce(@NonNull IServerInstance serverInstance) {

        log.info("Generating nonce");

        // Generate a random 128-bit nonce
        byte[] nonce = new byte[16]; // 128 bits are 16 bytes
        secureRandom.nextBytes(nonce);

        // Encode the nonce to Base64 for easy handling
        String base64Nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);


        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction tx = session.beginTransaction();

            session.persist(new HttpNonces(base64Nonce)); // Store nonce
            log.info("Nonce {} stored", base64Nonce);

            tx.commit();
        }

        return base64Nonce;

    }


}
