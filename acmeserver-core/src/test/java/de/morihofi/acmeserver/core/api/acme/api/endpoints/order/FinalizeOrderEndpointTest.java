package de.morihofi.acmeserver.core.api.acme.api.endpoints.order;

import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEUnauthorizedException;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeOrderEndpointTest {

    static class DummyServerInstance implements IServerInstance {
        @Override public String getServerURL() { return ""; }
        @Override public Session getDatabaseSession() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
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

        assertThrows(ACMEUnauthorizedException.class,
                () -> endpoint.verifyAuthorizationsComplete(List.of(id1, id2)));
    }

    @Test
    @DisplayName("verifyAuthorizationsComplete passes when all identifiers valid")
    void testVerifyAuthorizationsComplete() {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(new DummyServerInstance());
        AcmeOrderIdentifier id1 = new AcmeOrderIdentifier("dns", "example.com");
        AcmeOrderIdentifier id2 = new AcmeOrderIdentifier("dns", "example.org");
        id1.setChallenges(List.of(challengeWithStatus(id1, AcmeStatus.VALID)));
        id2.setChallenges(List.of(challengeWithStatus(id2, AcmeStatus.VALID)));

        assertDoesNotThrow(() -> endpoint.verifyAuthorizationsComplete(List.of(id1, id2)));
    }
}
