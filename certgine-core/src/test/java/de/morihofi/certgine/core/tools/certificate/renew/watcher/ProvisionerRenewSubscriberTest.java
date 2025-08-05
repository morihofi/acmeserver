/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.certificate.renew.watcher;

import com.google.common.jimfs.Jimfs;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
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
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.Security;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

class ProvisionerRenewSubscriberTest {

    static class DummyServerInstance implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final EventBus bus;
        DummyServerInstance(CryptoStoreManager mgr, EventBus bus) {
            this.mgr = mgr; this.bus = bus;
        }
        @NotNull
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @NonNull
        @Override public org.hibernate.Session getDatabaseSession() { return null; }
        @NotNull
        @NonNull
        @Override public ICryptoStoreManager getCryptoStoreManager() { return mgr; }
        @NotNull
        @NonNull
        @Override public Config getAppConfig() { return new Config(); }
        @NotNull
        @NonNull
        @Override public INonceManager getNonceManager() { return null; }
        @NotNull
        @NonNull
        @Override public RootCa getRootCa() { return null; }
        @NotNull
        @NonNull
        @Override public TsaAuthority getTsaAuthority() { return null; }
        @NotNull
        @NonNull
        @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
        @NotNull
        @NonNull
        @Override public INetworkClient getNetworkClient() { return null; }
        @NotNull
        @NonNull
        @Override public EventBus getEventBus() { return bus; }
        @NotNull
        @NonNull
        @Override public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
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
        TimedScheduler ts = new TimedScheduler();
        CertificateRenewScheduler renewManager =
                new CertificateRenewScheduler(mgr, bus, ts, Clock.systemUTC());

        ProvisionerRenewSubscriber watcher = new ProvisionerRenewSubscriber(si, renewManager);
        bus.register(watcher);

        AcmeProvisioner prov = new AcmeProvisioner();
        prov.setName("test");
        prov.setInternalUuid("p1");

        try (MockedStatic<AcmeProvisioner> mock = Mockito.mockStatic(AcmeProvisioner.class)) {
            mock.when(() -> AcmeProvisioner.getAllProvisioners(si)).thenReturn(new AcmeProvisioner[]{prov});
            watcher.initialize();
        }

        String alias = mgr.getKeyStoreAliasForProvisionerIntermediate(prov.getInternalUuid());
        assertTrue(renewManager.isWatcherRegistered(alias));

        bus.publish(new ProvisionerDeletedEvent(prov));
        assertFalse(renewManager.isWatcherRegistered(alias));

        bus.publish(new ProvisionerCreatedEvent(prov));
        assertTrue(renewManager.isWatcherRegistered(alias));
    }
}
