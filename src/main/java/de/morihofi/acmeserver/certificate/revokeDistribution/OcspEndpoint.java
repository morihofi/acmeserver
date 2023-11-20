package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import io.javalin.http.Context;
import io.javalin.http.Handler;
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
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

public class OcspEndpoint implements Handler {

    private final Provisioner provisioner;
    private final CRL crlGenerator;
    public final Logger log = LogManager.getLogger(getClass());


    public OcspEndpoint(Provisioner provisioner, CRL crlGenerator) {
        this.provisioner = provisioner;
        this.crlGenerator = crlGenerator;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        byte[] ocspRequestBytes = context.bodyAsBytes();
        OCSPReq ocspRequest = new OCSPReq(ocspRequestBytes);

        // Erhalten der Seriennummer aus der Anfrage
        Req[] requestList = ocspRequest.getRequestList();
        if (requestList.length == 0) {
            throw new IllegalArgumentException("No request data in the OCSP request");
        }

        BigInteger serialNumber = requestList[0].getCertID().getSerialNumber();
        log.info("Checking revokation status for serial number " + serialNumber);

        // Verarbeiten der Anfrage und Erstellen der OCSP-Antwort
        OCSPResp ocspResponse = processOCSPRequest(serialNumber);

        // Senden der OCSP-Antwort
        context.contentType("application/ocsp-response");
        context.result(ocspResponse.getEncoded());
    }

    private OCSPResp processOCSPRequest(BigInteger serialNumber) throws OCSPException, CRLException, CertificateEncodingException, OperatorCreationException {
        // Annahme: Sie haben Zugriff auf Ihre CRL und CA-Informationen
        X509CRL crl = crlGenerator.getCurrentCrl(); // Ihre aktuelle CRL

        CertificateStatus certStatus;
        X509CRLEntry revokedCertificate = crl.getRevokedCertificate(serialNumber);
        // Überprüfung des Zertifikatsstatus anhand der CRL
        if (revokedCertificate != null) {
            // Zertifikat wurde widerrufen
            Date revocationDate = revokedCertificate.getRevocationDate();
            int revocationReason = revokedCertificate.hasExtensions() ? revokedCertificate.getRevocationReason().ordinal() : CRLReason.unspecified;
            certStatus = new RevokedStatus(revocationDate, revocationReason);
        } else {
            // Zertifikat ist gültig
            certStatus = CertificateStatus.GOOD;
        }

        log.info("Status for serial number " + serialNumber + " is: " + (certStatus != null ? "revoked" : "valid"));

        X509Certificate caCert = provisioner.getIntermediateCaCertificate();
        KeyPair caKeyPair = provisioner.getIntermediateKeyPair();

        // Bestimmung des Signaturalgorithmus basierend auf dem Schlüsseltyp
        String signatureAlgorithm;
        if (caKeyPair.getPrivate() instanceof RSAPrivateKey) {
            signatureAlgorithm = "SHA256withRSA";
        } else if (caKeyPair.getPrivate() instanceof ECPrivateKey) {
            signatureAlgorithm = "SHA256withECDSA";
        } else {
            throw new IllegalArgumentException("Unsupported key type");
        }

        // Erstellen der OCSP-Antwort
        SubjectPublicKeyInfo caPublicKeyInfo = SubjectPublicKeyInfo.getInstance(caCert.getPublicKey().getEncoded());
        DigestCalculator digCalc = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        BasicOCSPRespBuilder respBuilder = new BasicOCSPRespBuilder(caPublicKeyInfo, digCalc);

        respBuilder.addResponse(new CertificateID(digCalc,
                new JcaX509CertificateHolder(caCert), serialNumber), certStatus);

        // Erstellen und Signieren der OCSP-Antwort
        BasicOCSPResp basicResp = respBuilder.build(new JcaContentSignerBuilder(signatureAlgorithm).build(caKeyPair.getPrivate()),
                new X509CertificateHolder[]{new JcaX509CertificateHolder(caCert)},
                new Date());

        return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicResp);
    }
}
