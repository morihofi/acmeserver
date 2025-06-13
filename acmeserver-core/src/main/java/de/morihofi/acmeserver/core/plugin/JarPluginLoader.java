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

    private final Map<String, ClassLoader> registeredClasses;

    private JarPluginLoader(Map<String, ClassLoader> registeredClasses) {
        this.registeredClasses = registeredClasses;
    }

    /**
     * Scans the plugin directory and loads all classes found in JARs.
     *
     * @return loader instance containing the registered classes
     */
    public static JarPluginLoader getPluginLoader() {
        Map<String, ClassLoader> registered = new HashMap<>();
        log.info("Trying to load external jars ... Please wait");
        try {
            Path pluginsDir = Main.resolveDataPluginsDir();
            validateDirectory(pluginsDir);

            List<Path> jarPaths = getJarPaths(pluginsDir);
            if (jarPaths.isEmpty()) {
                log.info("No JAR files found in the plugins directory: {}. Skipping plugin registration", pluginsDir.toAbsolutePath());
                return new JarPluginLoader(registered);
            }

            for (Path jarPath : jarPaths) {
                URL jarUrl = jarPath.toUri().toURL();
                URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, JarPluginLoader.class.getClassLoader());
                loadClassesFromJar(jarUrl, cl, registered);
            }
        } catch (Exception e) {
            log.warn("Exception occurred while loading jars", e);
        }
        return new JarPluginLoader(registered);
    }

    private static void validateDirectory(Path pluginsDir) throws IOException {
        if (!Files.exists(pluginsDir)) {
            Files.createDirectories(pluginsDir);
        }
        if (!Files.isDirectory(pluginsDir)) {
            throw new IllegalStateException("Plugins directory is not a directory: " + pluginsDir.toAbsolutePath());
        }
    }

    private static List<Path> getJarPaths(Path pluginsDir) throws IOException {
        return Files.list(pluginsDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .toList();
    }

    private static void loadClassesFromJar(URL jarUrl, URLClassLoader classLoader, Map<String, ClassLoader> registered) {
        try (JarInputStream jarInputStream = new JarInputStream(jarUrl.openStream())) {
            processJarEntries(jarInputStream, classLoader, registered, jarUrl);
        } catch (IOException e) {
            log.warn("Failed to process JAR file: {}", jarUrl, e);
        }
    }

    private static void processJarEntries(JarInputStream jarInputStream, URLClassLoader classLoader, Map<String, ClassLoader> registered, URL jarUrl) throws IOException {
        JarEntry entry;
        while ((entry = jarInputStream.getNextJarEntry()) != null) {
            if (entry.getName().endsWith(".class")) {
                String className = entry.getName().replace("/", ".").substring(0, entry.getName().length() - 6);
                registered.put(className, classLoader);
                log.info("Registered class (from {}) : {}", jarUrl.getFile(), className);
            }
        }
    }

    /**
     * Returns the registered plugin classes as an unmodifiable map.
     */
    public Map<String, ClassLoader> getRegisteredPluginClasses() {
        return Collections.unmodifiableMap(registeredClasses);
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
