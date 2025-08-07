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
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Generates self-signed root CA certificates.
 */
public class RootCaGenerator implements CertificateGenerator {

    @Override
    public X509Certificate generate(X509Generator.Request req)
            throws CertificateException, OperatorCreationException, CertIOException {
        Objects.requireNonNull(req.getCertificateConfig(),
                "certificateConfig is required for ROOT‑CA generation");
        Objects.requireNonNull(req.getOwnKeyPair(),
                "ownKeyPair is required for ROOT‑CA generation");

        Date[] validity = CertificateGeneratorHelper.calculateValidity(req.getCertificateConfig());
        X500Name issuer = CertificateGeneratorHelper.toX500(
                req.getCertificateConfig().getMetadata(),
                "A common name is required in root CA. Please change it in your settings.");

        X509v3CertificateBuilder builder = CertificateGeneratorHelper.createBuilder(
                issuer, validity[0], validity[1], issuer,
                req.getOwnKeyPair().getPublic().getEncoded());

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        CertificateGeneratorHelper.addCaKeyUsage(builder);
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.anyExtendedKeyUsage));

        ContentSigner signer = CertificateGeneratorHelper.createContentSigner(
                req.getOwnKeyPair().getPrivate());

        return CertificateGeneratorHelper.toCertificate(builder, signer, req.getOwnKeyPair());
    }
}

