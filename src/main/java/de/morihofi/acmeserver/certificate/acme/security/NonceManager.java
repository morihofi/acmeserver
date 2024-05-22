/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.HttpNonces;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadNonceException;
import de.morihofi.acmeserver.tools.safety.TypeSafetyHelper;
import jakarta.persistence.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class NonceManager {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(NonceManager.class);

    /**
     * Checks if a nonce from a decoded protected request body has already been used.
     *
     * @param decodedProtected The decoded protected request body as a JSON string.
     * @throws ACMEBadNonceException If the nonce has already been used.
     */
    public static void checkNonceFromDecodedProtected(String decodedProtected) {
        JsonObject reqBodyProtectedObj = JsonParser.parseString(decodedProtected).getAsJsonObject();
        String nonce = reqBodyProtectedObj.get("nonce").getAsString();

        if (isNonceUsed(nonce)) {
            throw new ACMEBadNonceException("Nonce already used");
        }
    }

    /**
     * Checks if the nonce has already been used. If not, it adds the nonce to the database.
     *
     * @param nonce The nonce to be checked
     * @return true if the nonce already exists, false if it was added
     */
    public static boolean isNonceUsed(String nonce) {

        if (Main.debug) {
            // Nonce protection is disabled when DEBUG environment variable is set to TRUE
            return false;
        }

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            org.hibernate.Transaction transaction = session.beginTransaction();

            // Check if the nonce exists in the database
            String hql = "SELECT 1 FROM HttpNonces hn WHERE hn.nonce = :nonce";
            Query query = session.createQuery(hql, HttpNonces.class);
            query.setParameter("nonce", nonce);
            List<HttpNonces> results = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), HttpNonces.class);

            boolean nonceExists = !results.isEmpty();

            if (!nonceExists) {
                // If the nonce does not exist, add it to the database
                HttpNonces newNonce = new HttpNonces(nonce, LocalDateTime.now());
                session.persist(newNonce);
            }

            transaction.commit();
            // Return true if nonce exists, false if it was added
            return nonceExists;
        } catch (Exception e) {
            LOG.error("Error checking or adding nonce", e);
            return false;
        }
    }
}
