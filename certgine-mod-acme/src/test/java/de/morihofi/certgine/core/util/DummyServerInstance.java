/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.util;

import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.server.StartupFlag;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.mockito.Mockito;

/**
 * Simple {@link IServerInstance} implementation for tests.
 *
 * <p>All properties are publicly accessible so tests may override only the
 * fields they require. Unused methods return dummy values.</p>
 */
@NoArgsConstructor
public class DummyServerInstance implements IServerInstance {

    /** Base URL of the server. */
    public String serverURL = "http://localhost";

    /** Session returned by {@link #getDatabaseSession()}. */
    public Session databaseSession = Mockito.mock(Session.class);

    /** Crypto store manager. */
    public ICryptoStoreManager cryptoStoreManager = Mockito.mock(ICryptoStoreManager.class);

    /** Application configuration. */
    public Config appConfig = new Config();

    /** Root certificate authority. */
    public RootCa rootCa = new RootCa();

    /** Build metadata. */
    public BuildMetadata buildMetadata = BuildMetadata.builder().build();

    /** Network client. */
    public INetworkClient networkClient = Mockito.mock(INetworkClient.class);

    /** Event bus instance. */
    public EventBus eventBus = Mockito.mock(EventBus.class);

    /** Startup flags. */
    public Set<StartupFlag> startupFlags = Set.of();

    /** Module registry. */
    public IModuleRegistry moduleRegistry = Mockito.mock(IModuleRegistry.class);

    @Override
    public String getServerURL() {
        return serverURL;
    }

    @Override
    public Session getDatabaseSession() {
        return databaseSession;
    }

    @Override
    public ICryptoStoreManager getCryptoStoreManager() {
        return cryptoStoreManager;
    }

    @Override
    public Config getAppConfig() {
        return appConfig;
    }

    @Override
    public RootCa getRootCa() {
        return rootCa;
    }

    @Override
    public BuildMetadata getBuildMetadata() {
        return buildMetadata;
    }

    @Override
    public INetworkClient getNetworkClient() {
        return networkClient;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Set<StartupFlag> getStartupFlags() {
        return startupFlags;
    }

    @Override
    public IModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }
}

