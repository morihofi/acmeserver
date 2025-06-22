package de.morihofi.acmeserver.cryptography.crl;

import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CrlGeneratorTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName(cn)
                .organisation("Org")
                .countryCode("DE")
                .build();
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }

    @Test
    @DisplayName("generate creates CRL with entries")
    void testGenerate() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate caCert = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.ROOT_CA)
                        .certificateConfig(cfg("Test CA"))
                        .ownKeyPair(kp)
                        .build());

        RevokedCertificate rc = new RevokedCertificate(BigInteger.ONE, new Date(), 0);

        X509CRL crl = CrlGenerator.generate(
                CrlGenerator.Request.builder()
                        .revokedCertificate(rc)
                        .caCert(caCert)
                        .caPrivateKey(kp.getPrivate())
                        .updateMinutes(5)
                        .build());

        assertNotNull(crl);
        assertNotNull(crl.getRevokedCertificate(BigInteger.ONE));
    }
}
