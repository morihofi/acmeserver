/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation;

import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.revocation.crl.CrlScheduler;
import de.morihofi.certgine.revocation.crl.CrlUpdateSubscriber;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.revocation.crl.CrlStore;
import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.acme.types.events.AcmeCertificateRevokedEvent;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import org.hibernate.Session;
import lombok.NonNull;
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
