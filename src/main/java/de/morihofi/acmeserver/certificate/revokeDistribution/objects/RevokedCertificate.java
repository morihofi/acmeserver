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

package de.morihofi.acmeserver.certificate.revokeDistribution.objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigInteger;
import java.util.Date;

/**
 * Represents a revoked certificate in a certificate revocation list (CRL). This class stores information about a certificate that has been
 * revoked, including its serial number, the date of revocation, and the reason for revocation.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class RevokedCertificate {
    /**
     * Serial number of the revoked certificate
     */
    private BigInteger serialNumber;
    /**
     * Date when the certificate was revoked
     */
    private Date revokationDate;
    /**
     * The reason why the certificate was revoked
     */
    private int revokationReason;

    /**
     * Constructs a new RevokedCertificate with specified details.
     *
     * @param serialNumber     The serial number of the revoked certificate.
     * @param revokationDate   The date when the certificate was revoked.
     * @param revokationReason The reason code for the revocation.
     */
    public RevokedCertificate(BigInteger serialNumber, Date revokationDate, int revokationReason) {
        this.serialNumber = serialNumber;
        this.revokationDate = revokationDate;
        this.revokationReason = revokationReason;
    }

    /**
     * Retrieves the serial number of the revoked certificate.
     *
     * @return The serial number as a {@link BigInteger}.
     */
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    /**
     * Sets the serial number of the revoked certificate.
     *
     * @param serialNumber The serial number to set.
     */
    public void setSerialNumber(BigInteger serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * Retrieves the date of revocation for the certificate.
     *
     * @return The revocation date as a {@link Date}.
     */
    public Date getRevokationDate() {
        return revokationDate;
    }

    /**
     * Sets the revocation date for the certificate.
     *
     * @param revokationDate The date to set for revocation.
     */
    public void setRevokationDate(Date revokationDate) {
        this.revokationDate = revokationDate;
    }

    /**
     * Retrieves the revocation reason code for the certificate.
     *
     * @return The revocation reason code as an integer.
     */
    public int getRevokationReason() {
        return revokationReason;
    }

    /**
     * Sets the revocation reason code for the certificate.
     *
     * @param revokationReason The reason code for revocation.
     */
    public void setRevokationReason(int revokationReason) {
        this.revokationReason = revokationReason;
    }
}
