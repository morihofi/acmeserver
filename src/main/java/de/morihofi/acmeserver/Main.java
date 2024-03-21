package de.morihofi.acmeserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.DatabaseConfig;
import de.morihofi.acmeserver.config.helper.DatabaseConfigDeserializer;
import de.morihofi.acmeserver.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.AlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;
import de.morihofi.acmeserver.config.helper.AlgorithmParamsDeserializer;
import de.morihofi.acmeserver.config.helper.KeyStoreParamsDeserializer;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS11KeyStoreParams;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS12KeyStoreParams;
import de.morihofi.acmeserver.postsetup.PostSetup;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.cli.CLIArgument;
import de.morihofi.acmeserver.tools.path.AppDirectoryHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.math.ec.ECPointMap;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

public class Main {

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(Main.class);

    /**
     * `serverdata` directory as an absolute path
     */
    public static final Path FILES_DIR = Paths.get(Objects.requireNonNull(AppDirectoryHelper.getAppDirectory())).resolve("serverdata").toAbsolutePath();

    @SuppressFBWarnings({"MS_PKGPROTECT"})
    public static CryptoStoreManager cryptoStoreManager;

    //Build Metadata
    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static String buildMetadataVersion;
    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static String buildMetadataBuildTime;
    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static String buildMetadataGitCommit;
    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static String buildMetadataGitClosestTagName;

    @SuppressFBWarnings({"MS_PKGPROTECT", "MS_CANNOT_BE_FINAL"})
    public static Config appConfig;

    public static void restartMain() throws Exception {
        coreComponentsInitialized = false;
        startupTime = 0;
        Main.main(runArgs);
    }

    public enum MODE {
        NORMAL, POSTSETUP, KEYSTORE_MIGRATION_PEM2KS
    }

    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static boolean debug = false;

    @SuppressFBWarnings("MS_PKGPROTECT")
    public static MODE selectedMode = MODE.NORMAL;

    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static long startupTime = 0; // Set after all routes are ready

    public static String[] runArgs = new String[]{};

    public enum SERVER_OPTION {
        /**
         * Enables the async certificate issuing,
         * that is currently a buggy in certbot.
         */
        USE_ASYNC_CERTIFICATE_ISSUING
    }

    public static Set<SERVER_OPTION> serverOptions = new HashSet<>();

    private static Gson configGson = new GsonBuilder()
            .registerTypeAdapter(AlgorithmParams.class, new AlgorithmParamsDeserializer())
            .registerTypeAdapter(KeyStoreParams.class, new KeyStoreParamsDeserializer())
            .registerTypeAdapter(DatabaseConfig.class, new DatabaseConfigDeserializer())
            .setPrettyPrinting()
            .create();

    public static Path configPath = FILES_DIR.resolve("settings.json");


    public static void main(String[] args) throws Exception {
        // runArgs are needed to restart the whole server
        runArgs = args;

        printBanner();

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        //Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Provider");
        Security.addProvider(new BouncyCastleProvider());
        log.info("Register Bouncy Castle JSSE Security Provider");
        Security.addProvider(new BouncyCastleJsseProvider());

        log.info("Initializing directories");
        ensureFilesDirectoryExists();

        loadServerConfiguration();
        loadBuildAndGitMetadata();


        //Parse CLI Arguments
        final String argPrefix = "--";
        final char splitCharacter = '=';

        for (String arg : args) {
            CLIArgument cliArgument = new CLIArgument(argPrefix, splitCharacter, arg);

            if (cliArgument.getParameterName().equals("normal")) {
                selectedMode = MODE.NORMAL;
            }
            if (cliArgument.getParameterName().equals("migrate-pem-to-keystore")) {
                selectedMode = MODE.KEYSTORE_MIGRATION_PEM2KS;
            }
            if (cliArgument.getParameterName().equals("postsetup")) {
                selectedMode = MODE.POSTSETUP;
            }
            if (cliArgument.getParameterName().equals("debug")) {
                debug = true;
                log.info("Debug mode activated by cli argument");
            }
            /*
             * Following are options that change the behavior of the server
             */
            if (cliArgument.getParameterName().equals("option-use-async-certificate-issuing")) {
                serverOptions.add(SERVER_OPTION.USE_ASYNC_CERTIFICATE_ISSUING);
                log.info("Enabled async certificate issuing");
            }
        }


        if (Objects.equals(System.getenv("DEBUG"), "TRUE")) {
            debug = true;
            log.info("Debug mode activated by DEBUG environment variable set to TRUE");
        }


        if (debug) {
            log.warn("!!! RUNNING IN DEBUG MODE - BEHAVIOR CAN BE DIFFERENT. DO NOT USE IN PRODUCTION !!!");
        }


        switch (selectedMode) {
            case NORMAL -> {
                initializeCoreComponents();
                log.info("Starting normally");
                AcmeApiServer.startServer(cryptoStoreManager, appConfig);
            }
            case POSTSETUP -> {
                //Do not init core components, due to changing passwords in UI
                log.info("Starting Post Setup");
                PostSetup.run(cryptoStoreManager, appConfig, FILES_DIR, args);
            }
            case KEYSTORE_MIGRATION_PEM2KS -> {
                initializeCoreComponents();
                log.info("Starting in KeyStore migration Mode (PEM to KeyStore)");
                KSMigrationTool.run(args, cryptoStoreManager, appConfig, FILES_DIR);
            }

        }


    }

    public static void loadServerConfiguration() throws IOException {
        log.info("Loading configuration ...");
        appConfig = configGson.fromJson(Files.readString(configPath), Config.class);
        log.info("Configuration loaded");
    }

    public static void saveServerConfiguration() throws IOException {
        log.info("Saving configuration ...");
        Files.writeString(configPath, configGson.toJson(appConfig));
        log.info("Configuration saved");
    }


    /**
     * Prints a banner with a stylized text art representation.
     */
    private static void printBanner() {
        System.out.println("""
                    _                       ____                          \s
                   / \\   ___ _ __ ___   ___/ ___|  ___ _ ____   _____ _ __\s
                  / _ \\ / __| '_ ` _ \\ / _ \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|
                 / ___ \\ (__| | | | | |  __/___) |  __/ |   \\ V /  __/ |  \s
                /_/   \\_\\___|_| |_| |_|\\___|____/ \\___|_|    \\_/ \\___|_|  \s
                """);
    }

    private static boolean coreComponentsInitialized = false;


    /**
     * Initializes the core components of the application, including database drivers and cryptographic
     * store management based on the application configuration. This method ensures that essential components
     * are set up before the application starts its main operations. It is designed to be idempotent, meaning
     * it will only perform initialization once, even if called multiple times.
     *
     * <p>The initialization process involves:</p>
     * <ul>
     *     <li>Checking if core components have already been initialized to prevent redundant operations.</li>
     *     <li>Initializing database drivers to ensure database connectivity.</li>
     *     <li>Determining the type of key store configuration specified in the application configuration
     *     (e.g., PKCS11 or PKCS12) and initializing the {@link CryptoStoreManager} accordingly with the
     *     respective key store configuration.</li>
     *     <li>Setting a flag to indicate that core components have been initialized, to avoid re-initialization.</li>
     * </ul>
     *
     * <p>If the key store configuration is not supported or if any required configuration parameters are missing,
     * the method will throw an {@link IllegalArgumentException}.</p>
     *
     * @throws ClassNotFoundException    if a database driver class cannot be found.
     * @throws CertificateException      if there is an issue with the certificates used in cryptographic operations.
     * @throws IOException               if there is an I/O issue with reading key store or configuration files.
     * @throws NoSuchAlgorithmException  if a particular cryptographic algorithm is not available.
     * @throws KeyStoreException         if there is an issue with key store initialization.
     * @throws NoSuchProviderException   if a security provider needed for cryptographic operations is not available.
     * @throws InvocationTargetException if an exception is thrown by an invoked method or constructor.
     * @throws InstantiationException    if an instance of a class cannot be created.
     * @throws IllegalAccessException    if there is illegal access to a class or field.
     * @throws NoSuchMethodException     if a method required for initialization is not found.
     */
    private static void initializeCoreComponents() throws ClassNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (coreComponentsInitialized) {
            return;
        }
        log.info("Initializing core components...");

        {
            //Initialize KeyStore

            if (appConfig.getKeyStore() instanceof PKCS11KeyStoreParams pkcs11KeyStoreParams) {

                cryptoStoreManager = new CryptoStoreManager(
                        new PKCS11KeyStoreConfig(
                                Paths.get(pkcs11KeyStoreParams.getLibraryLocation()),
                                pkcs11KeyStoreParams.getSlot(),
                                pkcs11KeyStoreParams.getPassword()
                        )
                );
            }
            if (appConfig.getKeyStore() instanceof PKCS12KeyStoreParams pkcs12KeyStoreParams) {

                cryptoStoreManager = new CryptoStoreManager(
                        new PKCS12KeyStoreConfig(
                                Paths.get(pkcs12KeyStoreParams.getLocation()),
                                pkcs12KeyStoreParams.getPassword()
                        )
                );
            }
            if (cryptoStoreManager == null) {
                throw new IllegalArgumentException("Could not create CryptoStoreManager, due to unsupported KeyStore configuration");
            }

            coreComponentsInitialized = true;
        }
    }

    /**
     * Ensures that the necessary files directory and configuration file exist.
     *
     * @throws IOException If an I/O error occurs while creating directories or checking for the configuration file.
     */
    private static void ensureFilesDirectoryExists() throws IOException {
        if (!Files.exists(FILES_DIR)) {
            log.info("First run detected, creating settings directory");
            Files.createDirectories(FILES_DIR);
        }
        if (!Files.exists(configPath)) {
            log.fatal("No configuration was found. Please create a file called \"settings.json\" in \"{}\". Then try again", FILES_DIR.toAbsolutePath());
            System.exit(1);
        }
    }

    /**
     * Loads build and Git metadata from resource files and populates corresponding variables.
     */
    private static void loadBuildAndGitMetadata() {

        loadMetadata("/build.properties", properties -> {
            log.info("Loading build metadata");
            buildMetadataVersion = properties.getProperty("build.version");
            buildMetadataBuildTime = properties.getProperty("build.date") + " UTC";
        });

        loadMetadata("/git.properties", properties -> {
            log.info("Loading git metadata");
            buildMetadataGitCommit = properties.getProperty("git.commit.id.full");
            buildMetadataGitCommit = properties.getProperty("git.commit.id.full");
            buildMetadataGitClosestTagName = properties.getProperty("git.closest.tag.name");
        });
    }

    private static void loadMetadata(String fileName, Consumer<Properties> propertiesConsumer) {
        try (InputStream is = Main.class.getResourceAsStream(fileName)) {
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                propertiesConsumer.accept(properties);
            } else {
                log.warn("Unable to load metadata from {}", fileName);
            }
        } catch (IOException e) {
            log.error("Unable to load metadata from {}", fileName, e);
        }
    }


    /**
     * Initializes the Certificate Authority (CA) by generating or loading the CA certificate and key pair.
     *
     * @throws NoSuchAlgorithmException           If the specified algorithm is not available.
     * @throws CertificateException               If an issue occurs during certificate generation or loading.
     * @throws IOException                        If an I/O error occurs while creating directories or writing files.
     * @throws OperatorCreationException          If there's an issue with operator creation during certificate generation.
     * @throws NoSuchProviderException            If the specified security provider is not available.
     * @throws InvalidAlgorithmParameterException If there's an issue with algorithm parameters during key pair generation.
     */
    static void initializeCA(CryptoStoreManager cryptoStoreManager) throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException, NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {


        KeyStore caKeyStore = cryptoStoreManager.getKeyStore();
        if (!caKeyStore.containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)) {


            // Create CA

            KeyPair caKeyPair = null;
            if (appConfig.getRootCA().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                log.info("Using RSA algorithm");
                log.info("Generating RSA {} bit Key Pair for Root CA", rsaParams.getKeySize());
                caKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(), caKeyStore.getProvider().getName());
            }
            if (appConfig.getRootCA().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                log.info("Using ECDSA algorithm (Elliptic curves");

                log.info("Generating ECDSA Key Pair using curve {} for Root CA", ecdsaAlgorithmParams.getCurveName());
                caKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName(), caKeyStore.getProvider().getName());

            }
            if (caKeyPair == null) {
                throw new IllegalArgumentException("Unknown algorithm " + appConfig.getRootCA().getAlgorithm() + " used for root certificate");
            }

            log.info("Creating CA");
            X509Certificate caCertificate = CertificateAuthorityGenerator.generateCertificateAuthorityCertificate(appConfig.getRootCA(), caKeyPair);

            // Dumping CA Certificate to HDD, so other clients can install it
            log.info("Writing CA to keystore");
            caKeyStore.setKeyEntry(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA, caKeyPair.getPrivate(), "".toCharArray(), //No password
                    new X509Certificate[]{caCertificate});
            // Save CA in Keystore
            log.info("Saving keystore");
            cryptoStoreManager.saveKeystore();
        }

    }

}