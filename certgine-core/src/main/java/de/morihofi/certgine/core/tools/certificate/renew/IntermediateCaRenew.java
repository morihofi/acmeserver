/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.tools.certificate.renew;

import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.certgine.types.intf.IServerInstance;
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
                        .certificateConfig(provisioner.getIntermediateCa().getCertificateConfig())
                        .issuerKeyPair(serverInstance.getCryptoStoreManager().getCertificateAuthorityKeyPair(serverInstance.getRootCa()))
                        .issuerCertificate(serverInstance.getCryptoStoreManager().getCertificateAuthorityX509Certificate(serverInstance.getRootCa()))
                        .ownKeyPair(provisionerKeyPair)
                        .crlDistributionUrl(provisioner.getFullCrlUrl(serverInstance))
                        .ocspServiceEndpoint(provisioner.getFullOcspUrl(serverInstance))
                        .build()
        );

        serverInstance.getCryptoStoreManager().removeIntermediateCaCertificate(intermediateAlias);

        X509Certificate[] chain = new X509Certificate[]{
                renewedCertificate,
                serverInstance.getCryptoStoreManager().getCertificateAuthorityX509Certificate(serverInstance.getRootCa())
        };

        return new CertificateRenewScheduler.CertificateData(chain, provisionerKeyPair);
    }
}
