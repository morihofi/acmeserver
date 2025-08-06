package de.morihofi.certgine.revocation;

import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.revocation.crl.CrlScheduler;
import de.morihofi.certgine.revocation.crl.CrlStore;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.acme.types.events.ProvisionerCreatedEvent;
import de.morihofi.certgine.acme.types.events.ProvisionerDeletedEvent;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;
import lombok.NonNull;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class CrlSchedulerEventTest {
    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr; private final EventBus bus;
        DummyServer(CryptoStoreManager mgr, EventBus bus){this.mgr=mgr;this.bus=bus;}
        @NotNull
        @NonNull
        @Override public String getServerURL(){return "";}

        @NotNull
        @NonNull
        @Override public Session getDatabaseSession(){return null;}
        @NotNull
        @NonNull
        @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}

        @NotNull
        @NonNull
        @Override public Config getAppConfig(){return new Config();}
        @NotNull
        @NonNull
        @Override public INonceManager getNonceManager(){return null;}

        @NotNull
        @NonNull
        @Override public RootCa getRootCa(){return null;}
        @NotNull
        @NonNull
        @Override public TsaAuthority getTsaAuthority(){return null;}
        @NotNull
        @NonNull
        @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @NotNull
        @NonNull
        @Override public INetworkClient getNetworkClient(){return null;}

        @NotNull
        @NonNull
        @Override public EventBus getEventBus(){return bus;}
        @NotNull
        @NonNull
        @Override public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags(){return java.util.Collections.emptySet();}

        @Override
        public @NonNull IModuleRegistry getModuleRegistry() {
            return null;
        }
    }

    @Test
    @DisplayName("scheduler updates on provisioner events")
    void testUpdatesOnEvents() {
        CryptoStoreManager mgr = Mockito.mock(CryptoStoreManager.class);
        EventBus bus = new EventBus();
        TimedScheduler ts = new TimedScheduler();
        IServerInstance si = new DummyServer(mgr,bus);
        CrlScheduler scheduler = new CrlScheduler(si, ts);
        bus.register(scheduler);
        AcmeProvisioner prov = new AcmeProvisioner(); prov.setName("p");
        try (MockedStatic<CrlStore> mock = Mockito.mockStatic(CrlStore.class)) {
            bus.publish(new ProvisionerCreatedEvent(prov));
            mock.verify(() -> CrlStore.updateCachedCRL(CrlScheduler.UPDATE_MINUTES, prov, si));
            bus.publish(new ProvisionerDeletedEvent(prov));
            mock.verify(() -> CrlStore.updateCachedCRL(CrlScheduler.UPDATE_MINUTES, prov, si), Mockito.times(2));
        }
        ts.shutdown();
    }
}
