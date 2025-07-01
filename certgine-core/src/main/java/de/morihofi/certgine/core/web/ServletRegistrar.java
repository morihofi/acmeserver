package de.morihofi.certgine.core.web;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.ui.frontend.modern.WebUiServletHolderHolder;
import jakarta.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.ServletMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper responsible for servlet registration and unloading.
 */
@Slf4j
public class ServletRegistrar {

    private final IServerInstance serverInstance;
    private final List<MountedServlet> mountedServlets = new ArrayList<>();

    private record MountedServlet(ServletHolder holder, ServletMapping mapping, boolean protect) {}

    public ServletRegistrar(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Registers all bundled servlets to the given context.
     */
    public void addBundledServlets(ServletContextHandler context) throws Exception {
        addServlet(context, de.morihofi.certgine.acme.AcmeHttpServlet.class);
        addServlet(context, de.morihofi.certgine.acme.GetHttpsForFreeServlet.class);
        addServlet(context, de.morihofi.certgine.ui.frontend.legacy.LegacyWebUiServlet.class);
        addServlet(context, de.morihofi.certgine.core.servlet.download.RootCaDownloadServlet.class);
        addServlet(context, de.morihofi.certgine.core.servlet.api.ApiServlet.class);
        {
            ServletHolder holder = new WebUiServletHolderHolder(serverInstance);
            context.addServlet(holder, "/*");
            ServletMapping mapping = context.getServletHandler().getServletMapping(holder.getName());
            mountedServlets.add(new MountedServlet(holder, mapping, true));
        }

        X509Certificate tsaCert = serverInstance
                .getCryptoStoreManager()
                .getTimestampAuthorityCertificate(serverInstance.getTsaAuthority().getInternalUuid());
        de.morihofi.certgine.cryptography.tsa.TimeStampAuthority auth = new de.morihofi.certgine.cryptography.tsa.TimeStampAuthority(
                serverInstance.getCryptoStoreManager().getTimestampAuthorityKeyPair(serverInstance.getTsaAuthority().getInternalUuid()).getPrivate(),
                tsaCert,
                List.of(tsaCert,
                        serverInstance.getCryptoStoreManager().getCertificateAuthorityX509Certificate(serverInstance.getRootCa())));
        addServlet(context, new de.morihofi.certgine.tsa.TimeStampServlet(auth));
        addServlet(context, de.morihofi.certgine.revocation.RevocationHttpServlet.class);
    }

    public void addServlet(ServletContextHandler context, HttpServlet servlet, String mountPath, boolean protect) {
        log.info("Adding servlet {} at mount {}; is unload protected = {}", servlet.getClass().getName(), mountPath, protect);
        ServletHolder holder = new ServletHolder(servlet);
        context.addServlet(holder, mountPath);
        ServletMapping mapping = context.getServletHandler().getServletMapping(holder.getName());
        mountedServlets.add(new MountedServlet(holder, mapping, protect));
    }

    public void addServlet(ServletContextHandler context, HttpServlet servlet) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        ServletMount s = servlet.getClass().getAnnotation(ServletMount.class);
        addServlet(context, servlet, s.servletMountPoint(), s.protect());
    }

    public void addServlet(ServletContextHandler context, Class<? extends HttpServlet> servletClazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<? extends HttpServlet> servletClazzConstructor;
        try {
            servletClazzConstructor = servletClazz.getConstructor(IServerInstance.class);
        } catch (NoSuchMethodException e) {
            servletClazzConstructor = servletClazz.getConstructor();
        }
        HttpServlet servlet = servletClazzConstructor.getParameterCount() == 0
                ? servletClazzConstructor.newInstance()
                : servletClazzConstructor.newInstance(serverInstance);
        addServlet(context, servlet);
    }

    public void unloadUnprotectedServlets(ServletContextHandler context) {
        var handler = context.getServletHandler();
        List<ServletHolder> keepHolders = new ArrayList<>();
        List<ServletMapping> keepMappings = new ArrayList<>();

        for (MountedServlet ms : new ArrayList<>(mountedServlets)) {
            if (ms.protect()) {
                keepHolders.add(ms.holder());
                keepMappings.add(ms.mapping());
                continue;
            }
            log.info("Unloading servlet {}", ms.holder().getHeldClass().getName());
            try {
                ms.holder().stop();
            } catch (Exception e) {
                log.error("Failed to stop servlet {}", ms.holder().getName(), e);
            }
            mountedServlets.remove(ms);
        }

        handler.setServlets(keepHolders.toArray(new ServletHolder[0]));
        handler.setServletMappings(keepMappings.toArray(new ServletMapping[0]));
    }

    public void unloadServlet(ServletContextHandler context, Class<? extends HttpServlet> servletClazz) {
        var handler = context.getServletHandler();
        List<ServletHolder> keepHolders = new ArrayList<>();
        List<ServletMapping> keepMappings = new ArrayList<>();

        for (MountedServlet ms : new ArrayList<>(mountedServlets)) {
            if (!ms.holder().getHeldClass().equals(servletClazz)) {
                keepHolders.add(ms.holder());
                keepMappings.add(ms.mapping());
                continue;
            }

            if (ms.protect()) {
                log.info("Servlet {} is marked as protected; skipping unload", servletClazz.getName());
                keepHolders.add(ms.holder());
                keepMappings.add(ms.mapping());
                continue;
            }

            log.info("Unloading servlet with class {}", servletClazz.getName());
            try {
                ms.holder().stop();
            } catch (Exception e) {
                log.error("Failed to stop servlet {}", ms.holder().getName(), e);
            }
            mountedServlets.remove(ms);
        }

        handler.setServlets(keepHolders.toArray(new ServletHolder[0]));
        handler.setServletMappings(keepMappings.toArray(new ServletMapping[0]));
    }
}
