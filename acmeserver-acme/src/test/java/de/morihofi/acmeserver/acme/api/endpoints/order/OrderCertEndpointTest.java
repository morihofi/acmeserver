package de.morihofi.acmeserver.acme.api.endpoints.order;
import de.morihofi.acmeserver.types.events.EventBus;

import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderCertEndpointTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    static class DummyServerInstance implements IServerInstance {
        private final CryptoStoreManager mgr;
        DummyServerInstance(CryptoStoreManager mgr) { this.mgr = mgr; }
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NonNull
        @Override public Session getDatabaseSession() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return mgr; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.database.entities.TsaAuthority getTsaAuthority() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
            @NonNull
        @Override public EventBus getEventBus() { return new EventBus(); }
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
