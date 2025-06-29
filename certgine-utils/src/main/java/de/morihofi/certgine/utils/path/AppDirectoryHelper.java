/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.path;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * Helper class for retrieving application directory and JAR file path. This class provides methods to obtain the application's directory
 * and JAR file path.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppDirectoryHelper {

    /**
     * Gets the application directory where the application is located.
     *
     * @return The application directory as a string, or null if an error occurs.
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static String getAppDirectory(Class<?> targetClass) {
        try {
            return Paths.get(targetClass.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Gets the file path to the application's JAR file.
     *
     * @return The JAR file path as a string, or null if an error occurs.
     */
    public static String getAppJarFilePath(Class<?> targetClass) {
        try {
            return Paths.get(targetClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
