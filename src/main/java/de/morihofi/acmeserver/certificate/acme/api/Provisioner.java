package de.morihofi.acmeserver.certificate.acme.api;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.CertificateExpiration;
import de.morihofi.acmeserver.config.DomainNameRestrictionConfig;
import de.morihofi.acmeserver.config.MetadataConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.security.*;
import java.security.cert.X509Certificate;

public class Provisioner {


    /**
     * Get the ACME Server URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    public String getApiURL() {
        return "https://" + Main.appConfig.getServer().getDnsName() + ":" + Main.appConfig.getServer().getPorts().getHttps() + "/" + provisionerName;
    }

    /**
     * Retrieves the server URL constructed from the application's configuration.
     * This method combines the DNS name and HTTPS port specified in the app configuration
     * to form the complete server URL.
     *
     * @return a String representing the full HTTPS URL of the server
     */
    public String getServerURL() {
        return "https://" + Main.appConfig.getServer().getDnsName() + ":" + Main.appConfig.getServer().getPorts().getHttps();
    }


    private final String provisionerName;
    private MetadataConfig acmeMetadataConfig;
    private CertificateExpiration generatedCertificateExpiration;
    private boolean wildcardAllowed;
    private final CryptoStoreManager cryptoStoreManager;

    private final DomainNameRestrictionConfig domainNameRestriction;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Provisioner(String provisionerName, X509Certificate intermediateCaCertificate, KeyPair intermediateCaKeyPair, MetadataConfig acmeMetadataConfig, CertificateExpiration generatedCertificateExpiration, DomainNameRestrictionConfig domainNameRestriction, boolean wildcardAllowed, CryptoStoreManager cryptoStoreManager) {
        this.provisionerName = provisionerName;
        this.acmeMetadataConfig = acmeMetadataConfig;
        this.generatedCertificateExpiration = generatedCertificateExpiration;
        this.domainNameRestriction = domainNameRestriction;
        this.wildcardAllowed = wildcardAllowed;
        this.cryptoStoreManager = cryptoStoreManager;
    }

    public boolean isWildcardAllowed() {
        return wildcardAllowed;
    }

    public void setWildcardAllowed(boolean wildcardAllowed) {
        this.wildcardAllowed = wildcardAllowed;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public DomainNameRestrictionConfig getDomainNameRestriction() {
        return domainNameRestriction;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public CertificateExpiration getGeneratedCertificateExpiration() {
        return generatedCertificateExpiration;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setGeneratedCertificateExpiration(CertificateExpiration generatedCertificateExpiration) {
        this.generatedCertificateExpiration = generatedCertificateExpiration;
    }

    public String getProvisionerName() {
        return provisionerName;
    }


    public String getCrlPath() {
        return "/crl/" + getProvisionerName() + ".crl";
    }

    public String getOcspPath() {
        return "/" + getProvisionerName() + "/ocsp";
    }


    public X509Certificate getIntermediateCaCertificate() throws KeyStoreException {

        String alias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);
        KeyStore keyStore = cryptoStoreManager.getKeyStore();
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    public KeyPair getIntermediateCaKeyPair() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {

        String alias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);

        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        return new KeyPair(
                keyStore.getCertificate(alias).getPublicKey(),
                (PrivateKey) keyStore.getKey(alias, "".toCharArray())
        );
    }


    public MetadataConfig getAcmeMetadataConfig() {
        return acmeMetadataConfig;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setAcmeMetadataConfig(MetadataConfig acmeMetadataConfig) {
        this.acmeMetadataConfig = acmeMetadataConfig;
    }

    public String getFullOcspUrl() {
        return getServerURL() + getOcspPath();
    }

    public String getFullCrlUrl() {
        return getServerURL() + getCrlPath();
    }

    public CryptoStoreManager getCryptoStoreManager() {
        return cryptoStoreManager;
    }

}
