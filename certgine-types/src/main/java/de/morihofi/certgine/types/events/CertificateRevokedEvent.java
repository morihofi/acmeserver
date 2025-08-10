package de.morihofi.certgine.types.events;

import java.math.BigInteger;

/**
 * Event published when a certificate has been revoked.
 */
public class CertificateRevokedEvent extends AbstractEvent {
    private final BigInteger serialNumber;

    /**
     * Creates a new event.
     *
     * @param serialNumber serial number of the revoked certificate
     */
    public CertificateRevokedEvent(BigInteger serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * Returns the serial number of the revoked certificate.
     *
     * @return serial number
     */
    public BigInteger getSerialNumber() {
        return serialNumber;
    }
}
