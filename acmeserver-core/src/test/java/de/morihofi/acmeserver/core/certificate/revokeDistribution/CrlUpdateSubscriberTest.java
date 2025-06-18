package de.morihofi.acmeserver.core.certificate.revokeDistribution;

import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.events.AcmeCertificateRevokedEvent;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


class CrlUpdateSubscriberTest {
    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr; private final EventBus bus;
        DummyServer(CryptoStoreManager mgr, EventBus bus){this.mgr=mgr;this.bus=bus;}
        @NotNull
        @Override public String getServerURL(){return "";}

        @NotNull
        @Override public Session getDatabaseSession(){return null;}
        @NotNull
        @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}

        @NotNull
        @Override public Config getAppConfig(){return new Config();}
        @NotNull
        @Override public INonceManager getNonceManager(){return null;}

        @NotNull
        @Override public RootCa getRootCa(){return null;}
        @NotNull
        @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @NotNull
        @Override public INetworkClient getNetworkClient(){return null;}

        @NotNull
        @Override public EventBus getEventBus(){return bus;}
    }

    @Test
    @DisplayName("subscriber updates CRL on revoke")
    void testUpdate() {
        CryptoStoreManager mgr = Mockito.mock(CryptoStoreManager.class);
        EventBus bus = new EventBus();
        IServerInstance si = new DummyServer(mgr,bus);
        CrlUpdateSubscriber sub = new CrlUpdateSubscriber(si);
        bus.register(sub);
        AcmeProvisioner prov = new AcmeProvisioner(); prov.setName("p");
        AcmeAccount acc = new AcmeAccount(); acc.setAcmeProvisioner(prov);
        AcmeOrder order = new AcmeOrder(); order.setAccount(acc);
        try (MockedStatic<CrlStore> mock = Mockito.mockStatic(CrlStore.class)) {
            bus.publish(new AcmeCertificateRevokedEvent(order));
            mock.verify(() -> CrlStore.updateCachedCRL(CRLScheduler.UPDATE_MINUTES, prov, si));
        }
    }
}
