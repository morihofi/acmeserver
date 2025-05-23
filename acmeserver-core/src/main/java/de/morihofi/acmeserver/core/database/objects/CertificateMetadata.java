package de.morihofi.acmeserver.core.database.objects;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CertificateMetadata {

    public CertificateMetadata() {
    }

    public CertificateMetadata(String commonName, String organisation, String organisationalUnit, String countryCode) {
        this.commonName = commonName;
        this.organisation = organisation;
        this.organisationalUnit = organisationalUnit;
        this.countryCode = countryCode;
    }

    private String commonName;
    private String organisation;
    private String organisationalUnit;
    private String countryCode;
}

