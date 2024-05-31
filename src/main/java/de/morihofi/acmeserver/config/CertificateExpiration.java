/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Represents expiration information for a certificate, specifying months, days, and years.
 */
public class CertificateExpiration implements Serializable {
    @ConfigurationField(name = "Months", required = true)
    private Integer months;
    @ConfigurationField(name = "Days", required = true)
    private Integer days;
    @ConfigurationField(name = "Years", required = true)
    private Integer years;

    /**
     * Get the number of months until expiration.
     *
     * @return The number of months.
     */
    public Integer getMonths() {
        return this.months;
    }

    /**
     * Set the number of months until expiration.
     *
     * @param months The number of months to set.
     */
    public void setMonths(Integer months) {
        this.months = months;
    }

    /**
     * Get the number of days until expiration.
     *
     * @return The number of days.
     */
    public Integer getDays() {
        return this.days;
    }

    /**
     * Set the number of days until expiration.
     *
     * @param days The number of days to set.
     */
    public void setDays(Integer days) {
        this.days = days;
    }

    /**
     * Get the number of years until expiration.
     *
     * @return The number of years.
     */
    public Integer getYears() {
        return this.years;
    }

    /**
     * Set the number of years until expiration.
     *
     * @param years The number of years to set.
     */
    public void setYears(Integer years) {
        this.years = years;
    }
}
