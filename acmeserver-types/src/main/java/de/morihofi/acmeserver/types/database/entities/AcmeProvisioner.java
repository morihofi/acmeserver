package de.morihofi.acmeserver.types.database.entities;

import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.Session;

import java.io.Serializable;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.List;

@Entity
@Data
public class AcmeProvisioner implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Embedded
    private ProvisionerMeta meta;

    @Embedded
    private CertificateConfig certificateConfig;

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

    @Embedded
    private AcmeProvisionerDomainNameRestriction acmeProvisionerDomainNameRestriction;

    /**
     * Get the ACME Server URL, reachable from other Hosts
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
        return "/acme/crl/" + getName() + "/certs-revoked.crl";
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
        return "/acme/" + getName() + "/ocsp";
    }

    /**
     * Retrieves the intermediate Certificate Authority (CA) certificate. This method fetches the X.509 certificate associated with the
     * intermediate CA from the KeyStore. It uses a specific alias to locate the certificate.
     *
     * @return The intermediate CA's {@link X509Certificate}.
     * @throws KeyStoreException If an error occurs while accessing the KeyStore.
     */
    @NonNull
    public X509Certificate getIntermediateCaCertificate(@NonNull ICryptoStoreManager cryptoStoreManager) throws KeyStoreException {

        String alias = cryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(getName());
        KeyStore keyStore = cryptoStoreManager.getKeyStore();
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    /**
     * Retrieves the KeyPair associated with the intermediate Certificate Authority (CA). This method fetches both the public and private
     * keys for the intermediate CA from the KeyStore. It utilizes a specific alias to locate these keys.
     *
     * @return A {@link KeyPair} consisting of the intermediate CA's public and private keys.
     * @throws KeyStoreException         If an error occurs while accessing the KeyStore.
     * @throws UnrecoverableKeyException If the key cannot be recovered (typically due to an incorrect password or corruption).
     * @throws NoSuchAlgorithmException  If the algorithm for recovering the key is not available.
     */
    public KeyPair getIntermediateCaKeyPair(@NonNull ICryptoStoreManager cryptoStoreManager) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {

        String alias = cryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(getName());

        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        return new KeyPair(
                keyStore.getCertificate(alias).getPublicKey(),
                (PrivateKey) keyStore.getKey(alias, "".toCharArray())
        );
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

