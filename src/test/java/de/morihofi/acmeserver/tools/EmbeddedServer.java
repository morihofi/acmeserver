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

package de.morihofi.acmeserver.tools;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.WebServer;
import de.morihofi.acmeserver.config.CertificateExpiration;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.config.ServerConfig;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;
import de.morihofi.acmeserver.config.databaseConfig.JDBCUrlDatabaseConfig;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS12KeyStoreParams;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class EmbeddedServer {
    private final Config config = new Config();
    private final Path acmeServerDataPath;

    public EmbeddedServer(String host, int httpsPort, Path acmeServerDataPath, String jdbcUrl, String dbUser, String dbPass) {
        this.acmeServerDataPath = acmeServerDataPath;

        // Basic settings
        config.setServer(new ServerConfig());
        config.getServer().setDnsName(host);
        config.getServer().getPorts().setHttp(0); // Deactivate http by default. Must be enabled manually
        config.getServer().getPorts().setHttps(httpsPort);

        // Database
        {
            JDBCUrlDatabaseConfig jdbcUrlDatabaseConfig = new JDBCUrlDatabaseConfig();
            jdbcUrlDatabaseConfig.setJdbcUrl(jdbcUrl);
            jdbcUrlDatabaseConfig.setUser(dbUser);
            jdbcUrlDatabaseConfig.setPassword(dbPass);

            config.setDatabase(jdbcUrlDatabaseConfig);
        }

        // Init Bouncy Castle
        Main.initBouncyCastle();
        // Build Metadata
        Main.loadBuildAndGitMetadata();
    }

    public EmbeddedServer(Path acmeServerDataPath) {
        this.acmeServerDataPath = acmeServerDataPath;
    }

    public void configureKeystorePKCS12(String location, String password) {
        // Keystore

        PKCS12KeyStoreParams ksConfig = new PKCS12KeyStoreParams();
        ksConfig.setLocation(location);
        ksConfig.setPassword(password);

        config.setKeyStore(ksConfig);
    }

    public void configureRsaRootCertificate(int days, int months, int years, String cn) {
        config.getRootCA().setAlgorithm(new RSAAlgorithmParams(4096));
        config.getRootCA().setExpiration(new CertificateExpiration(days, months, years));
        config.getRootCA().getMetadata().setCommonName(cn);
    }

    public void addSimpleProvisioner(String name, int days, int months, int years) {

        ProvisionerConfig provisionerConfig = new ProvisionerConfig();
        provisionerConfig.setName(name);
        provisionerConfig.setIssuedCertificateExpiration(new CertificateExpiration(days, months, years));
        provisionerConfig.getIntermediate().setAlgorithm(new RSAAlgorithmParams(4096));
        provisionerConfig.getIntermediate().setExpiration(new CertificateExpiration(7, 0, 0));
        provisionerConfig.getIntermediate().getMetadata().setCommonName(name);

        config.getProvisioner().add(provisionerConfig);
    }

    public Config getConfig() {
        return config;
    }

    public Path getAcmeServerDataPath() {
        return acmeServerDataPath;
    }

    public X509Certificate getRootCertificate() throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        return (X509Certificate) getServerInstance().getCryptoStoreManager().getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);
    }

    private ServerInstance serverInstance = null;

    private ServerInstance getServerInstance() throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (serverInstance != null) {
            return serverInstance;
        }

        serverInstance = Main.getServerInstance(
                config,
                false,
                acmeServerDataPath.resolve("settings.json")
        );
        return serverInstance;
    }

    public void start() {
        try {
            WebServer webServer = new WebServer(
                    getServerInstance()
            );
            webServer.startServer();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
