/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.path;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Helper class for retrieving application directory and JAR file path. This class provides methods to obtain the application's directory
 * and JAR file path.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppDirectoryHelper {

    /**
     * Gets the application directory where the application is located.
     *
     * @param targetClass a class from the application to resolve its location
     * @return an {@link Optional} containing the application directory path, or {@link Optional#empty()} if it cannot be
     *         resolved
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static Optional<Path> getAppDirectory(Class<?> targetClass) {
        try {
            return Optional.ofNullable(Paths
                    .get(targetClass.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent());
        } catch (URISyntaxException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the file path to the application's JAR file.
     *
     * @param targetClass a class from the application to resolve its location
     * @return an {@link Optional} containing the JAR file path, or {@link Optional#empty()} if it cannot be resolved
     */
    public static Optional<Path> getAppJarFilePath(Class<?> targetClass) {
        try {
            return Optional.of(Paths
                    .get(targetClass.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
