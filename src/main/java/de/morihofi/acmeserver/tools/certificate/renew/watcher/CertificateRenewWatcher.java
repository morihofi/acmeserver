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

/**
 * A utility class for monitoring and renewing X.509 certificates before they expire.
 * This class periodically checks if a certificate needs renewal based on the configured period and threshold.
 * If the certificate is close to expiration, it triggers a renewal process by executing a provided runnable.
 */
public class CertificateRenewWatcher {

    private final Logger log = LogManager.getLogger(getClass());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int RENEWAL_THRESHOLD_DAYS = 7; // Tage vor Ablauf, an denen das Zertifikat erneuert werden soll

    private final CryptoStoreManager cryptoStoreManager;
    private final String alias;
    private final int period;
    private final TimeUnit timeUnit;
    private final Runnable execute;


    /**
     * Initializes the CertificateRenewWatcher.
     *
     * @param cryptoStoreManager The CryptoStoreManager responsible for managing the certificate.
     * @param alias              The alias of the certificate to monitor and renew.
     * @param period             The period at which to check for certificate renewal.
     * @param timeUnit           The time unit for the period (e.g., TimeUnit.MINUTES).
     * @param execute            A runnable to execute when certificate renewal is needed.
     */
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

    /**
     * Checks if the certificate needs to be renewed based on the configured threshold.
     * If renewal is needed, the provided runnable is executed.
     */
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
     * Gets the configured period for certificate renewal checks.
     *
     * @return The period in the specified time unit.
     */
    public int getPeriod() {
        return period;
    }

    /**
     * Gets the configured time unit for the certificate renewal checks.
     *
     * @return The time unit (e.g., TimeUnit.MINUTES).
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
