package de.morihofi.acmeserver.database;

public enum AcmeStatus {
    PENDING("pending"),
    READY("ready"),
    PROCESSING("processing"),
    VALID("valid"),
    INVALID("invalid"),

    REVOKED("revoked"),

    DEACTIVATED("deactivated"),

    EXPIRED("expired");


    private final String rfcName;

    AcmeStatus(String rfcName) {
        this.rfcName = rfcName;
    }

    public String getRfcName() {
        return rfcName;
    }
}
