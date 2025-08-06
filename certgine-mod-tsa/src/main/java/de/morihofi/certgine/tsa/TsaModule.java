package de.morihofi.certgine.tsa;

import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.tsa.servlets.TimeStampServlet;
import de.morihofi.certgine.tsa.types.entities.TsaAuthority;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import de.morihofi.certgine.utils.scheduler.CertificateRenewScheduler;
import jakarta.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import java.util.Set;

/**
 * Certgine module providing RFC 3161 timestamping servlet.
 */
@Slf4j
@ModuleDescriptor(moduleName = "tsa", description = "RFC 3161 Time Stamp Authority")
public class TsaModule extends CertgineModule {

    public TsaModule(IServerInstance serverInstance) {
        super(serverInstance);
    }

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of(TsaAuthority.class);
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(TimeStampServlet.class);
    }

    @Override
    public void onModuleInitialize(IServerInstance serverInstance) {
        CertificateRenewScheduler scheduler =
                serverInstance.getModuleRegistry().getService(CertificateRenewScheduler.class);
        if (scheduler != null) {
            TsaRenewSubscriber subscriber = new TsaRenewSubscriber(serverInstance, scheduler);
            serverInstance.getEventBus().register(subscriber);
            subscriber.initialize();
        } else {
            log.warn("CertificateRenewScheduler service not available; TSA renew watchers disabled");
        }
        ensureDefaultTsa(serverInstance);
    }

    private void ensureDefaultTsa(IServerInstance serverInstance) {
        try (Session session = serverInstance.getDatabaseSession()) {
            EventBus bus = serverInstance.getEventBus();
            CryptoStoreManager csm =
                    (CryptoStoreManager) serverInstance.getCryptoStoreManager();
            TsaInitHelper.initializeTsa(session, csm, serverInstance.getRootCa(), bus);
        } catch (Exception e) {
            log.warn("Failed to initialize default TSA", e);
        }
    }
}
