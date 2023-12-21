package de.morihofi.acmeserver.tools.certificate.generator;

import de.morihofi.acmeserver.config.CertificateConfig;
import de.morihofi.acmeserver.config.CertificateExpiration;
import de.morihofi.acmeserver.config.CertificateMetadata;
import de.morihofi.acmeserver.tools.certificate.CertMisc;
import de.morihofi.acmeserver.tools.certificate.X509;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

public class CertificateAuthorityGenerator {
    private CertificateAuthorityGenerator() {

    }

    /**
     * Generates an X509 certificate for a certificate authority (CA) based on the provided configuration and key pair.
     *
     * <p>This method creates a self-signed CA certificate using the provided configuration and key pair. It sets up
     * the certificate with basic constraints indicating it is a CA and includes various key usages such as
     * digital signature, key encipherment, and certificate signing. The certificate is signed with the private key
     * from the provided key pair using an appropriate signature algorithm based on the key type.</p>
     *
     * @param certificateConfig The configuration object containing the metadata and expiration details for the certificate.
     * @param keyPair           The key pair used for signing the certificate.
     * @return An X509Certificate representing the CA certificate.
     * @throws IOException               If an I/O error occurs during the certificate generation process.
     * @throws OperatorCreationException If there's an error during the creation of cryptographic operators.
     * @throws CertificateException      If there's an error in processing the certificate data.
     */
    public static X509Certificate generateCertificateAuthorityCertificate(CertificateConfig certificateConfig, KeyPair keyPair) throws IOException, OperatorCreationException, CertificateException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, certificateConfig.getExpiration().getYears());
        calendar.add(Calendar.MONTH, certificateConfig.getExpiration().getMonths());
        calendar.add(Calendar.DATE, certificateConfig.getExpiration().getDays());

        String organisation = certificateConfig.getMetadata().getOrganisation();
        String organisationalUnit = certificateConfig.getMetadata().getOrganisationalUnit();
        String countryCode = certificateConfig.getMetadata().getCountryCode();

        if (certificateConfig.getMetadata().getCommonName() == null || "".equals(certificateConfig.getMetadata().getCommonName())) {
            throw new IllegalArgumentException("A common name is required in root CA. Please change it in your settings.");
        }
        String issuerNameString = "CN=" + certificateConfig.getMetadata().getCommonName();
        if (organisation != null && !organisation.isEmpty()) {
            issuerNameString += ", O=" + organisation;
        }
        if (organisationalUnit != null && !organisationalUnit.isEmpty()) {
            issuerNameString += ", OU=" + organisationalUnit;
        }
        if (countryCode != null && !countryCode.isEmpty()) {
            issuerNameString += ", C=" + countryCode;
        }


        X500Name issuerName = new X500Name(issuerNameString);
        BigInteger serialNumber = CertMisc.generateSerialNumber();

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerName,
                serialNumber,
                new Date(),
                calendar.getTime(),
                issuerName,
                keyPair.getPublic()
        );

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature |
                        KeyUsage.nonRepudiation |
                        KeyUsage.keyEncipherment |
                        KeyUsage.dataEncipherment |
                        KeyUsage.keyAgreement |
                        KeyUsage.keyCertSign |
                        KeyUsage.cRLSign
        ));

        ContentSigner signer = new JcaContentSignerBuilder(CertMisc.getSignatureAlgorithmBasedOnKeyType(keyPair.getPrivate())).build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);

        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    /**
     * Generates an X509 certificate for an intermediate certificate authority (CA).
     *
     * <p>This method generates a certificate for an intermediate CA using the provided parameters. It includes
     * basic constraints indicating that the certificate is a CA, key usage extensions for various purposes
     * (such as digital signature, key encipherment, etc.), and extensions for CRL Distribution Points and
     * Authority Information Access. The certificate is signed using the private key of the root CA and the
     * appropriate signature algorithm.</p>
     *
     * @param cryptoStoreManager  Manager of the keyStore we use
     * @param intermediateKeyPair The key pair for the intermediate CA.
     * @param certificateMetadata The metadata for the intermediate CA certificate.
     * @param expiration          The expiration details for the certificate.
     * @param crlDistributionUrl  The URL for the Certificate Revocation List (CRL) distribution point.
     * @param ocspServiceEndpoint The URL for the Online Certificate Status Protocol (OCSP) service endpoint.
     * @return An X509Certificate representing the intermediate CA certificate.
     * @throws CertificateException      If there's an error in processing the certificate data.
     * @throws OperatorCreationException If there's an error during the creation of cryptographic operators.
     * @throws CertIOException           If there's an IO error during certificate generation.
     */
    public static X509Certificate createIntermediateCaCertificate(CryptoStoreManager cryptoStoreManager, KeyPair intermediateKeyPair, CertificateMetadata certificateMetadata, CertificateExpiration expiration, String crlDistributionUrl, String ocspServiceEndpoint) throws CertificateException, OperatorCreationException, CertIOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {

        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        X509Certificate caCertificate = (X509Certificate) keyStore.getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);

        X500Name issuerName = X509.getX500NameFromX509Certificate(caCertificate); // Consider getting this from CA certificate
        BigInteger serialNumber = CertMisc.generateSerialNumber();
        Date startDate = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, expiration.getYears());
        calendar.add(Calendar.MONTH, expiration.getMonths());
        calendar.add(Calendar.DATE, expiration.getDays());
        Date endDate = calendar.getTime();

        String organisation = certificateMetadata.getOrganisation();
        String organisationalUnit = certificateMetadata.getOrganisationalUnit();
        String countryCode = certificateMetadata.getCountryCode();

        if (certificateMetadata.getCommonName() == null || "".equals(certificateMetadata.getCommonName())) {
            throw new IllegalArgumentException("A common name is required in intermediate CA. Please change it in your settings.");
        }
        String issuerNameString = "CN=" + certificateMetadata.getCommonName();
        if (organisation != null && !organisation.isEmpty()) {
            issuerNameString += ", O=" + organisation;
        }
        if (organisationalUnit != null && !organisationalUnit.isEmpty()) {
            issuerNameString += ", OU=" + organisationalUnit;
        }
        if (countryCode != null && !countryCode.isEmpty()) {
            issuerNameString += ", C=" + countryCode;
        }

        X500Name subjectName = new X500Name(issuerNameString);
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuerName, serialNumber, startDate, endDate, subjectName,
                SubjectPublicKeyInfo.getInstance(intermediateKeyPair.getPublic().getEncoded())
        );

        // Basic Constraints
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));

        // Key Usage
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign | KeyUsage.cRLSign));

        // CRL Distribution Points
        GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, crlDistributionUrl);
        DistributionPointName dpn = new DistributionPointName(new GeneralNames(gn));
        DistributionPoint distp = new DistributionPoint(dpn, null, null);
        certBuilder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(new DistributionPoint[]{distp}));

        // Authority Information Access (OCSP Endpoint)
        AccessDescription accessDescription = new AccessDescription(AccessDescription.id_ad_ocsp, new GeneralName(GeneralName.uniformResourceIdentifier, ocspServiceEndpoint));
        ASN1EncodableVector authorityInformationAccessVector = new ASN1EncodableVector();
        authorityInformationAccessVector.add(accessDescription);
        certBuilder.addExtension(Extension.authorityInfoAccess, false, new DERSequence(authorityInformationAccessVector));

        // Signature Algorithm
        PrivateKey caPrivateKey = (PrivateKey) keyStore.getKey(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA, "".toCharArray());

        String signatureAlgorithm = CertMisc.getSignatureAlgorithmBasedOnKeyType(caPrivateKey);

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(caPrivateKey);
        X509CertificateHolder holder = certBuilder.build(signer);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);
        return converter.getCertificate(holder);
    }


}
