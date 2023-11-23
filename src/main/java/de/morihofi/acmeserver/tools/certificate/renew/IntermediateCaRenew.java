package de.morihofi.acmeserver.tools.certificate.renew;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.X509;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
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
    public static void renewIntermediateCertificate(KeyPair provisionerKeyPair, Path certificatePath, Provisioner provisioner, ProvisionerConfig provisionerCfg, KeyPair caKeyPair, X509Certificate caCertificate) throws CertificateException, OperatorCreationException, IOException {


        // Generate a new certificate
        X509Certificate renewedCertificate = CertificateAuthorityGenerator.createIntermediateCaCertificate(
                caKeyPair, // This should be your CA's key pair
                provisionerKeyPair,
                provisionerCfg.getIntermediate().getMetadata().getCommonName(), // Or any other CN you prefer
                // Specify the expiration as per your requirement
                provisionerCfg.getIntermediate().getExpiration(),
                provisioner.getFullCrlUrl(),
                provisioner.getFullOcspUrl(),
                caCertificate
        );

        // Save the renewed certificate
        Files.writeString(certificatePath, PemUtil.certificateToPEM(renewedCertificate.getEncoded()));

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
    public static void renewIntermediateCertificate(Path privateKeyPath, Path publicKeyPath, Path certificatePath, Provisioner provisioner, ProvisionerConfig provisionerCfg, KeyPair caKeyPair, X509Certificate caCertificate) throws CertificateException, OperatorCreationException, IOException {
        // Load the existing key pair
        KeyPair provisionerKeyPair = PemUtil.loadKeyPair(privateKeyPath, publicKeyPath);

        renewIntermediateCertificate(provisionerKeyPair, certificatePath, provisioner, provisionerCfg, caKeyPair, caCertificate);
    }
}
