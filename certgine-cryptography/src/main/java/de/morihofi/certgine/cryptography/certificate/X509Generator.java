/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.certificate;

import de.morihofi.certgine.cryptography.keys.KeyHelper;
import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import de.morihofi.certgine.types.api.acme.dns.Identifier;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Single entry point for creating every certificate variant (ROOT‑CA, INTERMEDIATE‑CA, SERVER)
 * with a single {@link Request} generated via Lombok's builder.
 */
public class X509Generator {

    /**
     * Describes the certificate flavour that should be produced.
     */
    public enum Type {
        ROOT_CA,
        INTERMEDIATE_CA,
        SERVER,
        TIMESTAMPING,
        CODE_SIGNING
    }

    /**
     * Immutable request object that gathers **all** inputs required by the three generation
     * paths. Only the relevant subset must be filled for a concrete {@link Type}. Validation
     * happens at runtime to keep the public API minimal.
     */
    @Getter
    @Builder
    public static class Request {
        /** Certificate flavour that shall be generated. */
        @NonNull private final Type type;

        /* ——— Common parameters ——— */
        private final CertificateConfig certificateConfig;
        private final IServerInstance serverInstance;

        /* ——— Key material ——— */
        private final KeyPair ownKeyPair;                 // generated key‑pair for the certificate itself (ROOT + INTERMEDIATE)
        private final KeyPair issuerKeyPair;              // key‑pair of the issuer/parent (INTERMEDIATE + SERVER)
        private final X509Certificate issuerCertificate;  // cert of the issuer/parent (INTERMEDIATE + SERVER)

        /* ——— ROOT/INTERMEDIATE specific ——— */
        private final String crlDistributionUrl;          // INTERMEDIATE (optional for ROOT)
        private final String ocspServiceEndpoint;         // INTERMEDIATE (optional for ROOT)

        /* ——— Server‑certificate specifics ——— */
        private final byte[] serverPublicKeyBytes;
        @Singular private final List<Identifier> identifiers;
        private final Date startDate;
        private final Date endDate;

    }


    // -------------------------------------------------------------------------------------------------
    // Public façade
    // -------------------------------------------------------------------------------------------------

    /**
     * Creates the requested X509 certificate.
     */
    public static X509Certificate generate(@NonNull Request req)
            throws CertificateException, OperatorCreationException, CertIOException {
        return switch (req.getType()) {
            case ROOT_CA -> generateRootCa(req);
            case INTERMEDIATE_CA -> generateIntermediateCa(req);
            case SERVER -> generateServer(req);
            case TIMESTAMPING -> generateTimestampAuthority(req);
            case CODE_SIGNING -> generateCodeSigning(req);
        };
    }

    // -------------------------------------------------------------------------------------------------
    // Concrete generators
    // -------------------------------------------------------------------------------------------------

    private static X509Certificate generateRootCa(Request req)
            throws CertificateException, OperatorCreationException, CertIOException {

        Objects.requireNonNull(req.getCertificateConfig(), "certificateConfig is required for ROOT‑CA generation");
        Objects.requireNonNull(req.getOwnKeyPair(), "ownKeyPair is required for ROOT‑CA generation");

        Date[] validity = calculateValidity(req.getCertificateConfig());
        X500Name issuer = toX500(req.getCertificateConfig().getMetadata(),
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
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.anyExtendedKeyUsage));

        ContentSigner signer = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getOwnKeyPair().getPrivate()))
                .build(req.getOwnKeyPair().getPrivate());

        return toCertificate(builder, signer, req.getOwnKeyPair());
    }

    private static X509Certificate generateIntermediateCa(Request req)
            throws CertificateException, OperatorCreationException, CertIOException {

        Objects.requireNonNull(req.getIssuerKeyPair(), "issuerKeyPair (ROOT key) is required for INTERMEDIATE generation");
        Objects.requireNonNull(req.getIssuerCertificate(), "issuerCertificate (ROOT cert) is required for INTERMEDIATE generation");
        validateIssuerForIntermediate(req.getIssuerCertificate());
        Objects.requireNonNull(req.getOwnKeyPair(), "ownKeyPair (INTERMEDIATE key) is required");
        Objects.requireNonNull(req.getCertificateConfig(), "certificateConfig is required");

        Date[] validity = calculateValidity(req.getCertificateConfig());
        X500Name issuerName = X509CertificateTools.getX500NameFromX509Certificate(req.getIssuerCertificate());
        X500Name subjectName = toX500(req.getCertificateConfig().getMetadata(),
                "A common name is required in intermediate CA. Please change it in your settings.");

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuerName,
                RandomGenerator.generateRandomId(),
                validity[0], validity[1],
                subjectName,
                SubjectPublicKeyInfo.getInstance(req.getOwnKeyPair().getPublic().getEncoded()));

        // basic constraints: CA with path length 0
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment |
                        KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign | KeyUsage.cRLSign));

        addCrlAndOcsp(builder, req.getCrlDistributionUrl(), req.getOcspServiceEndpoint());

        ContentSigner signer = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getIssuerKeyPair().getPrivate()))
                .build(req.getIssuerKeyPair().getPrivate());

        return toCertificate(builder, signer, req.getIssuerKeyPair());
    }

    private static X509Certificate generateServer(Request req)
            throws CertificateException, OperatorCreationException, CertIOException {

        Objects.requireNonNull(req.getIssuerKeyPair(), "issuerKeyPair (INTERMEDIATE key) is required");
        Objects.requireNonNull(req.getIssuerCertificate(), "issuerCertificate (INTERMEDIATE cert) is required");
        validateIssuerIsCa(req.getIssuerCertificate());
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

        // Subject Alternative Names
        List<GeneralName> altNames = new ArrayList<>();
        req.getIdentifiers().forEach(id -> {
            switch (id.getTypeAsEnumConstant()) {
                case DNS -> altNames.add(new GeneralName(GeneralName.dNSName, id.getValue()));
                case IP -> altNames.add(new GeneralName(GeneralName.iPAddress, id.getValue()));
            }
        });
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(altNames.toArray(new GeneralName[0])));

        addCrlAndOcsp(builder, req.crlDistributionUrl, req.ocspServiceEndpoint);


        ContentSigner signer = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getIssuerKeyPair().getPrivate()))
                .build(req.getIssuerKeyPair().getPrivate());

        return toCertificate(builder, signer, req.getIssuerKeyPair());
    }

    private static X509Certificate generateTimestampAuthority(Request req)
            throws CertificateException, OperatorCreationException, CertIOException {

        Objects.requireNonNull(req.getIssuerKeyPair(), "issuerKeyPair (ROOT key) is required");
        Objects.requireNonNull(req.getIssuerCertificate(), "issuerCertificate (ROOT cert) is required");
        validateIssuerIsCa(req.getIssuerCertificate());
        Objects.requireNonNull(req.getOwnKeyPair(), "ownKeyPair is required");
        Objects.requireNonNull(req.getCertificateConfig(), "certificateConfig is required");

        Date[] validity = calculateValidity(req.getCertificateConfig());
        X500Name issuer = X509CertificateTools.getX500NameFromX509Certificate(req.getIssuerCertificate());
        X500Name subject = toX500(req.getCertificateConfig().getMetadata(),
                "A common name is required in timestamping authority. Please change it in your settings.");

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuer,
                RandomGenerator.generateRandomId(),
                validity[0], validity[1],
                subject,
                SubjectPublicKeyInfo.getInstance(req.getOwnKeyPair().getPublic().getEncoded()));

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature));
        builder.addExtension(Extension.extendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

        ContentSigner signer = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getIssuerKeyPair().getPrivate()))
                .build(req.getIssuerKeyPair().getPrivate());

        return toCertificate(builder, signer, req.getIssuerKeyPair());
    }

    private static X509Certificate generateCodeSigning(Request req)
            throws CertificateException, OperatorCreationException, CertIOException {

        Objects.requireNonNull(req.getIssuerKeyPair(), "issuerKeyPair (ROOT key) is required");
        Objects.requireNonNull(req.getIssuerCertificate(), "issuerCertificate (ROOT cert) is required");
        validateIssuerIsCa(req.getIssuerCertificate());
        Objects.requireNonNull(req.getOwnKeyPair(), "ownKeyPair is required");
        Objects.requireNonNull(req.getCertificateConfig(), "certificateConfig is required");

        Date[] validity = calculateValidity(req.getCertificateConfig());
        X500Name issuer = X509CertificateTools.getX500NameFromX509Certificate(req.getIssuerCertificate());
        X500Name subject = toX500(req.getCertificateConfig().getMetadata(),
                "A common name is required in code signing certificate. Please change it in your settings.");

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuer,
                RandomGenerator.generateRandomId(),
                validity[0], validity[1],
                subject,
                SubjectPublicKeyInfo.getInstance(req.getOwnKeyPair().getPublic().getEncoded()));

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        builder.addExtension(Extension.extendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_codeSigning));

        ContentSigner signer = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getIssuerKeyPair().getPrivate()))
                .build(req.getIssuerKeyPair().getPrivate());

        return toCertificate(builder, signer, req.getIssuerKeyPair());
    }

    // -------------------------------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------------------------------

    private static void validateIssuerIsCa(X509Certificate issuer) {
        if (issuer.getBasicConstraints() < 0) {
            throw new IllegalArgumentException("issuerCertificate is not a CA certificate");
        }
    }

    private static void validateIssuerForIntermediate(X509Certificate issuer) {
        validateIssuerIsCa(issuer);
        int pathLen = issuer.getBasicConstraints();
        if (pathLen < 1) {
            throw new IllegalArgumentException(
                    "issuerCertificate path length constraint prohibits issuing an intermediate certificate");
        }
    }

    private static Date[] calculateValidity(CertificateConfig config) {
        Calendar cal = Calendar.getInstance();
        Date start = cal.getTime();
        cal.add(Calendar.YEAR, config.getExpiration().getYears());
        cal.add(Calendar.MONTH, config.getExpiration().getMonths());
        cal.add(Calendar.DATE, config.getExpiration().getDays());
        return new Date[]{start, cal.getTime()};
    }

    private static X509Certificate toCertificate(X509v3CertificateBuilder builder,
                                                 ContentSigner signer,
                                                 KeyPair signerKey)
            throws CertificateException {
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(holder);
        try {
            cert.verify(signerKey.getPublic());
        } catch (Exception e) {
            throw new CertificateException("Generated certificate verification failed", e);
        }
        return cert;
    }

    private static void addCrlAndOcsp(X509v3CertificateBuilder builder,
                                      String crlUrl,
                                      String ocspUrl) throws CertIOException {
        if (crlUrl != null) {
            GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, crlUrl);
            DistributionPointName dpn = new DistributionPointName(new GeneralNames(gn));
            builder.addExtension(Extension.cRLDistributionPoints, false,
                    new CRLDistPoint(new DistributionPoint[]{new DistributionPoint(dpn, null, null)}));
        }
        if (ocspUrl != null) {
            AccessDescription ad = new AccessDescription(AccessDescription.id_ad_ocsp,
                    new GeneralName(GeneralName.uniformResourceIdentifier, ocspUrl));
            ASN1EncodableVector vec = new ASN1EncodableVector();
            vec.add(ad);
            builder.addExtension(Extension.authorityInfoAccess, false, new DERSequence(vec));
        }
    }

    private static X500Name toX500(CertificateMetadata meta, String errorMsg) {
        if (meta.getCommonName() == null || meta.getCommonName().isEmpty()) {
            throw new IllegalArgumentException(errorMsg);
        }
        StringBuilder sb = new StringBuilder("CN=" + meta.getCommonName());
        if (meta.getOrganisation() != null && !meta.getOrganisation().isEmpty()) {
            sb.append(", O=").append(meta.getOrganisation());
        }
        if (meta.getOrganisationalUnit() != null && !meta.getOrganisationalUnit().isEmpty()) {
            sb.append(", OU=").append(meta.getOrganisationalUnit());
        }
        if (meta.getCountryCode() != null && !meta.getCountryCode().isEmpty()) {
            sb.append(", C=").append(meta.getCountryCode());
        }
        return new X500Name(sb.toString());
    }
}

