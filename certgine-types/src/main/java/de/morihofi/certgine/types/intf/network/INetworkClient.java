package de.morihofi.certgine.types.intf.network;

import de.morihofi.certgine.types.intf.network.dns.IDoHClient;

import java.net.Proxy;
import java.util.List;

/**
 * NetworkClient is responsible for configuring and managing the OkHttpClient instance, including DNS settings and proxy configuration.
 */
public interface INetworkClient {
    /**
     * OkHttpClient instance for handling network requests.
     */
    okhttp3.OkHttpClient getOkHttpClient();

    /**
     * DoHClient instance for handling DNS over HTTPS requests.
     */
    IDoHClient getDoHClient();

    /**
     * unmodifiable List of DNS servers configured for the network client.
     */
    List<String> getDnsServer();

    /**
     * Retrieves and configures Proxy based on application settings. The method configures the proxy settings and authentication details, if
     * required.
     *
     * @return A Proxy object configured based on application settings.
     */
    Proxy getProxy();

}
