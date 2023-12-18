package de.morihofi.acmeserver.certificate.acme.api;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.CertificateExpiration;
import de.morihofi.acmeserver.config.DomainNameRestrictionConfig;
import de.morihofi.acmeserver.config.MetadataConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;

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

    public DomainNameRestrictionConfig getDomainNameRestriction() {
        return domainNameRestriction;
    }

    public CertificateExpiration getGeneratedCertificateExpiration() {
        return generatedCertificateExpiration;
    }

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
        KeyPair keyPair = new KeyPair(
                keyStore.getCertificate(alias).getPublicKey(),
                (PrivateKey) keyStore.getKey(alias, "".toCharArray())
        );

        return keyPair;
    }


    public MetadataConfig getAcmeMetadataConfig() {
        return acmeMetadataConfig;
    }

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

    @Deprecated(forRemoval = true)
    public void setIntermediateCaCertificate(X509Certificate intermediateCertificate) {
        //This function does not do anything. It will be removed
    }
    @Deprecated(forRemoval = true)
    public void setIntermediateCaKeyPair(KeyPair intermediateKeyPair) {
        //This function does not do anything. It will be removed
    }
}
