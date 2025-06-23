package de.morihofi.acmeserver.cryptography.keystore;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CryptoStoreManagerTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName(cn)
                .build();
        return new CertificateConfig(meta, new CertificateExpiration(0, 0, 1), null);
    }

    @Test
    @DisplayName("constructor creates PKCS12 keystore")
    void testConstructor() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("test.p12");
            PKCS12KeyStoreConfig cfg = new PKCS12KeyStoreConfig(path, "pw".toCharArray());
            CryptoStoreManager mgr = new CryptoStoreManager(cfg);
            assertNotNull(mgr.getKeyStore());
            mgr.saveKeystore();
            assertTrue(Files.exists(path));
        }
    }

    @Test
    @DisplayName("adding intermediate stores full chain")
    void testAddIntermediateStoresChain() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("store.p12");
            CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(path, "pw".toCharArray()));

            // root
            KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.ROOT_CA)
                    .certificateConfig(cfg("root"))
                    .ownKeyPair(rootKey)
                    .build());
            RootCa root = new RootCa();
            root.setInternalUuid("root");
            root.setCertificateConfig(cfg("root"));
            mgr.addCertificateAuthority(root, rootKey, rootCert);

            // intermediate
            KeyPair interKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate interCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.INTERMEDIATE_CA)
                    .issuerKeyPair(rootKey)
                    .issuerCertificate(rootCert)
                    .ownKeyPair(interKey)
                    .certificateConfig(cfg("int"))
                    .build());

            mgr.addIntermediateCertificateAuthority(new X509Certificate[]{interCert, rootCert}, interKey, "int");

            X509Certificate[] chain = mgr.getFullIntermediateCertificateChain("int");
            assertEquals(2, chain.length);
            assertEquals(interCert, chain[0]);
            assertEquals(rootCert, chain[1]);
        }
    }

    @Test
    @DisplayName("adding timestamp authority stores full chain")
    void testAddTimestampAuthorityStoresChain() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("store2.p12");
            CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(path, "pw".toCharArray()));

            KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.ROOT_CA)
                    .certificateConfig(cfg("root"))
                    .ownKeyPair(rootKey)
                    .build());
            RootCa root = new RootCa();
            root.setInternalUuid("root");
            root.setCertificateConfig(cfg("root"));
            mgr.addCertificateAuthority(root, rootKey, rootCert);

            KeyPair tsaKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate tsaCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.TIMESTAMPING)
                    .certificateConfig(cfg("tsa"))
                    .issuerKeyPair(rootKey)
                    .issuerCertificate(rootCert)
                    .ownKeyPair(tsaKey)
                    .build());

            mgr.addTimestampAuthority(new X509Certificate[]{tsaCert, rootCert}, tsaKey, "tsa");

            X509Certificate[] chain = Stream.of(mgr.getKeyStore().getCertificateChain(mgr.getKeyStoreAliasForTimestampAuthority("tsa")))
                    .filter(c -> c instanceof X509Certificate)
                    .map(c -> (X509Certificate) c)
                    .toArray(X509Certificate[]::new);
            assertEquals(2, chain.length);
        }
    }

    @Test
    @DisplayName("adding server certificate stores full chain")
    void testAddServerCertificateChain() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("store3.p12");
            CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(path, "pw".toCharArray()));

            // root
            KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.ROOT_CA)
                    .certificateConfig(cfg("root"))
                    .ownKeyPair(rootKey)
                    .build());
            RootCa root = new RootCa();
            root.setInternalUuid("root");
            root.setCertificateConfig(cfg("root"));
            mgr.addCertificateAuthority(root, rootKey, rootCert);

            // intermediate
            KeyPair interKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate interCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.INTERMEDIATE_CA)
                    .issuerKeyPair(rootKey)
                    .issuerCertificate(rootCert)
                    .ownKeyPair(interKey)
                    .certificateConfig(cfg("int"))
                    .build());
            mgr.addIntermediateCertificateAuthority(new X509Certificate[]{interCert, rootCert}, interKey, "int");

            // server cert
            KeyPair serverKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate serverCert = X509Generator.generate(X509Generator.Request.builder()
                    .type(X509Generator.Type.SERVER)
                    .issuerKeyPair(interKey)
                    .issuerCertificate(interCert)
                    .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                    .identifier(new de.morihofi.acmeserver.types.api.acme.dns.Identifier(de.morihofi.acmeserver.types.api.acme.dns.Identifier.IDENTIFIER_TYPE.DNS, "example.com"))
                    .startDate(new java.util.Date())
                    .endDate(new java.util.Date(System.currentTimeMillis() + 1000))
                    .build());

            mgr.addServerCertificate(new X509Certificate[]{serverCert, interCert, rootCert}, serverKey, "srv");

            X509Certificate[] chain = Stream.of(mgr.getKeyStore().getCertificateChain("srv"))
                    .filter(c -> c instanceof X509Certificate)
                    .map(c -> (X509Certificate) c)
                    .toArray(X509Certificate[]::new);
            assertEquals(3, chain.length);
        }
    }
}
