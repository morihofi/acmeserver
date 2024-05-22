/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.provisioners;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRLGenerator;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRLScheduler;
import de.morihofi.acmeserver.config.CertificateExpiration;
import de.morihofi.acmeserver.config.DomainNameRestrictionConfig;
import de.morihofi.acmeserver.config.MetadataConfig;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

/**
 * Represents a Provisioner in a certificate management system. This class encapsulates all the necessary configurations and behaviors
 * associated with a provisioner. It includes details such as the provisioner's name, ACME metadata configuration, certificate expiration
 * settings, domain name restrictions, wildcard allowance, and manages cryptographic store operations.
 * <p>
 * The Provisioner class is responsible for handling various aspects of certificate provisioning and management, ensuring adherence to
 * specified security and operational policies.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class Provisioner {

    /**
     * The name of the provisioner. Immutable after initial assignment.
     */
    private final String provisionerName;
    /**
     * Manager for cryptographic store operations. Provides functionality for managing cryptographic elements such as keys and
     * certificates.
     */
    private final CryptoStoreManager cryptoStoreManager;
    /**
     * Configuration for domain name restrictions. Defines the constraints and rules for domain names in the context of this provisioner.
     */
    private final DomainNameRestrictionConfig domainNameRestriction;
    /**
     * Provisioner Config from config file
     */
    private final ProvisionerConfig config;
    /**
     * Configuration for ACME (Automated Certificate Management Environment) metadata. This configuration can be changed during the
     * lifecycle of the object.
     */
    private MetadataConfig acmeMetadataConfig;
    /**
     * Configuration for the expiration of generated certificates. This configuration can be adjusted as needed.
     */
    private CertificateExpiration generatedCertificateExpiration;
    /**
     * Flag indicating whether wildcards are allowed in certificate requests. Can be toggled to enable or disable wildcard support.
     */
    private boolean wildcardAllowed;
    private boolean ipAllowed;

    /**
     * Constructs a new Provisioner object. This constructor initializes the Provisioner with the specified settings and configurations. It
     * sets up various aspects like the provisioner's name, ACME metadata configuration, certificate expiration settings, domain name
     * restrictions, wildcard allowance, and the crypto store manager.
     *
     * @param provisionerName                The name of the provisioner.
     * @param acmeMetadataConfig             The ACME metadata configuration.
     * @param generatedCertificateExpiration The settings for the expiration of generated certificates.
     * @param domainNameRestriction          The configuration for domain name restrictions.
     * @param wildcardAllowed                A boolean value indicating whether wildcards are allowed.
     * @param cryptoStoreManager             The manager for cryptographic store operations.
     * @param config                         configuration of the provisioner
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Provisioner(String provisionerName, MetadataConfig acmeMetadataConfig, CertificateExpiration generatedCertificateExpiration,
            DomainNameRestrictionConfig domainNameRestriction, boolean wildcardAllowed, CryptoStoreManager cryptoStoreManager,
            ProvisionerConfig config, boolean ipAllowed) {
        this.provisionerName = provisionerName;
        this.acmeMetadataConfig = acmeMetadataConfig;
        this.generatedCertificateExpiration = generatedCertificateExpiration;
        this.domainNameRestriction = domainNameRestriction;
        this.wildcardAllowed = wildcardAllowed;
        this.cryptoStoreManager = cryptoStoreManager;
        this.config = config;
        this.ipAllowed = ipAllowed;
    }

    /**
     * Get the ACME Server URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    public String getApiURL() {
        return "https://" + Main.appConfig.getServer().getDnsName() + (Main.appConfig.getServer().getPorts().getHttps() != 443 ? ":"
                + Main.appConfig.getServer().getPorts().getHttps() : "") + "/acme/" + provisionerName;
    }

    /**
     * Retrieves the server URL constructed from the application's configuration. This method combines the DNS name and HTTPS port specified
     * in the app configuration to form the complete server URL.
     *
     * @return a String representing the full HTTPS URL of the server
     */
    public String getServerURL() {
        return "https://" + Main.appConfig.getServer().getDnsName() + (Main.appConfig.getServer().getPorts().getHttps() != 443 ? ":"
                + Main.appConfig.getServer().getPorts().getHttps() : "");
    }

    /**
     * Checks if wildcard is allowed in the configuration. This method returns a boolean indicating whether wildcard usage is permitted.
     *
     * @return {@code true} if wildcard is allowed, otherwise {@code false}.
     */
    public boolean isWildcardAllowed() {
        return wildcardAllowed;
    }

    /**
     * Sets the wildcard allowance status. This method allows enabling or disabling the usage of wildcards.
     *
     * @param wildcardAllowed A boolean value to set the wildcard allowance status.
     */
    public void setWildcardAllowed(boolean wildcardAllowed) {
        this.wildcardAllowed = wildcardAllowed;
    }

    /**
     * Retrieves the domain name restriction configuration. This configuration dictates the constraints on domain names. Note: The returned
     * object is a direct reference and any changes will affect the original object.
     *
     * @return The {@link DomainNameRestrictionConfig} object representing domain name restrictions.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public DomainNameRestrictionConfig getDomainNameRestriction() {
        return domainNameRestriction;
    }

    /**
     * Retrieves the configuration for generated certificate expiration. This object provides details about the expiration settings for
     * generated certificates. Note: The returned object is a direct reference and any changes will affect the original object.
     *
     * @return The {@link CertificateExpiration} object representing the expiration settings.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public CertificateExpiration getGeneratedCertificateExpiration() {
        return generatedCertificateExpiration;
    }

    /**
     * Sets the configuration for generated certificate expiration. This method allows setting the expiration details for newly generated
     * certificates. Note: The input object is used as a direct reference, and changes to it will reflect in the system.
     *
     * @param generatedCertificateExpiration The {@link CertificateExpiration} object to set.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setGeneratedCertificateExpiration(CertificateExpiration generatedCertificateExpiration) {
        this.generatedCertificateExpiration = generatedCertificateExpiration;
    }

    /**
     * Retrieves the name of the provisioner. This method returns the name assigned to the provisioner, which is used in various other
     * operations within the system.
     *
     * @return A {@code String} representing the name of the provisioner.
     */
    public String getProvisionerName() {
        return provisionerName;
    }

    /**
     * Constructs and returns the path for the Certificate Revocation List (CRL). This method creates a path string for the CRL using the
     * provisioner's name. The path is typically used to access or store the CRL file in a specific directory structure.
     *
     * @return A {@code String} representing the path for the CRL file, specific to the provisioner.
     */
    public String getCrlPath() {
        return "/acme/crl/" + getProvisionerName() + ".crl";
    }

    /**
     * Constructs and returns the path for the Online Certificate Status Protocol (OCSP) service. This method generates the path used to
     * access the OCSP service, incorporating the provisioner's name. The path is usually part of the URL used to interact with the OCSP
     * service.
     *
     * @return A {@code String} representing the OCSP service path, associated with the provisioner.
     */
    public String getOcspPath() {
        return "/acme/" + getProvisionerName() + "/ocsp";
    }

    /**
     * Retrieves the intermediate Certificate Authority (CA) certificate. This method fetches the X.509 certificate associated with the
     * intermediate CA from the KeyStore. It uses a specific alias to locate the certificate.
     *
     * @return The intermediate CA's {@link X509Certificate}.
     * @throws KeyStoreException If an error occurs while accessing the KeyStore.
     */
    public X509Certificate getIntermediateCaCertificate() throws KeyStoreException {

        String alias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);
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

    /**
     * Returns the full OCSP (Online Certificate Status Protocol) URL. This method combines the server URL with the OCSP path to construct
     * the full OCSP URL.
     *
     * @return A {@code String} representing the full OCSP URL.
     */
    public String getFullOcspUrl() {
        return getServerURL() + getOcspPath();
    }

    /**
     * Returns the full CRL (Certificate Revocation List) URL. This method concatenates the server URL with the CRL path to create the full
     * CRL URL.
     *
     * @return A {@code String} representing the full CRL URL.
     */
    public String getFullCrlUrl() {
        return getServerURL() + getCrlPath();
    }

    /**
     * Retrieves the instance of the CryptoStoreManager. This manager is responsible for managing cryptographic elements such as keys and
     * certificates.
     *
     * @return The {@code CryptoStoreManager} instance currently in use.
     */
    public CryptoStoreManager getCryptoStoreManager() {
        return cryptoStoreManager;
    }

    public ProvisionerConfig getConfig() {
        return config;
    }

    public CRLGenerator getCrlGenerator() {
        return CRLScheduler.getCrlGeneratorForProvisioner(provisionerName);
    }

    public boolean isIpAllowed() {
        return ipAllowed;
    }

    public void setIpAllowed(boolean ipAllowed) {
        this.ipAllowed = ipAllowed;
    }
}
