package de.morihofi.acmeserver.types.database.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RsaCertificateAlgorithm extends CertificateAlgorithm{
    private Integer keySize;
}
