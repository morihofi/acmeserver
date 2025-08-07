/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.keystore;

import de.morihofi.certgine.types.cryptography.keystore.PKCS11KeyStoreConfig;
import de.morihofi.certgine.utils.javaversion.JavaVersion;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.CancellationException;

/**
 * Loads {@link KeyStore} instances backed by a PKCS#11 provider.
 */
@Slf4j
public class Pkcs11KeyStoreLoader implements KeyStoreLoader {

    private final PKCS11KeyStoreConfig config;

    /**
     * Creates a loader for the given configuration.
     *
     * @param config PKCS#11 configuration
     */
    public Pkcs11KeyStoreLoader(@NonNull PKCS11KeyStoreConfig config) {
        this.config = config;
    }

    @Override
    public KeyStore load() throws CertificateException, IOException, NoSuchAlgorithmException,
            KeyStoreException, ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException, NoSuchMethodException,
            NoSuchProviderException {
        String libraryLocation = config.getLibraryPath().toAbsolutePath().toString();
        log.info("Using PKCS#11 KeyStore with native library at {} with slot {}", libraryLocation,
                config.getSlot());
        return loadPkcs11Keystore(config.getPin(), config.getSlot(), libraryLocation);
    }

    /**
     * Loads a PKCS#11 keystore using the specified parameters.
     *
     * @param pin             PIN used to unlock the HSM
     * @param slot            slot number
     * @param libraryLocation path to the native PKCS#11 library
     * @return initialized keystore
     */
    static KeyStore loadPkcs11Keystore(char[] pin, int slot,
                                       @NonNull String libraryLocation)
            throws CertificateException, IOException, NoSuchAlgorithmException,
            ClassNotFoundException, InvocationTargetException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, KeyStoreException {

        Provider selectedProvider = getPkcs11Provider(slot, libraryLocation);

        KeyStore keyStore = KeyStore.getInstance("PKCS11", selectedProvider);

        // register password handler
        AuthProvider authProvider = (AuthProvider) selectedProvider;
        authProvider.setCallbackHandler(new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof PasswordCallback passwordCallback) {
                        handlePasswordCallback(passwordCallback);
                    } else {
                        throw new UnsupportedCallbackException(callback,
                                "Callback not supported " + callback.getClass().getName());
                    }
                }
            }

            private void handlePasswordCallback(PasswordCallback passCb) {
                if (pin == null) {
                    throw new CancellationException("KeyStore Password is null");
                }
                passCb.setPassword(pin);
            }
        });

        keyStore.load(null, null);
        return keyStore;
    }

    /**
     * Retrieves a PKCS#11 provider for the specified slot and library location.
     */
    private static Provider getPkcs11Provider(int slot, @NonNull String libraryLocation)
            throws InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, ClassNotFoundException {
        String pkcs11ConfigSettings = "name = Slot" + slot + "\n" + "library = " + libraryLocation +
                "\n" + "slotListIndex = " + slot;
        ByteArrayInputStream confStream = new ByteArrayInputStream(
                pkcs11ConfigSettings.getBytes(StandardCharsets.UTF_8));

        Provider p11Provider;
        if (JavaVersion.getJreVersion().isAtLeast(JavaVersion.JRE_VERSION_9)) {
            p11Provider = Security.getProvider("SunPKCS11");
            pkcs11ConfigSettings = "--" + pkcs11ConfigSettings;
            Method method = Provider.class.getMethod("configure", String.class);
            p11Provider = (Provider) method.invoke(p11Provider, pkcs11ConfigSettings);
        } else {
            Class<?> cl = Class.forName("sun.security.pkcs11.SunPKCS11");
            Constructor<?> cons = cl.getConstructor(InputStream.class);
            p11Provider = (Provider) cons.newInstance(confStream);
        }

        Security.addProvider(p11Provider);
        return p11Provider;
    }
}
