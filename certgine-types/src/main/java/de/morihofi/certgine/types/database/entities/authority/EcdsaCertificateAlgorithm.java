package de.morihofi.certgine.types.database.entities.authority;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class EcdsaCertificateAlgorithm extends CertificateAlgorithm {
    private String curveName;
}
