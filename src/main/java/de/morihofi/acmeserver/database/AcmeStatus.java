package de.morihofi.acmeserver.database;

public enum AcmeStatus {
    PENDING("pending"),
    READY("ready"),
    PROCESSING("processing"),
    VALID("valid"),
    INVALID("invalid");


    private final String rfcName;

    AcmeStatus(String rfcName) {
        this.rfcName = rfcName;
    }

    public String getRfcName() {
        return rfcName;
    }
}
