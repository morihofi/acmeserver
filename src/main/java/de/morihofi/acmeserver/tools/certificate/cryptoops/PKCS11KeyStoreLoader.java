/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.certificate.cryptoops;

import de.morihofi.acmeserver.tools.javaversion.JavaVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.AuthProvider;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.concurrent.CancellationException;

/**
 * Class for handling PKCS#11 compatible keystore logic
 */
public class PKCS11KeyStoreLoader {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Demo for loading a PKCS#11 HSM KeyStore
     *
     * @param args app args
     * @throws Exception if something goes wrong
     */
    public static void main(String[] args) throws Exception {
        String libraryLocation = "C:\\SoftHSM2\\lib\\softhsm2-x64.dll";
        int slot = 0;
        String pin = "1234";

        KeyStore keyStore = loadPKCS11Keystore(pin, slot, libraryLocation);

        System.out.println(keyStore.size());
        for (Iterator<String> it = keyStore.aliases().asIterator(); it.hasNext(); ) {
            String alias = it.next();

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, "".toCharArray());

            System.out.println(alias);
        }
    }

    /**
     * Loads a PKCS11 Keystore using the specified PIN, slot, and library location. This method involves retrieving a PKCS11 provider and
     * initializing a KeyStore instance with it. It sets up a callback handler to manage password input for the keystore.
     *
     * @param pin             The PIN code for accessing the keystore.
     * @param slot            The slot index of the PKCS11 provider.
     * @param libraryLocation The file path of the PKCS11 library.
     * @return An initialized KeyStore instance.
     * @throws CertificateException      if any certificate-related issues occur.
     * @throws IOException               if there is an I/O problem with keystore data.
     * @throws NoSuchAlgorithmException  if the algorithm for keystore integrity check is not available.
     * @throws ClassNotFoundException    if the PKCS11 provider class is not found.
     * @throws InvocationTargetException if an error occurs during method invocation.
     * @throws InstantiationException    if an error occurs during instantiation of the provider.
     * @throws IllegalAccessException    if there is illegal access during provider instantiation.
     * @throws NoSuchMethodException     if a required method is not found.
     * @throws KeyStoreException         if there is an issue with keystore operations.
     */
    public static KeyStore loadPKCS11Keystore(String pin, int slot, String libraryLocation) throws CertificateException, IOException,
            NoSuchAlgorithmException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, KeyStoreException {

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
                        throw new UnsupportedCallbackException(callback, "Callback not supported " + callback.getClass().getName());
                    }
                }
            }

            private void handlePasswordCallback(PasswordCallback passCb) {
                if (pin == null) {
                    throw new CancellationException("KeyStore Password is null");
                }

                passCb.setPassword(pin.toCharArray());
            }
        });

        keyStore.load(null, null);
        return keyStore;
    }

    /**
     * Retrieves a PKCS11 provider for the specified slot and library location. This method configures the provider differently based on the
     * JRE version. For JRE version 9 and above, it uses the built-in SunPKCS11 provider and configures it using reflection. For older
     * versions, it creates an instance of the SunPKCS11 provider using its constructor.
     *
     * @param slot            The slot index of the PKCS11 provider.
     * @param libraryLocation The file path of the PKCS11 library.
     * @return The configured PKCS11 provider.
     * @throws InvocationTargetException if an error occurs during method invocation.
     * @throws InstantiationException    if an error occurs during instantiation.
     * @throws IllegalAccessException    if an error occurs accessing the class or method.
     * @throws NoSuchMethodException     if a required method is not found.
     * @throws ClassNotFoundException    if the SunPKCS11 class is not found.
     */
    private static Provider getPkcs11Provider(int slot, String libraryLocation) throws InvocationTargetException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        String pkcs11ConfigSettings = "name = Slot" + slot + "\n" + "library = " + libraryLocation + "\n" + "slotListIndex = " + slot;
        ByteArrayInputStream confStream = new ByteArrayInputStream(pkcs11ConfigSettings.getBytes(StandardCharsets.UTF_8));

        // instantiate the provider
        Provider p11Provider;
        if (JavaVersion.getJreVersion().isAtLeast(JavaVersion.JRE_VERSION_9)) {
            p11Provider = Security.getProvider("SunPKCS11");
            // add marker ("--") for inline config
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
