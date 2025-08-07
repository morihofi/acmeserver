/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.path;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AppDirectoryHelper} verifying successful and failing lookups.
 */
class AppDirectoryHelperTest {

    @Test
    void getAppDirectoryReturnsPath() {
        Optional<Path> dir = AppDirectoryHelper.getAppDirectory(AppDirectoryHelperTest.class);
        assertTrue(dir.isPresent());
        assertTrue(Files.isDirectory(dir.get()));
    }

    @Test
    void getAppDirectoryReturnsEmptyOnFailure() {
        assertTrue(AppDirectoryHelper.getAppDirectory(String.class).isEmpty());
    }

    @Test
    void getAppJarFilePathReturnsPath() {
        Optional<Path> jar = AppDirectoryHelper.getAppJarFilePath(AppDirectoryHelperTest.class);
        assertTrue(jar.isPresent());
        assertTrue(Files.exists(jar.get()));
    }

    @Test
    void getAppJarFilePathReturnsEmptyOnFailure() {
        assertTrue(AppDirectoryHelper.getAppJarFilePath(String.class).isEmpty());
    }
}
