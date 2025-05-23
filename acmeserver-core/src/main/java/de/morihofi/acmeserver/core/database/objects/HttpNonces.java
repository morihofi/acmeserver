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

import de.morihofi.acmeserver.core.tools.ServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.lang.invoke.MethodHandles;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Entity
@Table(name = "httpnonces")
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class HttpNonces {

    @Id
    @Column(name = "nonce", nullable = false)
    private String nonce;

    @Column(name = "timestamp", nullable = true) // Column name "timestamp" instead of redeemTimestamp to keep compatibility
    private LocalDateTime redeemTimestamp;

    @Column(name = "generated", nullable = true)
    private LocalDateTime generationTimestamp;

    /**
     * Constructs a new nonce with the given nonce string and timestamp.
     *
     * @param nonce     The nonce string.
     */
    public HttpNonces(String nonce) {
        this.nonce = nonce;
        this.redeemTimestamp = null;
        this.generationTimestamp = LocalDateTime.now();
    }

    /**
     * Generates a nonce (number used once) for security purposes.
     *
     * @return A randomly generated nonce as a hexadecimal string.
     * @throws IllegalArgumentException If there is an issue creating the nonce.
     */
    public static String createNonce(ServerInstance serverInstance) {
        try {
            log.info("Generating nonce");

            // Generate a random 128-bit nonce
            byte[] nonce = new byte[16]; // 128 bits are 16 bytes
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(nonce);

            // Encode the nonce to Base64 for easy handling
            String base64Nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);


            try (Session session = serverInstance.getHibernateUtil().getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();

                session.persist(new HttpNonces(base64Nonce)); // Store nonce
                log.info("Nonce {} stored", base64Nonce);

                tx.commit();
            }

            return base64Nonce;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create nonce", e);
        }
    }


}
