package de.morihofi.acmeserver.core.plugin;

import de.morihofi.acmeserver.core.Main;
import de.morihofi.acmeserver.types.plugin.PluginInfo;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Utility for loading external plugin JARs placed in the plugin directory.
 */
@Slf4j
public class JarPluginLoader {

    /** Path inside JAR pointing to the plugin descriptor listing plugin classes. */
    private static final String PLUGIN_DESCRIPTOR = "META-INF/acmeserver-plugin";
    /** Path inside JAR containing plugin metadata in JSON format. */
    private static final String PLUGIN_METADATA = "META-INF/acmeserver-plugin.json";

    private final Map<String, ClassLoader> registeredClasses;
    private final Map<ClassLoader, Path> loaderRoots;
    private final Map<String, PluginInfo> pluginInfos;

    private JarPluginLoader(Map<String, ClassLoader> registeredClasses,
                            Map<ClassLoader, Path> loaderRoots,
                            Map<String, PluginInfo> pluginInfos) {
        this.registeredClasses = registeredClasses;
        this.loaderRoots = loaderRoots;
        this.pluginInfos = pluginInfos;
    }

    /**
     * Scans the plugin directory and loads plugin classes referenced in
     * {@value #PLUGIN_DESCRIPTOR} descriptors contained in plugin JARs.
     *
     * @return loader instance containing the registered classes
     */
    public static JarPluginLoader getPluginLoader() {
        Map<String, ClassLoader> registered = new HashMap<>();
        Map<ClassLoader, Path> roots = new HashMap<>();
        Map<String, PluginInfo> infos = new HashMap<>();
        log.info("Trying to load external jars ... Please wait");
        try {
            Path pluginsDir = Main.resolveDataPluginsDir();
            validateDirectory(pluginsDir);

            List<Path> pluginDirs = getPluginDirs(pluginsDir);
            if (pluginDirs.isEmpty()) {
                log.info("No plugins found in {}", pluginsDir.toAbsolutePath());
                return new JarPluginLoader(registered, roots, infos);
            }

            for (Path pluginDir : pluginDirs) {
                List<URL> jars = getJarUrls(pluginDir);
                if (jars.isEmpty()) {
                    continue;
                }
                URLClassLoader cl = new URLClassLoader(jars.toArray(new URL[0]), JarPluginLoader.class.getClassLoader());
                for (URL jarUrl : jars) {
                    loadClassesFromJar(jarUrl, cl, registered, infos);
                }
                roots.put(cl, pluginDir);
            }
        } catch (Exception e) {
            log.warn("Exception occurred while loading jars", e);
        }
        return new JarPluginLoader(registered, roots, infos);
    }

    private static void validateDirectory(Path pluginsDir) throws IOException {
        if (!Files.exists(pluginsDir)) {
            Files.createDirectories(pluginsDir);
        }
        if (!Files.isDirectory(pluginsDir)) {
            throw new IllegalStateException("Plugins directory is not a directory: " + pluginsDir.toAbsolutePath());
        }
    }

    private static List<Path> getPluginDirs(Path pluginsDir) throws IOException {
        try (Stream<Path> stream = Files.list(pluginsDir)) {
            return stream.filter(Files::isDirectory).toList();
        }
    }

    private static List<URL> getJarUrls(Path pluginDir) throws IOException {
        try (Stream<Path> stream = Files.list(pluginDir)) {
            return stream.filter(p -> p.toString().endsWith(".jar"))
                    .map(p -> {
                        try {
                            return p.toUri().toURL();
                        } catch (Exception e) {
                            log.warn("Could not convert JAR file path to URL: {}", p, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private static void loadClassesFromJar(URL jarUrl, URLClassLoader classLoader,
                                           Map<String, ClassLoader> registered,
                                           Map<String, PluginInfo> infos) {
        try (JarInputStream jarInputStream = new JarInputStream(jarUrl.openStream())) {
            JarEntry entry;
            String metadataJson = null;
            Set<String> classNames = new LinkedHashSet<>();
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if (PLUGIN_DESCRIPTOR.equals(entry.getName())) {
                    try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(jarInputStream))) {
                        reader.lines()
                                .map(String::trim)
                                .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                                .forEach(classNames::add);
                    }
                } else if (PLUGIN_METADATA.equals(entry.getName())) {
                    metadataJson = new String(jarInputStream.readAllBytes());
                }
            }
            if (metadataJson != null) {
                try {
                    Gson gson = new Gson();
                    PluginInfo info = gson.fromJson(metadataJson, PluginInfo.class);
                    if (info != null && info.getPluginClass() != null) {
                        infos.put(info.getPluginClass(), info);
                        classNames.add(info.getPluginClass());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to parse plugin metadata for {}", jarUrl.getFile(), ex);
                }
            }
            for (String className : classNames) {
                registered.put(className, classLoader);
                log.info("Registered class (from {}) : {}", jarUrl.getFile(), className);
            }
        } catch (IOException e) {
            log.warn("Failed to process JAR file: {}", jarUrl, e);
        }
    }

    /**
     * Returns the registered plugin classes as an unmodifiable map.
     */
    public Map<String, ClassLoader> getRegisteredPluginClasses() {
        return Collections.unmodifiableMap(registeredClasses);
    }

    /** Returns metadata for the given plugin class if available. */
    public PluginInfo getPluginInfo(String className) {
        return pluginInfos.get(className);
    }

    /** Returns the plugin directory for the given class name. */
    public Path getPluginRoot(String className) {
        ClassLoader cl = registeredClasses.get(className);
        return loaderRoots.get(cl);
    }

    /**
     * Loads the class with the given fully qualified name using its registered class loader.
     *
     * @param fqcn fully qualified class name
     * @return the loaded class
     * @throws ClassNotFoundException if the class is not available
     */
    public Class<?> getNewInitializedClassInstance(String fqcn) throws ClassNotFoundException {
        ClassLoader cl = registeredClasses.get(fqcn);
        if (cl == null) {
            throw new ClassNotFoundException("Class not found or not registered: " + fqcn);
        }
        return Class.forName(fqcn, true, cl);
    }
}
