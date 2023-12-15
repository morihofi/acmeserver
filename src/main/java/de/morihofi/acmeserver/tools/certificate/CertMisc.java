package de.morihofi.acmeserver.tools.certificate;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;

public class CertMisc {

    /**
     * Generates a secure random serial number.
     *
     * @return A BigInteger representing a 160-bit secure random serial number.
     */
    public static BigInteger generateSerialNumber() {
        return new BigInteger(160, new SecureRandom()); // Secure random serial number
    }

    /**
     * Determines the appropriate signature algorithm based on the type of the provided private key.
     *
     * @param privateKey The private key for which the signature algorithm needs to be determined.
     * @return A String representing the signature algorithm.
     * @throws IllegalArgumentException If the private key is of a non-supported type.
     */
    public static String getSignatureAlgorithmBasedOnKeyType(PrivateKey privateKey) {
        String signatureAlgorithm;
        if (privateKey instanceof RSAPrivateKey) {
            signatureAlgorithm = "SHA256withRSA";
        } else if (privateKey instanceof ECPrivateKey) {
            signatureAlgorithm = "SHA256withECDSA";
        } else if (privateKey.getClass().getName().equals("sun.security.pkcs11.P11Key$P11PrivateKey")) {
            // Assuming RSA key for PKCS#11 - may need to be adjusted based on actual key type and capabilities
            signatureAlgorithm = "SHA256withRSA";
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + privateKey.getClass().getName());
        }
        return signatureAlgorithm;
    }


}
