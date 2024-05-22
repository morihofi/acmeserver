package de.morihofi.acmeserver.config.certificateAlgorithms;

/**
 * Represents the parameters for the RSA (Rivest–Shamir–Adleman) algorithm. This class extends {@link AlgorithmParams} to include parameters
 * specific to RSA, particularly the size of the key used in the algorithm.
 */
public class RSAAlgorithmParams extends AlgorithmParams {
    private int keySize;

    /**
     * Retrieves the key size used in the RSA algorithm. The key size is an integer that specifies the length of the RSA key in bits.
     *
     * @return The key size in bits as an {@code int}.
     */
    public int getKeySize() {
        return this.keySize;
    }

    /**
     * Sets the key size to be used in the RSA algorithm. This method allows specifying the length of the RSA key in bits.
     *
     * @param keySize The key size in bits as an {@code int}.
     */
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }
}
