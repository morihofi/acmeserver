package de.morihofi.acmeserver.core.helper.tsa;

import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.events.TsaAuthorityCreatedEvent;
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
    public static TsaAuthority initializeTsa(@NonNull HibernateUtil hu,
                                             CryptoStoreManager csm,
                                             RootCa root,
                                             EventBus bus) throws NoSuchAlgorithmException,
            CertificateException, IOException, OperatorCreationException,
            KeyStoreException, UnrecoverableKeyException, NoSuchProviderException {
        try (Session s = hu.getSessionFactory().openSession()) {
            TsaAuthority tsa;
            if (TsaAuthority.getAll(s).length == 0) {
                Transaction tx = s.beginTransaction();
                KeyPair kp = KeyPairGenerator.generateRSAKeyPair(2048, csm.getKeyStore().getProvider().getName());
                tsa = new TsaAuthority();
                tsa.setCertificateConfig(new CertificateConfig(
                        new CertificateMetadata("ACME Default TSA", "", "", ""),
                        new CertificateExpiration(0,0,3),
                        new RsaCertificateAlgorithm(4096)
                ));
                tsa.setInternalUuid(UUID.randomUUID().toString());
                X509Certificate cert = X509Generator.generate(
                        X509Generator.Request.builder()
                                .type(X509Generator.Type.TIMESTAMPING)
                                .certificateConfig(tsa.getCertificateConfig())
                                .issuerKeyPair(csm.getCerificateAuthorityKeyPair(root))
                                .issuerCertificate(csm.getCerificateAuthorityX509Certificate(root))
                                .ownKeyPair(kp)
                                .build()
                );
                String alias = csm.getKeyStoreAliasForTimestampAuthority(tsa.getInternalUuid());
                csm.getKeyStore().setKeyEntry(alias, kp.getPrivate(), "".toCharArray(), new X509Certificate[]{cert,
                        csm.getCerificateAuthorityX509Certificate(root)});
                csm.saveKeystore();
                s.persist(tsa);
                tx.commit();
                bus.publish(new TsaAuthorityCreatedEvent(tsa));
            } else {
                tsa = TsaAuthority.getAll(s)[0];
            }
            return tsa;
        }
    }
}
