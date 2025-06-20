package de.morihofi.acmeserver.core.certificate.queue;

import de.morihofi.acmeserver.cryptography.certificate.queue.CertificateIssuanceSubscriber;
import de.morihofi.acmeserver.cryptography.certificate.queue.CertificateIssuer;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
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


class CertificateIssuanceSubscriberTest {

    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final EventBus bus;
        private final Session session;
        DummyServer(CryptoStoreManager mgr, EventBus bus, Session s) {this.mgr=mgr;this.bus=bus;this.session=s;}
        @NonNull
        @Override public String getServerURL(){return "";}

        @NonNull
        @Override public Session getDatabaseSession(){return session;}
        @NonNull
        @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}

        @NonNull
        @Override public Config getAppConfig(){return new Config();}
        @NonNull
        @Override public INonceManager getNonceManager(){return null;}

        @NonNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa(){return null;}
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
