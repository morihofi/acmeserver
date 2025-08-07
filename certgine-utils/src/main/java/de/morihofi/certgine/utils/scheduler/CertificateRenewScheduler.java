/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.scheduler;


import de.morihofi.certgine.types.cryptography.CryptoStoreManagerConstants;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.utils.lambda.BiFunctionWithException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Scheduler that periodically checks registered certificates and renews them
 * when they are close to expiry.
 */
@Slf4j
public class CertificateRenewScheduler {
    public static final String DEFAULT_CRON = "0 */6 * * *"; // every six hours
    private static final int RENEWAL_THRESHOLD_DAYS = 7; // days before expiration for trigger renewal

    private final ICryptoStoreManager cryptoStoreManager;
    private final Clock clock;
    private final Map<String, RenewEntry> renewMap = Collections.synchronizedMap(new HashMap<>());
    private final TimedScheduler timedScheduler;
    private final TimedScheduler.ScheduledHandle scheduleHandle;

    /**
     * Constructs a new scheduler instance.
     *
     * @param cryptoStoreManager The CryptoStoreManager instance used for key and certificate management.
     * @param clock              Clock used for time calculations.
     */
    public CertificateRenewScheduler(
            ICryptoStoreManager cryptoStoreManager,
            Clock clock) {
        this.cryptoStoreManager = cryptoStoreManager;
        this.clock = clock;
        this.timedScheduler = new TimedScheduler();
        this.scheduleHandle = this.timedScheduler.schedule(DEFAULT_CRON, this::schedule);
    }

    /**
     * Constructs a new scheduler instance using the system UTC clock.
     *
     * @param cryptoStoreManager The CryptoStoreManager instance used for key and certificate management.
     * @param eventBus           Event bus for publishing renewal events.
     */
    public CertificateRenewScheduler(
            ICryptoStoreManager cryptoStoreManager) {
        this(cryptoStoreManager, Clock.systemUTC());
    }

    /**
     * Registers a new certificate renew watcher with the specified alias and regeneration function.
     *
     * @param alias                The alias of the certificate in the keystore.
     * @param provisioner          The provisioner responsible for renewing the certificate.
     * @param regenerationFunction The function used to regenerate the certificate.
     */
    public void registerNewCertificateRenewWatcher(String alias,
                                                   BiFunctionWithException<X509Certificate, KeyPair, CertificateData> regenerationFunction) {
        registerNewCertificateRenewWatcher(alias, regenerationFunction, null);
    }

    /**
     * Registers a new certificate renew watcher with the specified alias, regeneration function, and post-regeneration trigger.
     *
     * @param alias                    The alias of the certificate in the keystore.
     * @param provisioner              The provisioner responsible for renewing the certificate.
     * @param regenerationFunction     The function used to regenerate the certificate.
     * @param triggerAfterRegeneration The runnable to execute after the certificate has been regenerated.
     */
    public void registerNewCertificateRenewWatcher(String alias,
                                                   BiFunctionWithException<X509Certificate, KeyPair, CertificateData> regenerationFunction,
                                                   Runnable triggerAfterRegeneration) {

        if (renewMap.containsKey(alias)) {
            throw new IllegalArgumentException("An watcher was already registered for keystore alias " + alias);
        }

        renewMap.put(alias, new RenewEntry(regenerationFunction, triggerAfterRegeneration));
    }

    /**
     * Removes a previously registered renew watcher.
     *
     * @param alias keystore alias of the watcher to remove
     */
    public void unregisterCertificateRenewWatcher(String alias) {
        renewMap.remove(alias);
    }

    /**
     * Checks whether a renew watcher is already registered for the given alias.
     *
     * @param alias keystore alias to check
     * @return {@code true} if a watcher for this alias exists
     */
    public boolean isWatcherRegistered(String alias) {
        return renewMap.containsKey(alias);
    }

    /**
     * Determines if the certificate should be renewed based on the configured threshold.
     *
     * @param certificate The X.509 certificate to check.
     * @return True if the certificate should be renewed; otherwise, false.
     */
    private boolean shouldRenew(X509Certificate certificate) {
        Instant now = Instant.now(clock);
        Instant expiryInstant = certificate.getNotAfter().toInstant();
        long daysUntilExpiry = Duration.between(now, expiryInstant).toDays();
        return daysUntilExpiry <= RENEWAL_THRESHOLD_DAYS;
    }

    private X509Certificate fetchCertificate(String alias) throws Exception {
        X509Certificate certificate = cryptoStoreManager.getCertificate(alias);
        if (certificate == null) {
            log.warn("Certificate for alias {} does not exist", alias);
        }
        return certificate;
    }

    private void performRenewal(String alias, RenewEntry renewEntry, X509Certificate certificate) throws Exception {
        CertificateData newCertificateData =
                renewEntry.renewFunction().apply(certificate, cryptoStoreManager.getKeyPairForAlias(alias));

        if (newCertificateData.certificateChain() == null || newCertificateData.keyPair() == null) {
            log.warn(
                    "Certificate for alias {} hasn't saved, because returned certificate chain or keypair is null",
                    alias);
            return;
        }

        log.info("Saving certificate and key for alias {} in keystore", alias);
        if (alias.startsWith(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA)) {
            String id = alias.substring(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA.length());
            cryptoStoreManager.addIntermediateCertificateAuthority(
                    newCertificateData.certificateChain(), newCertificateData.keyPair(), id);
        } else if (alias.startsWith(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA)) {
            String id = alias.substring(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA.length());
            cryptoStoreManager.addTimestampAuthority(
                    newCertificateData.certificateChain(), newCertificateData.keyPair(), id);
        } else {
            cryptoStoreManager.addServerCertificate(
                    newCertificateData.certificateChain(), newCertificateData.keyPair(), alias);
        }
        postRenewalActions(alias, renewEntry);
    }

    private void postRenewalActions(String alias, RenewEntry renewEntry) {
        if (renewEntry.triggerAfterRegeneration != null) {
            log.info("Running post configuration runnable for alias {}", alias);
            renewEntry.triggerAfterRegeneration.run();
        }
    }

    /**
     * Checks if the certificate needs to be renewed based on the configured threshold. If renewal is needed, the provided runnable is
     * executed.
     */
    @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
    public void schedule() {
        for (Map.Entry<String, RenewEntry> entry : renewMap.entrySet()) {
            String alias = entry.getKey();
            RenewEntry renewEntry = entry.getValue();

            log.info("Checking if certificate for alias {} needs to be renewed", alias);
            try {
                X509Certificate certificate = fetchCertificate(alias);
                if (certificate == null) {
                    continue;
                }

                if (shouldRenew(certificate)) {
                    log.info("Certificate for alias {} needs to be renewed, renewing now ...", alias);
                    performRenewal(alias, renewEntry, certificate);
                } else {
                    ZonedDateTime notAfter = certificate.getNotAfter().toInstant().atZone(clock.getZone());
                    log.info(
                            "Certificate for alias {} doesn't need to be renewed -> NotAfter date {} is more than {} days in the future",
                            alias,
                            notAfter.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            RENEWAL_THRESHOLD_DAYS);
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
        renewMap.clear();
        scheduleHandle.cancel();
        timedScheduler.shutdown();
    }

    /**
     * Represents an entry in the renewal map, containing the provisioner, renewal function, and post-regeneration trigger.
     */
    private record RenewEntry(
            BiFunctionWithException<X509Certificate, KeyPair, CertificateData> renewFunction,
            Runnable triggerAfterRegeneration) {
    }

    /**
     * Represents the data required for certificate renewal, including the certificate chain and key pair.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record CertificateData(X509Certificate[] certificateChain, KeyPair keyPair) {
    }
}
