package de.morihofi.acmeserver.cryptography.keys;

import lombok.NonNull;

import java.security.PrivateKey;

public class KeyHelper {
    /**
     * Determines the appropriate signature algorithm based on the type of the provided private key.
     *
     * @param privateKey The private key for which the signature algorithm needs to be determined.
     * @return A String representing the signature algorithm.
     * @throws IllegalArgumentException If the private key is of a non-supported type.
     */
    @NonNull
    public static String getSignatureAlgorithmBasedOnKeyType(@NonNull PrivateKey privateKey) {
        String algorithm = privateKey.getAlgorithm();
        if (algorithm == null) {
            throw new IllegalArgumentException("Private key algorithm is null for class: " + privateKey.getClass().getName());
        }

        switch (algorithm.toUpperCase(java.util.Locale.ROOT)) {
            case "RSA":
                return "SHA256withRSA";
            case "EC":
            case "ECDSA":
                return "SHA256withECDSA";
            case "DSA":
                return "SHA256withDSA";
            case "ED25519":
                return "Ed25519";
            case "ED448":
                return "Ed448";
            default:
                throw new IllegalArgumentException("Unsupported key algorithm: " + algorithm + " (" + privateKey.getClass().getName() + ")");
        }
    }


}
