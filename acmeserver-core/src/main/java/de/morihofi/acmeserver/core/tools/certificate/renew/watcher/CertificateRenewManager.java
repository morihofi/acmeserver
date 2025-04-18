/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.tools.certificate.renew.watcher;

import de.morihofi.acmeserver.core.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.core.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.core.tools.certificate.cryptoops.KeyStoreUtil;
import de.morihofi.acmeserver.core.tools.lambda.TriFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

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

/**
 * Manages the automatic renewal of certificates. Registers certificates to be monitored and renewed if they are about to expire.
 */
@Slf4j
public class CertificateRenewManager {
    private static final int PERIOD = 6;
    private static final TimeUnit TIME_UNIT = TimeUnit.HOURS;
    private static final int RENEWAL_THRESHOLD_DAYS = 7; // Tage vor Ablauf, an denen das Zertifikat erneuert werden soll


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final CryptoStoreManager cryptoStoreManager;
    private final Map<String, RenewEntry> renewMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor for CertificateRenewManager.
     *
     * @param cryptoStoreManager The CryptoStoreManager instance used for key and certificate management.
     */
    public CertificateRenewManager(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    /**
     * Registers a new certificate renew watcher with the specified alias and regeneration function.
     *
     * @param alias                The alias of the certificate in the keystore.
     * @param provisioner          The provisioner responsible for renewing the certificate.
     * @param regenerationFunction The function used to regenerate the certificate.
     */
    public void registerNewCertificateRenewWatcher(String alias, Provisioner provisioner,
                                                   TriFunction<Provisioner, X509Certificate, KeyPair, CertificateData> regenerationFunction) {
        registerNewCertificateRenewWatcher(alias, provisioner, regenerationFunction, null);
    }

    /**
     * Registers a new certificate renew watcher with the specified alias, regeneration function, and post-regeneration trigger.
     *
     * @param alias                    The alias of the certificate in the keystore.
     * @param provisioner              The provisioner responsible for renewing the certificate.
     * @param regenerationFunction     The function used to regenerate the certificate.
     * @param triggerAfterRegeneration The runnable to execute after the certificate has been regenerated.
     */
    public void registerNewCertificateRenewWatcher(String alias, Provisioner provisioner,
                                                   TriFunction<Provisioner, X509Certificate, KeyPair, CertificateData> regenerationFunction, Runnable triggerAfterRegeneration) {

        if (renewMap.containsKey(alias)) {
            throw new IllegalArgumentException("An watcher was already registered for keystore alias " + alias);
        }

        renewMap.put(alias, new RenewEntry(provisioner, regenerationFunction, triggerAfterRegeneration));
    }

    /**
     * Starts the scheduler that periodically checks for certificates that need to be renewed.
     */
    public void startScheduler() {
        log.info("Initialized Certificate Renew Scheduler");
        // Start the scheduled task
        scheduler.scheduleAtFixedRate(this::schedule, 0, PERIOD, TIME_UNIT);
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
     * Checks if the certificate needs to be renewed based on the configured threshold. If renewal is needed, the provided runnable is
     * executed.
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
                    CertificateData newCertificateData =
                            function.apply(provisioner, certificateFromKeyStore, KeyStoreUtil.getKeyPair(alias, keyStore));

                    if (newCertificateData.certificateChain() == null || newCertificateData.keyPair() == null) {
                        log.warn("Certificate for alias {} hasn't saved, because returned certificate chain or keypair is null", alias);
                        continue;
                    }

                    log.info("Saving certificate and key for alias {} in keystore", alias);
                    // Save the new certificate in keystore
                    keyStore.deleteEntry(alias);
                    keyStore.setKeyEntry(
                            alias,
                            newCertificateData.keyPair().getPrivate(),
                            "".toCharArray(),
                            newCertificateData.certificateChain()
                    );
                    cryptoStoreManager.saveKeystore();

                    if (renewEntry.triggerAfterRegeneration != null) {
                        log.info("Running post configuration runnable");
                        renewEntry.triggerAfterRegeneration.run();
                    }
                } else {
                    log.info("Certificate for alias {} doesn't need to be renewed -> NotAfter date {} is more than {} days in the future",
                            alias, certificateFromKeyStore.getNotAfter(), RENEWAL_THRESHOLD_DAYS);
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

    /**
     * Represents an entry in the renewal map, containing the provisioner, renewal function, and post-regeneration trigger.
     */
    private record RenewEntry(Provisioner provisioner,
                              TriFunction<Provisioner, X509Certificate, KeyPair, CertificateData> renewFunction, Runnable triggerAfterRegeneration) {
    }

    /**
     * Represents the data required for certificate renewal, including the certificate chain and key pair.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record CertificateData(X509Certificate[] certificateChain, KeyPair keyPair) {
    }
}
