/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.crl;

import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.events.AcmeCertificateRevokedEvent;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Subscriber that regenerates the CRL when a certificate gets revoked.
 */
@Slf4j
@RequiredArgsConstructor
public class CrlUpdateSubscriber implements EventSubscriber {
    private final IServerInstance serverInstance;

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return List.of(AcmeCertificateRevokedEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof AcmeCertificateRevokedEvent ev) {
            AcmeProvisioner prov = ev.getOrder().getAccount().getAcmeProvisioner();
            CrlStore.updateCachedCRL(CrlScheduler.UPDATE_MINUTES, prov, serverInstance);
        }
    }
}
