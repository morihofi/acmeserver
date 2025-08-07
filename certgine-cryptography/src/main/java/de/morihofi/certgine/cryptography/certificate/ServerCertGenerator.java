package de.morihofi.certgine.cryptography.certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Generates TLS server certificates.
 */
public class ServerCertGenerator implements CertificateGenerator {

    @Override
    public X509Certificate generate(X509Generator.Request req)
            throws CertificateException, OperatorCreationException, CertIOException {
        Objects.requireNonNull(req.getIssuerKeyPair(),
                "issuerKeyPair (INTERMEDIATE key) is required");
        Objects.requireNonNull(req.getIssuerCertificate(),
                "issuerCertificate (INTERMEDIATE cert) is required");
        CertificateGeneratorHelper.validateIssuerIsCa(req.getIssuerCertificate());
        Objects.requireNonNull(req.getServerPublicKeyBytes(), "serverPublicKeyBytes is required");
        Objects.requireNonNull(req.getIdentifiers(), "identifiers is required");
        Objects.requireNonNull(req.getStartDate(), "startDate is required");
        Objects.requireNonNull(req.getEndDate(), "endDate is required");

        X500Name issuerName = X509CertificateTools.getX500NameFromX509Certificate(
                req.getIssuerCertificate());
        X500Name subjectName = new X500Name("CN=" + req.getIdentifiers().getFirst().getValue());

        X509v3CertificateBuilder builder = CertificateGeneratorHelper.createBuilder(
                issuerName, req.getStartDate(), req.getEndDate(), subjectName,
                req.getServerPublicKeyBytes());

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        CertificateGeneratorHelper.addSubjectAlternativeNames(builder, req.getIdentifiers());
        CertificateGeneratorHelper.addCrlAndOcsp(builder,
                req.getCrlDistributionUrl(), req.getOcspServiceEndpoint());

        ContentSigner signer = CertificateGeneratorHelper.createContentSigner(
                req.getIssuerKeyPair().getPrivate());

        return CertificateGeneratorHelper.toCertificate(builder, signer, req.getIssuerKeyPair());
    }
}

