package de.morihofi.acmeserver.core.api.acme.challenges;

import de.morihofi.acmeserver.cryptography.acme.AcmeTokenCryptography;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

@Disabled("TLS-ALPN handshake unsupported in test environment")
class TLSALPNChallengeTest {

    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private SSLServerSocket serverSocket;
    private ExecutorService executor;

    @AfterEach
    void tearDown() throws Exception {
        if (serverSocket != null) {
            serverSocket.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void startServer(KeyPair keyPair, X509Certificate cert) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("key", keyPair.getPrivate(), "".toCharArray(), new java.security.cert.Certificate[]{cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "".toCharArray());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);

        serverSocket = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("0.0.0.0", 10443));
        SSLParameters params = serverSocket.getSSLParameters();
        params.setApplicationProtocols(new String[]{"acme-tls/1"});
        serverSocket.setSSLParameters(params);

        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (SSLSocket sock = (SSLSocket) serverSocket.accept()) {
                SSLParameters p = sock.getSSLParameters();
                p.setApplicationProtocols(new String[]{"acme-tls/1"});
                sock.setSSLParameters(p);
                sock.startHandshake();
            } catch (Exception ignored) {}
        });
    }

    private static X509Certificate challengeCert(byte[] digest, KeyPair kp) throws Exception {
        X500Name name = new X500Name("CN=test");
        Date now = new Date();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE, now, new Date(now.getTime() + 10000), name, kp.getPublic());
        builder.addExtension(new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.31"), false, new DEROctetString(digest));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);
    }

    private static class DummyServerInstance implements IServerInstance {
        @Override public String getServerURL() { return ""; }
        @Override public org.hibernate.Session getDatabaseSession() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
    }

    @Test
    @DisplayName("TLS-ALPN challenge succeeds on matching digest")
    void testTlsAlpnSuccess() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(2048, BouncyCastleProvider.PROVIDER_NAME);
        AcmeAccount account = new AcmeAccount();
        account.setPublicKeyPEM(PemUtil.convertToPem(kp.getPublic()));

        String token = "tok";
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(AcmeTokenCryptography.keyAuthorizationFor(token, kp.getPublic()).getBytes());
        X509Certificate cert = challengeCert(digest, kp);
        startServer(kp, cert);

        ChallengeResult r = TLSALPNChallenge.check(token, "127.0.0.1:10443", account, new DummyServerInstance());
        assertTrue(r.successful());
    }

    @Test
    @DisplayName("TLS-ALPN challenge fails on mismatch")
    void testTlsAlpnFailure() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(2048, BouncyCastleProvider.PROVIDER_NAME);
        AcmeAccount account = new AcmeAccount();
        account.setPublicKeyPEM(PemUtil.convertToPem(kp.getPublic()));

        byte[] wrongDigest = new byte[32];
        X509Certificate cert = challengeCert(wrongDigest, kp);
        startServer(kp, cert);

        ChallengeResult r = TLSALPNChallenge.check("tok", "127.0.0.1:10443", account, new DummyServerInstance());
        assertFalse(r.successful());
    }
}
