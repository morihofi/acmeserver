/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.web;

import de.morihofi.acmeserver.acme.AcmeHttpServlet;
import de.morihofi.acmeserver.acme.GetHttpsForFreeServlet;
import de.morihofi.acmeserver.ui.frontend.legacy.LegacyWebUiServlet;
import de.morihofi.acmeserver.revocation.crl.CrlScheduler;
import de.morihofi.acmeserver.revocation.crl.CrlUpdateSubscriber;
import de.morihofi.acmeserver.revocation.RevocationHttpServlet;
import de.morihofi.acmeserver.core.Main;
import de.morihofi.acmeserver.cryptography.certificate.queue.CertificateIssuanceSubscriber;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.ProvisionerRenewSubscriber;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.TsaRenewSubscriber;
import de.morihofi.acmeserver.tsa.TimeStampServlet;
import de.morihofi.acmeserver.cryptography.tsa.TimeStampAuthority;
import de.morihofi.acmeserver.cryptography.keystore.KeyStoreUtil;
import de.morihofi.acmeserver.cluster.ClusterManager;
import de.morihofi.acmeserver.types.events.AbstractEvent;
import de.morihofi.acmeserver.types.events.AcmeTlsCertificateHotReloadEvent;
import de.morihofi.acmeserver.types.events.EventSubscriber;
import de.morihofi.acmeserver.types.server.StartupFlag;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.MozillaSslConfigHelper;
import jakarta.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.VirtualThreadPool;

import java.lang.management.ManagementFactory;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Web Server for the Website, API and ACME Service
 */
@Slf4j
public class WebServer implements EventSubscriber {
    /**
     * Scheduler for automatically renewing certificates.
     */
    private final CertificateRenewScheduler certificateRenewScheduler;

    /**
     * Instance of IServerInstance providing access to server-related configurations and utilities.
     */
    private final IServerInstance serverInstance;
    private final ClusterManager clusterManager;

    private final Server server;

    private ServerConnector sslConnector = null;


    /**
     * Constructor for WebServer.
     *
     * @param serverInstance The instance of IServerInstance providing access to server-related configurations and utilities.
     */
    public WebServer(IServerInstance serverInstance, ClusterManager clusterManager) {
        this.serverInstance = serverInstance;
        this.clusterManager = clusterManager;
        this.certificateRenewScheduler = new CertificateRenewScheduler(serverInstance.getCryptoStoreManager(), serverInstance.getEventBus());

        log.info("Registering WebServer as event listener for TLS Certificate Renew Events");
        serverInstance.getEventBus().register(this);

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("WebServer-ThreadPool");

        this.server = new Server(threadPool);

        certificateRenewScheduler.registerNewCertificateRenewWatcher(
                CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI,
                null,
                (p, cert, kp) -> JettyCertificateHelper.generateAcmeApiClientCertificate(serverInstance),
                () -> {
                    try {
                        loadOrReloadTlsCertificate();
                    } catch (Exception e) {
                        log.error("Failed to reload TLS certificate after renewal", e);
                    }
                }
        );
    }

    public WebServer(IServerInstance serverInstance) {
        this(serverInstance, null);
    }

    /**
     * Method to start the Web and API Server
     *
     * @throws Exception thrown when startup fails
     */
    public void startServer() throws Exception {
        log.info("Starting ACME API WebServer");

        HttpConfiguration httpConfig = getHttpConfiguration();

        if (serverInstance.getAppConfig().getServer().getPorts().getHttp() > 0) {
            // HTTP Configuration
            ServerConnector httpConnector = new ServerConnector(server);
            httpConnector.setPort(serverInstance.getAppConfig().getServer().getPorts().getHttp());

            server.addConnector(httpConnector);
            log.info("HTTP is configured on port {}", serverInstance.getAppConfig().getServer().getPorts().getHttp());
        }

        if (serverInstance.getAppConfig().getServer().getPorts().getHttps() > 0) {
            // HTTPS Configuration
            // Ensure certificate exists or is valid
            CertificateRenewScheduler.CertificateData data =
                    JettyCertificateHelper.generateAcmeApiClientCertificate(serverInstance);
            if (data != null) {
                serverInstance.getCryptoStoreManager().getKeyStore().setKeyEntry(
                        CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI,
                        data.keyPair().getPrivate(),
                        "".toCharArray(),
                        data.certificateChain()
                );
                serverInstance.getCryptoStoreManager().saveKeystore();
            }

            loadOrReloadTlsCertificate();
        } else {
            log.error("HTTPS support is DISABLED");
            throw new IllegalArgumentException("HTTPS support cannot be disabled");
        }


        // Configure our servlet
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add ACME API Servlet
        addProtectedServlet(context, new AcmeHttpServlet(serverInstance), AcmeHttpServlet.PATH_MOUNT);
        // Add GetHttpsForFree Servlet
        addProtectedServlet(context, new GetHttpsForFreeServlet(), GetHttpsForFreeServlet.PATH_MOUNT);
        // Add Legacy CA download page
        addProtectedServlet(context, new LegacyWebUiServlet(serverInstance), LegacyWebUiServlet.PATH_MOUNT);
        // Add root CA download servlet
        addProtectedServlet(context, new RootCaDownloadServlet(serverInstance), RootCaDownloadServlet.PATH_MOUNT);

        // Add timestamping servlet
        String tsaAlias = serverInstance.getCryptoStoreManager()
                .getKeyStoreAliasForTimestampAuthority(serverInstance.getTsaAuthority().getInternalUuid());
        KeyPair tsaKey = KeyStoreUtil.getKeyPair(
                tsaAlias, serverInstance.getCryptoStoreManager().getKeyStore());
        X509Certificate tsaCert = (X509Certificate) serverInstance
                .getCryptoStoreManager().getKeyStore().getCertificate(tsaAlias);

        TimeStampAuthority auth = new TimeStampAuthority(
                tsaKey.getPrivate(),
                tsaCert,
                java.util.List.of(tsaCert,
                        serverInstance.getCryptoStoreManager().getCerificateAuthorityX509Certificate(serverInstance.getRootCa())),
                "1.3.6.1.4.1.13762.3");
        addProtectedServlet(context, new TimeStampServlet(auth), TimeStampServlet.PATH_MOUNT);

        // Add revocation servlet
        addProtectedServlet(context, new RevocationHttpServlet(serverInstance), RevocationHttpServlet.PATH_MOUNT);


        // Start Jetty
        server.start();

        log.info("Starting the CRL generation Scheduler");
        CrlScheduler.startScheduler(serverInstance);
        serverInstance.getEventBus().register(new CrlUpdateSubscriber(serverInstance));

        // Register and initialize provisioner certificate watcher
        ProvisionerRenewSubscriber provisionerWatcher =
                new ProvisionerRenewSubscriber(serverInstance, certificateRenewScheduler);
        serverInstance.getEventBus().register(provisionerWatcher);
        provisionerWatcher.initialize();

        // Register and initialize TSA Certificate watcher
        TsaRenewSubscriber tsaWatcher = new TsaRenewSubscriber(serverInstance, certificateRenewScheduler);
        serverInstance.getEventBus().register(tsaWatcher);
        tsaWatcher.initialize();

        if (clusterManager == null || clusterManager.isLeader()) {
            log.info("Starting the certificate renew watcher");
            certificateRenewScheduler.startScheduler();
        } else {
            log.info("Skipping certificate renew watcher start on follower");
        }

        if (serverInstance.getStartupFlags().contains(StartupFlag.USE_ASYNC_CERTIFICATE_ISSUING)) {
            log.info("Registering async certificate issuance subscriber");
            CertificateIssuanceSubscriber sub = new CertificateIssuanceSubscriber(serverInstance);
            serverInstance.getEventBus().register(sub);
            sub.initialize();
        }

        // TODO: Show listening at ports and check if really ready

        log.info("\u2705 Ready for incoming requests");
        Main.startupTime = (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000L; // in seconds
        log.info("Startup took {} seconds", Main.startupTime);
    }

    /**
     * Adds the protected servlet to the given context. Protected servlets cannot be unloaded or removed.
     * The functionality will be added in the future to allow for dynamic servlet management for plugins etc.
     * At the moment this is just a wrapper.
     *
     * @param context   The ServletContextHandler to which the servlet will be added.
     * @param servlet   The HttpServlet instance to be added.
     * @param mountPath The path at which the servlet will be mounted.
     */
    private void addProtectedServlet(ServletContextHandler context, HttpServlet servlet, String mountPath) {
        context.addServlet(new ServletHolder(servlet), mountPath);
    }

    private HttpConfiguration getHttpConfiguration() {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false); // Do not send server version in HTTP headers
        return httpConfig;
    }

    private SslContextFactory.Server getSslContextFactory() {
        SslContextFactory.Server factory = new SslContextFactory.Server();
        factory.setKeyStore(serverInstance.getCryptoStoreManager().getKeyStore());
        factory.setKeyStorePassword("");
        factory.setKeyManagerPassword("");
        factory.setCertAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI);
        factory.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        factory.setProtocol("TLS");
        factory.setKeyManagerFactoryAlgorithm("PKIX");

        return factory;
    }

    /**
     * Configures TLS for the server using the provided CryptoStoreManager.
     * This method sets up the SSL context factory and initializes the SSL connector.
     */
    private void loadOrReloadTlsCertificate() throws Exception {
        log.info("Loading or reloading TLS certificate...");

        SslContextFactory.Server newSslContextFactory = getSslContextFactory();

        HttpConfiguration httpConfig = getHttpConfiguration();
        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setSniHostCheck(serverInstance.getAppConfig().getServer().getSslServerConfig().isEnableSniCheck());

        if (serverInstance.getAppConfig().getServer().getMozillaSslConfig().isEnabled()) {
            JettySslHelper.applyMozillaTlsConfig(
                    MozillaSslConfigHelper.getConfigurationGuidelinesForVersion(
                            serverInstance.getAppConfig()
                                    .getServer()
                                    .getMozillaSslConfig()
                                    .getVersion(),
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
            // Create connector for the first time
            ServerConnector newSslConnector = new ServerConnector(server, newSslContextFactory, new HttpConnectionFactory(httpConfig));
            newSslConnector.setPort(serverInstance.getAppConfig().getServer().getPorts().getHttps());

            this.sslConnector = newSslConnector;
            server.addConnector(this.sslConnector);
            if (server.isRunning()) {
                this.sslConnector.start();
            }
            log.info("TLS connector initialized.");
        } else {
            // Reload existing connector without recreation
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
        return List.of(AcmeTlsCertificateHotReloadEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof AcmeTlsCertificateHotReloadEvent) {
            log.info("Reconfiguring TLS due to event: {}", event.getClass().getSimpleName());
            try {
                loadOrReloadTlsCertificate();
            } catch (Exception e) {
                log.error("Failed to reconfigure TLS", e);
            }
        }
    }
}
