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

import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.util.List;

/**
 * Represents an ACME identifier entity used for managing order identifiers, challenges, and certificates.
 */
@Entity
@Data
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
@RequiredArgsConstructor
public class AcmeOrderIdentifier implements Serializable {

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated authorization ID.
     *
     * @param authorizationId The unique identifier of the authorization associated with the ACME identifier.
     * @param serverInstance  The server instance for database connection.
     * @return The ACME identifier matching the provided authorization ID, or null if not found.
     */
    public static AcmeOrderIdentifier getACMEIdentifierByAuthorizationId(@NonNull String authorizationId, @NonNull IServerInstance serverInstance) {
        AcmeOrderIdentifier identifier = null;
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();
            identifier = session.createQuery("FROM AcmeOrderIdentifier WHERE authorizationId = :authorizationId", AcmeOrderIdentifier.class)
                    .setParameter("authorizationId", authorizationId)
                    .setMaxResults(1)
                    .uniqueResult();

            if (identifier != null) {
                log.info("Got ACME identifier of type {} with value {} for authorization ID: {} ",
                        identifier.getType(),
                        identifier.getDataValue(),
                        authorizationId
                );
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get ACME identifiers for authorization id {}", authorizationId, e);
        }
        return identifier;
    }

    /**
     * Unique identifier for the ACME order identifier.
     */
    @Id
    @Column(name = "identifierId", nullable = false)
    private String identifierId;

    /**
     * The type of the ACME order identifier (e.g., "dns", "ip").
     */
    @Column(name = "type")
    private final String type;

    /**
     * The data value of the ACME order identifier (e.g., the domain name or IP address).
     */
    @Column(name = "dataValue")
    private final String dataValue;

    /**
     * The ACME order associated with this identifier.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    private AcmeOrder order;

    /**
     * Indicates whether challenges have been generated for this identifier.
     */
    @Column(name = "hasChallengesGenerated", nullable = false)
    private boolean hasChallengesGenerated = false;

    /**
     * The list of challenges associated with this identifier.
     */
    @OneToMany(mappedBy = "identifier")
    private List<AcmeOrderIdentifierChallenge> challenges;

    /**
     * The unique authorization ID for this identifier.
     */
    @Column(name = "authorizationId", nullable = false)
    private String authorizationId;

    /**
     * Gets the challenge status for this identifier.
     *
     * @return The challenge status, VALID if at least one challenge is valid, otherwise PENDING.
     */
    @NonNull
    public AcmeStatus getChallengeStatus() {
        // Checks whether there is at least one challenge with the status VALID
        for (AcmeOrderIdentifierChallenge challenge : challenges) {
            if (challenge.getStatus() == AcmeStatus.VALID) {
                return AcmeStatus.VALID;
            }
        }
        // Default return if no challenge has the status VALID
        return AcmeStatus.PENDING;
    }

}
