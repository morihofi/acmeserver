package de.morihofi.acmeserver.core.tools.certificate.renew.watcher;

import de.morihofi.acmeserver.core.tools.certificate.renew.TimeStampRenew;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.acmeserver.types.events.AbstractEvent;
import de.morihofi.acmeserver.types.events.EventSubscriber;
import de.morihofi.acmeserver.types.events.TsaAuthorityCreatedEvent;
import de.morihofi.acmeserver.types.intf.IServerInstance;
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
