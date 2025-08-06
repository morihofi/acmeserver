package de.morihofi.certgine.types.modules;

import jakarta.servlet.http.HttpServlet;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;
import java.util.Set;

public interface IModuleRegistry {

    /**
     * Registers a module and its contents.
     *
     * @param info module metadata and instance
     */
    void registerModule(ModuleInfo info);

    /**
     * Retrieves a service implementation by its interface.
     *
     * @param serviceInterface the interface class
     * @param <T>              type of the service
     * @return the implementation instance, or null if not registered
     */
    <T> T getService(Class<T> serviceInterface);

    /**
     * Returns all registered service interfaces.
     *
     * @return set of interface classes
     */
    Set<Class<?>> getServiceInterfaces();

    /**
     * Returns all collected entity classes.
     *
     * @return set of JPA entity classes
     */
    Set<Class<?>> getEntityClasses();

    /**
     * Returns all registered HTTP servlet classes.
     *
     * @return set of HTTP handler classes
     */
    Set<Class<? extends HttpServlet>> getHttpHandlerClasses();

    /**
     * Returns a map of all registered modules by name.
     *
     * @return map of module name to ModuleInfo
     */
    Map<String, ModuleInfo> getModules();

    /**
     * Metadata about a loaded module.
     */
    @Data
    @Builder
    public static class ModuleInfo {

        /**
         * Unique name of the module.
         */
        @NonNull
        private final String moduleName;

        /**
         * Short description of the module.
         */
        private final String description;

        /**
         * The module implementation instance.
         */
        @NonNull
        private final CertgineModule module;
    }
}

