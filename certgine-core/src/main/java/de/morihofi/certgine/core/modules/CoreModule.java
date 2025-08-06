package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.core.servlet.api.ApiServlet;
import de.morihofi.certgine.core.servlet.download.RootCaDownloadServlet;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import de.morihofi.certgine.types.modules.ModuleScheduledTask;
import de.morihofi.certgine.utils.scheduler.CertificateRenewScheduler;
import jakarta.servlet.http.HttpServlet;

import java.util.Map;
import java.util.Set;

/**
 * Module exposing core servlet handlers bundled with the server.
 */
@ModuleDescriptor(moduleName = "core", description = "Core server handlers")
public class CoreModule extends CertgineModule {

    private CertificateRenewScheduler certificateRenewScheduler;

    public CoreModule(IServerInstance serverInstance) {
        super(serverInstance);
    }

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of();
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(
                RootCaDownloadServlet.class,
                ApiServlet.class
        );
    }

    @Override
    public void onModuleInitialize(IServerInstance serverInstance) {
        certificateRenewScheduler = new CertificateRenewScheduler(
                serverInstance.getCryptoStoreManager()
        );
        String moduleName = getClass().getAnnotation(ModuleDescriptor.class).moduleName();
        ((ModuleRegistry) serverInstance.getModuleRegistry()).registerService(
                moduleName, CertificateRenewScheduler.class, certificateRenewScheduler);
    }

    @Override
    public void onUnLoad() {
        if (certificateRenewScheduler != null) {
            certificateRenewScheduler.shutdown();
        }
    }

    @Override
    public Map<String, ModuleScheduledTask> getScheduledTasks() {
        return Map.of(CertificateRenewScheduler.DEFAULT_CRON, new ModuleScheduledTask() {
            @Override
            public Runnable task() {
                return () -> certificateRenewScheduler.schedule();
            }

            @Override
            public void cancel() {
                certificateRenewScheduler.shutdown();
            }
        });
    }
}
