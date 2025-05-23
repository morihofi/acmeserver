package de.morihofi.acmeserver.core.database.objects;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class ProvisionerMeta {
    private String website;
    private String tos;
}

