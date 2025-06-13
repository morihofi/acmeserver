package de.morihofi.acmeserver.core.plugin;

import de.morihofi.acmeserver.core.Main;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.IServerPlugin;
import de.morihofi.acmeserver.types.plugin.PluginProperties;
import de.morihofi.acmeserver.types.plugin.PluginInfo;
import de.morihofi.acmeserver.core.plugin.PluginConfigException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manager responsible for discovering and initializing server plugins.
 */
@Slf4j
public class PluginManager {
    private final IServerInstance serverInstance;
    private final JarPluginLoader loader;
    /** List of successfully initialized plugins. */
    private final List<IServerPlugin> plugins = new ArrayList<>();

    /**
     * Creates a new plugin manager bound to the given server instance.
     *
     * @param serverInstance server instance used for initialization
     */
    public PluginManager(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        this.loader = JarPluginLoader.getPluginLoader();
    }

    /**
     * Loads all plugins found in the plugin directory.
     */
    public void loadPlugins() {
        for (Map.Entry<String, ClassLoader> entry : loader.getRegisteredPluginClasses().entrySet()) {
            String className = entry.getKey();
            try {
                Class<?> cls = loader.getNewInitializedClassInstance(className);
                if (!IServerPlugin.class.isAssignableFrom(cls)) {
                    continue;
                }
                IServerPlugin plugin = (IServerPlugin) cls.getDeclaredConstructor().newInstance();
                if (!plugin.dependenciesAvailable()) {
                    log.warn("Skipping plugin {} due to missing dependencies", className);
                    continue;
                }

                Path pluginRoot = loader.getPluginRoot(className);
                PluginConfigStore store = new PluginConfigStore(
                        pluginRoot.resolve("config.json"));
                PluginProperties props = store.load();
                if (plugin.getPluginVersion() != props.getVersion()) {
                    plugin.propertyUpdate(props.getVersion(), props.getProperties());
                    props.setVersion(plugin.getPluginVersion());
                }

                plugin.initialize(serverInstance, props.getProperties());
                store.save(props);
                plugins.add(plugin);
                log.info("Initialized plugin {}", className);
            } catch (NoClassDefFoundError e) {
                log.warn("Dependencies missing for plugin {}", className, e);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException | ClassNotFoundException e) {
                log.warn("Failed to load plugin {}", className, e);
            } catch (PluginConfigException e) {
                log.warn("Failed to access configuration for plugin {}", className, e);
            }
        }
    }

    /**
     * Returns all successfully initialized plugins.
     */
    public List<IServerPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    /** Returns metadata for a loaded plugin if available. */
    public PluginInfo getPluginInfo(IServerPlugin plugin) {
        return loader.getPluginInfo(plugin.getClass().getName());
    }

    JarPluginLoader getLoader() {
        return loader;
    }
}
