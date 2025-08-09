/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.keystore;

import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

/**
 * Loader for PKCS#12 file based keystores.
 */
@Slf4j
public class Pkcs12KeyStoreLoader implements KeyStoreLoader {

    private final PKCS12KeyStoreConfig config;

    /**
     * Creates a loader for the given configuration.
     *
     * @param config PKCS#12 configuration
     */
    public Pkcs12KeyStoreLoader(@NonNull PKCS12KeyStoreConfig config) {
        this.config = config;
    }

    @Override
    /**
     * Loads the configured PKCS#12 key store from disk or creates a new one if absent.
     *
     * @return an initialized {@link KeyStore}
     * @throws CertificateException      if certificate parsing fails
     * @throws IOException               if reading the key store fails
     * @throws NoSuchAlgorithmException  if required algorithms are unavailable
     * @throws KeyStoreException         if the keystore type is unsupported
     * @throws ClassNotFoundException    if provider classes cannot be found
     * @throws InvocationTargetException if reflection calls fail
     * @throws InstantiationException    if providers cannot be instantiated
     * @throws IllegalAccessException    if access is denied during reflection
     * @throws NoSuchMethodException     if a required method is absent
     * @throws NoSuchProviderException   if the requested provider is missing
     */
    public KeyStore load() throws CertificateException, IOException, NoSuchAlgorithmException,
            KeyStoreException, ClassNotFoundException, java.lang.reflect.InvocationTargetException,
            InstantiationException, IllegalAccessException, NoSuchMethodException,
            NoSuchProviderException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
        log.info("Using PKCS#12 KeyStore at {}", config.getPath().toAbsolutePath());
        if (Files.exists(config.getPath())) {
            log.info("KeyStore does exist, loading existing into memory");
            try (InputStream is = Files.newInputStream(config.getPath())) {
                keyStore.load(is, config.getPassword());
            }
        } else {
            log.info("KeyStore does not exist, creating new KeyStore");
            keyStore.load(null, config.getPassword());
        }
        return keyStore;
    }
}
