/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.challenges;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.events.EventBus;

import de.morihofi.certgine.cryptography.acme.AcmeTokenCryptography;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.utils.crypto.Hashing;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.network.DNSConfig;
import de.morihofi.certgine.types.config.network.NetworkConfig;
import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.intf.network.dns.IDoHClient;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.utils.base64.Base64Tools;
import de.morihofi.certgine.utils.network.dns.DNSLookup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import lombok.NonNull;
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

import static org.junit.jupiter.api.Assertions.*;

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
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @NonNull
        @Override public org.hibernate.Session getDatabaseSession() { return null; }
        @NotNull
        @NonNull
        @Override public ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NotNull
        @NonNull
        @Override public Config getAppConfig() { return cfg; }
        @NotNull
        @NonNull
        @Override public INonceManager getNonceManager() { return null; }
        @NotNull
        @NonNull
        @Override public RootCa getRootCa() { return null; }
        @NotNull
        @NonNull
        @Override public TsaAuthority getTsaAuthority() { return null; }
        @NotNull
        @NonNull
        @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
        @NotNull
        @NonNull
        @Override public INetworkClient getNetworkClient() { return net; }
        @NotNull
        @NonNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @NotNull
        @NonNull
        @Override public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }

        @Override
        public @NonNull IModuleRegistry getModuleRegistry() {
            return null;
        }
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
}

