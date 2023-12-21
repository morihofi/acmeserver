package de.morihofi.acmeserver.tools.path;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.URISyntaxException;
import java.nio.file.Paths;

public class AppDirectoryHelper {
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static String getAppDirectory() {
        try {
            return Paths.get(AppDirectoryHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static String getAppJarFilePath() {
        try {
            return Paths.get(AppDirectoryHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
