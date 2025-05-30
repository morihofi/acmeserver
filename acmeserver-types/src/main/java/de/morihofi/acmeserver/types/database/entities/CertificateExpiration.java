package de.morihofi.acmeserver.types.database.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
public class CertificateExpiration implements Serializable {
    private final int days;
    private final int months;
    private final int years;
}
