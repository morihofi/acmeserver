package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.tools.certificate.CertMisc;
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
import java.util.Date;

public class OcspEndpoint implements Handler {

    private final Provisioner provisioner;
    private final CRL crlGenerator;
    public final Logger log = LogManager.getLogger(getClass());


    public OcspEndpoint(Provisioner provisioner, CRL crlGenerator) {
        this.provisioner = provisioner;
        this.crlGenerator = crlGenerator;
    }


    /**
     * Handles an HTTP request for OCSP (Online Certificate Status Protocol) by processing the provided OCSP request,
     * checking the revocation status for the specified certificate serial number, and sending the corresponding OCSP response.
     *
     * @param context The Context object representing the HTTP request and response.
     * @throws Exception if there is an issue with handling the HTTP request or processing the OCSP request.
     */
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

        // Processing the request and creating the OCSP response
        OCSPResp ocspResponse = processOCSPRequest(serialNumber);

        // Sending the OCSP response
        context.contentType("application/ocsp-response");
        context.result(ocspResponse.getEncoded());
    }

    /**
     * Processes an OCSP (Online Certificate Status Protocol) request for a given certificate serial number.
     * This method checks the status of the certificate using the current Certificate Revocation List (CRL)
     * and generates an OCSP response accordingly.
     *
     * @param serialNumber The serial number of the certificate for which the OCSP response is requested.
     * @return An OCSPResp object representing the OCSP response for the given certificate.
     * @throws OCSPException if there is an issue with OCSP processing.
     * @throws CRLException if there is an issue with CRL processing.
     * @throws CertificateEncodingException if there is an issue with encoding certificates.
     * @throws OperatorCreationException if there is an issue with operator creation.
     */
    private OCSPResp processOCSPRequest(BigInteger serialNumber) throws OCSPException, CRLException, CertificateEncodingException, OperatorCreationException {
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

        log.info("Status for serial number " + serialNumber + " is: " + (certStatus != null ? "revoked" : "valid"));

        X509Certificate caCert = provisioner.getIntermediateCaCertificate();
        KeyPair caKeyPair = provisioner.getIntermediateKeyPair();


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
}
