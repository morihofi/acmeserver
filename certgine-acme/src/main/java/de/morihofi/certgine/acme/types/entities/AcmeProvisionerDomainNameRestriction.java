/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Embeddable
@Data
@NoArgsConstructor
public class AcmeProvisionerDomainNameRestriction implements Serializable {
    private boolean enabled;

    @ElementCollection
    @CollectionTable(name = "provisioner_domain_suffixes", joinColumns = @JoinColumn(name = "provisioner_id"))
    @Column(name = "domain_suffix")
    private List<String> mustEndWith;
}

