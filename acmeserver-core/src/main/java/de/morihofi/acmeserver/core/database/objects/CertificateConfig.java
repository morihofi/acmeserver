package de.morihofi.acmeserver.core.database.objects;

import jakarta.persistence.*;
import lombok.Data;

@Embeddable
@Data
public class CertificateConfig {

    public CertificateConfig() {
    }

    public CertificateConfig(CertificateMetadata metadata, CertificateExpiration expiration, CertificateAlgorithm certificateAlgorithm) {
        this.metadata = metadata;
        this.expiration = expiration;
        this.certificateAlgorithm = certificateAlgorithm;
    }

    @Embedded
    private CertificateMetadata metadata;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "days", column = @Column(name = "int_exp_days")),
            @AttributeOverride(name = "months", column = @Column(name = "int_exp_months")),
            @AttributeOverride(name = "years", column = @Column(name = "int_exp_years"))
    })
    private CertificateExpiration expiration;

    @Embedded
    private CertificateAlgorithm certificateAlgorithm;
}

