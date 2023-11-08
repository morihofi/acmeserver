package de.morihofi.acmeserver.certificate.revokeDistribution.objects;

import java.math.BigInteger;
import java.util.Date;

public class RevokedCertificate {
    private BigInteger serialNumber;
    private Date revokationDate;
    private int revokationReason;


    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(BigInteger serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getRevokationDate() {
        return revokationDate;
    }

    public void setRevokationDate(Date revokationDate) {
        this.revokationDate = revokationDate;
    }

    public int getRevokationReason() {
        return revokationReason;
    }

    public void setRevokationReason(int revokationReason) {
        this.revokationReason = revokationReason;
    }

    public RevokedCertificate(BigInteger serialNumber, Date revokationDate, int revokationReason) {
        this.serialNumber = serialNumber;
        this.revokationDate = revokationDate;
        this.revokationReason = revokationReason;
    }
}
