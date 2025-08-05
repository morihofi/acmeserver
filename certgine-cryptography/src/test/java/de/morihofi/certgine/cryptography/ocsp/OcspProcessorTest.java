package de.morihofi.certgine.cryptography.ocsp;

import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class OcspProcessorTest {

    private static final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509Certificate createCaCert(KeyPair kp) throws Exception {
        return X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .ownKeyPair(kp)
                .build());
    }

    @Test
    @DisplayName("processOCSPRequest returns revoked status")
    void testProcessRevoked() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate caCert = createCaCert(kp);
        BigInteger serial = BigInteger.ONE;
        RevokedCertificate rc = new RevokedCertificate(serial, clock.instant(), 0);
        OCSPResp resp = OcspProcessor.processOCSPRequest(serial, rc, caCert, kp);
        assertEquals(OCSPRespBuilder.SUCCESSFUL, resp.getStatus());
        BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
        SingleResp sr = basic.getResponses()[0];
        assertTrue(sr.getCertStatus() instanceof RevokedStatus);
    }

    @Test
    @DisplayName("processOCSPRequest returns good status when not revoked")
    void testProcessGood() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate caCert = createCaCert(kp);
        BigInteger serial = BigInteger.TWO;
        OCSPResp resp = OcspProcessor.processOCSPRequest(serial, null, caCert, kp);
        BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
        SingleResp sr = basic.getResponses()[0];
        assertEquals(CertificateStatus.GOOD, sr.getCertStatus());
    }
}
