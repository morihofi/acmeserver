/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.path;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves paths to resources within the project and detects runtime context.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourcePathResolver {

    private static final Map<String, Path> RESOLVED_PATH_CACHE = new ConcurrentHashMap<>();
    private static final boolean RUNNING_FROM_JAR = detectRunningFromJar();

    /**
     * Resolves the source directory for templates or other resources at runtime.
     * Paths are cached to avoid repeated filesystem lookups.
     *
     * @param relativeToProjectRoot the path relative to the module root (e.g., "src/main/jte")
     * @return absolute path to the directory if it exists
     * @throws IllegalStateException if the path cannot be resolved
     */
    public static Path resolveModulePath(String relativeToProjectRoot) {
        return RESOLVED_PATH_CACHE.computeIfAbsent(relativeToProjectRoot, ResourcePathResolver::resolvePathUncached);
    }

    private static Path resolvePathUncached(String relativeToProjectRoot) {
        Path cwd = Paths.get("").toAbsolutePath();

        Path candidate = cwd.resolve(relativeToProjectRoot);
        if (Files.exists(candidate)) {
            return candidate;
        }

        Path current = cwd;
        for (int i = 0; i < 5; i++) {
            current = current.getParent();
            if (current == null) {
                break;
            }
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
     * @return {@code true} if running from a JAR, {@code false} otherwise
     */
    public static boolean isRunningFromJar() {
        return RUNNING_FROM_JAR;
    }

    private static boolean detectRunningFromJar() {
        Class<?> clazz = MethodHandles.lookup().lookupClass();
        String className = clazz.getName().replace('.', '/') + ".class";
        URL classURL = clazz.getClassLoader().getResource(className);
        boolean runningFromJar = classURL != null && "jar".equals(classURL.getProtocol());
        log.debug("Running from JAR detected: {}", runningFromJar);
        return runningFromJar;
    }
}
