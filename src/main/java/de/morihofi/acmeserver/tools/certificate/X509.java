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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Class for doing basic X509 certificate stuff
 */
public class X509 {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Extracts the X.500 Distinguished Name (DN) from an X.509 certificate.
     *
     * @param cert The X.509 certificate from which to extract the DN.
     * @return An X500Name object representing the subject DN of the certificate.
     * @throws CertificateEncodingException If there is an issue encoding the certificate or extracting the DN.
     */
    public static X500Name getX500NameFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        return new JcaX509CertificateHolder(cert).getSubject();
    }

    /**
     * Converts a byte array representing an X.509 certificate into an X.509 certificate object.
     *
     * @param certificateBytes The byte array containing the X.509 certificate data.
     * @return An X509Certificate object representing the parsed X.509 certificate.
     * @throws CertificateException If there is an issue parsing the X.509 certificate from the byte array.
     */
    public static X509Certificate convertToX509Cert(byte[] certificateBytes) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateBytes));
    }

    /**
     * Private constructor to prevent object creation
     */
    private X509() {}
}
