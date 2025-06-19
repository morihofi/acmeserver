package de.morihofi.acmeserver.ui.frontend.legacy.helper;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class TemplateHelper {
    /**
     * Resolves the source directory for templates (or other resources) at runtime.
     *
     * @param relativeToProjectRoot The path relative to the module root (e.g., "src/main/jte").
     * @return Absolute path to the directory if it exists.
     */
    public static Path resolveModulePath(String relativeToProjectRoot) {
        Path cwd = Paths.get("").toAbsolutePath();

        Path candidate = cwd.resolve(relativeToProjectRoot);
        if (Files.exists(candidate)) {
            return candidate;
        }

        // Try walking up the tree (e.g. in multi-module apps run from root)
        Path current = cwd;
        for (int i = 0; i < 5; i++) { // walk max 5 levels up
            current = current.getParent();
            if (current == null) break;

            Path maybe = current.resolve(relativeToProjectRoot);
            if (Files.exists(maybe)) {
                return maybe;
            }
        }

        throw new IllegalStateException("Could not find path for: " + relativeToProjectRoot + " (cwd=" + cwd + ")");
    }


    /**
     * Checks if the application is running from a JAR file.
     *
     * @return true if running from a JAR, false otherwise
     */
    public static boolean isRunningFromJar() {
        // The path to a class that exists safely in the JAR.
        // We're using MethodHandles.lookup().lookupClass() to get the current class
        Class<?> clazz = MethodHandles.lookup().lookupClass();
        String className = clazz.getName().replace('.', '/') + ".class";
        URL classURL = clazz.getClassLoader().getResource(className);
        return classURL != null && classURL.getProtocol().equals("jar");
    }

    public static TemplateEngine createTemplateEngine() {
        boolean isDev = !isRunningFromJar();

        if (isDev) {
            log.info("Looks like this application is running from an IDE or outside a jar, using a JRE compiler resolver");

            DirectoryCodeResolver codeResolver = new DirectoryCodeResolver(resolveModulePath("acmeserver-web-legacy/src/main/jte"));
            return TemplateEngine.create(codeResolver, ContentType.Html);
        } else {
            log.info("Running inside a JAR, using a precompiled template engine");

            return TemplateEngine.createPrecompiled(ContentType.Html);
        }
    }
}
