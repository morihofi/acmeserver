package de.morihofi.acmeserver.tools.network;

import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.MozillaSslConfigHelper;
import io.javalin.jetty.JettyServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class JettySslHelper {

    private JettySslHelper() {
    }

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(JettySslHelper.class);

    /**
     * Creates an SSLContext with the specified certificate chain and key pair.
     *
     * @param certificateChain The certificate chain.
     * @param keyPair          The key pair.
     * @return An initialized SSLContext.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws CertificateException      If there is an issue with the certificate.
     * @throws IOException               If there is an issue reading the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     * @throws KeyManagementException    If there is an issue with key management.
     * @throws UnrecoverableKeyException If the private key cannot be recovered.
     */
    public static SSLContext createSSLContext(X509Certificate[] certificateChain, KeyPair keyPair)
            throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException,
            KeyManagementException, UnrecoverableKeyException, NoSuchProviderException {

        // Create a new KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        // Add the certificate and key to the KeyStore
        keyStore.setKeyEntry("server", keyPair.getPrivate(), "".toCharArray(), certificateChain);

        // Initialize the KeyManagerFactory with the KeyStore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "".toCharArray());

        // Initialize the TrustManagerFactory with the KeyStore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);


        // Create and initialize the SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }

    /**
     * Creates and configures an SSLContext for secure communication using the provided KeyStore,
     * certificate alias, and key password.
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
     * Creates a Jetty server instance configured with SSL and/or HTTP connectors based on the provided ports and SSL context.
     *
     * @param httpsPort       The port for HTTPS. Set to 0 to disable HTTPS.
     * @param httpPort        The port for HTTP. Set to 0 to disable HTTP.
     * @param certificatePath The path to the SSL certificate chain file.
     * @param privateKeyPath  The path to the private key file.
     * @param publicKeyPath   The path to the public key file.
     * @return A configured Jetty Server instance.
     * @throws CertificateException      If there is an issue with the SSL certificate.
     * @throws IOException               If there is an issue reading the certificate or key files.
     * @throws UnrecoverableKeyException If the private key cannot be recovered.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     * @throws KeyManagementException    If there is an issue with key management.
     * @throws NoSuchProviderException   If a required security provider is not available.
     */
    public static Server getSslJetty(int httpsPort, int httpPort, Path certificatePath, Path privateKeyPath, Path publicKeyPath, boolean enableSniCheck, MozillaSslConfigHelper.BasicConfiguration mozillaConfig)
            throws Exception {

        log.info("Loading Key Pair");
        KeyPair jettyKeyPair = PemUtil.loadKeyPair(privateKeyPath, publicKeyPath);

        X509Certificate[] certificateChain = PemUtil.loadCertificateChain(certificatePath);

        SSLContext sslContext = createSSLContext(certificateChain, jettyKeyPair);

        return getSslJetty(httpsPort, httpPort, sslContext, null, enableSniCheck, mozillaConfig);
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
    public static Server getSslJetty(int httpsPort, int httpPort, KeyStore keyStore, String alias, JettyServer jettyServer, boolean enableSniCheck, MozillaSslConfigHelper.BasicConfiguration mozillaConfig)
            throws Exception {


        SSLContext sslContext = createSSLContext(keyStore, alias, "");

        return getSslJetty(httpsPort, httpPort, sslContext, jettyServer, enableSniCheck, mozillaConfig);
    }

    public static void updateSslJetty(int httpsPort, int httpPort, KeyStore keyStore, String keystoreAliasAcmeapi, JettyServer jettyServer, boolean enableSniCheck, MozillaSslConfigHelper.BasicConfiguration mozillaConfig) throws Exception {
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
    public static Server getSslJetty(int httpsPort, int httpPort, SSLContext sslContext, JettyServer jettyServer, boolean enableSniCheck, MozillaSslConfigHelper.BasicConfiguration mozillaConfig) throws Exception {
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
            sslContextFactory.setSslContext(sslContext); // Your SSL context
            if(mozillaConfig != null){
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
            log.info("API HTTPS support is DISABLED. THIS IS NOT RECOMMENDED; YOU MAY ENCOUNTER UNEXPECTED BEHAVIOR!");
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


}
