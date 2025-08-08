package de.morihofi.certgine.cryptography.certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Generates intermediate CA certificates.
 */
public class IntermediateCaGenerator implements ICertificateGenerator {

    @Override
    public X509Certificate generate(X509Generator.Request req)
            throws CertificateException, OperatorCreationException, CertIOException {
        Objects.requireNonNull(req.getIssuerKeyPair(),
                "issuerKeyPair (ROOT key) is required for INTERMEDIATE generation");
        Objects.requireNonNull(req.getIssuerCertificate(),
                "issuerCertificate (ROOT cert) is required for INTERMEDIATE generation");
        CertificateGeneratorHelper.validateIssuerForIntermediate(req.getIssuerCertificate());
        Objects.requireNonNull(req.getOwnKeyPair(),
                "ownKeyPair (INTERMEDIATE key) is required");
        Objects.requireNonNull(req.getCertificateConfig(),
                "certificateConfig is required");

        Date[] validity = CertificateGeneratorHelper.calculateValidity(req.getCertificateConfig());
        X500Name issuerName = X509CertificateTools.getX500NameFromX509Certificate(
                req.getIssuerCertificate());
        X500Name subjectName = CertificateGeneratorHelper.toX500(
                req.getCertificateConfig().getMetadata(),
                "A common name is required in intermediate CA. Please change it in your settings.");

        X509v3CertificateBuilder builder = CertificateGeneratorHelper.createBuilder(
                issuerName, validity[0], validity[1], subjectName,
                req.getOwnKeyPair().getPublic().getEncoded());

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        CertificateGeneratorHelper.addCaKeyUsage(builder);
        CertificateGeneratorHelper.addCrlAndOcsp(builder,
                req.getCrlDistributionUrl(), req.getOcspServiceEndpoint());

        ContentSigner signer = CertificateGeneratorHelper.createContentSigner(
                req.getIssuerKeyPair().getPrivate());

        return CertificateGeneratorHelper.toCertificate(builder, signer, req.getIssuerKeyPair());
    }
}

