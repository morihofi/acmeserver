/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.challenges;

import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.cryptography.acme.AcmeTokenCryptography;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.network.DNSConfig;
import de.morihofi.certgine.types.config.network.NetworkConfig;
import de.morihofi.certgine.core.util.DummyServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.intf.network.dns.IDoHClient;
import de.morihofi.certgine.utils.base64.Base64Tools;
import de.morihofi.certgine.utils.crypto.Hashing;
import de.morihofi.certgine.utils.network.dns.DNSLookup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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

import static org.junit.jupiter.api.Assertions.*;

class DNSChallengeTest {
    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
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
            ChallengeResult result = DNSChallenge.check(token, "example.com", acc, server());
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
            ChallengeResult result = DNSChallenge.check(token, "example.com", acc, server());
            assertFalse(result.successful());
        }
    }

    private static DummyServerInstance server() {
        DummyServerInstance server = new DummyServerInstance();
        server.networkClient = new INetworkClient() {
            @Override
            public okhttp3.OkHttpClient getOkHttpClient() {
                return new okhttp3.OkHttpClient();
            }

            @Override
            public IDoHClient getDoHClient() {
                return null;
            }

            @Override
            public List<String> getDnsServer() {
                return Collections.emptyList();
            }
        };
        Config cfg = new Config();
        NetworkConfig nc = new NetworkConfig();
        nc.setDnsConfig(new DNSConfig());
        cfg.setNetwork(nc);
        server.appConfig = cfg;
        return server;
    }
}

