/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.certificate.generator;

import de.morihofi.acmeserver.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.tools.certificate.CertMisc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.lang.invoke.MethodHandles;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

public class CertificateRevokationListGenerator {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Generates a Certificate Revocation List (CRL) using the provided list of revoked certificates, the Certificate Authority (CA)
     * certificate, and the CA's private key. The generated CRL will include details about the revoked certificates and will be signed using
     * the CA's private key.
     *
     * @param revokedCertificates A list of {@link RevokedCertificate} objects representing the certificates that need to be revoked.
     * @param caCert              The X509 certificate of the Certificate Authority.
     * @param caPrivateKey        The private key of the Certificate Authority used to sign the CRL.
     * @param updateMinutes       The number of minutes after which the CRL should be updated.
     * @return An {@link X509CRL} object representing the generated CRL.
     * @throws CertificateEncodingException if an encoding error occurs with the CA certificate.
     * @throws CRLException                 if an error occurs during the CRL generation.
     * @throws OperatorCreationException    if there's an error in creating the content signer.
     */
    public static X509CRL generateCRL(List<RevokedCertificate> revokedCertificates,
            X509Certificate caCert,
            PrivateKey caPrivateKey, int updateMinutes) throws CertificateEncodingException, CRLException, OperatorCreationException {

        // Create the CRL Builder
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(
                new JcaX509CertificateHolder(caCert).getSubject(),
                new Date()
        );

        // Add an expiration date
        crlBuilder.setNextUpdate(new Date(System.currentTimeMillis() + updateMinutes * 60 * 1000L)); // Update in 5 minutes

        // Add the revoked serial numbers
        for (RevokedCertificate revokedCertificate : revokedCertificates) {
            crlBuilder.addCRLEntry(revokedCertificate.getSerialNumber(), revokedCertificate.getRevokationDate(),
                    revokedCertificate.getRevokationReason());
        }

        // Sign the CRL with the CA's private key
        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(CertMisc.getSignatureAlgorithmBasedOnKeyType(caPrivateKey));
        signerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);
        X509CRLHolder crlHolder = crlBuilder.build(signerBuilder.build(caPrivateKey));

        // Convert the CRL to a Java CRL object
        JcaX509CRLConverter converter = new JcaX509CRLConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        return converter.getCRL(crlHolder);
    }

    private CertificateRevokationListGenerator() {}
}
