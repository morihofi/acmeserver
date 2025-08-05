/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.certificate.queue;

import de.morihofi.certgine.acme.certificate.queue.CertificateIssuanceSubscriber;
import de.morihofi.certgine.acme.certificate.queue.CertificateIssuer;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import org.hibernate.Session;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


class CertificateIssuanceSubscriberTest {

    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final EventBus bus;
        private final Session session;
        DummyServer(CryptoStoreManager mgr, EventBus bus, Session s) {this.mgr=mgr;this.bus=bus;this.session=s;}
        @NotNull
        @NonNull
        @Override public String getServerURL(){return "";}

        @NotNull
        @NonNull
        @Override public Session getDatabaseSession(){return session;}
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
    }

    @Test
    @DisplayName("subscriber issues certificate on event")
    void testIssuance() throws Exception {
        CryptoStoreManager mgr = Mockito.mock(CryptoStoreManager.class);
        EventBus bus = new EventBus();
        Session session = Mockito.mock(Session.class);
        IServerInstance si = new DummyServer(mgr,bus,session);
        CertificateIssuanceSubscriber sub = new CertificateIssuanceSubscriber(si);
        bus.register(sub);
        AcmeOrder order = new AcmeOrder();
        try (MockedStatic<CertificateIssuer> mock = Mockito.mockStatic(CertificateIssuer.class)) {
            var m = CertificateIssuanceSubscriber.class.getDeclaredMethod("issue", AcmeOrder.class);
            m.setAccessible(true);
            m.invoke(sub, order);
            mock.verify(() -> CertificateIssuer.generateCertificateForOrder(order, mgr, session, si));
        }
    }
}
