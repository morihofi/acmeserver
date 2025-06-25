package de.morihofi.acmeserver.types.database.entities.authority;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CertificateConfig implements Serializable {

    @Embedded
    private CertificateMetadata metadata;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "days", column = @Column(name = "int_exp_days")),
            @AttributeOverride(name = "months", column = @Column(name = "int_exp_months")),
            @AttributeOverride(name = "years", column = @Column(name = "int_exp_years"))
    })
    private CertificateExpiration expiration;

    @OneToOne(cascade = CascadeType.ALL)
    private CertificateAlgorithm certificateAlgorithm;
}

