/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.database.entities.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Certgine user sessions
 */
@Entity
@Table(name = "user_session")
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
@NoArgsConstructor
public class UserSession {

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
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant sessionCreated;

    /**
     * Timestamp when the session expires.
     */
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant sessionExpire;


}
