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

package de.morihofi.acmeserver.database.objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;

/**
 * ACME Server user sessions
 */
@Entity
@Table(name = "user_session")
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class UserSession {

    /**
     * Logger instance for logging purposes.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Unique identifier for the user session.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Token representing the user session.
     */
    @Column(nullable = false)
    private String sessionToken;

    /**
     * User associated with the session.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * Timestamp when the session was created.
     */
    private Timestamp sessionCreated;

    /**
     * Timestamp when the session expires.
     */
    private Timestamp sessionExpire;

    /**
     * Gets the unique identifier of the session.
     *
     * @return the session ID.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the session.
     *
     * @param id the session ID.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the session token.
     *
     * @return the session token.
     */
    public String getSessionToken() {
        return sessionToken;
    }

    /**
     * Sets the session token.
     *
     * @param sessionToken the session token.
     */
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    /**
     * Gets the user associated with the session.
     *
     * @return the user.
     */
    public Users getUser() {
        return user;
    }

    /**
     * Sets the user associated with the session.
     *
     * @param user the user.
     */
    public void setUser(Users user) {
        this.user = user;
    }

    /**
     * Gets the timestamp when the session was created.
     *
     * @return the creation timestamp.
     */
    public Timestamp getSessionCreated() {
        return sessionCreated;
    }

    /**
     * Sets the timestamp when the session was created.
     *
     * @param sessionCreated the creation timestamp.
     */
    public void setSessionCreated(Timestamp sessionCreated) {
        this.sessionCreated = sessionCreated;
    }

    /**
     * Gets the timestamp when the session expires.
     *
     * @return the expiration timestamp.
     */
    public Timestamp getSessionExpire() {
        return sessionExpire;
    }

    /**
     * Sets the timestamp when the session expires.
     *
     * @param sessionExpire the expiration timestamp.
     */
    public void setSessionExpire(Timestamp sessionExpire) {
        this.sessionExpire = sessionExpire;
    }
}
