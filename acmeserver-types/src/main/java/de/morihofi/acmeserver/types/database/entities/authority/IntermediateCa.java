package de.morihofi.acmeserver.types.database.entities.authority;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * Represents an intermediate certificate authority configuration.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntermediateCa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private CertificateConfig certificateConfig;

    @Column(unique = true, nullable = false)
    private String internalUuid;
}
