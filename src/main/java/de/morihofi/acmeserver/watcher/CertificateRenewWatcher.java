package de.morihofi.acmeserver.watcher;

import de.morihofi.acmeserver.config.CertificateExpiration;
import de.morihofi.acmeserver.tools.CertTools;
import de.morihofi.acmeserver.tools.PemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CertificateRenewWatcher {

    private final Logger log = LogManager.getLogger(getClass());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int RENEWAL_THRESHOLD_DAYS = 7; // Tage vor Ablauf, an denen das Zertifikat erneuert werden soll


    private Path privateKeyPath;
    private Path publicKeyPath;
    private Path certificatePath;
    private int period;
    private TimeUnit timeUnit;

    private Path caPrivateKeyPath;
    private Path caPublicKeyPath;
    private Path caCertificatePath;
    private Runnable execute;


    public CertificateRenewWatcher(Path privateKeyPath, Path publicKeyPath, Path certificatePath, int period, TimeUnit timeUnit, Runnable execute) {
        this.privateKeyPath = privateKeyPath;
        this.publicKeyPath = publicKeyPath;
        this.certificatePath = certificatePath;
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

            KeyPair keyPair = PemUtil.loadKeyPair(privateKeyPath, publicKeyPath);
            byte[] certificateBytes = CertTools.getCertificateBytes(certificatePath, keyPair);
            X509Certificate certificate = CertTools.convertToX509Cert(certificateBytes);

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
