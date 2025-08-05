/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.account;

import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.account.objects.ExternalAccountBinding;
import de.morihofi.certgine.acme.types.entities.AcmeExternalAccountBinding;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.types.exception.exceptions.ACMEUserActionRequiredException;
import de.morihofi.certgine.types.intf.IServerInstance;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.keys.HmacKey;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class NewAccountEndpointTest {
    static class DummyServer implements IServerInstance {
        @Override public String getServerURL() { return ""; }
        @Override public Session getDatabaseSession() { return null; }
        @Override public de.morihofi.certgine.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @Override public de.morihofi.certgine.types.config.Config getAppConfig() { return null; }
        @Override public de.morihofi.certgine.types.intf.INonceManager getNonceManager() { return null; }
        @Override public de.morihofi.certgine.types.database.entities.authority.RootCa getRootCa() { return null; }
        @Override public de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority getTsaAuthority() { return null; }
        @Override public de.morihofi.certgine.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @Override public de.morihofi.certgine.types.intf.network.INetworkClient getNetworkClient() { return null; }
        @Override public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
        @Override public de.morihofi.certgine.types.events.EventBus getEventBus() { return new de.morihofi.certgine.types.events.EventBus(); }
    }

    @Test
    @DisplayName("throws when binding required and missing")
    void testBindingRequiredMissing() throws Exception {
        NewAccountEndpoint endpoint = new NewAccountEndpoint(new DummyServer());
        AcmeProvisioner p = new AcmeProvisioner();
        p.setExternalAccountBindingRequired(true);
        Method m = NewAccountEndpoint.class.getDeclaredMethod("validateExternalAccountBinding",
                ExternalAccountBinding.class, AcmeProvisioner.class, String.class);
        m.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> m.invoke(endpoint, null, p, "{}"));
        assertTrue(ex.getCause() instanceof ACMEUserActionRequiredException);
    }

    @Test
    @DisplayName("valid binding returns key")
    void testValidBinding() throws Exception {
        NewAccountEndpoint endpoint = new NewAccountEndpoint(new DummyServer());
        AcmeProvisioner p = new AcmeProvisioner();
        p.setExternalAccountBindingRequired(false);

        byte[] keyBytes = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        AcmeExternalAccountBinding binding = new AcmeExternalAccountBinding();
        binding.setHmacKey(Base64.getEncoder().encodeToString(keyBytes));
        binding.setKeyId("kid1");
        try (MockedStatic<AcmeExternalAccountBinding> mock = Mockito.mockStatic(AcmeExternalAccountBinding.class)) {
            mock.when(() -> AcmeExternalAccountBinding.getForKid(Mockito.any(), Mockito.eq("kid1"))).thenReturn(binding);

            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload("{}");
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
            jws.setKey(new HmacKey(keyBytes));
            jws.setKeyIdHeaderValue("kid1");
            jws.setHeader("url", "test");
            String compact = jws.getCompactSerialization();
            String[] parts = compact.split("\\.");
            ExternalAccountBinding eab = new ExternalAccountBinding();
            java.lang.reflect.Field f;
            f = ExternalAccountBinding.class.getDeclaredField("protectedHeader");
            f.setAccessible(true); f.set(eab, parts[0]);
            f = ExternalAccountBinding.class.getDeclaredField("payload");
            f.setAccessible(true); f.set(eab, parts[1]);
            f = ExternalAccountBinding.class.getDeclaredField("signature");
            f.setAccessible(true); f.set(eab, parts[2]);

            Method m = NewAccountEndpoint.class.getDeclaredMethod("validateExternalAccountBinding",
                    ExternalAccountBinding.class, AcmeProvisioner.class, String.class);
            m.setAccessible(true);
            Object result = m.invoke(endpoint, eab, p, "{}");
            assertSame(binding, result);
        }
    }
}
