package de.morihofi.acmeserver.tools.path;

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

    private AppDirectoryHelper() {}
}
