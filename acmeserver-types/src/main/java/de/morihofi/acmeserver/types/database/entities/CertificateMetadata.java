package de.morihofi.acmeserver.types.database.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
public class CertificateMetadata implements Serializable {
    private final String commonName;
    private final String organisation;
    private final String organisationalUnit;
    private final String countryCode;
}

