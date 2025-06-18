package de.morihofi.acmeserver.acme.revokeDistribution;

import de.morihofi.acmeserver.types.events.*;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Arrays;

/**
 * Subscriber that regenerates the CRL when a certificate gets revoked.
 */
@Slf4j
@RequiredArgsConstructor
public class CrlUpdateSubscriber implements EventSubscriber {
    private final IServerInstance serverInstance;

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return Arrays.asList(AcmeCertificateRevokedEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof AcmeCertificateRevokedEvent ev) {
            AcmeProvisioner prov = ev.getOrder().getAccount().getAcmeProvisioner();
            CrlStore.updateCachedCRL(CRLScheduler.UPDATE_MINUTES, prov, serverInstance);
        }
    }
}
