/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.tsa;

import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.types.database.entities.authority.*;
import de.morihofi.certgine.tsa.types.entities.TsaAuthority;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.tsa.types.events.TsaAuthorityCreatedEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

/** Helper for initializing Timestamp Authority. */
@Slf4j
public class TsaInitHelper {
    public static TsaAuthority initializeTsa(@NonNull Session s,
                                             CryptoStoreManager csm,
                                             RootCa root,
                                             EventBus bus) throws NoSuchAlgorithmException,
            CertificateException, IOException, OperatorCreationException,
            KeyStoreException, UnrecoverableKeyException, NoSuchProviderException {

            TsaAuthority tsa;
            if (TsaAuthority.getAll(s).length == 0) {
                Transaction tx = s.beginTransaction();
                KeyPair kp = KeyPairGenerator.generateRSAKeyPair(4096, csm.getKeyStoreProviderName());
                tsa = new TsaAuthority();
                tsa.setCertificateConfig(new CertificateConfig(
                        CertificateMetadata.builder()
                                .commonName("Certgine Default TSA")
                                .build(),
                        new CertificateExpiration(0,0,3),
                        new RsaCertificateAlgorithm(4096)
                ));
                tsa.setInternalUuid(UUID.randomUUID().toString());
                X509Certificate cert = X509Generator.generate(
                        X509Generator.Request.builder()
                                .type(X509Generator.Type.TIMESTAMPING)
                                .certificateConfig(tsa.getCertificateConfig())
                                .issuerKeyPair(csm.getCertificateAuthorityKeyPair(root))
                                .issuerCertificate(csm.getCertificateAuthorityX509Certificate(root))
                                .ownKeyPair(kp)
                                .build()
                );

                X509Certificate[] chain = new X509Certificate[]{
                        cert,
                        csm.getCertificateAuthorityX509Certificate(root)
                };

                csm.addTimestampAuthority(chain, kp, tsa.getInternalUuid());

                s.persist(tsa);
                tx.commit();
                bus.publish(new TsaAuthorityCreatedEvent(tsa));
            } else {
                tsa = TsaAuthority.getAll(s)[0];
            }
            return tsa;
        }
}
