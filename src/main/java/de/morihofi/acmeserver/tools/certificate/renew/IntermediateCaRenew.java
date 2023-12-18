package de.morihofi.acmeserver.tools.certificate.renew;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.X509;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class IntermediateCaRenew {


    public static void renewIntermediateCertificate(KeyPair provisionerKeyPair, Provisioner provisioner, ProvisionerConfig provisionerCfg, CryptoStoreManager cryptoStoreManager, String intermediateAlias) throws CertificateException, OperatorCreationException, IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {


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
        cryptoStoreManager.getKeyStore().setKeyEntry(
                intermediateAlias,
                provisionerKeyPair.getPrivate(),
                "".toCharArray(),
                chain
        );
        cryptoStoreManager.saveKeystore();


        // provisioner's certificate reference
    }


}
