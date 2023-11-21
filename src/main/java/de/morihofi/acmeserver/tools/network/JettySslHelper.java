package de.morihofi.acmeserver.tools.network;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.tools.CertTools;
import de.morihofi.acmeserver.tools.PemUtil;
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

    public static SSLContext createSSLContext(X509Certificate[] certificateChain, KeyPair keyPair) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
        // Erstellen Sie ein neues KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        // Fügen Sie das Zertifikat und den Schlüssel zum KeyStore hinzu
        keyStore.setKeyEntry("server", keyPair.getPrivate(), "password".toCharArray(), certificateChain);

        // Initialisieren Sie das KeyManagerFactory mit dem KeyStore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "password".toCharArray());

        // Initialisieren Sie das TrustManagerFactory mit dem KeyStore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Erstellen und initialisieren Sie den SSL-Kontext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }

    public static Server getSslJetty(int httpsPort, int httpPort, Path certificatePath, Path privateKeyPath, Path publicKeyPath) throws CertificateException, IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException {

        log.info("Loading Key Pair");
        KeyPair jettyKeyPair = PemUtil.loadKeyPair(privateKeyPath, publicKeyPath);

        X509Certificate[] certificateChain = PemUtil.loadCertificateChain(certificatePath);

        SSLContext sslContext = createSSLContext(certificateChain, jettyKeyPair);

        return getSslJetty(httpsPort, httpPort, sslContext);
    }

    /*
    This is a workaround, cause Javalin's SSL Plugin needs Conscrypt, which is platform dependent.
     */
    public static Server getSslJetty(int httpsPort, int httpPort, SSLContext sslContext) {
        /*
            If port is not 0, the Service (e.g. HTTP/HTTPS) is enabled. Otherwise, it is disabled
        */
        Server server = new Server();
        List<Connector> connectors = new ArrayList<>();
        if (httpsPort != 0 && sslContext != null) {
            log.info("API HTTPS support is ENABLED");
            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setSslContext(sslContext); // Ihr SSL-Kontext

            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https));
            sslConnector.setPort(httpsPort);

            connectors.add(sslConnector);
        } else {
            log.info("API HTTPS support is DISABLED. THIS IS NOT RECOMMENDED, YOU MAY ENCOUNTER A UNEXPECTED BEHAVIOR!");
        }
        if (httpPort != 0) {
            log.info("API HTTP support is ENABLED");
            // HTTP-Konfiguration
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
