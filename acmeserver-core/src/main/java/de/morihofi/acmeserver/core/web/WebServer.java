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
import de.morihofi.acmeserver.acme.revokeDistribution.CRLScheduler;
import de.morihofi.acmeserver.acme.revokeDistribution.CrlUpdateSubscriber;
import de.morihofi.acmeserver.core.Main;
import de.morihofi.acmeserver.cryptography.certificate.queue.CertificateIssuanceSubscriber;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.ProvisionerRenewSubscriber;
import de.morihofi.acmeserver.types.server.StartupFlag;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * Web Server for the Website, API and ACME Service
 */
@Slf4j
public class WebServer {
    /**
     * Scheduler for automatically renewing certificates.
     */
    private final CertificateRenewScheduler certificateRenewScheduler;

    /**
     * Instance of IServerInstance providing access to server-related configurations and utilities.
     */
    private final IServerInstance serverInstance;

    private final Server server = new Server();
    private ServerConnector sslConnector;


    /**
     * Constructor for WebServer.
     *
     * @param serverInstance The instance of IServerInstance providing access to server-related configurations and utilities.
     * @throws IOException if an I/O error occurs during initialization.
     */
    public WebServer(IServerInstance serverInstance) throws IOException {
        this.serverInstance = serverInstance;
        this.certificateRenewScheduler = new CertificateRenewScheduler(serverInstance.getCryptoStoreManager(), serverInstance.getEventBus());
    }

    /**
     * Method to start the Web and API Server
     *
     * @throws Exception thrown when startup fails
     */
    public void startServer() throws Exception {
        log.info("Starting ACME API WebServer");


        if (serverInstance.getAppConfig().getServer().getPorts().getHttps() > 0) {
            /*
            log.info("HTTPS support is ENABLED");
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setSniRequired(serverInstance.getAppConfig().getServer().getSslServerConfig().isEnableSniCheck());
            sslContextFactory.setSslContext(getWebServerConfig().getSslConfig().getSslContext());

            // TODO: Implement client auth
            //sslContextFactory.setNeedClientAuth(getWebServerConfig().getSslConfig().isRequireClientAuth());

            HttpConfiguration httpsConfig = new HttpConfiguration();
            SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
            secureRequestCustomizer.setSniHostCheck(serverInstance.getAppConfig().getServer().getSslServerConfig().isEnableSniCheck());
            httpsConfig.addCustomizer(secureRequestCustomizer);


            if (serverInstance.getAppConfig().getServer().getMozillaSslConfig() != null) {
                log.info("Applying Mozilla SSL configuration");
                JettySslHelper.configureMozillaSsl(sslContextFactory, secureRequestCustomizer, serverInstance.getAppConfig().getServer().getMozillaSslConfig());
            }

            sslConnector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory()
            );
            sslConnector.setPort(serverInstance.getAppConfig().getServer().getPorts().getHttps());

            server.addConnector(sslConnector);

             */
        }else{
            log.error("HTTPS support is DISABLED");
            throw new IllegalArgumentException("HTTPS support cannot be disabled");
        }

        if (serverInstance.getAppConfig().getServer().getPorts().getHttp() > 0) {
            log.info("HTTP support is ENABLED");
            // HTTP Configuration
            ServerConnector httpConnector = new ServerConnector(server);
            httpConnector.setPort(serverInstance.getAppConfig().getServer().getPorts().getHttp());

            server.addConnector(httpConnector);
        } else {
            log.info("HTTP support is DISABLED");
        }

        // Configure our servlet
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add ACME API Servlet
        context.addServlet(new ServletHolder(new AcmeHttpServlet(serverInstance)), "/acme/*"); // MUST be mounted at /acme/

        // Start Jetty
        server.start();

        log.info("Starting the CRL generation Scheduler");
        CRLScheduler.startScheduler(serverInstance);
        serverInstance.getEventBus().register(new CrlUpdateSubscriber(serverInstance));

        // Register and initialize provisioner certificate watcher
        ProvisionerRenewSubscriber provisionerWatcher =
                new ProvisionerRenewSubscriber(serverInstance, certificateRenewScheduler);
        serverInstance.getEventBus().register(provisionerWatcher);
        provisionerWatcher.initialize();

        log.info("Starting the certificate renew watcher");
        certificateRenewScheduler.startScheduler();

        if (serverInstance.getStartupFlags().contains(StartupFlag.USE_ASYNC_CERTIFICATE_ISSUING)) {
            log.info("Registering async certificate issuance subscriber");
            CertificateIssuanceSubscriber sub = new CertificateIssuanceSubscriber(serverInstance);
            serverInstance.getEventBus().register(sub);
            sub.initialize();
        }

        log.info("\u2705 Configure Routes completed. Ready for incoming requests");
        Main.startupTime = (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000L; // in seconds
        log.info("Startup took {} seconds", Main.startupTime);
    }

    /**
     * Retrieves or initializes provisioners based on configuration and generates ACME Web API client certificates when required.
     *
     * @param cryptoStoreManager    Instance of {@link CryptoStoreManager} for accessing KeyStores
     * @return A list of provisioners.
     * @throws Exception If an error occurs during provisioning or certificate generation.
     */
    /*private List<AcmeProvisioner> getProvisioners(CryptoStoreManager cryptoStoreManager) throws Exception {

        List<AcmeProvisioner> provisioners = new ArrayList<>();

        for (AcmeProvisioner config : AcmeProvisioner.getAllProvisioners(serverInstance)) {
            String provisionerName = config.getName();

            if (!ConfigCheck.isValidProvisionerName(provisionerName)) {
                throw new IllegalArgumentException("Invalid provisioner name in config. Can only contain a-z, numbers, \"-\" and \"_\"");
            }
            final String IntermediateKeyAlias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);

            KeyPair intermediateKeyPair = null;
            X509Certificate intermediateCertificate;
            final AcmeProvisioner provisioner = AcmeProvisioner.getForName(serverInstance, config.getName());

            // Check if root ca does exist
            assert cryptoStoreManager.getKeyStore().containsAlias(serverInstance.getRootCaAlias());

            if (!cryptoStoreManager.getKeyStore().containsAlias(IntermediateKeyAlias)) {

                // *****************************************
                // Create Intermediate Certificate

                if (config.getIntermediate().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                    log.info("Using RSA algorithm");
                    log.info("Generating RSA {} bit Key Pair for Intermediate CA", rsaParams.getKeySize());
                    intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(),
                            cryptoStoreManager.getKeyStore().getProvider().getName());
                }
                if (config.getIntermediate().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                    log.info("Using ECDSA algorithm (Elliptic curves)");

                    log.info("Generating ECDSA Key Pair using curve {} for Intermediate CA", ecdsaAlgorithmParams.getCurveName());
                    intermediateKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName(),
                            cryptoStoreManager.getKeyStore().getProvider().getName());
                }
                if (intermediateKeyPair == null) {
                    throw new IllegalArgumentException("Unknown algorithm " + config.getIntermediate().getAlgorithm()
                            + " used for intermediate certificate in provisioner " + provisionerName);
                }

                log.info("Generating Intermediate CA");
                intermediateCertificate = X509Generator.generate(
                        X509Generator.Request.builder()
                                .type(X509Generator.Type.INTERMEDIATE_CA)
                                .certificateConfig(provisioner.getCertificateConfig())
                                .ownKeyPair(intermediateKeyPair)
                                .issuerKeyPair(cryptoStoreManager.getCerificateAuthorityKeyPair(serverInstance.getRootCa()))
                                .issuerCertificate((X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(serverInstance.getRootCaAlias()))
                                .crlDistributionUrl(provisioner.getFullCrlUrl(serverInstance))
                                .ocspServiceEndpoint(provisioner.getFullOcspUrl(serverInstance))
                                .build()
                );
                log.info("Storing generated Intermedia CA");
                X509Certificate[] chain = new X509Certificate[]{intermediateCertificate,
                        (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(serverInstance.getRootCaAlias())};
                cryptoStoreManager.getKeyStore().setKeyEntry(
                        IntermediateKeyAlias,
                        intermediateKeyPair.getPrivate(),
                        "".toCharArray(),
                        chain
                );
                log.info("Saving KeyStore");
                cryptoStoreManager.saveKeystore();
            }


            // Initialize the CertificateRenewWatcher for this provisioner
            certificateRenewScheduler.registerNewCertificateRenewWatcher(IntermediateKeyAlias, provisioner,
                    (givenProvisioner, x509Certificate, keyPair) -> {
                        return IntermediateCaRenew.renewIntermediateCertificate(keyPair, givenProvisioner,
                                serverInstance, IntermediateKeyAlias);
            });

            provisioners.add(provisioner);
        }
        return provisioners;
    }
*/


}
