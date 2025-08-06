package de.morihofi.certgine.revocation;

import de.morihofi.certgine.revocation.crl.CrlScheduler;
import de.morihofi.certgine.revocation.crl.CrlUpdateSubscriber;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import de.morihofi.certgine.types.modules.ModuleScheduledTask;
import jakarta.servlet.http.HttpServlet;

import java.util.Map;
import java.util.Set;

/**
 * Module exposing certificate revocation HTTP endpoints.
 */
@ModuleDescriptor(moduleName = "revocation", description = "Certificate revocation endpoints")
public class RevocationModule implements CertgineModule {

    private IServerInstance serverInstance;
    private CrlScheduler crlScheduler;
    private CrlUpdateSubscriber updateSubscriber;

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of();
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(RevocationHttpServlet.class);
    }

    @Override
    public void onModuleInitialize(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        this.crlScheduler = new CrlScheduler(serverInstance);
        this.updateSubscriber = new CrlUpdateSubscriber(serverInstance);
        serverInstance.getEventBus().register(crlScheduler);
        serverInstance.getEventBus().register(updateSubscriber);
    }

    @Override
    public void onUnLoad() {
        if (serverInstance != null) {
            serverInstance.getEventBus().unregister(crlScheduler);
            serverInstance.getEventBus().unregister(updateSubscriber);
        }
    }

    @Override
    public Map<String, ModuleScheduledTask> getScheduledTasks() {
        return Map.of(CrlScheduler.CRON_EXPRESSION, new ModuleScheduledTask() {
            @Override
            public Runnable task() {
                return () -> crlScheduler.schedule();
            }
        });
    }
}
