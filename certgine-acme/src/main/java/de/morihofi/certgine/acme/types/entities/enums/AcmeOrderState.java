/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.entities.enums;

/**
 * Enum for ACME order states, used for background certificate generation
 */
public enum AcmeOrderState {
    /**
     * Default state
     */
    IDLE,
    /**
     * Tells the CertificateIssuer Thread to create a certificate for the order
     */
    NEED_A_CERTIFICATE,

    /**
     * Generation of certificate failed -> CSR is maybe unsupported
     */
    GENERATION_FAILED
}
