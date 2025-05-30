package de.morihofi.acmeserver.types.database.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
public class ProvisionerMeta implements Serializable {
    private final String website;
    private final String tos;
}

