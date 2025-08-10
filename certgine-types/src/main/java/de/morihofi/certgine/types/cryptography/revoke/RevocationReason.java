package de.morihofi.certgine.types.cryptography.revoke;

/**
 * Enumeration of certificate revocation reasons as defined by RFC 5280.
 * Each reason has an associated numeric code used in CRL and OCSP responses.
 */
public enum RevocationReason {
    /** No specific reason given. */
    UNSPECIFIED(0),
    /** The private key of the certificate has been compromised. */
    KEY_COMPROMISE(1),
    /** The issuing CA has been compromised. */
    CA_COMPROMISE(2),
    /** The certificate holder's affiliation has changed. */
    AFFILIATION_CHANGED(3),
    /** The certificate has been replaced by another certificate. */
    SUPERSEDED(4),
    /** The certificate holder has ceased operations. */
    CESSATION_OF_OPERATION(5),
    /** The certificate is temporarily on hold. */
    CERTIFICATE_HOLD(6),
    /** The certificate was mistakenly placed on the revocation list. */
    REMOVE_FROM_CRL(8);

    private final int code;

    RevocationReason(int code) {
        this.code = code;
    }

    /**
     * Numeric code defined in RFC 5280 for this reason.
     *
     * @return integer reason code
     */
    public int getCode() {
        return code;
    }

    /**
     * Resolves the enum constant for the given numeric reason code.
     *
     * @param code reason code as defined in RFC 5280
     * @return matching {@link RevocationReason}
     * @throws IllegalArgumentException if the code is unknown
     */
    public static RevocationReason fromCode(int code) {
        for (RevocationReason reason : values()) {
            if (reason.code == code) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Invalid revocation reason: " + code);
    }
}
