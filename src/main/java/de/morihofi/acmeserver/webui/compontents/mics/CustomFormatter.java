package de.morihofi.acmeserver.webui.compontents.mics;

import de.morihofi.acmeserver.webui.JteLocalizer;

public class CustomFormatter {
    public static String formatDuration(long seconds, JteLocalizer localizer) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return localizer.lookup("web.core.misc.duration", days, hours, minutes, secs);
    }
}
