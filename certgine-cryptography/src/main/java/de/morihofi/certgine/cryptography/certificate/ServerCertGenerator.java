package de.morihofi.certgine.cryptography.certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
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

import static de.morihofi.certgine.cryptography.certificate.CertificateGeneratorHelper.toCertificate;

/**
 * Generates TLS server certificates.
 */
public class ServerCertGenerator implements ICertificateGenerator {

    @Override
    public X509Certificate generate(X509Generator.Request req)
            throws CertificateException, OperatorCreationException, CertIOException {

        Objects.requireNonNull(req.getIssuerKeyPair(), "issuerKeyPair (INTERMEDIATE key) is required");
        Objects.requireNonNull(req.getIssuerCertificate(), "issuerCertificate (INTERMEDIATE cert) is required");
        CertificateGeneratorHelper.validateIssuerIsCa(req.getIssuerCertificate());
        Objects.requireNonNull(req.getServerPublicKeyBytes(), "serverPublicKeyBytes is required");
        Objects.requireNonNull(req.getIdentifiers(), "identifiers is required");
        Objects.requireNonNull(req.getStartDate(), "startDate is required");
        Objects.requireNonNull(req.getEndDate(), "endDate is required");

        X500Name issuerName = X509CertificateTools.getX500NameFromX509Certificate(req.getIssuerCertificate());
        X500Name subjectName = new X500Name("CN=" + req.getIdentifiers().getFirst().getValue());

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuerName,
                RandomGenerator.generateRandomId(),
                req.getStartDate(), req.getEndDate(),
                subjectName,
                SubjectPublicKeyInfo.getInstance(req.getServerPublicKeyBytes()));

        // Basic Constraints - Not a CA
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        // Key Usage
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        // Server Authentication
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(new KeyPurposeId[] {
                        KeyPurposeId.id_kp_serverAuth
                }));


        // Subject Alternative Names
        List<GeneralName> altNames = new ArrayList<>();
        req.getIdentifiers().forEach(id -> {
            switch (id.getType()) {
                case DNS -> altNames.add(new GeneralName(GeneralName.dNSName, id.getValue()));
                case IP -> altNames.add(new GeneralName(GeneralName.iPAddress, id.getValue()));
            }
        });
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(altNames.toArray(new GeneralName[0])));

        CertificateGeneratorHelper.addCrlAndOcsp(builder, req.getCrlDistributionUrl(), req.getOcspServiceEndpoint());


        ContentSigner signer = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getIssuerKeyPair().getPrivate()))
                .build(req.getIssuerKeyPair().getPrivate());

        return toCertificate(builder, signer, req.getIssuerKeyPair());
    }
}

