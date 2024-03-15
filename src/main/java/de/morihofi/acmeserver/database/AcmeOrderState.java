package de.morihofi.acmeserver.database;

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
