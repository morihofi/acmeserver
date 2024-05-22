package de.morihofi.acmeserver.config.certificateAlgorithms;

/**
 * Represents the parameters for the Elliptic Curve Digital Signature Algorithm (ECDSA). This class extends {@link AlgorithmParams} to
 * include parameters specific to ECDSA, particularly the name of the elliptic curve used.
 */
public class EcdsaAlgorithmParams extends AlgorithmParams {
    private String curveName;

    /**
     * Retrieves the name of the elliptic curve used in the ECDSA algorithm. The curve name is a string that identifies the specific
     * elliptic curve being used, such as 'secp256k1' or 'secp384r1'.
     *
     * @return The name of the elliptic curve as a {@code String}.
     */
    public String getCurveName() {
        return curveName;
    }

    /**
     * Sets the name of the elliptic curve to be used in the ECDSA algorithm. This method allows specifying a particular elliptic curve by
     * name.
     *
     * @param curveName The name of the elliptic curve as a {@code String}.
     */
    public void setCurveName(String curveName) {
        this.curveName = curveName;
    }
}
