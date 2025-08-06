/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.intf;

import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.server.StartupFlag;
import lombok.NonNull;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class IServerInstanceTest {

    static class DummyInstance implements IServerInstance {
        private final RootCa rootCa;

        DummyInstance(String alias) {
            this.rootCa = new RootCa();
            this.rootCa.setInternalUuid(alias);
        }

        @NotNull
        @NonNull
        @Override
        public String getServerURL() { return ""; }

        @NotNull
        @NonNull
        @Override
        public Session getDatabaseSession() { return null; }

        @NotNull
        @NonNull
        @Override
        public ICryptoStoreManager getCryptoStoreManager() { return null; }

        @NotNull
        @NonNull
        @Override
        public Config getAppConfig() { return null; }

        @NotNull
        @NonNull
        @Override
        public INonceManager getNonceManager() { return null; }

        @NotNull
        @NonNull
        @Override
        public RootCa getRootCa() { return rootCa; }

        @NotNull
        @Override
        public TsaAuthority getTsaAuthority() { return null; }

        @NotNull
        @NonNull
        @Override
        public BuildMetadata getBuildMetadata() { return null; }

        @NotNull
        @NonNull
        @Override
        public INetworkClient getNetworkClient() { return null; }

        @NotNull
        @NonNull
        @Override
        public EventBus getEventBus() { return null; }

        @NotNull
        @Override
        public @NonNull Set<StartupFlag> getStartupFlags() {
            return Set.of();
        }

        @Override
        public @NonNull IModuleRegistry getModuleRegistry() {
            return null;
        }
    }

}

