package de.morihofi.acmeserver.certificate.acme.api;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.CertificateExpiration;
import de.morihofi.acmeserver.config.DomainNameRestrictionConfig;
import de.morihofi.acmeserver.config.MetadataConfig;

import java.security.KeyPair;
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

    public String getServerURL() {
        return "https://" + Main.appConfig.getServer().getDnsName() + ":" + Main.appConfig.getServer().getPorts().getHttps();
    }


    private String provisionerName;
    private X509Certificate intermediateCaCertificate;
    private KeyPair intermediateCaKeyPair;
    private MetadataConfig acmeMetadataConfig;
    private CertificateExpiration generatedCertificateExpiration;

    private DomainNameRestrictionConfig domainNameRestriction;

    public Provisioner(String provisionerName, X509Certificate intermediateCaCertificate, KeyPair intermediateCaKeyPair, MetadataConfig acmeMetadataConfig, CertificateExpiration generatedCertificateExpiration, DomainNameRestrictionConfig domainNameRestriction) {
        this.provisionerName = provisionerName;
        this.intermediateCaCertificate = intermediateCaCertificate;
        this.intermediateCaKeyPair = intermediateCaKeyPair;
        this.acmeMetadataConfig = acmeMetadataConfig;
        this.generatedCertificateExpiration = generatedCertificateExpiration;
        this.domainNameRestriction = domainNameRestriction;
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

    public X509Certificate getIntermediateCertificate(){
        return intermediateCaCertificate;
    }

    public KeyPair getIntermediateKeyPair() {
        return intermediateCaKeyPair;
    }

    public String getCrlPath() {
        return "/crl/" + getProvisionerName() + ".crl";
    }

    public String getOcspPath() {
        return "/" + getProvisionerName() + "/ocsp";
    }


    public X509Certificate getIntermediateCaCertificate() {
        return intermediateCaCertificate;
    }

    public void setIntermediateCaCertificate(X509Certificate intermediateCaCertificate) {
        this.intermediateCaCertificate = intermediateCaCertificate;
    }



    public void setIntermediateCaKeyPair(KeyPair intermediateCaKeyPair) {
        this.intermediateCaKeyPair = intermediateCaKeyPair;
    }

    public MetadataConfig getAcmeMetadataConfig() {
        return acmeMetadataConfig;
    }

    public void setAcmeMetadataConfig(MetadataConfig acmeMetadataConfig) {
        this.acmeMetadataConfig = acmeMetadataConfig;
    }
}
