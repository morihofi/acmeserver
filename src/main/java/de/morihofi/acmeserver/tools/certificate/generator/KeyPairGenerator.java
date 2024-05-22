package de.morihofi.acmeserver.tools.certificate.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.ECGenParameterSpec;

public class KeyPairGenerator {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Generates an RSA KeyPair with the specified key size.
     *
     * @param rsaKeySize The size of the RSA key to generate.
     * @return A KeyPair containing the generated RSA public and private keys.
     * @throws NoSuchAlgorithmException If RSA key pair generation is not supported by the security provider.
     * @throws NoSuchProviderException  If the specified security provider is not found.
     */
    public static KeyPair generateRSAKeyPair(int rsaKeySize, String givenProviderName) throws NoSuchAlgorithmException,
            NoSuchProviderException {
        java.security.KeyPairGenerator rsa = java.security.KeyPairGenerator.getInstance("RSA", givenProviderName);
        rsa.initialize(rsaKeySize);
        return rsa.generateKeyPair();
    }

    /**
     * Generates an ECDSA KeyPair with the specified elliptic curve.
     *
     * @param curveName The name of the elliptic curve to use for key pair generation.
     * @return A KeyPair containing the generated ECDSA public and private keys.
     * @throws NoSuchAlgorithmException           If ECDSA key pair generation is not supported by the security provider.
     * @throws NoSuchProviderException            If the specified security provider is not found.
     * @throws InvalidAlgorithmParameterException If the provided curve name is invalid or not supported.
     */
    public static KeyPair generateEcdsaKeyPair(String curveName, String givenProviderName) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException {
        java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("ECDSA", givenProviderName);

        ECGenParameterSpec ecSpec = new ECGenParameterSpec(curveName);
        keyPairGenerator.initialize(ecSpec);

        return keyPairGenerator.generateKeyPair();
    }
}
