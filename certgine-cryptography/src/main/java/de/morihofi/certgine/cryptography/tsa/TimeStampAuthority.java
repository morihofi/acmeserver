package de.morihofi.certgine.cryptography.tsa;

import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampResponseGenerator;
import org.bouncycastle.tsp.TimeStampTokenGenerator;
import org.bouncycastle.tsp.TSPAlgorithms;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Simple RFC 3161 timestamp authority.
 */
public class TimeStampAuthority {
    private final PrivateKey privateKey;
    private final X509Certificate signingCert;
    private final List<X509Certificate> certificateChain;
    private final ASN1ObjectIdentifier policy;

    private static final Set<ASN1ObjectIdentifier> ALLOWED_ALGORITHMS = Set.of(
            TSPAlgorithms.MD5,
            TSPAlgorithms.RIPEMD160,
            TSPAlgorithms.SHA1,
            TSPAlgorithms.SHA224,
            TSPAlgorithms.SHA256,
            TSPAlgorithms.SHA384,
            TSPAlgorithms.SHA512
    );

    /**
     * Create a new instance.
     *
     * @param privateKey       signing key
     * @param signingCert      certificate used for signing
     * @param certificateChain full certificate chain including signer
     * @param policyOid        policy OID
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

        ASN1ObjectIdentifier reqAlg = request.getMessageImprintAlgOID();
        if (!ALLOWED_ALGORITHMS.contains(reqAlg)) {
            throw new org.bouncycastle.tsp.TSPException("Unsupported algorithm: " + reqAlg);
        }

        String signatureAlgorithm = getSignatureAlgorithmForHash(reqAlg);

        var sigInfo = new JcaSimpleSignerInfoGeneratorBuilder()
                .build(signatureAlgorithm, privateKey, signingCert);

        var digCalc = new JcaDigestCalculatorProviderBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build()
                .get(new AlgorithmIdentifier(reqAlg));

        TimeStampTokenGenerator tokenGen = new TimeStampTokenGenerator(sigInfo, digCalc, policy);
        tokenGen.addCertificates(new JcaCertStore(certificateChain));

        TimeStampResponseGenerator respGen = new TimeStampResponseGenerator(tokenGen, ALLOWED_ALGORITHMS);
        TimeStampResponse resp = respGen.generate(request, RandomGenerator.generateRandomId(), new Date());
        return resp.getEncoded();
    }

    /**
     * Maps digest algorithm OIDs to appropriate signature algorithm strings.
     */
    private String getSignatureAlgorithmForHash(ASN1ObjectIdentifier hashAlg) {
        return switch (hashAlg.getId()) {
            case "1.3.14.3.2.26" -> "SHA1withRSA";       // SHA-1
            case "2.16.840.1.101.3.4.2.4" -> "SHA224withRSA"; // SHA-224
            case "2.16.840.1.101.3.4.2.1" -> "SHA256withRSA"; // SHA-256
            case "2.16.840.1.101.3.4.2.2" -> "SHA384withRSA"; // SHA-384
            case "2.16.840.1.101.3.4.2.3" -> "SHA512withRSA"; // SHA-512
            case "1.3.36.3.2.1" -> "RIPEMD160withRSA";        // RIPEMD160
            case "1.2.840.113549.2.5" -> "MD5withRSA";         // MD5
            default -> throw new IllegalArgumentException("Unsupported hash algorithm: " + hashAlg);
        };
    }

    public static String getHashAlgorithmName(ASN1ObjectIdentifier alg) {
        return switch (alg.getId()) {
            case "1.2.840.113549.2.5" -> "MD5";
            case "1.3.36.3.2.1" -> "RIPEMD160";
            case "1.3.14.3.2.26" -> "SHA-1";
            case "2.16.840.1.101.3.4.2.4" -> "SHA-224";
            case "2.16.840.1.101.3.4.2.1" -> "SHA-256";
            case "2.16.840.1.101.3.4.2.2" -> "SHA-384";
            case "2.16.840.1.101.3.4.2.3" -> "SHA-512";
            default -> throw new IllegalArgumentException("Unsupported hash algorithm: " + alg);
        };
    }

}
