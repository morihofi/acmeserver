/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.certificate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;

public class CertMisc {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

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
