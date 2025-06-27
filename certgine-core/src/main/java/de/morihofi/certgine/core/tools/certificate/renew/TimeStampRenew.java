package de.morihofi.certgine.core.tools.certificate.renew;

import de.morihofi.certgine.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/** Utility to renew TSA certificates. */
@Slf4j
public class TimeStampRenew {

    public static CertificateRenewScheduler.CertificateData renew(KeyPair tsaKeyPair,
                                                                  TsaAuthority tsa,
                                                                  IServerInstance si,
                                                                  String alias) throws CertificateException,
            OperatorCreationException, IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        X509Certificate renewed = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.TIMESTAMPING)
                        .certificateConfig(tsa.getCertificateConfig())
                        .issuerKeyPair(si.getCryptoStoreManager().getCertficateAuthorityKeyPair(si.getRootCa()))
                        .issuerCertificate(si.getCryptoStoreManager().getCertficateAuthorityX509Certificate(si.getRootCa()))
                        .ownKeyPair(tsaKeyPair)
                        .build()
        );
        X509Certificate rootCert = si.getCryptoStoreManager().getCertficateAuthorityX509Certificate(si.getRootCa());
        X509Certificate[] chain = new X509Certificate[]{renewed, rootCert};
        return new CertificateRenewScheduler.CertificateData(chain, tsaKeyPair);
    }
}
