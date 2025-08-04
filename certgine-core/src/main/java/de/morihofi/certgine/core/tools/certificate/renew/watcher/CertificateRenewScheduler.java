/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.certificate.renew.watcher;


import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.utils.lambda.TriFunction;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.events.ProvisionerCertificateRenewedEvent;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;
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
    private static final String DEFAULT_CRON = "0 */6 * * *"; // every six hours
    private static final int RENEWAL_THRESHOLD_DAYS = 7; // days before expiration for trigger renewal

    private final TimedScheduler scheduler;
    private final ICryptoStoreManager cryptoStoreManager;
    private final EventBus eventBus;
    private final Clock clock;
    private final Map<String, RenewEntry> renewMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructs a new scheduler instance.
     *
     * @param cryptoStoreManager The CryptoStoreManager instance used for key and certificate management.
     * @param clock              Clock used for time calculations.
     */
    public CertificateRenewScheduler(
            ICryptoStoreManager cryptoStoreManager,
            EventBus eventBus,
            TimedScheduler scheduler,
            Clock clock) {
        this.cryptoStoreManager = cryptoStoreManager;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /**
     * Constructs a new scheduler instance using the system UTC clock.
     *
     * @param cryptoStoreManager The CryptoStoreManager instance used for key and certificate management.
     * @param eventBus           Event bus for publishing renewal events.
     * @param scheduler          Scheduler used for periodic execution.
     */
    public CertificateRenewScheduler(
            ICryptoStoreManager cryptoStoreManager,
            EventBus eventBus,
            TimedScheduler scheduler) {
        this(cryptoStoreManager, eventBus, scheduler, Clock.systemUTC());
    }

    /**
     * Registers a new certificate renew watcher with the specified alias and regeneration function.
     *
     * @param alias                The alias of the certificate in the keystore.
     * @param provisioner          The provisioner responsible for renewing the certificate.
     * @param regenerationFunction The function used to regenerate the certificate.
     */
    public void registerNewCertificateRenewWatcher(String alias, AcmeProvisioner provisioner,
                                                   TriFunction<AcmeProvisioner, X509Certificate, KeyPair, CertificateData> regenerationFunction) {
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
    public void registerNewCertificateRenewWatcher(String alias, AcmeProvisioner provisioner,
                                                   TriFunction<AcmeProvisioner, X509Certificate, KeyPair, CertificateData> regenerationFunction, Runnable triggerAfterRegeneration) {

        if (renewMap.containsKey(alias)) {
            throw new IllegalArgumentException("An watcher was already registered for keystore alias " + alias);
        }

        renewMap.put(alias, new RenewEntry(provisioner, regenerationFunction, triggerAfterRegeneration));
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
     * Starts the scheduler that periodically checks for certificates that need to be renewed.
     */
    public void startScheduler() {
        startScheduler(DEFAULT_CRON);
    }

    /**
     * Starts the scheduler with the given cron expression.
     *
     * @param cron cron expression defining the execution times
     */
    public void startScheduler(String cron) {
        log.info("Initialized Certificate Renew Scheduler");
        scheduler.schedule(cron, this::schedule);
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

    /**
     * Checks if the certificate needs to be renewed based on the configured threshold. If renewal is needed, the provided runnable is
     * executed.
     */
    @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
    private void schedule() {
        for (Map.Entry<String, RenewEntry> entry : renewMap.entrySet()) {
            String alias = entry.getKey();
            RenewEntry renewEntry = entry.getValue();

            TriFunction<AcmeProvisioner, X509Certificate, KeyPair, CertificateData> function = renewEntry.renewFunction();
            AcmeProvisioner provisioner = renewEntry.provisioner();

            log.info("Checking if certificate for alias {} needs to be renewed", alias);
            try {
                X509Certificate certificateFromKeyStore = cryptoStoreManager.getCertificate(alias);

                if (certificateFromKeyStore == null) {
                    log.warn("Certificate for alias {} does not exist", alias);
                    continue;
                }

                if (shouldRenew(certificateFromKeyStore)) {
                    log.info("Certificate for alias {} needs to be renewed, renewing now ...", alias);

                    CertificateData newCertificateData =
                            function.apply(provisioner, certificateFromKeyStore, cryptoStoreManager.getKeyPairForAlias(alias));

                    if (newCertificateData.certificateChain() == null || newCertificateData.keyPair() == null) {
                        log.warn("Certificate for alias {} hasn't saved, because returned certificate chain or keypair is null", alias);
                        continue;
                    }

                    log.info("Saving certificate and key for alias {} in keystore", alias);
                    if (alias.startsWith(CryptoStoreManager.KEYSTORE_ALIASPREFIX_INTERMEDIATECA)) {
                        String id = alias.substring(CryptoStoreManager.KEYSTORE_ALIASPREFIX_INTERMEDIATECA.length());
                        cryptoStoreManager.addIntermediateCertificateAuthority(newCertificateData.certificateChain(), newCertificateData.keyPair(), id);
                    } else if (alias.startsWith(CryptoStoreManager.KEYSTORE_ALIASPREFIX_TSA)) {
                        String id = alias.substring(CryptoStoreManager.KEYSTORE_ALIASPREFIX_TSA.length());
                        cryptoStoreManager.addTimestampAuthority(newCertificateData.certificateChain(), newCertificateData.keyPair(), id);
                    } else {
                        cryptoStoreManager.addServerCertificate(newCertificateData.certificateChain(), newCertificateData.keyPair(), alias);
                    }
                    eventBus.publish(new ProvisionerCertificateRenewedEvent(provisioner));

                    if (renewEntry.triggerAfterRegeneration != null) {
                        log.info("Running post configuration runnable");
                        renewEntry.triggerAfterRegeneration.run();
                    }
                } else {
                    ZonedDateTime notAfter = certificateFromKeyStore.getNotAfter()
                            .toInstant()
                            .atZone(clock.getZone());
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
        scheduler.shutdown();
        renewMap.clear();
    }

    /**
     * Represents an entry in the renewal map, containing the provisioner, renewal function, and post-regeneration trigger.
     */
    private record RenewEntry(AcmeProvisioner provisioner,
                              TriFunction<AcmeProvisioner, X509Certificate, KeyPair, CertificateData> renewFunction, Runnable triggerAfterRegeneration) {
    }

    /**
     * Represents the data required for certificate renewal, including the certificate chain and key pair.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record CertificateData(X509Certificate[] certificateChain, KeyPair keyPair) {
    }
}
