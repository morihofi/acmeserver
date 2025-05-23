package de.morihofi.acmeserver.core.database.objects;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CertificateExpiration {
    public CertificateExpiration() {
    }

    public CertificateExpiration(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    private int days;
    private int months;
    private int years;
}
