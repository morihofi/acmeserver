package de.morihofi.acmeserver.core.tools.certificate.renew.watcher;

import de.morihofi.acmeserver.core.tools.certificate.renew.IntermediateCaRenew;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.events.AbstractEvent;
import de.morihofi.acmeserver.types.events.EventSubscriber;
import de.morihofi.acmeserver.types.events.ProvisionerCreatedEvent;
import de.morihofi.acmeserver.types.events.ProvisionerDeletedEvent;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Event subscriber that keeps the {@link CertificateRenewScheduler} in sync with
 * the provisioners available on the server. When new provisioners are created
 * or removed, corresponding to renew watchers are registered or deleted.
 */
@Slf4j
@RequiredArgsConstructor
public class ProvisionerRenewSubscriber implements EventSubscriber {
    private final IServerInstance serverInstance;
    private final CertificateRenewScheduler renewManager;

    /**
     * Registers watchers for all provisioners currently present in the system.
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
        renewManager.registerNewCertificateRenewWatcher(alias, prov,
                (p, cert, kp) -> IntermediateCaRenew.renewIntermediateCertificate(kp, p,
                        serverInstance, alias));
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
