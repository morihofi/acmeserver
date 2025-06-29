/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.cryptography.revoke;

import java.math.BigInteger;
import java.util.Date;

/**
 * Represents a revoked certificate in a certificate revocation list (CRL). This class stores information about a certificate that has been
 * revoked, including its serial number, the date of revocation, and the reason for revocation.
 *
 * @param serialNumber     Serial number of the revoked certificate
 * @param revocationDate   Date when the certificate was revoked
 * @param revocationReason The reason why the certificate was revoked
 */
public record RevokedCertificate(BigInteger serialNumber, Date revocationDate, int revocationReason) {
}
