/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.crl;


import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CrlScheduler {

    //FIXME: Add trigger for update crl on provisioner removal/add

    /** Update interval in minutes used for scheduled CRL generation. */
    public static final int UPDATE_MINUTES = 720; // 12 hours
    public static final String CRON_EXPRESSION = "0 */12 * * *";

    private final TimedScheduler scheduler;
    private final IServerInstance serverInstance;

    public CrlScheduler(@NonNull IServerInstance serverInstance, TimedScheduler scheduler) {
        this.serverInstance = serverInstance;
        this.scheduler = scheduler;
    }

    public void startScheduler() {
        log.info("Initialized CRL Generation Scheduler");
        scheduler.schedule(CRON_EXPRESSION, this::schedule);
    }

    private void schedule() {
        log.info("CRL Generation Scheduler is running");

        for (AcmeProvisioner provisioner : AcmeProvisioner.getAllProvisioners(serverInstance)) {
            log.info("Generating CRL for {} provisioner", provisioner.getName());

            CrlStore.updateCachedCRL(UPDATE_MINUTES, provisioner, serverInstance);
        }

        log.info("CRL Scheduler finished execution");
    }

}
