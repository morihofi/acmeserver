package de.morihofi.acmeserver.types.database.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcdsaCertificateAlgorithm extends CertificateAlgorithm {
    private String curveName;
}
