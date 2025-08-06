package de.morihofi.certgine.core.web;

import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.types.cryptography.CryptoStoreManagerConstants;
import de.morihofi.certgine.utils.scheduler.CertificateRenewScheduler;
import de.morihofi.certgine.types.events.AcmeTlsCertificateHotReloadEvent;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.types.events.ServerShutdownEvent;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.network.ssl.mozillasslconfig.MozillaSslConfigHelper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

/**
 * Handles TLS certificate management, including renew watchers and reloading
 * the Jetty TLS connector when certificates change.
 */
@Slf4j
public class TlsCertificateManager implements EventSubscriber {

    private final IServerInstance serverInstance;
    private final Server server;
    private final CertificateRenewScheduler certificateRenewScheduler;

    private ServerConnector sslConnector;

    public TlsCertificateManager(IServerInstance serverInstance, Server server) {
        this.serverInstance = serverInstance;
        this.server = server;
        this.certificateRenewScheduler =
                serverInstance.getModuleRegistry().getService(CertificateRenewScheduler.class);
        if (certificateRenewScheduler == null) {
            throw new IllegalStateException("CertificateRenewScheduler service is not available");
        }

        certificateRenewScheduler.registerNewCertificateRenewWatcher(
                CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_SERVER,
                (cert, kp) -> JettyCertificateHelper.generateAcmeApiClientCertificate(serverInstance, Clock.systemUTC()),
                () -> {
                    try {
                        loadOrReloadTlsCertificate();
                    } catch (Exception e) {
                        log.error("Failed to reload TLS certificate after renewal", e);
                    }
                });

    }

    /**
     * Initializes watchers and starts the scheduler.
     */
    public void initialize() {
        // no-op; module-specific watchers are registered by their modules
    }

    /**
     * Stops the scheduler.
     */
    public void shutdown() {
        certificateRenewScheduler.shutdown();
    }

    /**
     * Ensures the TLS certificate exists and loads it into Jetty.
     */
    public void setupTls() throws Exception {
        CertificateRenewScheduler.CertificateData data =
                JettyCertificateHelper.generateAcmeApiClientCertificate(serverInstance, Clock.systemUTC());
        if (data != null) {
            serverInstance.getCryptoStoreManager().addServerCertificate(data.certificateChain(), data.keyPair(), "main");
        }
        loadOrReloadTlsCertificate();
    }

    private HttpConfiguration getHttpConfiguration() {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        return httpConfig;
    }

    private SslContextFactory.Server getSslContextFactory() throws IOException {
        SslContextFactory.Server factory = new SslContextFactory.Server();
        factory.setSslContext(serverInstance.getCryptoStoreManager().getSslContextForServer("main"));
        factory.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        factory.setProtocol("TLS");
        factory.setKeyManagerFactoryAlgorithm("PKIX");
        return factory;
    }

    /**
     * Loads or reloads the TLS certificate for the Jetty server.
     */
    public void loadOrReloadTlsCertificate() throws Exception {
        log.info("Loading or reloading TLS certificate...");

        SslContextFactory.Server newSslContextFactory = getSslContextFactory();
        HttpConfiguration httpConfig = getHttpConfiguration();
        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setSniHostCheck(serverInstance.getAppConfig().getServer().getSslServerConfig().isEnableSniCheck());

        if (serverInstance.getAppConfig().getServer().getMozillaSslConfig().isEnabled()) {
            JettySslHelper.applyMozillaTlsConfig(
                    MozillaSslConfigHelper.getConfigurationGuidelinesForVersion(
                            serverInstance.getAppConfig().getServer().getMozillaSslConfig().getVersion(),
                            JettySslHelper.getMozSslConfigVariant(serverInstance),
                            serverInstance.getNetworkClient()
                    ),
                    newSslContextFactory,
                    secureRequestCustomizer
            );
            httpConfig.addCustomizer(secureRequestCustomizer);
        }

        httpConfig.addCustomizer(secureRequestCustomizer);

        if (this.sslConnector == null) {
            ServerConnector newSslConnector = new ServerConnector(server, newSslContextFactory, new HttpConnectionFactory(httpConfig));
            newSslConnector.setPort(serverInstance.getAppConfig().getServer().getPorts().getHttps());
            this.sslConnector = newSslConnector;
            server.addConnector(this.sslConnector);
            if (server.isRunning()) {
                this.sslConnector.start();
            }
            log.info("TLS connector initialized.");
        } else {
            log.info("Reloading existing TLS connector with updated certificate...");
            SslConnectionFactory sslConnectionFactory = this.sslConnector.getConnectionFactory(SslConnectionFactory.class);
            SslContextFactory.Server currentFactory = sslConnectionFactory.getSslContextFactory();
            currentFactory.reload(factory -> {
                factory.setKeyStore(newSslContextFactory.getKeyStore());
                factory.setKeyStorePassword(newSslContextFactory.getKeyStorePassword());
                factory.setKeyManagerPassword(newSslContextFactory.getKeyManagerPassword());
                factory.setCertAlias(newSslContextFactory.getCertAlias());
            });
            log.info("TLS certificate reloaded.");
        }
    }

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return List.of(AcmeTlsCertificateHotReloadEvent.class, ServerShutdownEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) throws Exception {
        if (event instanceof AcmeTlsCertificateHotReloadEvent) {
            log.info("Reconfiguring TLS due to event: {}", event.getClass().getSimpleName());
            loadOrReloadTlsCertificate();
        } else if (event instanceof ServerShutdownEvent) {
            shutdown();
        }
    }
}
