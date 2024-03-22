package de.morihofi.acmeserver.tools.certificate.renew.watcher;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.renew.IntermediateCaRenew;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;

public class CertificateRenewInitializer {

    private CertificateRenewInitializer(){}

    public static final Logger log = LogManager.getLogger(CertificateRenewInitializer.class);

    /**
     * Initializes a watcher for renewing an intermediate CA certificate.
     * This method sets up a new CertificateRenewWatcher to monitor and renew the intermediate CA certificate
     * at specified intervals. The renewal process involves retrieving the KeyStore, generating a new key pair,
     * and renewing the certificate through the IntermediateCaRenew utility.
     *
     * @param cryptoStoreManager The manager responsible for cryptographic storage operations.
     * @param alias              The alias under which the certificate is stored in the KeyStore.
     * @param provisioner        The provisioner instance associated with the certificate.
     * @param provisionerCfg     Configuration details for the provisioner.
     */
    public static void initializeIntermediateCertificateRenewWatcher(CryptoStoreManager cryptoStoreManager, String alias, Provisioner provisioner, ProvisionerConfig provisionerCfg) {
        log.info("Initializing renew watcher for intermediate ca of {} provisioner", provisioner.getProvisionerName());
        CertificateRenewWatcher watcher = new CertificateRenewWatcher(
                cryptoStoreManager,
                alias,
                6, TimeUnit.HOURS,
                () -> {
                    // Renew logic for the Intermediate Certificate
                    try {
                        KeyStore keyStore = cryptoStoreManager.getKeyStore();
                        KeyPair keyPair = new KeyPair(
                                keyStore.getCertificate(alias).getPublicKey(),
                                (PrivateKey) keyStore.getKey(alias, "".toCharArray())
                        );

                        log.info("Renewing Intermediate Certificate for {}", provisioner.getProvisionerName());
                        IntermediateCaRenew.renewIntermediateCertificate(keyPair, provisioner, provisionerCfg, cryptoStoreManager, alias);
                    } catch (Exception e) {
                        log.error("Error renewing Intermediate Certificate for {}", provisioner.getProvisionerName(), e);
                    }
                }
        );
        cryptoStoreManager.getCertificateRenewWatchers().add(watcher);
    }

}
