/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order;
import de.morihofi.certgine.acme.security.INonceManager;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.events.EventBus;

import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.exception.exceptions.ACMEUnauthorizedException;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import org.hibernate.Session;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeOrderEndpointTest {
    static class DummyServerInstance implements IServerInstance {
        @NotNull
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @NonNull
        @Override public Session getDatabaseSession() { return null; }
        @NotNull
        @NonNull
        @Override public ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.config.Config getAppConfig() { return null; }
        @NotNull
        @NonNull
        @Override public INonceManager getNonceManager() { return null; }
        @NotNull
        @NonNull
        @Override public RootCa getRootCa() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.intf.network.INetworkClient getNetworkClient() { return null; }
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

    private static AcmeOrderIdentifierChallenge challengeWithStatus(AcmeOrderIdentifier id, AcmeStatus status) {
        AcmeOrderIdentifierChallenge c = new AcmeOrderIdentifierChallenge();
        c.setStatus(status);
        c.setIdentifier(id);
        return c;
    }

    @Test
    @DisplayName("verifyAuthorizationsComplete throws when any identifier invalid")
    void testVerifyAuthorizationsIncomplete() {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(new DummyServerInstance());
        AcmeOrderIdentifier id1 = new AcmeOrderIdentifier("dns", "example.com");
        AcmeOrderIdentifier id2 = new AcmeOrderIdentifier("dns", "example.org");
        id1.setChallenges(List.of(challengeWithStatus(id1, AcmeStatus.VALID)));
        id2.setChallenges(List.of(challengeWithStatus(id2, AcmeStatus.PENDING)));

        java.lang.reflect.Method m;
        try {
            m = FinalizeOrderEndpoint.class.getDeclaredMethod("verifyAuthorizationsComplete", List.class);
            m.setAccessible(true);
            java.lang.reflect.InvocationTargetException ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> m.invoke(endpoint, List.of(id1, id2)));
            assertInstanceOf(ACMEUnauthorizedException.class, ex.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("verifyAuthorizationsComplete passes when all identifiers valid")
    void testVerifyAuthorizationsComplete() {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(new DummyServerInstance());
        AcmeOrderIdentifier id1 = new AcmeOrderIdentifier("dns", "example.com");
        AcmeOrderIdentifier id2 = new AcmeOrderIdentifier("dns", "example.org");
        id1.setChallenges(List.of(challengeWithStatus(id1, AcmeStatus.VALID)));
        id2.setChallenges(List.of(challengeWithStatus(id2, AcmeStatus.VALID)));

        java.lang.reflect.Method m;
        try {
            m = FinalizeOrderEndpoint.class.getDeclaredMethod("verifyAuthorizationsComplete", List.class);
            m.setAccessible(true);
            assertDoesNotThrow(() -> m.invoke(endpoint, List.of(id1, id2)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
