package de.morihofi.acmeserver;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.certificate.queue.CertificateIssuer;
import de.morihofi.acmeserver.certificate.revokeDistribution.*;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.tools.JavalinSecurityHelper;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.certificate.renew.IntermediateCaRenew;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewManager;
import de.morihofi.acmeserver.tools.regex.ConfigCheck;
import de.morihofi.acmeserver.webui.WebUI;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinJte;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.management.ManagementFactory;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

public class AcmeApiServer {
    private AcmeApiServer() {
    }

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(AcmeApiServer.class);

    /**
     * Holds a static reference to the {@link CryptoStoreManager} instance used throughout the application
     * for managing cryptographic operations, including key and certificate storage, generation, and renewal.
     * This manager is essential for handling the security aspects of the application, particularly those
     * related to SSL/TLS and encryption. The {@code cryptoStoreManager} is initialized as part of the
     * application's startup process and is used by various components to access and manage cryptographic
     * materials.
     */
    private static CryptoStoreManager cryptoStoreManager = null;

    /**
     * Holds a static reference to the {@link Javalin} web server instance. This server is responsible for
     * handling all HTTP requests to the application, serving as the backbone for the application's web
     * interface and API. The {@code app} is initialized during the application's startup sequence and is
     * used to configure routes, middleware, and other server settings. It is essential for the operation
     * of the web-based components of the application.
     */
    private static Javalin app = null;


    @SuppressFBWarnings({"MS_PKGPROTECT", "MS_CANNOT_BE_FINAL"})
    public static CertificateRenewManager certificateRenewManager;


    /**
     * Method to start the ACME API Server
     *
     * @param appConfig          Configuration instance of the ACME Server
     * @param cryptoStoreManager Instance of {@link CryptoStoreManager} for accessing KeyStores
     * @throws Exception thrown when startup fails
     */
    public static void startServer(CryptoStoreManager cryptoStoreManager, Config appConfig) throws Exception {
        AcmeApiServer.cryptoStoreManager = cryptoStoreManager;
        AcmeApiServer.certificateRenewManager = new CertificateRenewManager(cryptoStoreManager);

        log.info("Starting in Normal Mode");
        Main.initializeCA(cryptoStoreManager);

        log.info("Initializing database");
        HibernateUtil.initDatabase();

        log.info("Starting ACME API WebServer");
        app = Javalin.create(javalinConfig -> {
            //TODO: Make it compatible again with modules
            javalinConfig.staticFiles.add("/webstatic", Location.CLASSPATH); // Adjust the Location if necessary
            javalinConfig.fileRenderer(new JavalinJte(WebUI.createTemplateEngine()));
        });

        JavalinSecurityHelper.initSecureApi(app, cryptoStoreManager, appConfig, certificateRenewManager);

        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
            ctx.header("Access-Control-Max-Age", "3600");

            //This is for the WebUI to auto use same language as browser, if supported
            {
                String acceptLanguage = ctx.header("Accept-Language");
                Locale locale = Locale.forLanguageTag(acceptLanguage != null ? acceptLanguage.split(",")[0] : "en-US");

                ctx.attribute(WebUI.ATTR_LOCALE, locale);
            }

            log.info("API Call [{}] {}", ctx.method(), ctx.path());
        });
        app.options("/*", ctx -> {
            ctx.status(204); // No Content
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
            ctx.header("Access-Control-Max-Age", "3600");
        });

        app.exception(ACMEException.class, (exception, ctx) -> {
            Gson gson = new Gson();

            ctx.status(exception.getHttpStatusCode());
            ctx.header("Content-Type", "application/problem+json");
            //ctx.header("Link", "<" + provisioner.getApiURL() + "/directory>;rel=\"index\"");
            ctx.result(gson.toJson(exception.getErrorResponse()));
            log.error("ACME Exception thrown: {} ({})", exception.getErrorResponse().getDetail(), exception.getErrorResponse().getType());
        });


        // Global routes
        API.init(app, appConfig, cryptoStoreManager);
        WebUI.init(app, cryptoStoreManager);

        for (Provisioner provisioner : getProvisioners(appConfig.getProvisioner(), cryptoStoreManager)) {
            ProvisionerManager.registerProvisioner(app, provisioner);
        }

        log.info("Starting the CRL generation Scheduler");
        CRLScheduler.startScheduler();
        log.info("Starting the certificate renew watcher");
        certificateRenewManager.startScheduler();


        if (Main.serverOptions.contains(Main.SERVER_OPTION.USE_ASYNC_CERTIFICATE_ISSUING)) {
            log.info("Starting Certificate Issuer");
            CertificateIssuer.startThread(cryptoStoreManager);
        }


        app.start();
        log.info("\u2705 Configure Routes completed. Ready for incoming requests");
        Main.startupTime = (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000L; //in seconds
        log.info("Startup took {} seconds", Main.startupTime);


    }


    /**
     * Retrieves or initializes provisioners based on configuration and generates ACME Web API client certificates when required.
     *
     * @param provisionerConfigList A list of provisioner configurations.
     * @param cryptoStoreManager    Instance of {@link CryptoStoreManager} for accessing KeyStores
     * @return A list of provisioners.
     * @throws Exception If an error occurs during provisioning or certificate generation.
     */
    private static List<Provisioner> getProvisioners(List<ProvisionerConfig> provisionerConfigList, CryptoStoreManager cryptoStoreManager) throws Exception {

        List<Provisioner> provisioners = new ArrayList<>();

        for (ProvisionerConfig config : provisionerConfigList) {
            String provisionerName = config.getName();

            if (!ConfigCheck.isValidProvisionerName(provisionerName)) {
                throw new IllegalArgumentException("Invalid provisioner name in config. Can only contain a-z, numbers, \"-\" and \"_\"");
            }
            final String IntermediateKeyAlias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);

            KeyPair intermediateKeyPair = null;
            X509Certificate intermediateCertificate;
            final Provisioner provisioner = new Provisioner(provisionerName, config.getMeta(), config.getIssuedCertificateExpiration(), config.getDomainNameRestriction(), config.isWildcardAllowed(), cryptoStoreManager, config);


            //Check if root ca does exist
            assert cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);

            if (!cryptoStoreManager.getKeyStore().containsAlias(IntermediateKeyAlias)) {

                // *****************************************
                // Create Intermediate Certificate

                if (config.getIntermediate().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                    log.info("Using RSA algorithm");
                    log.info("Generating RSA {} bit Key Pair for Intermediate CA", rsaParams.getKeySize());
                    intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(), cryptoStoreManager.getKeyStore().getProvider().getName());
                }
                if (config.getIntermediate().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                    log.info("Using ECDSA algorithm (Elliptic curves");

                    log.info("Generating ECDSA Key Pair using curve {} for Intermediate CA", ecdsaAlgorithmParams.getCurveName());
                    intermediateKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName(), cryptoStoreManager.getKeyStore().getProvider().getName());

                }
                if (intermediateKeyPair == null) {
                    throw new IllegalArgumentException("Unknown algorithm " + config.getIntermediate().getAlgorithm() + " used for intermediate certificate in provisioner " + provisionerName);
                }


                log.info("Generating Intermediate CA");
                intermediateCertificate = CertificateAuthorityGenerator.createIntermediateCaCertificate(cryptoStoreManager, intermediateKeyPair, config.getIntermediate().getMetadata(), config.getIntermediate().getExpiration(), provisioner.getFullCrlUrl(), provisioner.getFullOcspUrl());
                log.info("Storing generated Intermedia CA");
                X509Certificate[] chain = new X509Certificate[]{intermediateCertificate, (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)};
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
            certificateRenewManager.registerNewCertificateRenewWatcher(IntermediateKeyAlias, provisioner, (givenProvisioner, x509Certificate, keyPair) -> {
                return IntermediateCaRenew.renewIntermediateCertificate(keyPair, givenProvisioner, givenProvisioner.getCryptoStoreManager(), IntermediateKeyAlias);
            });

            provisioners.add(provisioner);
        }
        return provisioners;
    }


    /**
     * see {@link #reloadConfiguration(Runnable)}
     *
     * @throws Exception {@link #reloadConfiguration(Runnable)}
     */
    public static void reloadConfiguration() throws Exception {
        reloadConfiguration(null);
    }

    /**
     * Reloads the application's configuration, effectively restarting the service. This method is designed
     * to shut down all components of the application gracefully and then restart them with the updated
     * configuration. This process is essential for applying configuration changes without stopping the
     * application process entirely. Optionally, a custom runnable can be executed after all components are
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
     * @param runWhenEverythingIsShutDown an optional {@link Runnable} to be executed after shutdown but
     *                                    before the application is restarted. This can be used for tasks
     *                                    that should run in a clean state, such as cleanup operations.
     * @throws Exception if any error occurs during the shutdown or restart process.
     */

    public static void reloadConfiguration(Runnable runWhenEverythingIsShutDown) throws Exception {
        log.info("Reloading configuration was triggered. Service will be disrupted for a moment, please wait ...");
        log.info("Initiating shutdown of components ...");

        log.info("Shutting down WebServer");
        app.stop();
        app = null;

        log.info("Shutting down CRL scheduler");
        CRLScheduler.shutdown();

        log.info("Shutting down Certificate watchers");

        log.info("Gracefully shutdown certificate watchers");
        certificateRenewManager.shutdown();

        if (Main.serverOptions.contains(Main.SERVER_OPTION.USE_ASYNC_CERTIFICATE_ISSUING)) {
            log.info("Shutting down Certificate Issuer");
            CertificateIssuer.shutdown();
        }

        log.info("Shutting down Database");
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

}
