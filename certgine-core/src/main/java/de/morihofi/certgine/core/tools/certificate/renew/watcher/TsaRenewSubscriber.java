/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.certificate.renew.watcher;

import de.morihofi.certgine.core.tools.certificate.renew.TimeStampRenew;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.tsa.types.entities.TsaAuthority;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.tsa.types.events.TsaAuthorityCreatedEvent;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/** Subscriber registering renew watcher for TSA certificates. */
@Slf4j
@RequiredArgsConstructor
public class TsaRenewSubscriber implements EventSubscriber {
    private final IServerInstance serverInstance;
    private final CertificateRenewScheduler renewManager;

    public void initialize() {
        for (TsaAuthority tsa : TsaAuthority.getAll(serverInstance)) {
            registerWatcher(tsa);
        }
    }

    private void registerWatcher(TsaAuthority tsa) {
        String alias = serverInstance.getCryptoStoreManager()
                .getKeyStoreAliasForTimestampAuthority(tsa.getInternalUuid());
        if (renewManager.isWatcherRegistered(alias)) {
            return;
        }
        AcmeProvisioner dummy = new AcmeProvisioner();
        renewManager.registerNewCertificateRenewWatcher(alias, dummy,
                (p, cert, kp) -> TimeStampRenew.renew(kp, tsa, serverInstance, alias));
        log.info("Registered TSA renew watcher {}", alias);
    }

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return List.of(TsaAuthorityCreatedEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof TsaAuthorityCreatedEvent created) {
            registerWatcher(created.getAuthority());
        }
    }
}
