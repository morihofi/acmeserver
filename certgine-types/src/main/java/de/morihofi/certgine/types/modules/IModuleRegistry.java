package de.morihofi.certgine.types.modules;

import jakarta.servlet.http.HttpServlet;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.HashMap;
import java.util.HashSet;
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
     * Unregisters a module and removes all of its contributions.
     *
     * @param moduleName unique name of the module to unload
     */
    void unregisterModule(String moduleName);

    /**
     * Reloads a module by unloading and registering it again.
     *
     * @param moduleName unique name of the module to reload
     */
    void reloadModule(String moduleName);

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
     * Shuts down any resources associated with the registry, such as scheduled tasks.
     * Default implementation is a no-op and may be overridden by implementations.
     */
    default void shutdownScheduler() {
        // default no-op
    }

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

        /**
         * Instance object returned by {@link CertgineModule#getModuleInstance()} for
         * module specific interactions. May be {@code null} if the module does not
         * expose an instance.
         */
        private CertgineModuleInstance moduleInstance;

        /**
         * Names of modules this module depends on.
         */
        @Builder.Default
        @NonNull
        private final Set<String> dependencies = new HashSet<>();

        /**
         * Entity classes contributed by this module.
         */
        @Builder.Default
        @NonNull
        private final Set<Class<?>> entityClasses = new HashSet<>();

        /**
         * HTTP handler classes contributed by this module.
         */
        @Builder.Default
        @NonNull
        private final Set<Class<? extends HttpServlet>> httpHandlerClasses = new HashSet<>();

        /**
         * Service implementations contributed by this module keyed by their interface.
         */
        @Builder.Default
        @NonNull
        private final Map<Class<?>, Object> services = new HashMap<>();
    }
}

