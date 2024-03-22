package de.morihofi.acmeserver.tools.certificate.renew.watcher;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.cryptoops.KeyStoreUtil;
import de.morihofi.acmeserver.tools.lambda.TriFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CertificateRenewManager {
    private static final int period = 6;
    private static final TimeUnit timeUnit = TimeUnit.HOURS;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int RENEWAL_THRESHOLD_DAYS = 7; // Tage vor Ablauf, an denen das Zertifikat erneuert werden soll

    private final CryptoStoreManager cryptoStoreManager;

    private final Map<String, RenewEntry> renewMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().getClass());

    public CertificateRenewManager(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }


    public void registerNewCertificateRenewWatcher(String alias, Provisioner provisioner, TriFunction<Provisioner, X509Certificate, KeyPair, CertificateData> regenerationFunction) {
        registerNewCertificateRenewWatcher(alias, provisioner, regenerationFunction, null);
    }

    public void registerNewCertificateRenewWatcher(String alias, Provisioner provisioner, TriFunction<Provisioner, X509Certificate, KeyPair, CertificateData> regenerationFunction, Runnable triggerAfterRegeneration) {

        if (renewMap.containsKey(alias)) {
            throw new IllegalArgumentException("An watcher was already registered for keystore alias " + alias);
        }

        renewMap.put(alias, new RenewEntry(provisioner, regenerationFunction, triggerAfterRegeneration));
    }

    public void startScheduler() {
        log.info("Initialized Certificate Renew Scheduler");
        // Start the scheduled task
        scheduler.scheduleAtFixedRate(this::schedule, 0, period, timeUnit);
    }


    /**
     * Determines if the certificate should be renewed based on the configured threshold.
     *
     * @param certificate The X.509 certificate to check.
     * @return True if the certificate should be renewed; otherwise, false.
     */
    private boolean shouldRenew(X509Certificate certificate) {
        Date now = new Date();
        Date expiryDate = certificate.getNotAfter();
        long daysUntilExpiry = (expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
        return daysUntilExpiry <= RENEWAL_THRESHOLD_DAYS;
    }


    /**
     * Checks if the certificate needs to be renewed based on the configured threshold.
     * If renewal is needed, the provided runnable is executed.
     */
    @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
    private void schedule() {

        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        for (Map.Entry<String, RenewEntry> entry : renewMap.entrySet()) {
            String alias = entry.getKey();
            RenewEntry renewEntry = entry.getValue();

            TriFunction<Provisioner, X509Certificate, KeyPair, CertificateData> function = renewEntry.renewFunction();
            Provisioner provisioner = renewEntry.provisioner();

            log.info("Checking if certificate for alias {} needs to be renewed", alias);
            try {
                X509Certificate certificateFromKeyStore = (X509Certificate) keyStore.getCertificate(alias);

                if (shouldRenew(certificateFromKeyStore)) {

                    log.info("Certificate for alias {} needs to be renewed, renewing now ...", alias);

                    // Now we call the user defined function to renew the certificate
                    CertificateData newCertificateData = function.apply(provisioner, certificateFromKeyStore, KeyStoreUtil.getKeyPair(alias, keyStore));

                    if (newCertificateData.certificateChain() == null || newCertificateData.keyPair() == null) {
                        log.warn("Certificate for alias {} hasn't saved, because returned certificate chain or keypair is null", alias);
                        continue;
                    }

                    log.info("Saving certificate and key for alias {} in keystore", alias);
                    //Save the new certificate in keystore
                    keyStore.deleteEntry(alias);
                    keyStore.setKeyEntry(
                            alias,
                            newCertificateData.keyPair().getPrivate(),
                            "".toCharArray(),
                            newCertificateData.certificateChain()
                    );
                    cryptoStoreManager.saveKeystore();

                    if(renewEntry.triggerAfterRegeneration != null){
                        log.info("Running post configuration runnable");
                        renewEntry.triggerAfterRegeneration.run();
                    }

                }else{
                    log.info("Certificate for alias {} doesn't need to be renewed -> NotAfter date {} is more than {} days in the future", alias, certificateFromKeyStore.getNotAfter(), RENEWAL_THRESHOLD_DAYS);
                }
            } catch (Exception ex) {
                log.error("Error renewing certificate", ex);
            }

        }


    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        log.info("Certificate Renew Watcher is shutting down");
        scheduler.shutdown();
        renewMap.clear();
    }

    private record RenewEntry(Provisioner provisioner,
                              TriFunction<Provisioner, X509Certificate, KeyPair, CertificateData> renewFunction, Runnable triggerAfterRegeneration) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record CertificateData(X509Certificate[] certificateChain, KeyPair keyPair) {
    }
}
