/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.impl;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.server.StartupFlag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import de.morihofi.certgine.types.events.EventBus;
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

    @NonNull
    private final BuildMetadata buildMetadata;

    @NonNull
    private final EventBus eventBus;

    @NonNull
    private final Set<StartupFlag> startupFlags;

    @NonNull
    private final ModuleRegistry moduleRegistry;

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
