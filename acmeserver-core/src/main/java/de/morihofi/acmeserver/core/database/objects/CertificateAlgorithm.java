package de.morihofi.acmeserver.core.database.objects;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CertificateAlgorithm {

    public CertificateAlgorithm() {
    }

    public CertificateAlgorithm(String type, String curveName) {
        this.type = type;
        this.curveName = curveName;
    }

    public CertificateAlgorithm(String type, Integer keySize) {
        this.keySize = keySize;
        this.type = type;
    }

    private String type;

    // Nullable depending on algorithm
    private String curveName;
    private Integer keySize;
}

