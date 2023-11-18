package de.morihofi.acmeserver.config;

public class CertificateExpiration {
    private Integer months;

    private Integer days;

    private Integer years;

    public Integer getMonths() {
        return this.months;
    }

    public void setMonths(Integer months) {
        this.months = months;
    }

    public Integer getDays() {
        return this.days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getYears() {
        return this.years;
    }

    public void setYears(Integer years) {
        this.years = years;
    }
}
