package de.morihofi.acmeserver.types.database.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Embeddable
@Data
@AllArgsConstructor
public class CertificateConfig {

    @Embedded
    private CertificateMetadata metadata;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "days", column = @Column(name = "int_exp_days")),
            @AttributeOverride(name = "months", column = @Column(name = "int_exp_months")),
            @AttributeOverride(name = "years", column = @Column(name = "int_exp_years"))
    })
    private final CertificateExpiration expiration;

    @Embedded
    private final CertificateAlgorithm certificateAlgorithm;
}

