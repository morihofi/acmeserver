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

package de.morihofi.acmeserver.core.tools.dateAndTime;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTools {

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

    /**
     * Adds a specified number of years, months, and days to a given date.
     * <p>
     * This method allows for manipulating the date by adding or subtracting values to the year, month, and day fields of the given date.
     * The operation is performed based on the calendar's rules, accounting for leap years, varying month lengths, and other
     * calendar-specific logic.
     *
     * @param startDate The starting date to which years, months, and days are to be added. Must not be null.
     * @param years     The number of years to add to the startDate. This value can be negative to subtract years.
     * @param months    The number of months to add to the startDate. This value can be negative to subtract months. Note that adding months
     *                  can potentially change the year of the result.
     * @param days      The number of days to add to the startDate. This value can be negative to subtract days. Adding or subtracting days
     *                  takes into account the month length and leap years, adjusting the month and year as necessary.
     * @return A {@link java.util.Date} object representing the date after adding the specified years, months, and days to the startDate.
     * The original startDate object is not modified.
     * @throws NullPointerException if the startDate is null.
     */
    public static Date addToDate(Date startDate, int years, int months, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, years);
        calendar.add(Calendar.MONTH, months);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    /**
     * Ensures that a proposed server certificate end date does not exceed the expiration date of the intermediate certificate.
     * <p>
     * In the context of SSL/TLS certificates, an intermediate certificate acts as a bridge between the trusted root and the server
     * certificates issued to end entities. For the chain of trust to be valid, each certificate must not expire before the certificate it
     * signs. This method ensures that the server certificate's validity does not extend beyond that of the intermediate certificate, which
     * could otherwise break the chain of trust.
     *
     * @param intermediateNotAfter The expiration date of the intermediate certificate. This date represents the latest possible valid end
     *                             date for the server certificate to maintain trust.
     * @param proposedEndDate      The initially proposed end date for the server certificate. This is the date that is intended to be set
     *                             as the expiration date before ensuring it does not outlive the intermediate certificate.
     * @return A {@link java.util.Date} object that represents the validated end date for the server certificate. If the proposed end date
     * is before the intermediate certificate's expiration, the proposed end date is returned. Otherwise, the expiration date of the
     * intermediate certificate is returned, ensuring the server certificate does not outlive it.
     * @throws NullPointerException if either of the date parameters is null.
     */
    public static Date makeDateForOutliveIntermediateCertificate(Date intermediateNotAfter, Date proposedEndDate) {
        // Ensure the server certificate does not outlive the intermediate certificate
        return (proposedEndDate.before(intermediateNotAfter)) ? proposedEndDate : intermediateNotAfter;
    }

    private DateTools() {
    }
}
