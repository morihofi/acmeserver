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

package de.morihofi.acmeserver.tools.certificate.generator;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.tools.certificate.CertMisc;
import de.morihofi.acmeserver.tools.certificate.X509;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;

/**
 * Class for generating the certificate for the Webserver
 */
public class ServerCertificateGenerator {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Generates an X509 server certificate using provided parameters and cryptographic elements.
     * <p>
     * This method creates a virtual Certificate Signing Request (CSR) and uses the provided intermediate certificate to sign the server
     * certificate. It sets up the certificate with basic constraints indicating it's not a certificate authority, key usage for digital
     * signature and key encipherment, subject alternative names based on provided DNS names, CRL Distribution Points, and Authority
     * Information Access for OCSP. The certificate is then signed using the private key from the intermediate key pair and the appropriate
     * signature algorithm.</p>
     *
     * @param intermediateKeyPair     The key pair for the intermediate certificate authority.
     * @param intermediateCertificate The byte array intermediate certificate.
     * @param serverPublicKeyBytes    The byte array representing the server's public key.
     * @param identifiers             An array of Identifiers to be associated with the server certificate.
     * @param startDate               The start date for the server certificate's validity period.
     * @param endDate                 The end date for the server certificate's validity period.
     * @param provisioner             The provisioner for obtaining CRL and OCSP URLs.
     * @return An X509Certificate which represents the server certificate.
     * @throws OperatorCreationException If there's an error during the creation of cryptographic operators.
     * @throws CertificateException      If there's an error in processing the certificate data.
     * @throws CertIOException           If there's an IO error during certificate generation.
     */
    public static X509Certificate createServerCertificate(KeyPair intermediateKeyPair, X509Certificate intermediateCertificate,
                                                          byte[] serverPublicKeyBytes, Identifier[] identifiers, Date startDate,
                                                          Date endDate, Provisioner provisioner) throws
            OperatorCreationException, CertificateException, CertIOException {

        // Create our virtual "CSR"
        X500Name issuerName = X509.getX500NameFromX509Certificate(intermediateCertificate);
        BigInteger serialNumber = CertMisc.generateSerialNumber();

        X500Name subjectName = new X500Name("CN=" + identifiers[0].getValue());
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuerName, serialNumber, startDate, endDate, subjectName,
                SubjectPublicKeyInfo.getInstance(serverPublicKeyBytes)
        );

        // Basic Constraints - Not a CA
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        // Key Usage
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // Subject Alternative Names
        ArrayList<GeneralName> dnsGeneralNameList = new ArrayList<>();
        for (Identifier identifier : identifiers) {
            if (identifier.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.DNS) {
                dnsGeneralNameList.add(new GeneralName(GeneralName.dNSName, identifier.getValue()));
            } else if (identifier.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.IP) {
                dnsGeneralNameList.add(new GeneralName(GeneralName.iPAddress, identifier.getValue()));
            }
        }
        GeneralNames subjectAltNames = new GeneralNames(dnsGeneralNameList.toArray(new GeneralName[0]));
        certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        if (provisioner != null) {

            // CRL Distribution Points
            GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, provisioner.getFullCrlUrl());
            DistributionPointName dpn = new DistributionPointName(new GeneralNames(gn));
            DistributionPoint distp = new DistributionPoint(dpn, null, null);
            certBuilder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(new DistributionPoint[]{distp}));

            // Authority Information Access (OCSP Endpoint)
            AccessDescription accessDescription = new AccessDescription(
                    AccessDescription.id_ad_ocsp,
                    new GeneralName(GeneralName.uniformResourceIdentifier, provisioner.getFullOcspUrl())
            );

            ASN1EncodableVector authorityInformationAccessVector = new ASN1EncodableVector();
            authorityInformationAccessVector.add(accessDescription);
            certBuilder.addExtension(
                    Extension.authorityInfoAccess, false, new DERSequence(authorityInformationAccessVector));
        }

        // Signature Algorithm
        String signatureAlgorithm = CertMisc.getSignatureAlgorithmBasedOnKeyType(intermediateKeyPair.getPrivate());

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(intermediateKeyPair.getPrivate());

        X509CertificateHolder holder = certBuilder.build(signer);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);
        return converter.getCertificate(holder);
    }

    /**
     * Private constructor to prevent object instantiation
     */
    private ServerCertificateGenerator() {
    }
}
