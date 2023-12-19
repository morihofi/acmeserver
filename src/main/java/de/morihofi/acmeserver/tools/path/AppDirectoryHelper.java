package de.morihofi.acmeserver.tools.path;

import java.net.URISyntaxException;
import java.nio.file.Paths;

public class AppDirectoryHelper {
    public static String getAppDirectory() {
        try {
            return Paths.get(AppDirectoryHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
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
