package de.morihofi.acmeserver.certificate.revokeDistribution.objects;

import java.math.BigInteger;
import java.util.Date;

/**
 * Represents a revoked certificate in a certificate revocation list (CRL).
 * This class stores information about a certificate that has been revoked,
 * including its serial number, the date of revocation, and the reason for revocation.
 */
public class RevokedCertificate {
    private BigInteger serialNumber;
    private Date revokationDate;
    private int revokationReason;

    /**
     * Constructs a new RevokedCertificate with specified details.
     *
     * @param serialNumber The serial number of the revoked certificate.
     * @param revokationDate The date when the certificate was revoked.
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
