package de.morihofi.certgine.revocation;

import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.revocation.crl.CrlScheduler;
import de.morihofi.certgine.revocation.crl.CrlStore;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class CrlSchedulerTest {
    @Test
    @DisplayName("scheduler triggers CRL regeneration")
    void testSchedule() {
        CryptoStoreManager mgr = Mockito.mock(CryptoStoreManager.class);
        EventBus bus = new EventBus();
        IServerInstance si = new DummyServer(mgr, bus);
        CrlScheduler scheduler = new CrlScheduler(si);
        try (MockedStatic<CrlStore> mock = Mockito.mockStatic(CrlStore.class)) {
            scheduler.schedule();
            mock.verify(() -> CrlStore.updateCachedCRL(CrlScheduler.UPDATE_MINUTES, si));
        }
    }

    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final EventBus bus;

        DummyServer(CryptoStoreManager mgr, EventBus bus) {
            this.mgr = mgr;
            this.bus = bus;
        }

        @Override
        public @NotNull String getServerURL() { return ""; }
        @Override
        public @NotNull Session getDatabaseSession() { return null; }
        @Override
        public @NotNull ICryptoStoreManager getCryptoStoreManager() { return mgr; }
        @Override
        public @NotNull Config getAppConfig() { return new Config(); }
        @Override
        public @NotNull RootCa getRootCa() { return null; }
        @Override
        public @NotNull BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
        @Override
        public @NotNull INetworkClient getNetworkClient() { return null; }
        @Override
        public @NotNull EventBus getEventBus() { return bus; }
        @Override
        public @NotNull java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
        @Override
        public @NotNull IModuleRegistry getModuleRegistry() { return null; }
    }
}
