package de.morihofi.acmeserver.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateTools {

    public static String formatDateForACME(Date date){
        // Set the date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Format the date and print it
        String formattedDate = dateFormat.format(date);

        return formattedDate;
    }
}
