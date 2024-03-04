package de.morihofi.acmeserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.config.Config;
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
import java.util.Objects;
import java.util.Properties;
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

    @SuppressFBWarnings({"MS_PKGPROTECT", "MS_CANNOT_BE_FINAL"})
    public static Config appConfig;

    public enum MODE {
        NORMAL, POSTSETUP, KEYSTORE_MIGRATION_PEM2KS
    }

    public static boolean debug = false;

    @SuppressFBWarnings("MS_PKGPROTECT")
    public static MODE selectedMode = MODE.NORMAL;

    public static void main(String[] args) throws Exception {
        Gson configGson = new GsonBuilder()
                .registerTypeAdapter(AlgorithmParams.class, new AlgorithmParamsDeserializer())
                .registerTypeAdapter(KeyStoreParams.class, new KeyStoreParamsDeserializer())
                .create();

        printBanner();

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        //Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Provider");
        Security.addProvider(new BouncyCastleProvider());
        log.info("Register Bouncy Castle JSSE Security Provider");
        Security.addProvider(new BouncyCastleJsseProvider());

        log.info("Initializing directories");
        Path configPath = FILES_DIR.resolve("settings.json");
        ensureFilesDirectoryExists(configPath);

        log.info("Loading server configuration");
        appConfig = configGson.fromJson(Files.readString(configPath), Config.class);

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
        }


        if(Objects.equals(System.getenv("DEBUG"), "TRUE")){
            debug = true;
            log.info("Debug mode activated by DEBUG environment variable set to TRUE");
        }


        if(debug){
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

    private static void initializeCoreComponents() throws ClassNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (coreComponentsInitialized) {
            return;
        }

        initializeDatabaseDrivers();

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
    private static void ensureFilesDirectoryExists(Path configPath) throws IOException {
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
            buildMetadataVersion = properties.getProperty("build.version");
            buildMetadataBuildTime = properties.getProperty("build.date") + " UTC";
        });

        loadMetadata("/git.properties", properties -> {
            buildMetadataGitCommit = properties.getProperty("git.commit.id.full");
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
            log.error("Unable to load metadata from " + fileName, e);
        }
    }


    /**
     * Initializes database drivers for MariaDB and H2.
     *
     * @throws ClassNotFoundException If a database driver class is not found.
     */
    private static void initializeDatabaseDrivers() throws ClassNotFoundException {
        log.info("Loading MariaDB JDBC driver");
        Class.forName("org.mariadb.jdbc.Driver");
        log.info("Loading H2 JDBC driver");
        Class.forName("org.h2.Driver");
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