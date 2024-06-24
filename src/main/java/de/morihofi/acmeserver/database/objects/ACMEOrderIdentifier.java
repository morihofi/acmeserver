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

package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.tools.ServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

/**
 * Represents an ACME identifier entity used for managing order identifiers, challenges, and certificates.
 */
@Entity
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrderIdentifier implements Serializable {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated authorization ID.
     *
     * @param authorizationId The unique identifier of the authorization associated with the ACME identifier.
     * @return The ACME identifier matching the provided authorization ID, or null if not found.
     */
    public static ACMEOrderIdentifier getACMEIdentifierByAuthorizationId(String authorizationId, ServerInstance serverInstance) {
        ACMEOrderIdentifier identifier = null;
        try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            identifier = session.createQuery("FROM ACMEOrderIdentifier WHERE authorizationId = :authorizationId", ACMEOrderIdentifier.class)
                    .setParameter("authorizationId", authorizationId)
                    .setMaxResults(1)
                    .getSingleResult();

            if (identifier != null) {
                LOG.info("(Authorization ID: {}) Got ACME identifier of type {} with value {}",
                        authorizationId,
                        identifier.getType(),
                        identifier.getDataValue()
                );
            }
            transaction.commit();
        } catch (Exception e) {
            LOG.error("Unable get ACME identifiers for authorization id {}", authorizationId, e);
        }
        return identifier;
    }
    @Id
    @Column(name = "identifierId", nullable = false)
    private String identifierId;
    @Column(name = "type")
    private String type;
    @Column(name = "dataValue")
    private String dataValue;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    private ACMEOrder order;
    @Column(name = "hasChallengesGenerated", nullable = false)
    private boolean hasChallengesGenerated = false;
    @OneToMany(mappedBy = "identifier")
    private List<ACMEOrderIdentifierChallenge> challenges;
    @Column(name = "authorizationId", nullable = false)
    private String authorizationId;

    /**
     * Create an instance of ACME identifier with a specified type and data value.
     *
     * @param type      The type of the identifier.
     * @param dataValue The data value of the identifier.
     */
    public ACMEOrderIdentifier(String type, String dataValue) {
        this.type = type;
        this.dataValue = dataValue;
    }

    /**
     * Default constructor for ACME identifier.
     */
    public ACMEOrderIdentifier() {
    }

    public List<ACMEOrderIdentifierChallenge> getChallenges() {
        return challenges;
    }

    /**
     * Get the associated order for this ACME identifier.
     *
     * @return The ACME order.
     */
    public ACMEOrder getOrder() {
        return order;
    }

    /**
     * Set the associated order for this ACME identifier.
     *
     * @param order The ACME order to set.
     */
    public void setOrder(ACMEOrder order) {
        this.order = order;
    }

    /**
     * Get the type of this ACME identifier.
     *
     * @return The type of the identifier.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of this ACME identifier.
     *
     * @param type The type of the identifier to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the data value of this ACME identifier.
     *
     * @return The data value of the identifier.
     */
    public String getDataValue() {
        return dataValue;
    }

    /**
     * Set the data value of this ACME identifier.
     *
     * @param dataValue The data value of the identifier to set.
     */
    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public String getIdentifierId() {
        return identifierId;
    }

    public void setIdentifierId(String identifierId) {
        this.identifierId = identifierId;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    public AcmeStatus getChallengeStatus() {
        // Checks whether there is at least one challenge with the status VALID
        for (ACMEOrderIdentifierChallenge challenge : challenges) {
            if (challenge.getStatus() == AcmeStatus.VALID) {
                return AcmeStatus.VALID;
            }
        }
        // Default return if no challenge has the status VALID
        return AcmeStatus.PENDING;
    }

    public boolean isHasChallengesGenerated() {
        return hasChallengesGenerated;
    }

    public void setHasChallengesGenerated(boolean hasChallengesGenerated) {
        this.hasChallengesGenerated = hasChallengesGenerated;
    }
}
