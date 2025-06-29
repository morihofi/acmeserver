/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.types.database.entities.acme.HttpNonces;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadNonceException;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.events.AcmeNonceRedeemedEvent;
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

    /**
    * Server.
    */
    private final HibernateUtil hibernateUtil;

    private final EventBus eventBus;


    /**
     * Debug mode flag. If true, nonce protection is disabled for debugging purposes.
     */
    private final boolean debug;

    /**
     * Constructs a new NonceManager instance
     *
     */
    public NonceManager(@NonNull HibernateUtil hibernateUtil, @NonNull EventBus eventBus) {
        this.debug = false;
        this.hibernateUtil = hibernateUtil;
        this.eventBus = eventBus;
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

        try (Session session = Objects.requireNonNull(hibernateUtil.getSessionFactory().openSession())) {
            Transaction transaction = session.beginTransaction();

            // Check if the nonce exists in the database
            String hql = "FROM HttpNonces hn WHERE hn.nonce = :nonce";
            Query<HttpNonces> query = session.createQuery(hql, HttpNonces.class);
            query.setParameter("nonce", nonce);
            query.setMaxResults(1);
            Optional<HttpNonces> result = query.uniqueResultOptional();

            if (result.isEmpty()) {
                // If the nonce does not exist
                throw new ACMEBadNonceException("Nonce unknown");
            }

            // Get our object
            HttpNonces nonceObj = result.get();

            if (nonceObj.getRedeemTimestamp() != null) {
                return true; // Nonce already used
            }

            // Set timestamp when the nonce was redeemed
            nonceObj.setRedeemTimestamp(LocalDateTime.now());

            // Update nonce entity
            session.merge(nonceObj);

            // Apply
            transaction.commit();
            eventBus.publish(new AcmeNonceRedeemedEvent(nonce));

            return false;

        } catch (Exception e) {
            log.error("Error checking or adding nonce", e);
            return true;
        }
    }
}
