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

package de.morihofi.acmeserver.core.tools;

import com.google.gson.Gson;
import de.morihofi.acmeserver.core.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.core.config.Config;
import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.core.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.core.tools.network.NetworkClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents the server instance that holds various configurations and utilities required for the operation of the server.
 */
@Getter
public class ServerInstance {
    /**
     * The configuration settings for the application.
     */
    private final Config appConfig;

    /**
     * Indicates whether the server is running in debug mode.
     */
    private final boolean debug;

    /**
     * Manages cryptographic operations and the keystore.
     */
    private final CryptoStoreManager cryptoStoreManager;

    /**
     * Handles network operations.
     */
    private final NetworkClient networkClient;

    /**
     * Manages Hibernate sessions and database operations.
     */
    private final HibernateUtil hibernateUtil;

    /**
     * Manages nonces for the ACME protocol.
     */
    private final NonceManager nonceManager;

    /**
     * Path to the application's configuration file.
     */
    private final Path appConfigPath;

    /**
     * Logger for logging events and messages.
     */
    private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructs a new ServerInstance with the specified configurations and utilities.
     *
     * @param appConfig          The application configuration settings.
     * @param appConfigPath      The path to the application's configuration file.
     * @param debug              Indicates whether the server is running in debug mode.
     * @param cryptoStoreManager Manages cryptographic operations and the keystore.
     * @param networkClient      Handles network operations.
     * @param hibernateUtil      Manages Hibernate sessions and database operations.
     * @param nonceManager       Manages nonces for the ACME protocol.
     */
    public ServerInstance(Config appConfig, Path appConfigPath, boolean debug, CryptoStoreManager cryptoStoreManager, NetworkClient networkClient, HibernateUtil hibernateUtil, NonceManager nonceManager) {
        this.appConfig = appConfig;
        this.appConfigPath = appConfigPath;
        this.debug = debug;
        this.cryptoStoreManager = cryptoStoreManager;
        this.networkClient = networkClient;
        this.hibernateUtil = hibernateUtil;
        this.nonceManager = nonceManager;
    }

}
