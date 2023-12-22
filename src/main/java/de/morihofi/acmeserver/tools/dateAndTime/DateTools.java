package de.morihofi.acmeserver.tools.dateAndTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateTools {

    private DateTools(){}

    /**
     * Formats a {@link Date} object as a string in the ACME date format.
     *
     * @param date The {@link Date} object to be formatted.
     * @return A string representing the formatted date in the "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" format in UTC time zone.
     */
    public static String formatDateForACME(Date date) {
        // Set the date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Format the date and return it as a string
        return dateFormat.format(date);
    }
}
