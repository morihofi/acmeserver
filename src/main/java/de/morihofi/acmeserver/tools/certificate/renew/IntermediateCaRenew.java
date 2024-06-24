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

package de.morihofi.acmeserver.tools.certificate.renew;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class IntermediateCaRenew {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Renews an intermediate certificate for a provisioner and updates it in the keystore.
     *
     * @param provisionerKeyPair The key pair associated with the provisioner.
     * @param provisioner        The provisioner for which the certificate is being renewed.
     * @param cryptoStoreManager The CryptoStoreManager responsible for managing certificates.
     * @param intermediateAlias  The alias of the intermediate certificate to renew.
     * @return object containing the new certificate and keyPair
     * @throws CertificateException      If there is an issue with certificate handling.
     * @throws OperatorCreationException If there is an issue with certificate operator creation.
     * @throws IOException               If there is an I/O error.
     * @throws UnrecoverableKeyException If there is an issue with unrecoverable keys.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    public static CertificateRenewManager.CertificateData renewIntermediateCertificate(KeyPair provisionerKeyPair, Provisioner provisioner,
            CryptoStoreManager cryptoStoreManager, String intermediateAlias) throws CertificateException, OperatorCreationException,
            IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {

        ProvisionerConfig provisionerCfg = provisioner.getConfig();

        // Generate a new certificate
        X509Certificate renewedCertificate = CertificateAuthorityGenerator.createIntermediateCaCertificate(
                cryptoStoreManager,
                provisionerKeyPair,
                provisionerCfg.getIntermediate().getMetadata(),
                // Specify the expiration as per your requirement
                provisionerCfg.getIntermediate().getExpiration(),
                provisioner.getFullCrlUrl(),
                provisioner.getFullOcspUrl()
        );

        cryptoStoreManager.getKeyStore().deleteEntry(intermediateAlias);
        X509Certificate[] chain = new X509Certificate[]{
                renewedCertificate,
                (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)
        };

        return new CertificateRenewManager.CertificateData(chain, provisionerKeyPair);
    }
}
