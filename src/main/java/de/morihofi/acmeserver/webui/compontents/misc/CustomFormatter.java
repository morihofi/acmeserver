package de.morihofi.acmeserver.webui.compontents.misc;

import de.morihofi.acmeserver.webui.JteLocalizer;

public class CustomFormatter {

    /**
     * Formats a duration in seconds into a human-readable string specifying days, hours, minutes, and seconds. This method uses a
     * {@link JteLocalizer} to localize the formatted string, making it suitable for internationalized applications. The method calculates
     * the number of days, hours, minutes, and seconds that correspond to the total duration in seconds and then formats these values into a
     * localized string.
     *
     * @param seconds   the duration in seconds to format.
     * @param localizer the {@link JteLocalizer} instance used for localizing the duration string. This object should have access to a
     *                  resource bundle that contains a template for the duration string, keyed by "web.core.misc.duration", which can
     *                  incorporate placeholders for days, hours, minutes, and seconds.
     * @return a localized, human-readable string representing the duration, formatted according to the template provided in the localizer's
     * resource bundle.
     */
    public static String formatDuration(long seconds, JteLocalizer localizer) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return localizer.lookup("web.core.misc.duration", days, hours, minutes, secs);
    }
}
