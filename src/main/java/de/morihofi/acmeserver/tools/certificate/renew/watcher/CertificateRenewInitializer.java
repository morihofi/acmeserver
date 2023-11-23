package de.morihofi.acmeserver.tools.certificate.renew.watcher;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.certificate.renew.IntermediateCaRenew;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class CertificateRenewInitializer {

    public static final Logger log = LogManager.getLogger(CertificateRenewInitializer.class);

    /**
     * Initializes a certificate renew watcher for the intermediate CA certificate of a provisioner.
     *
     * @param privateKeyPath  The path to the provisioner's private key file.
     * @param publicKeyPath   The path to the provisioner's public key file.
     * @param certificatePath The path to the provisioner's certificate file.
     * @param provisioner     The provisioner for which the certificate renew watcher is being initialized.
     * @param provisionerCfg  The configuration for the provisioner.
     * @param caKeyPair       The key pair of the Certificate Authority (CA) issuing the certificate.
     */
    public static void initializeIntermediateCertificateRenewWatcher(Path privateKeyPath, Path publicKeyPath, Path certificatePath, Provisioner provisioner, ProvisionerConfig provisionerCfg, KeyPair caKeyPair, X509Certificate caCertificate) {
        log.info("Initializing renew watcher for intermediate ca of " + provisioner.getProvisionerName() + " provisioner");
        CertificateRenewWatcher watcher = new CertificateRenewWatcher(
                privateKeyPath,
                publicKeyPath,
                certificatePath,
                6, TimeUnit.HOURS,
                () -> {
                    // Renew logic for the Intermediate Certificate
                    try {
                        log.info("Renewing Intermediate Certificate for " + provisioner.getProvisionerName());
                        IntermediateCaRenew.renewIntermediateCertificate(privateKeyPath, publicKeyPath, certificatePath, provisioner, provisionerCfg, caKeyPair, caCertificate);
                    } catch (Exception e) {
                        log.error("Error renewing Intermediate Certificate for " + provisioner.getProvisionerName(), e);
                    }
                }
        );
        // You might want to store the watcher reference if needed
    }

}
