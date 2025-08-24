package de.morihofi.certgine.cryptography.certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

import de.morihofi.certgine.cryptography.keys.KeyHelper;
import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Generates self-signed root CA certificates.
 */
public class RootCaGenerator implements ICertificateGenerator {

    @Override
    public X509Certificate generate(X509Generator.Request req)
            throws CertificateException, OperatorCreationException, CertIOException {
        Objects.requireNonNull(req.getCertificateConfig(), "certificateConfig is required for ROOT‑CA generation");
        Objects.requireNonNull(req.getOwnKeyPair(), "ownKeyPair is required for ROOT‑CA generation");

        Date[] validity = CertificateGeneratorHelper.calculateValidity(req.getCertificateConfig());
        X500Name issuer = CertificateGeneratorHelper.toX500(req.getCertificateConfig().getMetadata(),
                "A common name is required in root CA. Please change it in your settings.");

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuer,
                RandomGenerator.generateRandomId(),
                validity[0], validity[1],
                issuer,
                SubjectPublicKeyInfo.getInstance(req.getOwnKeyPair().getPublic().getEncoded()));

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment |
                        KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign | KeyUsage.cRLSign));

        ContentSigner signer = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getOwnKeyPair().getPrivate()))
                .build(req.getOwnKeyPair().getPrivate());

        return CertificateGeneratorHelper.toCertificate(builder, signer, req.getOwnKeyPair());
    }
}

