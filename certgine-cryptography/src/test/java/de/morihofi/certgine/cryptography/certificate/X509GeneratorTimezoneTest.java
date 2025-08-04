package de.morihofi.certgine.cryptography.certificate;

import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.api.acme.dns.Identifier;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class X509GeneratorTimezoneTest {

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
    void serverCertificateRespectsProvidedInstantTimes() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
            KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.ROOT_CA)
                    .certificateConfig(cfg("Root"))
                    .ownKeyPair(rootKey)
                    .build());

            KeyPair serverKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
            Instant start = Instant.parse("2024-03-10T01:30:00Z");
            Instant end = start.plusSeconds(18_000);
            X509Certificate cert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.SERVER)
                    .issuerKeyPair(rootKey)
                    .issuerCertificate(rootCert)
                    .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                    .identifier(new Identifier(Identifier.IDENTIFIER_TYPE.DNS, "example.com"))
                    .startDate(Date.from(start))
                    .endDate(Date.from(end))
                    .build());

            assertEquals(start, cert.getNotBefore().toInstant());
            assertEquals(end, cert.getNotAfter().toInstant());
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
