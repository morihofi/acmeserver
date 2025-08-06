/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.intf;

import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.server.StartupFlag;
import lombok.NonNull;
import org.hibernate.Session;
import de.morihofi.certgine.types.events.EventBus;

import java.util.Set;

public interface IServerInstance {
    /**
     * Get the base URL to this server instance
     *
     * @return e.g. <a href="https://acme.example.com:8443/">https://acme.example.com:8443/</a>
     */
    @NonNull
    String getServerURL();

    /**
     * Get a new connection session to the underlying Hibernate ORM Layer
     *
     * @return new Hibernate Session
     */
    @NonNull
    Session getDatabaseSession();

    /**
     * Get the current CryptoStoreManager
     *
     * @return current CryptoStoreManager instance
     */
    @NonNull
    ICryptoStoreManager getCryptoStoreManager();

    /**
     * Get runtime configuration
     *
     * @return the current configuration of the server instance
     */
    @NonNull
    Config getAppConfig();

    /**
     * Get the NonceManager for this server instance
     */
    @NonNull
    INonceManager getNonceManager();

    /**
     * Get the Root CA for this server instance
     */
    @NonNull
    RootCa getRootCa();

    /**
     * Get the Build Metadata for this server instance
     */
    @NonNull
    BuildMetadata getBuildMetadata();

    /**
     * Get the NetworkClient for this server instance for making network requests using preconfigured settings like proxy, dns-over-https, etc.
     */
    @NonNull
    INetworkClient getNetworkClient();

    /**
     * Access to the event bus associated with this server instance.
     *
     * @return event bus for publishing and subscribing to events
     */
    @NonNull
    EventBus getEventBus();

    /**
     * Get list of startup flags provided at run
     *
     * @return startup flags
     */
    @NonNull
    Set<StartupFlag> getStartupFlags();


    @NonNull
    IModuleRegistry getModuleRegistry();
}
