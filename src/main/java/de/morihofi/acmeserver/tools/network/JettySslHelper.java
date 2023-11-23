package de.morihofi.acmeserver.tools.network;

import de.morihofi.acmeserver.tools.certificate.PemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
            KeyManagementException, UnrecoverableKeyException {

        // Create a new KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        // Add the certificate and key to the KeyStore
        keyStore.setKeyEntry("server", keyPair.getPrivate(), "password".toCharArray(), certificateChain);

        // Initialize the KeyManagerFactory with the KeyStore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "password".toCharArray());

        // Initialize the TrustManagerFactory with the KeyStore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Create and initialize the SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return sslContext;
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
    public static Server getSslJetty(int httpsPort, int httpPort, Path certificatePath, Path privateKeyPath, Path publicKeyPath)
            throws CertificateException, IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
            KeyManagementException, NoSuchProviderException {

        log.info("Loading Key Pair");
        KeyPair jettyKeyPair = PemUtil.loadKeyPair(privateKeyPath, publicKeyPath);

        X509Certificate[] certificateChain = PemUtil.loadCertificateChain(certificatePath);

        SSLContext sslContext = createSSLContext(certificateChain, jettyKeyPair);

        return getSslJetty(httpsPort, httpPort, sslContext);
    }

    /**
     * Creates a Jetty server instance configured with SSL and/or HTTP connectors based on the provided ports and SSL context.
     *
     * @param httpsPort  The port for HTTPS. Set to 0 to disable HTTPS.
     * @param httpPort   The port for HTTP. Set to 0 to disable HTTP.
     * @param sslContext The SSL context to be used for HTTPS. Pass null to disable HTTPS.
     * @return A configured Jetty Server instance.
     */
    public static Server getSslJetty(int httpsPort, int httpPort, SSLContext sslContext) {
    /*
        If the port is not 0, the Service (e.g., HTTP/HTTPS) is enabled. Otherwise, it is disabled.
    */
        Server server = new Server();
        List<Connector> connectors = new ArrayList<>();

        if (httpsPort != 0 && sslContext != null) {
            log.info("API HTTPS support is ENABLED");
            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setSslContext(sslContext); // Your SSL context

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
        return server;
    }

}
