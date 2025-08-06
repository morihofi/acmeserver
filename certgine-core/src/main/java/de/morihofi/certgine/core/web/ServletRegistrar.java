package de.morihofi.certgine.core.web;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
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
        for (IModuleRegistry.ModuleInfo info : moduleRegistry.getModules().values()) {
            for (Class<? extends HttpServlet> servletClass : info.getHttpHandlerClasses()) {
                addServlet(context, servletClass, info.getModuleInstance());
            }
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

    public void addServlet(ServletContextHandler context, Class<? extends HttpServlet> servletClazz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        CertgineModuleInstance moduleInstance = null;
        for (IModuleRegistry.ModuleInfo info : moduleRegistry.getModules().values()) {
            if (info.getHttpHandlerClasses().contains(servletClazz)) {
                moduleInstance = info.getModuleInstance();
                break;
            }
        }
        addServlet(context, servletClazz, moduleInstance);
    }

    private void addServlet(
            ServletContextHandler context,
            Class<? extends HttpServlet> servletClazz,
            CertgineModuleInstance moduleInstance)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<? extends HttpServlet> servletClazzConstructor;
        if (moduleInstance != null) {
            try {
                servletClazzConstructor = servletClazz.getConstructor(CertgineModuleInstance.class);
                HttpServlet servlet = servletClazzConstructor.newInstance(moduleInstance);
                addServlet(context, servlet);
                return;
            } catch (NoSuchMethodException ignored) {
                // fall through to other constructors
            }
        }

        try {
            servletClazzConstructor = servletClazz.getConstructor(IServerInstance.class);
            HttpServlet servlet = servletClazzConstructor.newInstance(serverInstance);
            addServlet(context, servlet);
            return;
        } catch (NoSuchMethodException ignored) {
            // fall through to no-arg
        }

        servletClazzConstructor = servletClazz.getConstructor();
        HttpServlet servlet = servletClazzConstructor.newInstance();
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

    private record MountedServlet(ServletHolder holder, ServletMapping mapping, boolean protect) {
    }
}
