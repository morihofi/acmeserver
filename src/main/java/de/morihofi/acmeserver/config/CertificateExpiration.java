package de.morihofi.acmeserver.config;

import java.io.Serializable;

/**
 * Represents expiration information for a certificate, specifying months, days, and years.
 */
public class CertificateExpiration implements Serializable {
    private Integer months;
    private Integer days;
    private Integer years;

    /**
     * Get the number of months until expiration.
     * @return The number of months.
     */
    public Integer getMonths() {
        return this.months;
    }

    /**
     * Set the number of months until expiration.
     * @param months The number of months to set.
     */
    public void setMonths(Integer months) {
        this.months = months;
    }

    /**
     * Get the number of days until expiration.
     * @return The number of days.
     */
    public Integer getDays() {
        return this.days;
    }

    /**
     * Set the number of days until expiration.
     * @param days The number of days to set.
     */
    public void setDays(Integer days) {
        this.days = days;
    }

    /**
     * Get the number of years until expiration.
     * @return The number of years.
     */
    public Integer getYears() {
        return this.years;
    }

    /**
     * Set the number of years until expiration.
     * @param years The number of years to set.
     */
    public void setYears(Integer years) {
        this.years = years;
    }
}
