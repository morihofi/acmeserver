package de.morihofi.certgine.cryptography.ocsp;

import de.morihofi.certgine.cryptography.keys.KeyHelper;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * OCSP utility functions.
 */
@Slf4j
public final class OcspProcessor {

    private OcspProcessor() {
    }

    /**
     * Creates an OCSP response for the given certificate serial number.
     *
     * @param serialNumber       certificate serial number
     * @param revokedCertificate revocation information or {@code null} if the certificate is valid
     * @param caCert             issuing CA certificate
     * @param caKeyPair          key pair of the issuing CA
     * @return OCSP response for the certificate
     * @throws OCSPException                if OCSP processing fails
     * @throws CertificateEncodingException if the CA certificate cannot be encoded
     * @throws OperatorCreationException    if the signer cannot be created
     */
    public static OCSPResp processOCSPRequest(BigInteger serialNumber,
                                              RevokedCertificate revokedCertificate,
                                              @NonNull X509Certificate caCert,
                                              @NonNull KeyPair caKeyPair)
            throws OCSPException, CertificateEncodingException, OperatorCreationException {

        CertificateStatus certStatus;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
        if (revokedCertificate != null) {
            certStatus = new RevokedStatus(Date.from(revokedCertificate.revocationDate()),
                    revokedCertificate.revocationReason());
            log.debug("Certificate {} revoked at {}", serialNumber,
                    formatter.format(revokedCertificate.revocationDate()));
        } else {
            certStatus = CertificateStatus.GOOD;
        }

        log.info("Status for serial number {} is: {}", serialNumber,
                certStatus != CertificateStatus.GOOD ? "revoked" : "valid");

        SubjectPublicKeyInfo caPublicKeyInfo = SubjectPublicKeyInfo.getInstance(caCert.getPublicKey().getEncoded());
        DigestCalculator digCalc = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        BasicOCSPRespBuilder respBuilder = new BasicOCSPRespBuilder(caPublicKeyInfo, digCalc);

        respBuilder.addResponse(new CertificateID(digCalc,
                new JcaX509CertificateHolder(caCert), serialNumber), certStatus);

        Instant producedAt = Instant.now();
        BasicOCSPResp basicResp = respBuilder.build(
                new JcaContentSignerBuilder(KeyHelper.getSignatureAlgorithmBasedOnKeyType(caKeyPair.getPrivate()))
                        .build(caKeyPair.getPrivate()),
                new X509CertificateHolder[]{new JcaX509CertificateHolder(caCert)},
                Date.from(producedAt));

        log.debug("OCSP response produced at {}", formatter.format(producedAt));

        return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicResp);
    }
}
