package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.authority.RootCa;
import de.morihofi.acmeserver.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.server.StartupFlag;
import lombok.NonNull;
import org.hibernate.Session;
import de.morihofi.acmeserver.types.events.EventBus;

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
     * Get the Timestamp Authority for this server instance
     */
    @NonNull
    TsaAuthority getTsaAuthority();

    /**
     * Convenience method returning the keystore alias for the active root CA.
     *
     * @return alias of the root certificate authority in the keystore
     */
    @NonNull
    default String getRootCaAlias() {
        return getRootCa().getInternalUuid();
    }

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

    @NonNull
    Set<StartupFlag> getStartupFlags();
}
