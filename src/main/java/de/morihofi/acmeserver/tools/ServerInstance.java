/* © Copyright iFD GmbH 2024 www.ifd-gmbh.com */
package de.morihofi.acmeserver.tools;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.network.NetworkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerInstance {
    private final Config appConfig;
    private final boolean debug;
    private final CryptoStoreManager cryptoStoreManager;
    private final NetworkClient networkClient;
    private final HibernateUtil hibernateUtil;
    private final NonceManager nonceManager;
    private final Path appConfigPath;

    private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ServerInstance(Config appConfig, Path appConfigPath, boolean debug, CryptoStoreManager cryptoStoreManager, NetworkClient networkClient, HibernateUtil hibernateUtil, NonceManager nonceManager) {
        this.appConfig = appConfig;
        this.appConfigPath = appConfigPath;
        this.debug = debug;
        this.cryptoStoreManager = cryptoStoreManager;
        this.networkClient = networkClient;
        this.hibernateUtil = hibernateUtil;
        this.nonceManager = nonceManager;
    }

    public Config getAppConfig() {
        return appConfig;
    }

    public boolean isDebug() {
        return debug;
    }

    public HibernateUtil getHibernateUtil() {
        return hibernateUtil;
    }

    public CryptoStoreManager getCryptoStoreManager() {
        return cryptoStoreManager;
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public NonceManager getNonceManager() {
        return nonceManager;
    }

    public void saveServerConfiguration() throws IOException {
        LOG.info("Saving configuration {} ...", appConfigPath);
        Files.writeString(appConfigPath, new Gson().toJson(appConfig));
        LOG.info("Configuration saved");
    }
}
