/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.database.entities.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * ACME Server users
 */
@Entity
@Table(name = "users")
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
@NoArgsConstructor
public class Users {
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

}
