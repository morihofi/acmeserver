package de.morihofi.acmeserver.core.database.objects;

import de.morihofi.acmeserver.core.tools.ServerInstance;
import de.morihofi.acmeserver.core.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.Session;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

@Entity
@Data
public class AcmeProvisioner {

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
    public String getAcmeApiURL(ServerInstance serverInstance) {
        return serverInstance.getServerURL() + "/acme/" + getName();
    }

    /**
     * Returns the full OCSP (Online Certificate Status Protocol) URL. This method combines the server URL with the OCSP path to construct
     * the full OCSP URL.
     *
     * @return A {@code String} representing the full OCSP URL.
     */
    public String getFullOcspUrl(ServerInstance serverInstance) {
        return serverInstance.getServerURL() + getOcspPath();
    }

    /**
     * Returns the full CRL (Certificate Revocation List) URL. This method concatenates the server URL with the CRL path to create the full
     * CRL URL.
     *
     * @return A {@code String} representing the full CRL URL.
     */
    public String getFullCrlUrl(ServerInstance serverInstance) {
        return serverInstance.getServerURL() + getCrlPath();
    }

    /**
     * Constructs and returns the path for the Certificate Revocation List (CRL). This method creates a path string for the CRL using the
     * provisioner's name. The path is typically used to access or store the CRL file in a specific directory structure.
     *
     * @return A {@code String} representing the path for the CRL file, specific to the provisioner.
     */
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
    public X509Certificate getIntermediateCaCertificate(CryptoStoreManager cryptoStoreManager) throws KeyStoreException {

        String alias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(getName());
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
    public KeyPair getIntermediateCaKeyPair(CryptoStoreManager cryptoStoreManager) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {

        String alias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(getName());

        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        return new KeyPair(
                keyStore.getCertificate(alias).getPublicKey(),
                (PrivateKey) keyStore.getKey(alias, "".toCharArray())
        );
    }


    public static AcmeProvisioner getForName(ServerInstance si, String name){
        AcmeProvisioner provisioner;
        try (Session s = Objects.requireNonNull(si.getHibernateUtil().getSessionFactory()).openSession()) {
            provisioner = s.createQuery("FROM AcmeProvisioner p WHERE p.name = :name", AcmeProvisioner.class)
                    .setParameter("name", name)
                    .getSingleResult();
        }
        return provisioner;
    }

    public static AcmeProvisioner[] getAllProvisioners(ServerInstance si) {
        List<AcmeProvisioner> provisioners;
        try (Session s = Objects.requireNonNull(si.getHibernateUtil().getSessionFactory()).openSession()) {
            provisioners = s.createQuery("FROM AcmeProvisioner", AcmeProvisioner.class).list();
        }
        return provisioners.toArray(new AcmeProvisioner[0]);
    }

    public static AcmeProvisioner getProvisionerFromJavalin(ServerInstance si, Context context){
        String pName = context.pathParam("provisioner");
        AcmeProvisioner provisioner = AcmeProvisioner.getForName(si, pName);
        if(provisioner == null){
            throw new IllegalArgumentException("Specified Provisioner " + pName + " does not exist");
        }

        return provisioner;
    }

}

