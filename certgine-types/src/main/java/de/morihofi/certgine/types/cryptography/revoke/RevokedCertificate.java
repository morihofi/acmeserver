/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.cryptography.revoke;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents a revoked certificate in a certificate revocation list (CRL). This class stores information about a certificate that has been
 * revoked, including its serial number, the date of revocation, and the reason for revocation.
 *
 * @param serialNumber     Serial number of the revoked certificate
 * @param revocationDate   Instant when the certificate was revoked, in UTC
 * @param revocationReason The reason why the certificate was revoked
 */
public record RevokedCertificate(BigInteger serialNumber, Instant revocationDate,
                                 RevocationReason revocationReason) {

    /**
     * Formats the revocation date as an ISO-8601 string in UTC.
     *
     * @return revocation time formatted using {@link DateTimeFormatter#ISO_INSTANT}
     */
    public String formattedRevocationDate() {
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(revocationDate);
    }
}
