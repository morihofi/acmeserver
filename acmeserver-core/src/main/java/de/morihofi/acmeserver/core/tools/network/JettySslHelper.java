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

package de.morihofi.acmeserver.core.tools.network;

import de.morihofi.acmeserver.types.exception.ServerStartupException;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.MozillaSslConfigHelper;
import io.javalin.jetty.JettyServer;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JettySslHelper {


    /**
     * Creates and configures an SSLContext for secure communication using the provided KeyStore, certificate alias, and key password.
     *
     * @param keyStore    The KeyStore containing the SSL certificate and private key.
     * @param alias       The alias of the certificate in the KeyStore.
     * @param keyPassword The password for the private key.
     * @return An SSLContext configured for secure communication.
     * @throws Exception If an error occurs while creating or configuring the SSLContext.
     */
    public static SSLContext createSSLContext(KeyStore keyStore, String alias, String keyPassword)
            throws Exception {

        // Create an instance of SslContextFactory
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

        // Set the KeyStore and passwords
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(keyPassword);
        sslContextFactory.setKeyManagerPassword(keyPassword);

        // Set the alias for the certificate
        sslContextFactory.setCertAlias(alias);

        sslContextFactory.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        sslContextFactory.setProtocol("TLS");

        // Set the algorithm for the KeyManager
        sslContextFactory.setKeyManagerFactoryAlgorithm("PKIX");

        // Initialize SslContextFactory
        sslContextFactory.start();

        // Get the SSLContext object from SslContextFactory
        return sslContextFactory.getSslContext();
    }


    /**
     * Creates a Jetty Server instance configured for both secure (HTTPS) and non-secure (HTTP) communication.
     *
     * @param httpsPort   The port number for secure HTTPS communication.
     * @param httpPort    The port number for non-secure HTTP communication.
     * @param keyStore    The KeyStore containing the SSL certificate and private key.
     * @param alias       The alias of the certificate in the KeyStore.
     * @param jettyServer Jetty server wrapper of Javalin
     * @return A Jetty Server instance configured for both secure and non-secure communication.
     * @throws Exception If an error occurs while creating or configuring the Jetty Server.
     */
    public static Server getSslJetty(int httpsPort, int httpPort, KeyStore keyStore, String alias, JettyServer jettyServer,
            boolean enableSniCheck, MozillaSslConfigHelper.BasicConfiguration mozillaConfig)
            throws Exception {

        SSLContext sslContext = createSSLContext(keyStore, alias, "");

        return getSslJetty(httpsPort, httpPort, sslContext, jettyServer, enableSniCheck, mozillaConfig);
    }

    public static void updateSslJetty(int httpsPort, int httpPort, KeyStore keyStore, String keystoreAliasAcmeapi, JettyServer jettyServer,
            boolean enableSniCheck, MozillaSslConfigHelper.BasicConfiguration mozillaConfig) throws Exception {
        getSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, jettyServer, enableSniCheck, mozillaConfig);
    }

    /**
     * Creates a Jetty server instance configured with SSL and/or HTTP connectors based on the provided ports and SSL context.
     *
     * @param httpsPort   The port for HTTPS. Set to 0 to disable HTTPS.
     * @param httpPort    The port for HTTP. Set to 0 to disable HTTP.
     * @param sslContext  The SSL context to be used for HTTPS. Pass null to disable HTTPS.
     * @param jettyServer Jetty server wrapper of Javalin
     * @return A configured Jetty Server instance.
     */
    public static Server getSslJetty(int httpsPort, int httpPort, SSLContext sslContext, JettyServer jettyServer, boolean enableSniCheck,
            MozillaSslConfigHelper.BasicConfiguration mozillaConfig) throws Exception {
    /*
        If the port is not 0, the Service (e.g., HTTP/HTTPS) is enabled. Otherwise, it is disabled.
    */
        Server server;
        if (jettyServer != null) {
            server = jettyServer.server();
        } else {
            server = new Server();
        }

        for (Connector connector : server.getConnectors()) {
            if (connector instanceof ServerConnector serverConnector) {

                // Stoppe den Connector nur, wenn er läuft.
                if (serverConnector.isStarted()) {
                    serverConnector.stop();
                }
            }
        }

        List<Connector> connectors = new ArrayList<>();

        if (httpsPort != 0 && sslContext != null) {
            log.info("API HTTPS support is ENABLED");
            HttpConfiguration https = new HttpConfiguration();
            SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
            secureRequestCustomizer.setSniHostCheck(enableSniCheck);
            https.addCustomizer(secureRequestCustomizer);

            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setSslContext(sslContext);
            if (mozillaConfig != null) {
                log.info("Configuring TLS using Mozilla configuration");
                sslContextFactory.setExcludeProtocols();
                sslContextFactory.setExcludeCipherSuites();
                sslContextFactory.setRenegotiationAllowed(false);
                sslContextFactory.setUseCipherSuitesOrder(true);

                sslContextFactory.setIncludeCipherSuites(mozillaConfig.ciphers().toArray(new String[0]));
                sslContextFactory.setIncludeProtocols(mozillaConfig.protocols().toArray(new String[0]));

                secureRequestCustomizer.setStsMaxAge(mozillaConfig.hstsMinAge());
                secureRequestCustomizer.setStsIncludeSubDomains(false);
            }

            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https));
            sslConnector.setPort(httpsPort);

            connectors.add(sslConnector);
        } else {
            throw new ServerStartupException("HTTPS MUST BE ENABLED AND CAN'T BE DISABLED");
        }

        if (httpPort != 0) {
            log.info("API HTTP support is ENABLED");
            // HTTP Configuration
            ServerConnector httpConnector = new ServerConnector(server);
            httpConnector.setPort(httpPort);

            connectors.add(httpConnector);
        } else {
            log.info("API HTTP support is DISABLED");
        }

        server.setConnectors(connectors.toArray(new Connector[0]));

        for (Connector connector : server.getConnectors()) {
            if (connector instanceof ServerConnector sslConnector && connector.getProtocols().contains("ssl")) {
                sslConnector.start();
                break;
            }
        }

        return server;
    }

    private JettySslHelper() {
    }
}
