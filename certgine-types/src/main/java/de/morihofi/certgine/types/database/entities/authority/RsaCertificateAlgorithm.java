package de.morihofi.certgine.types.database.entities.authority;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RsaCertificateAlgorithm extends CertificateAlgorithm {
    private Integer keySize;
}
