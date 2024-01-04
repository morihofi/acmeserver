package de.morihofi.acmeserver.tools.certificate.renew;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class IntermediateCaRenew {

    /**
     * Renews an intermediate certificate for a provisioner and updates it in the keystore.
     *
     * @param provisionerKeyPair The key pair associated with the provisioner.
     * @param provisioner        The provisioner for which the certificate is being renewed.
     * @param provisionerCfg     The configuration for the provisioner.
     * @param cryptoStoreManager The CryptoStoreManager responsible for managing certificates.
     * @param intermediateAlias  The alias of the intermediate certificate to renew.
     * @throws CertificateException      If there is an issue with certificate handling.
     * @throws OperatorCreationException If there is an issue with certificate operator creation.
     * @throws IOException               If there is an I/O error.
     * @throws UnrecoverableKeyException If there is an issue with unrecoverable keys.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
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
    }


}
