package de.morihofi.acmeserver.core.certificate.transparency;

import de.morihofi.acmeserver.ct.CertificateTransparencyClient;
import de.morihofi.acmeserver.types.config.CertificateTransparencyConfig;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.events.*;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Subscriber that submits issued certificates to a Certificate Transparency log.
 */
@Slf4j
@RequiredArgsConstructor
public class AcmeCertificateTransparencySubscriber implements EventSubscriber {
    private final IServerInstance serverInstance;
    private final CertificateTransparencyClient ctClient;

    /** Convenience constructor creating a default client. */
    public AcmeCertificateTransparencySubscriber(@NonNull IServerInstance serverInstance) {
        this(serverInstance,
                new CertificateTransparencyClient(
                        serverInstance.getNetworkClient().getOkHttpClient(),
                        serverInstance.getAppConfig().getCertificateTransparency().getLogServer()));
    }

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return Arrays.asList(AcmeCertificateCreatedEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof AcmeCertificateCreatedEvent ev) {
            CertificateTransparencyConfig cfg = serverInstance.getAppConfig().getCertificateTransparency();
            if (cfg == null || !cfg.isEnabled()) {
                return;
            }
            try {
                AcmeProvisioner prov = ev.getOrder().getAccount().getAcmeProvisioner();
                String alias = serverInstance.getCryptoStoreManager()
                        .getKeyStoreAliasForProvisionerIntermediate(prov.getName());
                KeyStore ks = serverInstance.getCryptoStoreManager().getKeyStore();
                List<X509Certificate> chain = new ArrayList<>();
                chain.add(ev.getCertificate());
                for (java.security.cert.Certificate c : ks.getCertificateChain(alias)) {
                    chain.add((X509Certificate) c);
                }
                ctClient.submitChain(chain, cfg.isSubmitPreCertificate());
            } catch (Exception ex) {
                log.error("Failed to submit certificate to CT log", ex);
            }
        }
    }
}
