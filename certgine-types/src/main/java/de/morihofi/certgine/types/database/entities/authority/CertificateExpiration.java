package de.morihofi.certgine.types.database.entities.authority;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CertificateExpiration implements Serializable {
    private int days;
    private int months;
    private int years;
}
