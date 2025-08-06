package de.morihofi.certgine.core.web;

import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import jakarta.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.ServletMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper responsible for servlet registration and unloading.
 */
@Slf4j
public class ServletRegistrar {

    private final IServerInstance serverInstance;
    private final IModuleRegistry moduleRegistry;
    private final List<MountedServlet> mountedServlets = new ArrayList<>();

    private record MountedServlet(ServletHolder holder, ServletMapping mapping, boolean protect) {}

    public ServletRegistrar(IServerInstance serverInstance, IModuleRegistry moduleRegistry) {
        this.serverInstance = serverInstance;
        this.moduleRegistry = moduleRegistry;
    }

    /**
     * Registers all servlet handlers provided by loaded modules.
     *
     * @param context servlet context to register handlers on
     */
    public void addBundledServlets(ServletContextHandler context) throws Exception {
        for (Class<? extends HttpServlet> servletClass : moduleRegistry.getHttpHandlerClasses()) {
            addServlet(context, servletClass);
        }
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
