package de.morihofi.acmeserver.tools.dateAndTime;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class DateToolsTest {
    @Test
    public void testFormatDateForACME() throws ParseException {
        String dateStr = "2023-11-23T12:34:56.789Z";
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        inputDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = inputDateFormat.parse(dateStr);

        // Formatieren Sie das Datum mit Ihrer Methode
        String formattedDate = DateTools.formatDateForACME(date);

        // Überprüfen Sie, ob das formatierte Datum den erwarteten Wert hat
        assertEquals("2023-11-23T12:34:56.789Z", formattedDate);
    }
}