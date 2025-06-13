package de.morihofi.acmeserver.core.plugin;

import de.morihofi.acmeserver.core.Main;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Utility for loading external plugin JARs placed in the plugin directory.
 */
@Slf4j
public class JarPluginLoader {

    /** Path inside JAR pointing to the plugin descriptor listing plugin classes. */
    private static final String PLUGIN_DESCRIPTOR = "META-INF/acmeserver-plugin";

    private final Map<String, ClassLoader> registeredClasses;
    private final Map<ClassLoader, Path> loaderRoots;

    private JarPluginLoader(Map<String, ClassLoader> registeredClasses,
                            Map<ClassLoader, Path> loaderRoots) {
        this.registeredClasses = registeredClasses;
        this.loaderRoots = loaderRoots;
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
        log.info("Trying to load external jars ... Please wait");
        try {
            Path pluginsDir = Main.resolveDataPluginsDir();
            validateDirectory(pluginsDir);

            List<Path> pluginDirs = getPluginDirs(pluginsDir);
            if (pluginDirs.isEmpty()) {
                log.info("No plugins found in {}", pluginsDir.toAbsolutePath());
                return new JarPluginLoader(registered, roots);
            }

            for (Path pluginDir : pluginDirs) {
                List<URL> jars = getJarUrls(pluginDir);
                if (jars.isEmpty()) {
                    continue;
                }
                URLClassLoader cl = new URLClassLoader(jars.toArray(new URL[0]), JarPluginLoader.class.getClassLoader());
                for (URL jarUrl : jars) {
                    loadClassesFromJar(jarUrl, cl, registered);
                }
                roots.put(cl, pluginDir);
            }
        } catch (Exception e) {
            log.warn("Exception occurred while loading jars", e);
        }
        return new JarPluginLoader(registered, roots);
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
        try (var stream = Files.list(pluginsDir)) {
            return stream.filter(Files::isDirectory).toList();
        }
    }

    private static List<URL> getJarUrls(Path pluginDir) throws IOException {
        try (var stream = Files.list(pluginDir)) {
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
                                           Map<String, ClassLoader> registered) {
        try (JarInputStream jarInputStream = new JarInputStream(jarUrl.openStream())) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if (PLUGIN_DESCRIPTOR.equals(entry.getName())) {
                    try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(jarInputStream))) {
                        reader.lines()
                                .map(String::trim)
                                .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                                .forEach(className -> {
                                    registered.put(className, classLoader);
                                    log.info("Registered class (from {}) : {}", jarUrl.getFile(), className);
                                });
                    }
                    break;
                }
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
