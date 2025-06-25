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

package de.morihofi.acmeserver.core.impl;

import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.authority.RootCa;
import de.morihofi.acmeserver.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.server.StartupFlag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import de.morihofi.acmeserver.types.events.EventBus;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;

/**
 * Represents the server instance that holds various configurations and utilities required for the operation of the server.
 */
@Getter
@Builder
public class ServerInstance implements IServerInstance {

    // TODO: Re-add
    // Clear sensitive passwords from in memory config to avoid accidental exposure
    // this.appConfig.getDatabase().setPassword(null);
    // this.appConfig.getKeyStore().clearPassword();

    /**
     * The configuration settings for the application.
     */
    @NonNull
    private final Config appConfig;

    /**
     * Path to the application's configuration file.
     */
    @NonNull
    private final Path appConfigPath;

    /**
     * Indicates whether the server is running in debug mode.
     */
    private final boolean debug;

    /**
     * Manages cryptographic operations and the keystore.
     */
    @NonNull
    private final CryptoStoreManager cryptoStoreManager;

    /**
     * Handles network operations.
     */
    @NonNull
    private final INetworkClient networkClient;

    /**
     * Manages Hibernate sessions and database operations.
     */
    @NonNull
    private final HibernateUtil hibernateUtil;

    /**
     * Manages nonce's for the ACME protocol.
     */
    @NonNull
    private final INonceManager nonceManager;

    @Setter
    @NonNull
    private RootCa rootCa;

    @Setter
    @NonNull
    private TsaAuthority tsaAuthority;

    @NonNull
    private final BuildMetadata buildMetadata;

    @NonNull
    private final EventBus eventBus;

    @NonNull
    private final Set<StartupFlag> startupFlags;

    /**
     * Retrieves the server URL constructed from the application's configuration. This method combines the DNS name and HTTPS port specified
     * in the app configuration to form the complete server URL.
     *
     * @return a String representing the full HTTPS URL of the server
     */
    @NotNull
    @NonNull
    public String getServerURL() {
        return "https://" + this.getAppConfig().getServer().getDnsName() + (this.getAppConfig().getServer().getPorts().getHttps() != 443 ? ":"
                + this.getAppConfig().getServer().getPorts().getHttps() : "");
    }


    @NotNull
    @NonNull
    @Override
    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // Suppress false positive for non-null return value
    public Session getDatabaseSession() {
        if (getHibernateUtil().getSessionFactory() == null) {
            throw new IllegalStateException("Hibernate SessionFactory is not initialized. Please ensure that the HibernateUtil is properly configured.");
        }

        return getHibernateUtil().getSessionFactory().openSession();
    }

    @NotNull
    @NonNull
    @Override
    public EventBus getEventBus() {
        return eventBus;
    }


}
