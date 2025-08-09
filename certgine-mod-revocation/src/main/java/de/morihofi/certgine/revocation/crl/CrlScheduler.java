/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.crl;


import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.events.ProvisionerCreatedEvent;
import de.morihofi.certgine.acme.types.events.ProvisionerDeletedEvent;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
/**
 * Schedules periodic generation of Certificate Revocation Lists (CRLs) for all
 * provisioners and reacts to provisioner lifecycle events to update CRLs on
 * demand.
 */
public class CrlScheduler implements EventSubscriber {

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
     * Generates CRLs for all provisioners immediately. This method is typically
     * invoked on a schedule determined by {@link #CRON_EXPRESSION}.
     */
    public void schedule() {
        log.info("CRL Generation Scheduler is running");

        for (AcmeProvisioner provisioner : AcmeProvisioner.getAllProvisioners(serverInstance)) {
            log.info("Generating CRL for {} provisioner", provisioner.getName());

            CrlStore.updateCachedCRL(UPDATE_MINUTES, provisioner, serverInstance);
        }

        log.info("CRL Scheduler finished execution");
    }

    /**
     * {@inheritDoc}
     *
     * @return classes of events this subscriber can process
     */
    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return Arrays.asList(ProvisionerCreatedEvent.class, ProvisionerDeletedEvent.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param event the event to process
     */
    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof ProvisionerCreatedEvent created) {
            handleProvisionerChange(created.getProvisioner());
        } else if (event instanceof ProvisionerDeletedEvent deleted) {
            handleProvisionerChange(deleted.getProvisioner());
        }
    }

    private void handleProvisionerChange(AcmeProvisioner prov) {
        log.info("Updating CRL cache for provisioner {} due to configuration change", prov.getName());
        CrlStore.updateCachedCRL(UPDATE_MINUTES, prov, serverInstance);
    }

}
