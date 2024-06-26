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
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Set;

/**
 * ACME Server users
 */
@Entity
@Table(name = "users")
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class Users {

    /**
     * Logger for logging purposes.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * The email address of the user.
     */
    private String email;

    /**
     * The set of sessions associated with the user.
     */
    @OneToMany(mappedBy = "user")
    private Set<UserSession> sessions;

    /**
     * Indicates whether the user has administrative privileges.
     */
    private boolean isAdmin;

    /**
     * Gets the unique identifier for the user.
     *
     * @return The unique identifier for the user.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the user.
     *
     * @param id The unique identifier for the user.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the email address of the user.
     *
     * @return The email address of the user.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user.
     *
     * @param email The email address of the user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the set of sessions associated with the user.
     *
     * @return The set of sessions associated with the user.
     */
    public Set<UserSession> getSessions() {
        return sessions;
    }

    /**
     * Sets the set of sessions associated with the user.
     *
     * @param sessions The set of sessions associated with the user.
     */
    public void setSessions(Set<UserSession> sessions) {
        this.sessions = sessions;
    }

    /**
     * Checks if the user has administrative privileges.
     *
     * @return True if the user has administrative privileges, otherwise false.
     */
    public boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Sets the administrative privileges for the user.
     *
     * @param admin True to grant administrative privileges, otherwise false.
     */
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
