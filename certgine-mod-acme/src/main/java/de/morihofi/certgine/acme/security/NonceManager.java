/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.certgine.acme.types.events.AcmeNonceRedeemedEvent;
import de.morihofi.certgine.acme.types.entities.AcmeHttpNonce;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadNonceException;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages nonce validation and storage for ACME requests to ensure the integrity and security of transactions.
 */
@Slf4j
@RequiredArgsConstructor
public class NonceManager implements INonceManager {

    private final IServerInstance serverInstance;


    /**
     * Debug mode flag. If true, nonce protection is disabled for debugging purposes.
     */
    private final boolean debug;

    /**
     * Constructs a new NonceManager instance
     *
     */
    public NonceManager(@NonNull IServerInstance serverInstance) {
        this.debug = false;
        this.serverInstance = serverInstance;
    }


    public void checkNonceFromDecodedProtected(@NonNull String decodedProtected) {
        JsonObject reqBodyProtectedObj = JsonParser.parseString(decodedProtected).getAsJsonObject();
        String nonce = reqBodyProtectedObj.get("nonce").getAsString();

        if (isNonceUsed(nonce)) {
            throw new ACMEBadNonceException("Nonce already used");
        }
    }

    /**
     * Checks if the nonce has already been used. If not, it adds the nonce to the database.
     * This method ensures that each nonce is used only once.
     *
     * @param nonce The nonce to be checked.
     * @return true if the nonce already exists, false if it was added.
     */
    public boolean isNonceUsed(@NonNull String nonce) {

        if (debug) {
            // Nonce protection is disabled when DEBUG environment variable is set to TRUE
            return false;
        }

        try (Session session = Objects.requireNonNull(serverInstance.getDatabaseSession())) {
            Transaction transaction = session.beginTransaction();

            // Check if the nonce exists in the database
            String hql = "FROM AcmeHttpNonce hn WHERE hn.nonce = :nonce";
            Query<AcmeHttpNonce> query = session.createQuery(hql, AcmeHttpNonce.class);
            query.setParameter("nonce", nonce);
            query.setMaxResults(1);
            Optional<AcmeHttpNonce> result = query.uniqueResultOptional();

            if (result.isEmpty()) {
                // If the nonce does not exist
                throw new ACMEBadNonceException("Nonce unknown");
            }

            // Get our object
            AcmeHttpNonce nonceObj = result.get();

            if (nonceObj.getRedeemTimestamp() != null) {
                return true; // Nonce already used
            }

            // Set timestamp when the nonce was redeemed
            nonceObj.setRedeemTimestamp(LocalDateTime.now());

            // Update nonce entity
            session.merge(nonceObj);

            // Apply
            transaction.commit();
            serverInstance.getEventBus().publish(new AcmeNonceRedeemedEvent(nonce));

            return false;

        } catch (Exception e) {
            log.error("Error checking or adding nonce", e);
            return true;
        }
    }
}
