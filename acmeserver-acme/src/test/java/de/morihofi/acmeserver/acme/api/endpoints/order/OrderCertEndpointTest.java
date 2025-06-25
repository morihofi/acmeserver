package de.morihofi.acmeserver.acme.api.endpoints.order;
import de.morihofi.acmeserver.types.database.entities.authority.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.authority.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.authority.CertificateMetadata;
import de.morihofi.acmeserver.types.database.entities.authority.RootCa;
import de.morihofi.acmeserver.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.acmeserver.types.events.EventBus;

import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;

import java.security.Security;

class OrderCertEndpointTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    static class DummyServerInstance implements IServerInstance {
        private final CryptoStoreManager mgr;
        DummyServerInstance(CryptoStoreManager mgr) { this.mgr = mgr; }
        @NotNull
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @NonNull
        @Override public Session getDatabaseSession() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return mgr; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @NotNull
        @NonNull
        @Override public RootCa getRootCa() { return null; }
        @NotNull
        @NonNull
        @Override public TsaAuthority getTsaAuthority() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
            @NotNull
            @NonNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @NotNull
        @NonNull
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName(cn)
                .organisation("Org")
                .countryCode("DE")
                .build();
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }
}
