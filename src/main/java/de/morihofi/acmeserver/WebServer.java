/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver;

import com.google.gson.Gson;
import de.morihofi.acmeserver.api.API;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.certificate.queue.CertificateIssuer;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRLScheduler;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;
import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.JavalinSecurityHelper;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.certificate.helper.CaInitHelper;
import de.morihofi.acmeserver.tools.certificate.renew.IntermediateCaRenew;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewManager;
import de.morihofi.acmeserver.tools.network.logging.HTTPAccessLogger;
import de.morihofi.acmeserver.tools.regex.ConfigCheck;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinGson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class WebServer {
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    private final HTTPAccessLogger httpAccessLogger;
    private final CertificateRenewManager certificateRenewManager;
    private final ServerInstance serverInstance;

    /**
     * Holds a static reference to the {@link Javalin} web server instance. This server is responsible for handling all HTTP requests to the
     * application, serving as the backbone for the application's web interface and API. The {@code app} is initialized during the
     * application's startup sequence and is used to configure routes, middleware, and other server settings. It is essential for the
     * operation of the web-based components of the application.
     */
    private Javalin app = null;


    public WebServer(ServerInstance serverInstance) throws IOException {
        this.serverInstance = serverInstance;
        this.httpAccessLogger = new HTTPAccessLogger(serverInstance.getAppConfig());
        this.certificateRenewManager = new CertificateRenewManager(serverInstance.getCryptoStoreManager());
    }

    /**
     * Method to start the Web and API Server
     *
     * @throws Exception thrown when startup fails
     */
    public void startServer() throws Exception {
        LOG.info("Starting in Normal Mode");
        CaInitHelper.initializeCA(serverInstance);

        LOG.info("Initializing database");
        serverInstance.getHibernateUtil().initDatabase();

        LOG.info("Starting ACME API WebServer");
        app = Javalin.create(config -> {
            // Static Files
            config.staticFiles.add("/webstatic", Location.CLASSPATH);
            // Object Mapper
            config.jsonMapper(new JavalinGson());
            //
            config.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                pluginConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.withInfo(info -> {
                        info.setTitle("ACME Server OpenAPI");
                        info.setDescription("This is the API documentation of morihofi's ACME Server.");
                    });
                });
            }));
            config.registerPlugin(new SwaggerPlugin());
            config.registerPlugin(new ReDocPlugin());

        });

        JavalinSecurityHelper.initSecureApi(app, serverInstance, certificateRenewManager);

        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
            ctx.header("Access-Control-Max-Age", "3600");

            // Check for correct content type, except for ACME directory
            if (
                    ctx.method() == HandlerType.POST && // Only for POST Requests
                            (ctx.path().startsWith("/acme/") && !"application/jose+json".equals(ctx.contentType())) // True when ACME API
            ) {
                throw new ACMEMalformedException("Invalid Content-Type header on POST. Content-Type must be \"application/jose+json\"");
            }
        });
        app.options("/*", ctx -> {
            ctx.status(204); // No Content
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
            ctx.header("Access-Control-Max-Age", "3600");
        });

        app.after("/*", ctx -> {
            // This handler is just for access logging
            httpAccessLogger.log(ctx);
        });

        app.exception(ACMEException.class, (exception, ctx) -> {
            Gson gson = new Gson();

            ctx.status(exception.getHttpStatusCode());
            ctx.header("Content-Type", "application/problem+json");
            // ctx.header("Link", "<" + provisioner.getApiURL() + "/directory>;rel=\"index\"");
            ctx.result(gson.toJson(exception.getErrorResponse()));
            LOG.error("ACME Exception thrown {} : {} ({})", exception.getClass().getSimpleName(), exception.getErrorResponse().getDetail(),
                    exception.getErrorResponse().getType());
        });

        // Global routes
        API.init(app, serverInstance);

        for (Provisioner provisioner : getProvisioners(serverInstance.getAppConfig().getProvisioner(), serverInstance.getCryptoStoreManager())) {
            ProvisionerManager.registerProvisioner(app, provisioner, serverInstance);
        }

        LOG.info("Starting the CRL generation Scheduler");
        CRLScheduler.startScheduler();
        LOG.info("Starting the certificate renew watcher");
        certificateRenewManager.startScheduler();

        if (Main.getServerOptions().contains(Main.SERVER_OPTION.USE_ASYNC_CERTIFICATE_ISSUING)) {
            LOG.info("Starting Certificate Issuer");
            CertificateIssuer.startThread(serverInstance);
        }

        app.start();
        LOG.info("\u2705 Configure Routes completed. Ready for incoming requests");
        Main.startupTime = (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000L; // in seconds
        LOG.info("Startup took {} seconds", Main.startupTime);
    }

    /**
     * Retrieves or initializes provisioners based on configuration and generates ACME Web API client certificates when required.
     *
     * @param provisionerConfigList A list of provisioner configurations.
     * @param cryptoStoreManager    Instance of {@link CryptoStoreManager} for accessing KeyStores
     * @return A list of provisioners.
     * @throws Exception If an error occurs during provisioning or certificate generation.
     */
    private List<Provisioner> getProvisioners(List<ProvisionerConfig> provisionerConfigList,
                                              CryptoStoreManager cryptoStoreManager) throws Exception {

        List<Provisioner> provisioners = new ArrayList<>();

        for (ProvisionerConfig config : provisionerConfigList) {
            String provisionerName = config.getName();

            if (!ConfigCheck.isValidProvisionerName(provisionerName)) {
                throw new IllegalArgumentException("Invalid provisioner name in config. Can only contain a-z, numbers, \"-\" and \"_\"");
            }
            final String IntermediateKeyAlias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);

            KeyPair intermediateKeyPair = null;
            X509Certificate intermediateCertificate;
            final Provisioner provisioner = new Provisioner(
                    provisionerName,
                    config.getMeta(),
                    config.getIssuedCertificateExpiration(),
                    config.getDomainNameRestriction(),
                    config.isWildcardAllowed(),
                    cryptoStoreManager,
                    config,
                    config.isIpAllowed(),
                    serverInstance
            );

            // Check if root ca does exist
            assert cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);

            if (!cryptoStoreManager.getKeyStore().containsAlias(IntermediateKeyAlias)) {

                // *****************************************
                // Create Intermediate Certificate

                if (config.getIntermediate().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                    LOG.info("Using RSA algorithm");
                    LOG.info("Generating RSA {} bit Key Pair for Intermediate CA", rsaParams.getKeySize());
                    intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(),
                            cryptoStoreManager.getKeyStore().getProvider().getName());
                }
                if (config.getIntermediate().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                    LOG.info("Using ECDSA algorithm (Elliptic curves");

                    LOG.info("Generating ECDSA Key Pair using curve {} for Intermediate CA", ecdsaAlgorithmParams.getCurveName());
                    intermediateKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName(),
                            cryptoStoreManager.getKeyStore().getProvider().getName());
                }
                if (intermediateKeyPair == null) {
                    throw new IllegalArgumentException("Unknown algorithm " + config.getIntermediate().getAlgorithm()
                            + " used for intermediate certificate in provisioner " + provisionerName);
                }

                LOG.info("Generating Intermediate CA");
                intermediateCertificate =
                        CertificateAuthorityGenerator.createIntermediateCaCertificate(cryptoStoreManager, intermediateKeyPair,
                                config.getIntermediate().getMetadata(), config.getIntermediate().getExpiration(),
                                provisioner.getFullCrlUrl(), provisioner.getFullOcspUrl());
                LOG.info("Storing generated Intermedia CA");
                X509Certificate[] chain = new X509Certificate[]{intermediateCertificate,
                        (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)};
                cryptoStoreManager.getKeyStore().setKeyEntry(
                        IntermediateKeyAlias,
                        intermediateKeyPair.getPrivate(),
                        "".toCharArray(),
                        chain
                );
                LOG.info("Saving KeyStore");
                cryptoStoreManager.saveKeystore();
            }
            // Initialize the CertificateRenewWatcher for this provisioner
            certificateRenewManager.registerNewCertificateRenewWatcher(IntermediateKeyAlias, provisioner,
                    (givenProvisioner, x509Certificate, keyPair) -> {
                        return IntermediateCaRenew.renewIntermediateCertificate(keyPair, givenProvisioner,
                                givenProvisioner.getCryptoStoreManager(), IntermediateKeyAlias);
                    });

            provisioners.add(provisioner);
        }
        return provisioners;
    }

    /*    *//**
     * see {@link #reloadConfiguration(Runnable)}
     *//*
    public static void reloadConfiguration() throws Exception {
        reloadConfiguration(null);
    }*/

    /**
     * Reloads the application's configuration, effectively restarting the service. This method is designed to shut down all components of
     * the application gracefully and then restart them with the updated configuration. This process is essential for applying configuration
     * changes without stopping the application process entirely. Optionally, a custom runnable can be executed after all components are
     * shut down but before the application restarts.
     *
     * <p>Steps performed during the reload process include:</p>
     * <ul>
     *     <li>Logging the start of the configuration reload process.</li>
     *     <li>Initiating the shutdown sequence for all components, including the web server, CRL generators,
     *     certificate watchers, and optionally the certificate issuer if asynchronous certificate issuing is
     *     enabled.</li>
     *     <li>Shutting down the database connection pool and other database-related resources.</li>
     *     <li>Executing an optional {@link Runnable} provided as a parameter, which can be used for custom
     *     cleanup or preparation tasks before the application restarts.</li>
     *     <li>Restarting the application by calling the {@code main} method with the previously used runtime
     *     arguments, thereby reloading the configuration and reinitializing all components.</li>
     * </ul>
     *
     * <p>This method aims to minimize service disruption during the reload process and ensures that all
     * components are reinitialized cleanly with the new configuration settings.</p>
     *
     * @param runWhenEverythingIsShutDown an optional {@link Runnable} to be executed after shutdown but before the application is
     *                                    restarted. This can be used for tasks that should run in a clean state, such as cleanup
     *                                    operations.
     * @throws Exception if any error occurs during the shutdown or restart process.
     */
/*

    public static void reloadConfiguration(Runnable runWhenEverythingIsShutDown) throws Exception {
        LOG.info("Reloading configuration was triggered. Service will be disrupted for a moment, please wait ...");
        LOG.info("Initiating shutdown of components ...");

        LOG.info("Shutting down WebServer");
        app.stop();
        app = null;

        LOG.info("Shutting down CRL scheduler");
        CRLScheduler.shutdown();

        LOG.info("Shutting down Certificate watchers");

        LOG.info("Gracefully shutdown certificate watchers");
        certificateRenewManager.shutdown();

        if (Main.getServerOptions().contains(Main.SERVER_OPTION.USE_ASYNC_CERTIFICATE_ISSUING)) {
            LOG.info("Shutting down Certificate Issuer");
            CertificateIssuer.shutdown();
        }

        LOG.info("Shutting down Database");
        HibernateUtil.shutdown();

        // At this point the VM should shut down itself with exit code 0,
        // so that all threads are exited at this point. If you develop and/or reload configuration,
        // consider checking this by temporarily comment out following code after this comment

        if (runWhenEverythingIsShutDown != null) {
            // Run things you normally wouldn't do, cause due to running thread this can end
            // in an unpredictable behaviour
            runWhenEverythingIsShutDown.run();
        }

        // Reload config and turn everything back on by relaunching the main()
        Main.restartMain();
    }
*/
}
