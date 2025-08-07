package de.morihofi.certgine.cryptography.keystore;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

/**
 * Loads a {@link KeyStore} based on a specific configuration.
 */
public interface KeyStoreLoader {

    /**
     * Loads the configured {@link KeyStore}.
     *
     * @return initialized keystore instance
     */
    KeyStore load() throws CertificateException, IOException, NoSuchAlgorithmException,
            KeyStoreException, ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException, NoSuchMethodException,
            NoSuchProviderException;
}
