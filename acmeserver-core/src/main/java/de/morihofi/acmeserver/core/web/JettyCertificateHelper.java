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

package de.morihofi.acmeserver.core.web;

import de.morihofi.acmeserver.cryptography.certificate.X509CertificateTools;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.utils.datetime.DateTools;
import de.morihofi.acmeserver.types.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

@Slf4j
public class JettyCertificateHelper {


    /**
     * Generates an ACME API client certificate for the ACME Web Server API, if it doesn't already exist in the key store.
     *
     * @param serverInstance Reference to the server instance containing the crypto store manager and configuration.
     * @throws CertificateException      If there is an issue with certificate handling.
     * @throws IOException               If an I/O error occurs.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     * @throws NoSuchProviderException   If a required cryptographic provider is not available.
     * @throws OperatorCreationException If there is an issue creating a cryptographic operator.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws UnrecoverableKeyException If a keystore key cannot be recovered.
     */
    public static CertificateRenewScheduler.CertificateData generateAcmeApiClientCertificate(IServerInstance serverInstance) throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException,
            OperatorCreationException, KeyStoreException, UnrecoverableKeyException {

        ICryptoStoreManager cryptoStoreManager = serverInstance.getCryptoStoreManager();

        KeyPair rootCaKeyPair = cryptoStoreManager.getCerificateAuthorityKeyPair(serverInstance.getRootCa());

        boolean needsNew = !cryptoStoreManager.containsServerCertificate("main");
        if (!needsNew) {
            X509Certificate current = cryptoStoreManager.getServerCertificate("main");
            needsNew = current == null || !X509CertificateTools.isCertificateCurrentlyDateValid(current);
        }

        if (!needsNew) {
            return null;
        }

        log.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
        KeyPair acmeAPIKeyPair = KeyPairGenerator.generateRSAKeyPair(4096, cryptoStoreManager.getKeyStoreProviderName());

        log.info("Using root CA for generation");
        X509Certificate rootCertificate =
                cryptoStoreManager.getCerificateAuthorityX509Certificate(serverInstance.getRootCa());
        X509Certificate intermediateCertificate = rootCertificate;

        log.info("Creating Server Certificate");
        Date startDate = new Date();
        Date endDate = DateTools.makeDateForOutliveIntermediateCertificate(
                intermediateCertificate.getNotAfter(),
                DateTools.addToDate(startDate, 0, 1, 0)
        );

        X509Certificate acmeAPICertificate = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.SERVER)
                        .issuerKeyPair(rootCaKeyPair)
                        .issuerCertificate(intermediateCertificate)
                        .serverPublicKeyBytes(acmeAPIKeyPair.getPublic().getEncoded())
                        .identifier(new Identifier(Identifier.IDENTIFIER_TYPE.DNS, serverInstance.getAppConfig().getServer().getDnsName()))
                        .startDate(startDate)
                        .endDate(endDate)
                        .serverInstance(serverInstance)
                        .build()
        );

        X509Certificate[] chain = new X509Certificate[]{
                acmeAPICertificate,
                intermediateCertificate,
                rootCertificate
        };

        return new CertificateRenewScheduler.CertificateData(chain, acmeAPIKeyPair);
    }


}
