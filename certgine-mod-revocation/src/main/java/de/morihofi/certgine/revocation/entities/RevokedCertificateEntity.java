/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.math.BigInteger;
import java.time.Instant;

/**
 * JPA entity representing a revoked certificate persisted by the revocation module.
 */
@Entity
@Table(name = "revoked_certificate")
@Data
@NoArgsConstructor
public class RevokedCertificateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Serial number of the revoked certificate. */
    @Column(nullable = false, unique = true)
    private BigInteger serialNumber;

    /** Timestamp when the certificate was revoked. */
    @Column(nullable = false)
    private Instant revocationDate;

    /** Numeric revocation reason as defined in RFC 5280. */
    @Column(nullable = false)
    private int revocationReasonCode;
}

