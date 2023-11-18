package de.morihofi.acmeserver.certificate.acme.api;

import de.morihofi.acmeserver.Main;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class Provisioner {



    /**
     * Get the ACME Server URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    public String getApiURL() {
        return "https://" + Main.acmeThisServerDNSName + ":" + Main.acmeThisServerAPIPort + "/" + provisionerName;
    }

    public String getServerURL() {
        return "https://" + Main.acmeThisServerDNSName + ":" + Main.acmeThisServerAPIPort;
    }


    private String provisionerName;
    private X509Certificate intermediateCaCertificate;
    private KeyPair intermediateCaKeyPair;

    public Provisioner(String provisionerName, X509Certificate intermediateCaCertificate, KeyPair intermediateCaKeyPair) {
        this.provisionerName = provisionerName;
        this.intermediateCaCertificate = intermediateCaCertificate;
        this.intermediateCaKeyPair = intermediateCaKeyPair;
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
}
