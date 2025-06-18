package de.morihofi.acmeserver.core.tools.certificate.renew.watcher;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.events.ProvisionerCreatedEvent;
import de.morihofi.acmeserver.types.events.ProvisionerDeletedEvent;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.Security;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProvisionerRenewSubscriberTest {

    static class DummyServerInstance implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final EventBus bus;
        DummyServerInstance(CryptoStoreManager mgr, EventBus bus) {
            this.mgr = mgr; this.bus = bus;
        }
        @NotNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @Override public org.hibernate.Session getDatabaseSession() { return null; }
        @NotNull
        @Override public ICryptoStoreManager getCryptoStoreManager() { return mgr; }
        @NotNull
        @Override public Config getAppConfig() { return new Config(); }
        @NotNull
        @Override public INonceManager getNonceManager() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @NotNull
        @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
        @NotNull
        @Override public INetworkClient getNetworkClient() { return null; }
        @NotNull
        @Override public EventBus getEventBus() { return bus; }
        @NotNull
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
    }

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("watcher registers and unregisters on events")
    void testWatcherLifecycle() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path ksPath = fs.getPath("test.p12");
        CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "pw".toCharArray()));
        EventBus bus = new EventBus();
        IServerInstance si = new DummyServerInstance(mgr, bus);
        CertificateRenewScheduler renewManager = new CertificateRenewScheduler(mgr, bus);

        ProvisionerRenewSubscriber watcher = new ProvisionerRenewSubscriber(si, renewManager);
        bus.register(watcher);

        AcmeProvisioner prov = new AcmeProvisioner();
        prov.setName("test");

        try (MockedStatic<AcmeProvisioner> mock = Mockito.mockStatic(AcmeProvisioner.class)) {
            mock.when(() -> AcmeProvisioner.getAllProvisioners(si)).thenReturn(new AcmeProvisioner[]{prov});
            watcher.initialize();
        }

        String alias = mgr.getKeyStoreAliasForProvisionerIntermediate("test");
        assertTrue(renewManager.isWatcherRegistered(alias));

        bus.publish(new ProvisionerDeletedEvent(prov));
        assertFalse(renewManager.isWatcherRegistered(alias));

        bus.publish(new ProvisionerCreatedEvent(prov));
        assertTrue(renewManager.isWatcherRegistered(alias));
    }
}
