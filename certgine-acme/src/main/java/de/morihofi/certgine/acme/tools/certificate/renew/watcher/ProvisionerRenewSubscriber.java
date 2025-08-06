/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.tools.certificate.renew.watcher;

import de.morihofi.certgine.acme.tools.certificate.renew.IntermediateCaRenew;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.acme.types.events.ProvisionerCreatedEvent;
import de.morihofi.certgine.acme.types.events.ProvisionerDeletedEvent;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.scheduler.CertificateRenewScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Event subscriber keeping certificate renew watchers in sync with ACME provisioners.
 */
@Slf4j
@RequiredArgsConstructor
public class ProvisionerRenewSubscriber implements EventSubscriber {
    private final IServerInstance serverInstance;
    private final CertificateRenewScheduler renewManager;

    /**
     * Registers watchers for all existing provisioners.
     */
    public void initialize() {
        AcmeProvisioner[] all = AcmeProvisioner.getAllProvisioners(serverInstance);
        for (AcmeProvisioner prov : all) {
            registerWatcher(prov);
        }
    }

    private void registerWatcher(AcmeProvisioner prov) {
        String alias = serverInstance.getCryptoStoreManager()
                .getKeyStoreAliasForProvisionerIntermediate(prov.getInternalUuid());
        if (renewManager.isWatcherRegistered(alias)) {
            return;
        }
        renewManager.registerNewCertificateRenewWatcher(alias,
                (cert, kp) -> IntermediateCaRenew.renewIntermediateCertificate(kp, prov, serverInstance, alias));
        log.info("Registered renew watcher for provisioner {}", prov.getName());
    }

    private void unregisterWatcher(AcmeProvisioner prov) {
        String alias = serverInstance.getCryptoStoreManager()
                .getKeyStoreAliasForProvisionerIntermediate(prov.getInternalUuid());
        if (renewManager.isWatcherRegistered(alias)) {
            renewManager.unregisterCertificateRenewWatcher(alias);
            log.info("Unregistered renew watcher for provisioner {}", prov.getName());
        }
    }

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return Arrays.asList(ProvisionerCreatedEvent.class, ProvisionerDeletedEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof ProvisionerCreatedEvent created) {
            registerWatcher(created.getProvisioner());
        } else if (event instanceof ProvisionerDeletedEvent deleted) {
            unregisterWatcher(deleted.getProvisioner());
        }
    }
}

