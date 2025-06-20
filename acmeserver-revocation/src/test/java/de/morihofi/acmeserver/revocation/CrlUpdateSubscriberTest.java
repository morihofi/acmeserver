package de.morihofi.acmeserver.revocation;

import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.revocation.crl.CrlScheduler;
import de.morihofi.acmeserver.revocation.crl.CrlUpdateSubscriber;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.revocation.crl.CrlStore;
import de.morihofi.acmeserver.types.events.AcmeCertificateRevokedEvent;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import org.hibernate.Session;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


class CrlUpdateSubscriberTest {
    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr; private final EventBus bus;
        DummyServer(CryptoStoreManager mgr, EventBus bus){this.mgr=mgr;this.bus=bus;}
        @NonNull
        @Override public String getServerURL(){return "";}

        @NonNull
        @Override public Session getDatabaseSession(){return null;}
        @NonNull
        @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}

        @NonNull
        @Override public Config getAppConfig(){return new Config();}
        @NonNull
        @Override public INonceManager getNonceManager(){return null;}

        @NonNull
        @Override public RootCa getRootCa(){return null;}
        @NonNull
        @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @NonNull
        @Override public INetworkClient getNetworkClient(){return null;}

        @NonNull
        @Override public EventBus getEventBus(){return bus;}
        @NonNull
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags(){return java.util.Collections.emptySet();}
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
            mock.verify(() -> CrlStore.updateCachedCRL(CrlScheduler.UPDATE_MINUTES, prov, si));
        }
    }
}
