/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.crl;

import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Schedules periodic generation of Certificate Revocation Lists (CRLs).
 */
@Slf4j
public class CrlScheduler {

    /**
     * Update interval in minutes used for scheduled CRL generation.
     */
    public static final int UPDATE_MINUTES = 720; // 12 hours
    /**
     * Cron expression used for scheduling CRL generation.
     */
    public static final String CRON_EXPRESSION = "0 */12 * * *";

    private final IServerInstance serverInstance;

    /**
     * Creates a new scheduler using the provided server instance.
     *
     * @param serverInstance the server instance used to access data and
     *                       services required for CRL generation
     */
    public CrlScheduler(@NonNull IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Generates the CRL immediately. This method is typically invoked on a schedule
     * determined by {@link #CRON_EXPRESSION}.
     */
    public void schedule() {
        log.info("CRL Generation Scheduler is running");
        CrlStore.updateCachedCRL(UPDATE_MINUTES, serverInstance);
        log.info("CRL Scheduler finished execution");
    }
}
