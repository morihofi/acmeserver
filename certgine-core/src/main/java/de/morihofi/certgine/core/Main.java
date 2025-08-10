/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core;

import com.google.gson.Gson;
import de.morihofi.certgine.types.json.GsonFactory;
import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.core.impl.ServerInstance;
import de.morihofi.certgine.core.modules.ModuleManager;
import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.core.web.JettySslHelper;
import de.morihofi.certgine.core.web.WebServer;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.cryptography.keystore.Pkcs11KeyStoreLoader;
import de.morihofi.certgine.cryptography.keystore.Pkcs12KeyStoreLoader;
import de.morihofi.certgine.types.cryptography.keystore.PKCS11KeyStoreConfig;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.certgine.core.helper.cert.CaInitHelper;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.helper.KeyStoreParamsDeserializer;
import de.morihofi.certgine.types.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.certgine.types.config.keyStoreHelpers.PKCS11KeyStoreParams;
import de.morihofi.certgine.types.config.keyStoreHelpers.PKCS12KeyStoreParams;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.server.StartupFlag;
import de.morihofi.certgine.utils.cli.CLIArgument;
import de.morihofi.certgine.utils.meta.BuildMetadataImpl;
import de.morihofi.certgine.utils.network.http.NetworkClient;
import de.morihofi.certgine.utils.network.ssl.mozillasslconfig.MozillaSslConfigHelper;
import de.morihofi.certgine.utils.path.AppDirectoryHelper;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.events.ServerInitializedEvent;
import de.morihofi.certgine.types.events.ServerStartedEvent;
import de.morihofi.certgine.types.events.ServerShutdownEvent;
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
 * Main class for the Certgine application. This class handles the initialization and startup of the server.
 */
@Slf4j
public class Main {

    /**
     * `serverdata` directory as an absolute path. The default location can be
     * overridden using the {@code SERVERDATA_DIR} environment variable.
     */
    public static final Path FILES_DIR;

    static {
        String envDir = System.getenv("SERVERDATA_DIR");
        if (envDir != null && !envDir.isBlank()) {
            FILES_DIR = Paths.get(envDir).toAbsolutePath();
        } else {
            FILES_DIR = AppDirectoryHelper.getAppDirectory(MethodHandles.lookup().lookupClass())
                    .orElseThrow(() -> new IllegalStateException("Could not determine application directory"))
                    .resolve("serverdata").toAbsolutePath();
        }
    }

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
    private static final Gson CONFIG_GSON = GsonFactory.baseBuilder()
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
        EventBus eventBus = new EventBus();
        serverInstance = getServerInstance(config, debug, CONFIG_PATH, eventBus);
        eventBus.publish(new ServerInitializedEvent(serverInstance));


        WebServer ws = new WebServer(serverInstance);
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

    public static IServerInstance getServerInstance(Config config, boolean debug, Path configPath, EventBus eventBus) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvalidAlgorithmParameterException, OperatorCreationException, UnrecoverableKeyException {
        if (Objects.equals(System.getenv("DEBUG"), "TRUE")) {
            debug = true;
            log.info("Debug mode activated by DEBUG environment variable set to TRUE");
        }

        if (debug) {
            log.warn("!!! RUNNING IN DEBUG MODE - BEHAVIOR CAN BE DIFFERENT. DO NOT USE IN PRODUCTION !!!");
        }

        log.info("Initializing keystore ...");

        CryptoStoreManager cryptoStoreManager = switch (config.getKeyStore()) {
            case PKCS11KeyStoreParams p11 -> {
                PKCS11KeyStoreConfig cfg = new PKCS11KeyStoreConfig(
                        Paths.get(p11.getLibraryLocation()),
                        p11.getSlot(),
                        p11.getPassword());
                yield new CryptoStoreManager(cfg, new Pkcs11KeyStoreLoader(cfg));
            }
            case PKCS12KeyStoreParams p12 -> {
                PKCS12KeyStoreConfig cfg = new PKCS12KeyStoreConfig(
                        Paths.get(p12.getLocation()),
                        p12.getPassword());
                yield new CryptoStoreManager(cfg, new Pkcs12KeyStoreLoader(cfg));
            }
            default -> throw new IllegalArgumentException("Unsupported keystore");
        };

        log.info("Loading modules ...");
        ModuleManager moduleManager = new ModuleManager(eventBus);
        // Modules are initially loaded without a server instance. The instance is
        // injected once the server is fully constructed further below.
        moduleManager.loadModulesFromClasspath(null);

        Path modulesDir = FILES_DIR.resolve("modules");
        if (Files.isDirectory(modulesDir)) {
            try (var paths = Files.list(modulesDir)) {
                paths.filter(p -> p.toString().endsWith(".jar")).forEach(p -> {
                    try {
                        moduleManager.loadModule(p);
                    } catch (IOException ex) {
                        log.warn("Failed to load module from {}", p, ex);
                    }
                });
            } catch (IOException e) {
                log.warn("Failed to scan modules directory", e);
            }
        }

        ModuleRegistry moduleRegistry = moduleManager.getModuleRegistry();

        log.info("Initializing database ...");
        HibernateUtil hibernateUtil = new HibernateUtil(config, debug, eventBus,
                moduleRegistry);
        hibernateUtil.initDatabase();

        log.info("Initializing certificate authority ...");
        RootCa root = CaInitHelper.initializeCA(hibernateUtil, cryptoStoreManager, eventBus);

        log.info("Creating new server instance ...");

        IServerInstance preServerInstance = ServerInstance.builder()
                .appConfig(config)
                .appConfigPath(configPath)
                .debug(debug)
                .cryptoStoreManager(cryptoStoreManager)
                .networkClient(new NetworkClient(config.getNetwork()))
                .hibernateUtil(hibernateUtil)
                .moduleRegistry(moduleRegistry)
                .rootCa(root)
                .buildMetadata(BuildMetadataImpl.getInstance())
                .eventBus(eventBus)
                .startupFlags(startupFlags)
                .build();

        // Inject the fully constructed server instance into loaded modules and
        // capture their optional module interfaces.
        for (ModuleRegistry.ModuleInfo moduleInfo : moduleRegistry.getModules().values()) {
            moduleInfo.getModule().setServerInstance(preServerInstance);
            try {
                moduleInfo.setModuleInstance(moduleInfo.getModule().getModuleInstance());
            } catch (Exception e) {
                log.debug("Module {} does not provide a module instance", moduleInfo.getModuleName());
            }
        }

        log.info("Initializing modules ...");
        for (Map.Entry<String, ModuleRegistry.ModuleInfo> m : moduleRegistry.getModules().entrySet()){
            ModuleRegistry.ModuleInfo moduleInfo = m.getValue();
            String name = m.getKey();
            log.debug("Initializing module {} ...", name);
            moduleInfo.getModule().onModuleInitialize(preServerInstance);
        }
        log.info("All modules initialized");

        // ... and all is done, time to continue
        return preServerInstance;
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
                  ___ ___ ___ _____ ___ ___ _  _ ___\s
                 / __| __| _ \\_   _/ __|_ _| \\| | __|
                | (__| _||   / | || (_ || || .` | _|\s
                 \\___|___|_|_\\ |_| \\___|___|_|\\_|___|
                Welcome to Certgine!
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
