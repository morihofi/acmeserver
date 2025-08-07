package de.morihofi.certgine.cryptography.certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Generates timestamp authority certificates.
 */
public class TimestampAuthorityGenerator implements CertificateGenerator {

    @Override
    public X509Certificate generate(X509Generator.Request req)
            throws CertificateException, OperatorCreationException, CertIOException {
        Objects.requireNonNull(req.getIssuerKeyPair(),
                "issuerKeyPair (ROOT key) is required");
        Objects.requireNonNull(req.getIssuerCertificate(),
                "issuerCertificate (ROOT cert) is required");
        CertificateGeneratorHelper.validateIssuerIsCa(req.getIssuerCertificate());
        Objects.requireNonNull(req.getOwnKeyPair(), "ownKeyPair is required");
        Objects.requireNonNull(req.getCertificateConfig(), "certificateConfig is required");

        Date[] validity = CertificateGeneratorHelper.calculateValidity(req.getCertificateConfig());
        X500Name issuer = X509CertificateTools.getX500NameFromX509Certificate(
                req.getIssuerCertificate());
        X500Name subject = CertificateGeneratorHelper.toX500(
                req.getCertificateConfig().getMetadata(),
                "A common name is required in timestamping authority. Please change it in your settings.");

        X509v3CertificateBuilder builder = CertificateGeneratorHelper.createBuilder(
                issuer, validity[0], validity[1], subject,
                req.getOwnKeyPair().getPublic().getEncoded());

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        builder.addExtension(Extension.extendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

        ContentSigner signer = CertificateGeneratorHelper.createContentSigner(
                req.getIssuerKeyPair().getPrivate());

        return CertificateGeneratorHelper.toCertificate(builder, signer, req.getIssuerKeyPair());
    }
}

