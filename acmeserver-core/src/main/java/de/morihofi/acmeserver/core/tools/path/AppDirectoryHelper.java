/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.tools.path;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * Helper class for retrieving application directory and JAR file path. This class provides methods to obtain the application's directory
 * and JAR file path.
 */
public class AppDirectoryHelper {

    /**
     * Gets the application directory where the application is located.
     *
     * @return The application directory as a string, or null if an error occurs.
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static String getAppDirectory() {
        try {
            return Paths.get(AppDirectoryHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Gets the file path to the application's JAR file.
     *
     * @return The JAR file path as a string, or null if an error occurs.
     */
    public static String getAppJarFilePath() {
        try {
            return Paths.get(AppDirectoryHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Private constructor to prevent object creation
     */
    private AppDirectoryHelper() {}
}
