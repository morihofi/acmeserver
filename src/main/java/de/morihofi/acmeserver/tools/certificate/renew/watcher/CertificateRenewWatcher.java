package de.morihofi.acmeserver.tools.certificate.renew.watcher;

import de.morihofi.acmeserver.tools.certificate.X509;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CertificateRenewWatcher {

    private final Logger log = LogManager.getLogger(getClass());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int RENEWAL_THRESHOLD_DAYS = 7; // Tage vor Ablauf, an denen das Zertifikat erneuert werden soll

    private final CryptoStoreManager cryptoStoreManager;
    private final String alias;
    private final int period;
    private final TimeUnit timeUnit;
    private final Runnable execute;


    public CertificateRenewWatcher(CryptoStoreManager cryptoStoreManager, String alias, int period, TimeUnit timeUnit, Runnable execute) {
        this.cryptoStoreManager = cryptoStoreManager;
        this.alias = alias;
        this.period = period;
        this.timeUnit = timeUnit;
        this.execute = execute;


        log.info("Initialized Certificate renew watcher");
        // Start the scheduled task to update the CRL every 5 minutes (must be same as in generateCRL() function
        scheduler.scheduleAtFixedRate(this::check, 0, period, timeUnit);
    }

    private void check() {
        log.info("Checking if certificate needs to be renewed...");


        try {
            KeyStore keyStore = cryptoStoreManager.getKeyStore();

            byte[] certificateBytes = keyStore.getCertificate(alias).getEncoded();
            X509Certificate certificate = X509.convertToX509Cert(certificateBytes);

            if (shouldRenew(certificate)) {
                execute.run();
            }
        } catch (Exception e) {
            log.error("Error checking/renewing certificate", e);
        }


    }


    private boolean shouldRenew(X509Certificate certificate) {
        Date now = new Date();
        Date expiryDate = certificate.getNotAfter();
        long daysUntilExpiry = (expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
        return daysUntilExpiry <= RENEWAL_THRESHOLD_DAYS;
    }

}
