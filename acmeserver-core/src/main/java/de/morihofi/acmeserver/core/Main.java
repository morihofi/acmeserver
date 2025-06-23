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

package de.morihofi.acmeserver.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.core.web.JettySslHelper;
import de.morihofi.acmeserver.core.web.WebServer;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.database.entities.TsaAuthority;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.core.helper.cert.CaInitHelper;
import de.morihofi.acmeserver.core.helper.tsa.TsaInitHelper;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.helper.KeyStoreParamsDeserializer;
import de.morihofi.acmeserver.types.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.acmeserver.types.config.keyStoreHelpers.PKCS11KeyStoreParams;
import de.morihofi.acmeserver.types.config.keyStoreHelpers.PKCS12KeyStoreParams;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.server.StartupFlag;
import de.morihofi.acmeserver.utils.cli.CLIArgument;
import de.morihofi.acmeserver.utils.meta.BuildMetadataImpl;
import de.morihofi.acmeserver.utils.network.http.NetworkClient;
import de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.MozillaSslConfigHelper;
import de.morihofi.acmeserver.utils.path.AppDirectoryHelper;
import de.morihofi.acmeserver.cluster.ClusterManager;
import de.morihofi.acmeserver.cluster.DistributedEventBus;
import de.morihofi.acmeserver.types.events.ServerInitializedEvent;
import de.morihofi.acmeserver.types.events.ServerStartedEvent;
import de.morihofi.acmeserver.types.events.ServerShutdownEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * Main class for the ACME server application. This class handles the initialization and startup of the server, including configuration
 * loading, security provider registration, and mode selection.
 */
@Slf4j
public class Main {

    /**
     * `serverdata` directory as an absolute path.
     */
    public static final Path FILES_DIR =
            Paths.get(Objects.requireNonNull(AppDirectoryHelper.getAppDirectory(MethodHandles.lookup().lookupClass()))).resolve("serverdata").toAbsolutePath();

    /**
     * Path to the configuration file.
     */
    public static final Path CONFIG_PATH = FILES_DIR.resolve("settings.json");

    /**
     * Set of server options.
     */
    private static final Set<StartupFlag> startupFlags = new HashSet<>();

    /**
     * Gson instance for configuration deserialization.
     */
    private static final Gson CONFIG_GSON = new GsonBuilder()
            .registerTypeAdapter(KeyStoreParams.class, new KeyStoreParamsDeserializer())
            .setPrettyPrinting()
            .create();



    /**
     * Application startup time.
     */
    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static long startupTime = 0; // Set after all routes are ready

    /**
     * Instance of the server.
     */
    private static IServerInstance serverInstance;



    /**
     * Main application startup method.
     *
     * @param args arguments passed to the application.
     * @throws Exception if an error occurs during startup.
     */
    public static void main(String[] args) throws Exception {
        printBanner();

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();



        log.info("Initializing directories");
        ensureFilesDirectoryExists();

        // Parse CLI Arguments
        final String argPrefix = "--";
        final char splitCharacter = '=';

        boolean debug = false; // Debug mode by default deactivated

        for (String arg : args) {
            CLIArgument cliArgument = new CLIArgument(argPrefix, splitCharacter, arg);

            if (cliArgument.getParameterName().equals("debug")) {
                debug = true;
                log.info("Debug mode activated by cli argument");
            }
            /*
             * Following are options that change the behavior of the server
             */
            if (cliArgument.getParameterName().equals("option-use-async-certificate-issuing")) {
                startupFlags.add(StartupFlag.USE_ASYNC_CERTIFICATE_ISSUING);
                log.info("Enabled async certificate issuing");
            }
        }

        log.info("Loading configuration ...");
        Config config = loadServerConfiguration();

        // Do some stuff that's need to be done before Bouncy Castle providers are installed
        // Just to be sure that it is applied correctly.
        {
            MozillaSslConfigHelper.CONFIGURATION configuration = JettySslHelper.getMozSslConfigVariant(config);

            if (configuration.equals(MozillaSslConfigHelper.CONFIGURATION.OLD)) {
                // This is needed to be able to turn on TLS 1.0, TLS 1.1 and TLS 1.2
                // For this to work in IE8 you need to disable SSLv1, SSLv2 and SSLv3 and leave only TLSv1 enabled
                // otherwise you'll get "Failed to read record: Unsupported UNKNOWN(128)". This is due to
                // missing SSL support (not TLS!) in Bouncy Castle. BC supports TLSv1 and higher
                Security.setProperty("jdk.tls.disabledAlgorithms", "SSLv2Hello, SSLv3, RC4, MD5");
                //Security.setProperty("jdk.certpath.disabledAlgorithms", "SSLv2Hello, SSLv3, DTLSv1.0, RC4, DES, MD5withRSA, DH keySize < 1024, RSA keySize < 1024, EC keySize < 224, anon, NULL");
            }
            System.setProperty("jdk.tls.allowLegacyResumption",
                    String.valueOf(config.getServer().getSslServerConfig().isAllowLegacyResumption()));
        }

        // Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Provider");
        Security.addProvider(new BouncyCastleProvider());
        log.info("Register Bouncy Castle JSSE Security Provider");
        Security.addProvider(new BouncyCastleJsseProvider());

        // ... and continue building the server instance
        DistributedEventBus eventBus = new DistributedEventBus();
        ClusterManager clusterManager = null;
        if (config.getGrpc().isEnabled()) {
            clusterManager = new ClusterManager(config.getGrpc(), eventBus);
            eventBus.setClusterManager(clusterManager);
        }
        serverInstance = getServerInstance(config, debug, CONFIG_PATH, eventBus);
        eventBus.publish(new ServerInitializedEvent(serverInstance));


        WebServer ws = new WebServer(serverInstance, clusterManager);
        try {
            ws.startServer();
            eventBus.publish(new ServerStartedEvent(serverInstance));
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
                    eventBus.publish(new ServerShutdownEvent(serverInstance))));
        } catch (Exception ex) {
            log.error("Server startup failed", ex);
            System.exit(1);
        }

    }




    public static IServerInstance getServerInstance(Config config, boolean debug, Path configPath, DistributedEventBus eventBus) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvalidAlgorithmParameterException, OperatorCreationException, UnrecoverableKeyException {
        if (Objects.equals(System.getenv("DEBUG"), "TRUE")) {
            debug = true;
            log.info("Debug mode activated by DEBUG environment variable set to TRUE");
        }

        if (debug) {
            log.warn("!!! RUNNING IN DEBUG MODE - BEHAVIOR CAN BE DIFFERENT. DO NOT USE IN PRODUCTION !!!");
        }

        log.info("Initializing keystore ...");

        CryptoStoreManager cryptoStoreManager = switch (config.getKeyStore()) {
            case PKCS11KeyStoreParams p11 -> new CryptoStoreManager(new PKCS11KeyStoreConfig(
                    Paths.get(p11.getLibraryLocation()),
                    p11.getSlot(),
                    p11.getPassword()
            ));
            case PKCS12KeyStoreParams p12 -> new CryptoStoreManager(new PKCS12KeyStoreConfig(
                    Paths.get(p12.getLocation()),
                    p12.getPassword()
            ));
            default -> throw new IllegalArgumentException("Unsupported keystore");
        };

        log.info("Initializing database ...");
        HibernateUtil hibernateUtil = new HibernateUtil(config, debug, eventBus);
        hibernateUtil.initDatabase();

        log.info("Initializing certificate authorities ...");
        RootCa root = CaInitHelper.initializeCA(hibernateUtil, cryptoStoreManager, eventBus);
        TsaAuthority tsa = TsaInitHelper.initializeTsa(hibernateUtil, cryptoStoreManager, root, eventBus);

        log.info("Creating new server instance ...");
        return new ServerInstance(
                config,
                configPath,
                debug,
                cryptoStoreManager,
                new NetworkClient(config.getNetwork()),
                hibernateUtil,
                new NonceManager(serverInstance),
                root,
                tsa,
                BuildMetadataImpl.getInstance(),
                eventBus,
                startupFlags
        );
    }

    /**
     * Loads the server configuration from the configuration file.
     *
     * @return the loaded configuration.
     * @throws IOException if an I/O error occurs while reading the configuration file.
     */
    public static Config loadServerConfiguration() throws IOException {
        log.info("Loading configuration from {} ...", CONFIG_PATH);
        return CONFIG_GSON.fromJson(Files.readString(CONFIG_PATH), Config.class);
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
        if (!Files.exists(CONFIG_PATH)) {
            log.error("No configuration was found. Please create a file called \"settings.json\" in \"{}\". Then try again",
                    FILES_DIR.toAbsolutePath());
            System.exit(1);
        }
    }


}
