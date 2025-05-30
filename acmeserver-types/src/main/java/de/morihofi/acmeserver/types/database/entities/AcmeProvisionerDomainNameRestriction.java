package de.morihofi.acmeserver.types.database.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Embeddable
@Data
public class AcmeProvisionerDomainNameRestriction implements Serializable {
    private boolean enabled;

    @ElementCollection
    @CollectionTable(name = "provisioner_domain_suffixes", joinColumns = @JoinColumn(name = "provisioner_id"))
    @Column(name = "domain_suffix")
    private List<String> mustEndWith;
}

