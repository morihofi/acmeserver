package de.morihofi.acmeserver.acme.api.endpoints.order;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.acme.api.endpoints.order.FinalizeOrderEndpoint;

import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEUnauthorizedException;
import org.hibernate.Session;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeOrderEndpointTest {
    static class DummyServerInstance implements IServerInstance {
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NonNull
        @Override public Session getDatabaseSession() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.database.entities.TsaAuthority getTsaAuthority() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
        @NonNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @NonNull
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
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
