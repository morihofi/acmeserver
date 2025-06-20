package de.morihofi.acmeserver.cryptography.tsa;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampResponseGenerator;
import org.bouncycastle.tsp.TimeStampTokenGenerator;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

/**
 * Simple RFC 3161 timestamp authority.
 */
public class TimeStampAuthority {
    private final PrivateKey privateKey;
    private final X509Certificate signingCert;
    private final List<X509Certificate> certificateChain;
    private final ASN1ObjectIdentifier policy;

    /**
     * Create a new instance.
     *
     * @param privateKey        signing key
     * @param signingCert       certificate used for signing
     * @param certificateChain  full certificate chain including signer
     * @param policyOid         policy OID
     */
    public TimeStampAuthority(PrivateKey privateKey, X509Certificate signingCert,
                              List<X509Certificate> certificateChain, String policyOid) {
        this.privateKey = privateKey;
        this.signingCert = signingCert;
        this.certificateChain = certificateChain;
        this.policy = new ASN1ObjectIdentifier(policyOid);
    }

    /**
     * Generate a timestamp response.
     *
     * @param request parsed timestamp request
     * @return encoded timestamp response bytes
     * @throws OperatorCreationException when signing fails
     * @throws CertificateException      when certificate store fails
     * @throws IOException               when generation fails
     */
    public byte[] generate(TimeStampRequest request)
            throws OperatorCreationException, CertificateException, IOException,
            org.bouncycastle.tsp.TSPException {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(privateKey);
        var sigInfo = new JcaSimpleSignerInfoGeneratorBuilder()
                .build("SHA256withRSA", privateKey, signingCert);
        var digCalc = new org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder()
                .setProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME)
                .build()
                .get(new org.bouncycastle.asn1.x509.AlgorithmIdentifier(org.bouncycastle.tsp.TSPAlgorithms.SHA256));
        TimeStampTokenGenerator tokenGen = new TimeStampTokenGenerator(sigInfo, digCalc, policy);
        tokenGen.addCertificates(new JcaCertStore(certificateChain));
        java.util.Set<org.bouncycastle.asn1.ASN1ObjectIdentifier> algorithms =
                java.util.Collections.singleton(org.bouncycastle.tsp.TSPAlgorithms.SHA256);
        TimeStampResponseGenerator respGen = new TimeStampResponseGenerator(tokenGen, algorithms);
        TimeStampResponse resp = respGen.generate(request, BigInteger.valueOf(System.currentTimeMillis()), new Date());
        return resp.getEncoded();
    }
}
