package de.morihofi.acmeserver.core.certificate.revokeDistribution;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.revoke.CrlGenerator;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.javalin.http.Context;
import jakarta.servlet.ServletOutputStream;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.LocalTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OcspEndpointGetTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = new CertificateMetadata(cn, "Org", null, "DE");
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }

    static class DummyContext implements Context {
        private final String ocsp;
        byte[] result;
        DummyContext(String ocsp) { this.ocsp = ocsp; }
        @Override public String pathParam(String s) { return "ocspRequest".equals(s) ? ocsp : "provisioner".equals(s) ? "test" : null; }
        @Override public java.util.Map<String, String> pathParamMap() { return Collections.emptyMap(); }
        @Override public Context result(byte[] bytes) { this.result = bytes; return this; }
        @Override public Context result(java.io.InputStream inputStream) { return this; }
        @Override public java.io.InputStream resultInputStream() { return null; }
        @Override public ServletOutputStream outputStream() { return null; }
        @Override public io.javalin.http.HandlerType handlerType() { return null; }
        @Override public String matchedPath() { return null; }
        @Override public String endpointHandlerPath() { return null; }
        @Override public <T> T appData(io.javalin.config.Key<T> key) { return null; }
        @Override public io.javalin.json.JsonMapper jsonMapper() { return null; }
        @Override public <T> T with(Class<? extends io.javalin.plugin.ContextPlugin<?, T>> plugin) { return null; }
        @Override public boolean strictContentTypes() { return false; }
        @Override public String body() { return null; }
        @Override public byte[] bodyAsBytes() { return new byte[0]; }
        @Override public <T> T bodyAsClass(java.lang.Class<T> aClass) { return null; }
        @Override public <T> T bodyAsClass(java.lang.reflect.Type type) { return null; }
        @Override public <T> T bodyStreamAsClass(java.lang.reflect.Type type) { return null; }
        @Override public java.io.InputStream bodyInputStream() { return null; }
        @Override public <T> io.javalin.validation.BodyValidator<T> bodyValidator(java.lang.Class<T> aClass) { return null; }
        @Override public jakarta.servlet.http.HttpServletRequest req() { return null; }
        @Override public jakarta.servlet.http.HttpServletResponse res() { return null; }
        @Override public Context minSizeForCompression(int i) { return this; }
        @Override public void future(java.util.function.Supplier<? extends java.util.concurrent.CompletableFuture<?>> supplier) {}
        @Override public void redirect(String s, io.javalin.http.HttpStatus httpStatus) {}
        @Override public void writeJsonStream(java.util.stream.Stream<?> stream) {}
        @Override public Context skipRemainingHandlers() { return this; }
        @Override public java.util.Set<io.javalin.security.RouteRole> routeRoles() { return Collections.emptySet(); }
        @Override public Context contentType(String contentType) { return this; }
        @Override public Context status(io.javalin.http.HttpStatus status) { return this; }
        @Override public io.javalin.http.HttpStatus status() { return null; }
    }

    private OcspEndpointGet endpoint;
    private AcmeProvisioner prov;
    private IServerInstance si;

    @BeforeEach
    void init() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path ksPath = fs.getPath("store.p12");
        CryptoStoreManager csm = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "".toCharArray()));

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
                .certificateConfig(cfg("Inter"))
                .build());

        csm.getKeyStore().setKeyEntry(csm.getKeyStoreAliasForProvisionerIntermediate("test"),
                interKey.getPrivate(), "".toCharArray(), new java.security.cert.Certificate[]{interCert});

        prov = new AcmeProvisioner();
        prov.setName("test");

        si = new IServerInstance() {
            @Override public String getServerURL() { return "https://example.com"; }
            @Override public Session getDatabaseSession() { return null; }
            @Override public ICryptoStoreManager getCryptoStoreManager() { return csm; }
            @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
            @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
            @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
            @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
            @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
        };

        X509CRL crl = CrlGenerator.generate(CrlGenerator.Request.builder()
                .revokedCertificates(Collections.emptyList())
                .caCert(interCert)
                .caPrivateKey(interKey.getPrivate())
                .updateMinutes(5)
                .build());
        CrlStore.entryMap.put("test", new CrlStore.CrlEntry(LocalTime.now(), crl));

        endpoint = new OcspEndpointGet(si);
    }

    @AfterEach
    void cleanup() {
        CrlStore.entryMap.clear();
    }

    @Test
    @DisplayName("handle decodes url-encoded request")
    void testHandleDecodes() throws Exception {
        KeyPair interKey = si.getCryptoStoreManager().getIntermediateCerificateAuthorityKeyPair("test");
        X509Certificate interCert = si.getCryptoStoreManager().getX509CertificateForProvisioner("test");

        DigestCalculator dig = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        CertificateID certId = new CertificateID(dig, new JcaX509CertificateHolder(interCert), BigInteger.ONE);
        OCSPReq req = new OCSPReqBuilder().addRequest(certId).build();
        String b64 = java.util.Base64.getEncoder().encodeToString(req.getEncoded());
        String encoded = URLEncoder.encode(b64, StandardCharsets.UTF_8);

        DummyContext ctx = new DummyContext(encoded);
        try (MockedStatic<AcmeProvisioner> mock = Mockito.mockStatic(AcmeProvisioner.class)) {
            mock.when(() -> AcmeProvisioner.getForName(si, "test")).thenReturn(prov);
            endpoint.handle(ctx);
        }

        assertNotNull(ctx.result);
        OCSPResp resp = new OCSPResp(ctx.result);
        assertEquals(OCSPResp.SUCCESSFUL, resp.getStatus());
    }
}
