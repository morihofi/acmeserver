package de.morihofi.acmeserver.types.database.entities.acme;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvisionerMeta implements Serializable {
    private String website;
    private String tos;
}

