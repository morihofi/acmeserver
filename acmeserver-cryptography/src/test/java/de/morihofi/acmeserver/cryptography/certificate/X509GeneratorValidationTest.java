package de.morihofi.acmeserver.cryptography.certificate;

import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class X509GeneratorValidationTest {

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
    @DisplayName("intermediate generation fails when issuer path length is zero")
    void testIntermediatePathLenValidation() throws Exception {
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Root"))
                .ownKeyPair(rootKey)
                .build());

        KeyPair interKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate interCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.INTERMEDIATE_CA)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .ownKeyPair(interKey)
                .certificateConfig(cfg("Inter1"))
                .build());

        KeyPair nextKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        assertThrows(IllegalArgumentException.class, () ->
                X509Generator.generate(X509Generator.Request.builder()
                        .type(X509Generator.Type.INTERMEDIATE_CA)
                        .issuerKeyPair(interKey)
                        .issuerCertificate(interCert)
                        .ownKeyPair(nextKey)
                        .certificateConfig(cfg("Inter2"))
                        .build()));
    }

    @Test
    @DisplayName("server generation fails with non-CA issuer")
    void testServerIssuerMustBeCa() throws Exception {
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Root"))
                .ownKeyPair(rootKey)
                .build());

        KeyPair serverKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        Identifier id = new Identifier(Identifier.IDENTIFIER_TYPE.DNS, "example.com");
        Date start = new Date();
        Date end = new Date(start.getTime() + 86_400_000L);

        X509Certificate serverCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.SERVER)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                .identifier(id)
                .startDate(start)
                .endDate(end)
                .build());

        assertThrows(IllegalArgumentException.class, () ->
                X509Generator.generate(X509Generator.Request.builder()
                        .type(X509Generator.Type.SERVER)
                        .issuerKeyPair(serverKey)
                        .issuerCertificate(serverCert)
                        .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                        .identifier(id)
                        .startDate(start)
                        .endDate(end)
                        .build()));
    }
}
