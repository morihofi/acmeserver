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
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertTools {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Reads a certificate file in PEM format, converts it to X.509 format, and returns the certificate bytes.
     *
     * @param certificatePath The path to the certificate file in PEM format.
     * @param keyPair         The key pair containing the public key that should match the certificate's public key.
     * @return The bytes of the X.509 certificate.
     * @throws IOException              If there is an issue reading the certificate file.
     * @throws CertificateException     If there is an issue with the certificate format or content.
     * @throws IllegalArgumentException If the certificate's public key does not match the specified public key.
     */
    public static byte[] getCertificateBytes(Path certificatePath, KeyPair keyPair) throws IOException, CertificateException {
        try (PEMParser pemParser = new PEMParser(Files.newBufferedReader(certificatePath))) {
            Object pemObject = pemParser.readObject();
            if (pemObject instanceof X509CertificateHolder certificateHolder) {
                // Convert the read object to an X509Certificate
                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);

                // Check if the certificate's public key matches the specified public key
                if (!certificate.getPublicKey().equals(keyPair.getPublic())) {
                    throw new IllegalArgumentException("The public certificate does not match the specified public key.");
                }

                // Return the certificate bytes in X.509 format
                return certificate.getEncoded();
            } else {
                throw new IllegalArgumentException("The specified file does not contain a valid certificate.");
            }
        }
    }

    /**
     * Checks the validity of a given X.509 certificate as of the current date and time. This method uses the {@code checkValidity} method
     * of {@link X509Certificate} to determine whether the certificate is currently valid. The validity check is based on the certificate's
     * notBefore and notAfter dates.
     *
     * @param certificate the X.509 certificate to be checked for validity.
     * @return {@code true} if the certificate is currently valid; {@code false} if it is expired or not yet valid as of the current date.
     */
    public static boolean isCertificateValid(X509Certificate certificate) {
        try {
            certificate.checkValidity(new Date());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private CertTools() {}
}
