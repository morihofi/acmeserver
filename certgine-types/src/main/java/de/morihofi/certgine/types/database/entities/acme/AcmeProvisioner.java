/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.database.entities.acme;

import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.intf.IServerInstance;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import de.morihofi.certgine.types.database.entities.authority.IntermediateCa;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class AcmeProvisioner implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String internalUuid;

    @Embedded
    private ProvisionerMeta meta;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "intermediate_id", nullable = false)
    private IntermediateCa intermediateCa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "root_ca_id", nullable = false)
    private RootCa rootCa;


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "days", column = @Column(name = "cert_exp_days")),
            @AttributeOverride(name = "months", column = @Column(name = "cert_exp_months")),
            @AttributeOverride(name = "years", column = @Column(name = "cert_exp_years"))
    })
    private CertificateExpiration issuedCertificateExpiration;

    private boolean wildcardAllowed;

    private boolean ipAllowed;

    /**
     * If true, ACME External Account Binding must be provided when creating new accounts.
     */
    private boolean externalAccountBindingRequired;

    @Embedded
    private AcmeProvisionerDomainNameRestriction acmeProvisionerDomainNameRestriction;

    /**
     * Get the Certgine URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    @NonNull
    public String getAcmeApiURL(@NonNull IServerInstance serverInstance) {
        return serverInstance.getServerURL() + "/acme/" + getName();
    }

    /**
     * Returns the full OCSP (Online Certificate Status Protocol) URL. This method combines the server URL with the OCSP path to construct
     * the full OCSP URL.
     *
     * @return A {@code String} representing the full OCSP URL.
     */
    @NonNull
    public String getFullOcspUrl(@NonNull IServerInstance serverInstance) {
        return serverInstance.getServerURL() + getOcspPath();
    }

    /**
     * Returns the full CRL (Certificate Revocation List) URL. This method concatenates the server URL with the CRL path to create the full
     * CRL URL.
     *
     * @return A {@code String} representing the full CRL URL.
     */
    @NonNull
    public String getFullCrlUrl(@NonNull IServerInstance serverInstance) {
        return serverInstance.getServerURL() + getCrlPath();
    }

    /**
     * Constructs and returns the path for the Certificate Revocation List (CRL). This method creates a path string for the CRL using the
     * provisioner's name. The path is typically used to access or store the CRL file in a specific directory structure.
     *
     * @return A {@code String} representing the path for the CRL file, specific to the provisioner.
     */
    @NonNull
    public String getCrlPath() {
        return "/revocation/" + getName() + "/crl/certs-revoked.crl";
    }

    /**
     * Constructs and returns the path for the Online Certificate Status Protocol (OCSP) service. This method generates the path used to
     * access the OCSP service, incorporating the provisioner's name. The path is usually part of the URL used to interact with the OCSP
     * service.
     *
     * @return A {@code String} representing the OCSP service path, associated with the provisioner.
     */
    @NonNull
    public String getOcspPath() {
        return "/revocation/" + getName() + "/ocsp";
    }




    public static AcmeProvisioner getForName(@NonNull IServerInstance si, @NonNull String name){
        AcmeProvisioner provisioner;
        try (Session s = si.getDatabaseSession()) {
            provisioner = s.createQuery("FROM AcmeProvisioner p WHERE p.name = :name", AcmeProvisioner.class)
                    .setParameter("name", name)
                    .uniqueResult();
        }
        return provisioner;
    }

    public static AcmeProvisioner[] getAllProvisioners(@NonNull IServerInstance si) {
        List<AcmeProvisioner> provisioners;
        try (Session s = si.getDatabaseSession()) {
            provisioners = s.createQuery("FROM AcmeProvisioner", AcmeProvisioner.class).list();
        }
        return provisioners.toArray(new AcmeProvisioner[0]);
    }

}

