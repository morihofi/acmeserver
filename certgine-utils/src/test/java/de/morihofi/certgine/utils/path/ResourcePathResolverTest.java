/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.path;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResourcePathResolver} verifying path resolution, caching and runtime detection.
 */
class ResourcePathResolverTest {

    @Test
    void resolveModulePathFindsPathAndCaches() {
        Path first = ResourcePathResolver.resolveModulePath("certgine-utils/src/test/java");
        Path second = ResourcePathResolver.resolveModulePath("certgine-utils/src/test/java");
        assertTrue(Files.isDirectory(first));
        assertSame(first, second);
    }

    @Test
    void resolveModulePathThrowsForMissingPath() {
        assertThrows(IllegalStateException.class, () -> ResourcePathResolver.resolveModulePath("missing/dir"));
    }

    @Test
    void isRunningFromJarReturnsFalseInTests() {
        assertFalse(ResourcePathResolver.isRunningFromJar());
    }
}
