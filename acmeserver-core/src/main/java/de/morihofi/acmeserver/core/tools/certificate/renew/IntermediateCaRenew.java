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

package de.morihofi.acmeserver.core.tools.certificate.renew;

import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
public class IntermediateCaRenew {


    public static CertificateRenewScheduler.CertificateData renewIntermediateCertificate(KeyPair provisionerKeyPair, AcmeProvisioner provisioner,
                                                                                       IServerInstance serverInstance, String intermediateAlias) throws CertificateException, OperatorCreationException,
            IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {


        // Generate a new certificate
        X509Certificate renewedCertificate = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.INTERMEDIATE_CA)
                        .certificateConfig(provisioner.getCertificateConfig())
                        .issuerKeyPair(serverInstance.getCryptoStoreManager().getCerificateAuthorityKeyPair(serverInstance.getRootCa()))
                        .issuerCertificate(serverInstance.getCryptoStoreManager().getCerificateAuthorityX509Certificate(serverInstance.getRootCa()))
                        .ownKeyPair(provisionerKeyPair)
                        .crlDistributionUrl(provisioner.getFullCrlUrl(serverInstance))
                        .ocspServiceEndpoint(provisioner.getFullOcspUrl(serverInstance))
                        .build()
        );

        KeyStore ks = serverInstance.getCryptoStoreManager().getKeyStore();

        ks.deleteEntry(intermediateAlias);
        X509Certificate[] chain = new X509Certificate[]{
                renewedCertificate,
                serverInstance.getCryptoStoreManager().getCerificateAuthorityX509Certificate(serverInstance.getRootCa())
        };

        return new CertificateRenewScheduler.CertificateData(chain, provisionerKeyPair);
    }
}
