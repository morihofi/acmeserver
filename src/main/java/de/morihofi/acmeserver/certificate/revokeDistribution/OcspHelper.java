package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.tools.certificate.CertMisc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x509.CRLReason;
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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.*;
import java.util.Date;

public class OcspHelper {

    public static final Logger log = LogManager.getLogger(OcspHelper.class);


    /**
     * Processes an OCSP (Online Certificate Status Protocol) request for a given certificate serial number.
     * This method checks the status of the certificate using the current Certificate Revocation List (CRL)
     * and generates an OCSP response accordingly.
     *
     * @param serialNumber The serial number of the certificate for which the OCSP response is requested.
     * @param crlGenerator CRL Generator Instance
     * @param provisioner Provisioner Instance
     * @return An OCSPResp object representing the OCSP response for the given certificate.
     * @throws OCSPException if there is an issue with OCSP processing.
     * @throws CRLException if there is an issue with CRL processing.
     * @throws CertificateEncodingException if there is an issue with encoding certificates.
     * @throws OperatorCreationException if there is an issue with operator creation.
     */
    public static OCSPResp processOCSPRequest(BigInteger serialNumber, CRLGenerator crlGenerator, Provisioner provisioner) throws OCSPException, CRLException, CertificateEncodingException, OperatorCreationException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        CertificateStatus certStatus = getCertificateStatus(serialNumber, crlGenerator);

        log.info("Status for serial number {} is: {}", serialNumber, (certStatus != null ? "revoked" : "valid"));

        X509Certificate caCert = provisioner.getIntermediateCaCertificate();
        KeyPair caKeyPair = provisioner.getIntermediateCaKeyPair();


        // Creating the OCSP response
        SubjectPublicKeyInfo caPublicKeyInfo = SubjectPublicKeyInfo.getInstance(caCert.getPublicKey().getEncoded());
        DigestCalculator digCalc = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        BasicOCSPRespBuilder respBuilder = new BasicOCSPRespBuilder(caPublicKeyInfo, digCalc);

        respBuilder.addResponse(new CertificateID(digCalc,
                new JcaX509CertificateHolder(caCert), serialNumber), certStatus);

        // Creating and signing the OCSP response
        BasicOCSPResp basicResp = respBuilder.build(new JcaContentSignerBuilder(CertMisc.getSignatureAlgorithmBasedOnKeyType(caKeyPair.getPrivate())).build(caKeyPair.getPrivate()),
                new X509CertificateHolder[]{new JcaX509CertificateHolder(caCert)},
                new Date());

        return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicResp);
    }

    /**
     * Determines the status of a certificate by its serial number, using a provided Certificate Revocation List (CRL).
     * This method checks if the specified certificate has been revoked according to the current CRL provided by the
     * {@code crlGenerator}. If the certificate is found in the CRL, it is considered revoked, and the method returns a
     * {@link RevokedStatus} instance containing the revocation date and reason. If the certificate is not found in the CRL,
     * it is considered valid, and the method returns {@link CertificateStatus#GOOD}.
     * <p>
     * The method uses the {@code serialNumber} to look up the certificate in the CRL. The revocation reason is determined
     * by checking if the revoked certificate entry has extensions; if so, it uses the ordinal of the {@link CRLReason} enum
     * value. If there are no extensions, the reason defaults to {@code CRLReason.unspecified}.
     *
     * @param serialNumber The serial number of the certificate to check the status for.
     * @param crlGenerator The CRL generator instance used to obtain the current CRL.
     * @return A {@link CertificateStatus} indicating whether the certificate is valid or revoked. If revoked, additional
     *         details such as the revocation date and reason are provided.
     * @throws CRLException If there is an issue obtaining the current CRL from the {@code crlGenerator}.
     */
    private static CertificateStatus getCertificateStatus(BigInteger serialNumber, CRLGenerator crlGenerator) throws CRLException {
        X509CRL crl = crlGenerator.getCurrentCrl(); // Current CRL

        CertificateStatus certStatus;
        X509CRLEntry revokedCertificate = crl.getRevokedCertificate(serialNumber);
        // Checking the certificate status using the CRL
        if (revokedCertificate != null) {
            // Certificate has been revoked
            Date revocationDate = revokedCertificate.getRevocationDate();
            int revocationReason = revokedCertificate.hasExtensions() ? revokedCertificate.getRevocationReason().ordinal() : CRLReason.unspecified;
            certStatus = new RevokedStatus(revocationDate, revocationReason);
        } else {
            // Certificate is valid
            certStatus = CertificateStatus.GOOD;
        }
        return certStatus;
    }
}
