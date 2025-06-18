package de.morihofi.acmeserver.core.api.acme.challenges;
import de.morihofi.acmeserver.types.events.EventBus;

import de.morihofi.acmeserver.cryptography.acme.AcmeTokenCryptography;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.utils.crypto.Hashing;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.network.DNSConfig;
import de.morihofi.acmeserver.types.config.network.NetworkConfig;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.intf.network.dns.IDoHClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.utils.base64.Base64Tools;
import de.morihofi.acmeserver.utils.network.dns.DNSLookup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Collections;
import java.util.List;

class DNSChallengeTest {
    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }
/*
    static class DummyServerInstance implements IServerInstance {
        private final INetworkClient net = new INetworkClient() {
            @Override public okhttp3.OkHttpClient getOkHttpClient() { return new okhttp3.OkHttpClient(); }
            @Override public IDoHClient getDoHClient() { return null; }
            @Override public List<String> getDnsServer() { return Collections.emptyList(); }
            @Override public java.net.Proxy getProxy() { return java.net.Proxy.NO_PROXY; }
        };
        private final Config cfg;
        DummyServerInstance() {
            cfg = new Config();
            NetworkConfig nc = new NetworkConfig();
            nc.setDnsConfig(new DNSConfig());
            cfg.setNetwork(nc);
        }
        @NotNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @Override public org.hibernate.Session getDatabaseSession() { return null; }
        @NotNull
        @Override public ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NotNull
        @Override public Config getAppConfig() { return cfg; }
        @NotNull
        @Override public INonceManager getNonceManager() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @NotNull
        @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
        @NotNull
        @Override public INetworkClient getNetworkClient() { return net; }
            @NotNull
            @Override public EventBus getEventBus() { return new EventBus(); }
    }



    @Test
    @DisplayName("getDigest computes expected value")
    void testGetDigest() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        String token = "tok";
        String expected = Base64Tools.base64UrlEncode(Hashing.sha256hash(AcmeTokenCryptography.keyAuthorizationFor(token, kp.getPublic())));
        assertEquals(expected, DNSChallenge.getDigest(token, kp.getPublic()));
    }

    @Test
    @DisplayName("check returns success when TXT record matches")
    void testCheckSuccess() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        String token = "tok";
        AcmeAccount acc = new AcmeAccount();
        acc.setPublicKeyPEM(PemUtil.convertToPem(kp.getPublic()));

        String digest = DNSChallenge.getDigest(token, kp.getPublic());
        Record rec = new TXTRecord(Name.fromString("_acme-challenge.example.com."), 1, 60, digest);

        try (MockedStatic<DNSLookup> mock = Mockito.mockStatic(DNSLookup.class)) {
            mock.when(() -> DNSLookup.performDnsServerLookup(Mockito.anyString(), Mockito.eq(Type.TXT), Mockito.anyList())).thenReturn(List.of(rec));
            ChallengeResult result = DNSChallenge.check(token, "example.com", acc, new DummyServerInstance());
            assertTrue(result.successful());
        }
    }

    @Test
    @DisplayName("check fails when no record matches")
    void testCheckFail() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        String token = "tok";
        AcmeAccount acc = new AcmeAccount();
        acc.setPublicKeyPEM(PemUtil.convertToPem(kp.getPublic()));

        Record rec = new TXTRecord(Name.fromString("_acme-challenge.example.com."), 1, 60, "wrong");

        try (MockedStatic<DNSLookup> mock = Mockito.mockStatic(DNSLookup.class)) {
            mock.when(() -> DNSLookup.performDnsServerLookup(Mockito.anyString(), Mockito.eq(Type.TXT), Mockito.anyList())).thenReturn(List.of(rec));
            ChallengeResult result = DNSChallenge.check(token, "example.com", acc, new DummyServerInstance());
            assertFalse(result.successful());
        }
    }

 */
}

