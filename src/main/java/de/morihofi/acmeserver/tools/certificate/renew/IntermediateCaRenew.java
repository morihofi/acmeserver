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

    /**
     * Renews an intermediate certificate for a provisioner.
     *
     * @param provisionerKeyPair The provisioner's key pair.
     * @param certificatePath    The path to the provisioner's certificate file.
     * @param provisioner        The provisioner for which the certificate is being renewed.
     * @param provisionerCfg     The configuration for the provisioner.
     * @param caKeyPair          The key pair of the Certificate Authority (CA) issuing the certificate.
     * @throws CertificateException      If an issue occurs with certificate handling.
     * @throws OperatorCreationException If an issue occurs during operator creation.
     * @throws IOException               If an I/O error occurs during file operations.
     */
    public static void renewIntermediateCertificate(KeyPair provisionerKeyPair, Provisioner provisioner, ProvisionerConfig provisionerCfg, KeyPair caKeyPair, X509Certificate caCertificate, CryptoStoreManager cryptoStoreManager, String intermediateAlias) throws CertificateException, OperatorCreationException, IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {


        // Generate a new certificate
        X509Certificate renewedCertificate = CertificateAuthorityGenerator.createIntermediateCaCertificate(
                cryptoStoreManager,
                intermediateAlias,
                provisionerKeyPair,
                provisionerCfg.getIntermediate().getMetadata(),
                // Specify the expiration as per your requirement
                provisionerCfg.getIntermediate().getExpiration(),
                provisioner.getFullCrlUrl(),
                provisioner.getFullOcspUrl()
        );

        // Update the provisioner's certificate reference
        provisioner.setIntermediateCaCertificate(renewedCertificate);
    }

    /**
     * Renew an intermediate certificate for a provisioner using the provisioner's key pair.
     *
     * @param privateKeyPath  The path to the provisioner's private key file.
     * @param publicKeyPath   The path to the provisioner's public key file.
     * @param certificatePath The path to the provisioner's certificate file.
     * @param provisioner     The provisioner for which the certificate is being renewed.
     * @param provisionerCfg  The configuration for the provisioner.
     * @param caKeyPair       The key pair of the Certificate Authority (CA) issuing the certificate.
     * @throws CertificateException      If an issue occurs with certificate handling.
     * @throws OperatorCreationException If an issue occurs during operator creation.
     * @throws IOException               If an I/O error occurs during file operations.
     */
/*    public static void renewIntermediateCertificate(Path privateKeyPath, Path publicKeyPath, Path certificatePath, Provisioner provisioner, ProvisionerConfig provisionerCfg, KeyPair caKeyPair, X509Certificate caCertificate) throws CertificateException, OperatorCreationException, IOException {
        // Load the existing key pair
        KeyPair provisionerKeyPair = PemUtil.loadKeyPair(privateKeyPath, publicKeyPath);

        renewIntermediateCertificate(provisionerKeyPair, certificatePath, provisioner, provisionerCfg, caKeyPair, caCertificate);
    }*/
}
