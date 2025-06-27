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

package de.morihofi.certgine.core.web;

import de.morihofi.certgine.acme.AcmeHttpServlet;
import de.morihofi.certgine.acme.GetHttpsForFreeServlet;
import de.morihofi.certgine.core.servlet.download.RootCaDownloadServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.events.ServerShutdownEvent;
import de.morihofi.certgine.ui.frontend.legacy.LegacyWebUiServlet;
import de.morihofi.certgine.revocation.crl.CrlScheduler;
import de.morihofi.certgine.revocation.crl.CrlUpdateSubscriber;
import de.morihofi.certgine.ui.frontend.modern.WebUiServlet;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;
import de.morihofi.certgine.revocation.RevocationHttpServlet;
import de.morihofi.certgine.core.Main;
import de.morihofi.certgine.cryptography.certificate.queue.CertificateIssuanceSubscriber;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.certgine.core.tools.certificate.renew.watcher.ProvisionerRenewSubscriber;
import de.morihofi.certgine.core.tools.certificate.renew.watcher.TsaRenewSubscriber;
import de.morihofi.certgine.tsa.TimeStampServlet;
import de.morihofi.certgine.cryptography.tsa.TimeStampAuthority;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.AcmeTlsCertificateHotReloadEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.types.server.StartupFlag;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.network.ssl.mozillasslconfig.MozillaSslConfigHelper;
import jakarta.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
     * Shared scheduler instance for timed tasks.
     */
    private final TimedScheduler timedScheduler;

    /**
     * Instance of IServerInstance providing access to server-related configurations and utilities.
     */
    private final IServerInstance serverInstance;

    private final Server server;

    private ServerConnector sslConnector = null;


    /**
     * Constructor for WebServer.
     *
     * @param serverInstance The instance of IServerInstance providing access to server-related configurations and utilities.
     */
    public WebServer(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        this.timedScheduler = new TimedScheduler();
        this.certificateRenewScheduler = new CertificateRenewScheduler(
                serverInstance.getCryptoStoreManager(),
                serverInstance.getEventBus(),
                timedScheduler);

        log.info("Registering WebServer as event listener for TLS Certificate Renew Events");
        serverInstance.getEventBus().register(this);

        // We don't use virtual thread pool here, because it may deadlock in Java 21.
        // This is a known issue with Jetty and virtual threads and fixed in newer Java versions.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("WebServer-ThreadPool");

        this.server = new Server(threadPool);

        certificateRenewScheduler.registerNewCertificateRenewWatcher(
                CryptoStoreManager.KEYSTORE_ALIASPREFIX_SERVER,
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

                serverInstance.getCryptoStoreManager().addServerCertificate(
                        data.certificateChain(),
                        data.keyPair(),
                        "main"
                );
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

        addBundledServlets(context);


        // Start Jetty
        server.start();

        log.info("Starting the CRL generation Scheduler");
        CrlScheduler crlScheduler = new CrlScheduler(serverInstance, timedScheduler);
        crlScheduler.startScheduler();
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

        log.info("Starting the certificate renew watcher");
        certificateRenewScheduler.startScheduler();

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
     * Registers a predefined set of bundled servlets to the given servlet context.
     * <p>
     * This method adds core internal servlets that are part of the application by default.
     *
     * @param context The {@link ServletContextHandler} to which the bundled servlets will be added.
     * @throws Exception If servlet instantiation or registration fails.
     */
    private void addBundledServlets(ServletContextHandler context) throws Exception {
        // Add ACME API servlet
        addServlet(context, AcmeHttpServlet.class);
        // Add GetHttpsForFree servlet
        addServlet(context, GetHttpsForFreeServlet.class);
        // Add Legacy CA download page servlet
        addServlet(context, LegacyWebUiServlet.class);
        // Add Modern WebUI servlet
        addServlet(context, WebUiServlet.class);
        // Add root CA download servlet
        addServlet(context, RootCaDownloadServlet.class);

        // Add timestamping servlet
        X509Certificate tsaCert = serverInstance
                .getCryptoStoreManager()
                .getTimestampAuthorityCertificate(serverInstance.getTsaAuthority().getInternalUuid());

        TimeStampAuthority auth = new TimeStampAuthority(
                serverInstance.getCryptoStoreManager().getTimeampAuthorityKeyPair(serverInstance.getTsaAuthority().getInternalUuid()).getPrivate(),
                tsaCert,
                List.of(tsaCert,
                        serverInstance.getCryptoStoreManager().getCertficateAuthorityX509Certificate(serverInstance.getRootCa())),
                "1.3.6.1.4.1.13762.3");
        addServlet(context, new TimeStampServlet(auth));

        // Add revocation servlet
        addServlet(context, RevocationHttpServlet.class);
    }

    /**
     * Adds a servlet to the specified context at the given mount path.
     * Optionally marks the servlet as protected, which may prevent it from being dynamically
     * removed or unloaded in future versions when dynamic servlet management is implemented.
     *
     * @param context   The {@link ServletContextHandler} to which the servlet will be added.
     * @param servlet   The {@link HttpServlet} instance to be added.
     * @param mountPath The URL path at which the servlet will be mounted.
     * @param protect   Whether the servlet is protected from dynamic unloading (future feature).
     */
    private void addServlet(ServletContextHandler context, HttpServlet servlet, String mountPath, boolean protect) {
        log.info("Adding servlet {} at mount {}; is unload protected = {}", servlet.getClass().getName(), mountPath, protect);
        context.addServlet(new ServletHolder(servlet), mountPath);
    }

    /**
     * Adds a servlet to the specified context using mount configuration provided by a
     * {@link ServletMount} annotation on the servlet class. This allows for declarative configuration
     * of mount path and protection status.
     *
     * @param context The {@link ServletContextHandler} to which the servlet will be added.
     * @param servlet The {@link HttpServlet} instance to be added.
     * @throws NoSuchMethodException If the servlet's constructor could not be found.
     * @throws InvocationTargetException If the constructor throws an exception during instantiation.
     * @throws InstantiationException If the servlet class cannot be instantiated.
     * @throws IllegalAccessException If the constructor is not accessible.
     */
    private void addServlet(ServletContextHandler context, HttpServlet servlet) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        log.debug("Getting annotation for mount information");
        ServletMount s = servlet.getClass().getAnnotation(ServletMount.class);
        addServlet(context, servlet, s.servletMountPoint(), s.protect());
    }

    /**
     * Instantiates a servlet from its class, optionally using a constructor that accepts
     * an {@link IServerInstance}. Then adds the servlet to the context using metadata from
     * its {@link ServletMount} annotation.
     *
     * @param context       The {@link ServletContextHandler} to which the servlet will be added.
     * @param servletClazz  The class of the {@link HttpServlet} to be instantiated and added.
     * @throws NoSuchMethodException If neither a suitable constructor nor a no-arg constructor is found.
     * @throws InvocationTargetException If the constructor throws an exception during instantiation.
     * @throws InstantiationException If the servlet class cannot be instantiated.
     * @throws IllegalAccessException If the constructor is not accessible.
     */
    private void addServlet(ServletContextHandler context, Class<? extends HttpServlet> servletClazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        log.debug("Trying to locate constructor for class {} ...", servletClazz.getName());
        Constructor<? extends HttpServlet> servletClazzConstructor;
        try {
            servletClazzConstructor = servletClazz.getConstructor(IServerInstance.class);
        } catch (NoSuchMethodException e) {
            log.debug("No constructor with server instance param found, trying to find no-param constructor");
            servletClazzConstructor = servletClazz.getConstructor();
        }
        log.debug("Constructor found!");

        log.debug("Creating new instance ...");
        HttpServlet servlet = servletClazzConstructor.getParameterCount() == 0
                ? servletClazzConstructor.newInstance()
                : servletClazzConstructor.newInstance(serverInstance);

        log.debug("Instance created, adding servlet ...");
        addServlet(context, servlet);
    }



    private HttpConfiguration getHttpConfiguration() {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false); // Do not send server version in HTTP headers
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
        return List.of(AcmeTlsCertificateHotReloadEvent.class, ServerShutdownEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) throws Exception {
        switch (event) {
            case AcmeTlsCertificateHotReloadEvent e -> {
                log.info("Reconfiguring TLS due to event: {}", e.getClass().getSimpleName());
                loadOrReloadTlsCertificate();
            }
            case ServerShutdownEvent e -> {
                log.info("Received ServerShutdownEvent, shutting down WebServer...");
                server.stop();
                timedScheduler.shutdown();
            }
            default -> log.warn("Unhandled event type: {}", event.getClass().getSimpleName());
        }
    }
}
