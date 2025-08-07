package de.morihofi.certgine.cryptography.certificate;

import de.morihofi.certgine.cryptography.keys.KeyHelper;
import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.types.dns.DnsIdentifier;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * Internal utility methods used by the different certificate generators.
 */
final class CertificateGeneratorHelper {

    private CertificateGeneratorHelper() {
    }

    static Date[] calculateValidity(CertificateConfig config) {
        Calendar cal = Calendar.getInstance();
        Date start = cal.getTime();
        cal.add(Calendar.YEAR, config.getExpiration().getYears());
        cal.add(Calendar.MONTH, config.getExpiration().getMonths());
        cal.add(Calendar.DATE, config.getExpiration().getDays());
        return new Date[] {start, cal.getTime()};
    }

    static X509v3CertificateBuilder createBuilder(X500Name issuer, Date start, Date end,
            X500Name subject, byte[] publicKey) {
        return new X509v3CertificateBuilder(
                issuer,
                RandomGenerator.generateRandomId(),
                start, end,
                subject,
                SubjectPublicKeyInfo.getInstance(publicKey));
    }

    static X509Certificate toCertificate(X509v3CertificateBuilder builder, ContentSigner signer,
            KeyPair signerKey) throws CertificateException {
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

    static void addCrlAndOcsp(X509v3CertificateBuilder builder, String crlUrl, String ocspUrl)
            throws CertIOException {
        if (crlUrl != null) {
            GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, crlUrl);
            DistributionPointName dpn = new DistributionPointName(new GeneralNames(gn));
            builder.addExtension(Extension.cRLDistributionPoints, false,
                    new CRLDistPoint(new DistributionPoint[] {new DistributionPoint(dpn, null, null)}));
        }
        if (ocspUrl != null) {
            AccessDescription ad = new AccessDescription(AccessDescription.id_ad_ocsp,
                    new GeneralName(GeneralName.uniformResourceIdentifier, ocspUrl));
            ASN1EncodableVector vec = new ASN1EncodableVector();
            vec.add(ad);
            builder.addExtension(Extension.authorityInfoAccess, false, new DERSequence(vec));
        }
    }

    static X500Name toX500(CertificateMetadata meta, String errorMsg) {
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

    static void addCaKeyUsage(X509v3CertificateBuilder builder) throws CertIOException {
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment
                        | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign
                        | KeyUsage.cRLSign));
    }

    static ContentSigner createContentSigner(PrivateKey privateKey)
            throws OperatorCreationException {
        return new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(privateKey))
                .build(privateKey);
    }

    static void validateIssuerIsCa(X509Certificate issuer) {
        if (issuer.getBasicConstraints() < 0) {
            throw new IllegalArgumentException("issuerCertificate is not a CA certificate");
        }
    }

    static void validateIssuerForIntermediate(X509Certificate issuer) {
        validateIssuerIsCa(issuer);
        int pathLen = issuer.getBasicConstraints();
        if (pathLen < 1) {
            throw new IllegalArgumentException(
                    "issuerCertificate path length constraint prohibits issuing an intermediate certificate");
        }
    }

    static void addSubjectAlternativeNames(X509v3CertificateBuilder builder, List<DnsIdentifier> ids)
            throws CertIOException {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<GeneralName> altNames = new java.util.ArrayList<>();
        ids.forEach(id -> {
            switch (id.getType()) {
                case DNS -> altNames.add(new GeneralName(GeneralName.dNSName, id.getValue()));
                case IP -> altNames.add(new GeneralName(GeneralName.iPAddress, id.getValue()));
            }
        });
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(altNames.toArray(new GeneralName[0])));
    }
}

